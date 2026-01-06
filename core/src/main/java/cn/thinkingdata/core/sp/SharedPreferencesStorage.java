/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.core.sp;

import android.content.SharedPreferences;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class SharedPreferencesStorage<T> {

    public T data;
    public final String storageKey;
    private final Future<SharedPreferences> loadStoredPreferences;

    public SharedPreferencesStorage(final Future<SharedPreferences> loadStoredPreferences, final String storageKey) {
        this.loadStoredPreferences = loadStoredPreferences;
        this.storageKey = storageKey;
    }

    // return default value.
    public T create() {
        return null;
    }

    // save the data to sharedPreference. If the type of T is not String, override this method.
    public void save(SharedPreferences.Editor editor, T data) {
        if (data == null) return;
        if (!TDStorageEncryptPlugin.getInstance().isEnableEncrypt()) {
            saveOldData(editor, data);
            editor.apply();
            return;
        }
        String encryptData = TDStorageEncryptPlugin.getInstance().encryptData(data.toString());
        if (encryptData == null) {
            editor.putString(storageKey, data.toString());
        } else {
            editor.putString(storageKey, encryptData);
        }
        editor.apply();
    }

    // load the data from sharedPreference. If the type of T is not String, override this method.
    public void load(SharedPreferences sharedPreferences) {
        try {
            if (!TDStorageEncryptPlugin.getInstance().isEnableEncrypt()) {
                loadOldData(sharedPreferences);
                return;
            }
            String sData = sharedPreferences.getString(this.storageKey, null);
            if (sData == null) {
                loadOldData(sharedPreferences);
            } else {
                String decryptData = TDStorageEncryptPlugin.getInstance().decryptData(sData);
                if (null == decryptData) {
                    convertEncryptData(sData);
                } else {
                    convertEncryptData(decryptData);
                }
            }
        } catch (Exception e) {
            loadOldData(sharedPreferences);
        }
    }

    public void saveOldData(SharedPreferences.Editor editor, T data) {

    }

    public void loadOldData(SharedPreferences sharedPreferences) {
        //加载老数据
    }

    public void convertEncryptData(String convertData) {
        //转换加密数据
    }


    /**
     * Gets the value stored in SharedPreference.
     *
     * @return value of the storageKey.
     */
    public T get() {
        if (this.data == null) {
            synchronized (loadStoredPreferences) {
                SharedPreferences sharedPreferences = null;
                try {
                    sharedPreferences = loadStoredPreferences.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if (sharedPreferences != null) {
                    load(sharedPreferences);
                }
            }
        }
        return this.data;
    }

    /**
     * Set the value of the storage key and save it to sharedPreference.
     *
     * @param data value.
     */
    public void put(T data) {
        this.data = data;

        synchronized (loadStoredPreferences) {
            final SharedPreferences.Editor editor = getEditor();
            if (editor != null) {
                save(editor, this.data);
            }
        }
    }

    private SharedPreferences.Editor getEditor() {
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = loadStoredPreferences.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if (sharedPreferences != null) {
            return sharedPreferences.edit();
        } else {
            return null;
        }
    }

}
