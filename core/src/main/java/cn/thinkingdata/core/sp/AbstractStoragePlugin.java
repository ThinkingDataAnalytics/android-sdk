/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.sp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.Future;

/**
 * @author liulongbing
 * @since 2022/9/7
 */
public abstract class AbstractStoragePlugin implements StoragePlugin {

    public SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    public Future<SharedPreferences> storedSharedPrefs;

    public AbstractStoragePlugin(Context context, String name) {
        storedSharedPrefs = sPrefsLoader.loadPreferences(context, name);
        createStorage(context);
    }

    public abstract void createStorage(Context context);

    public abstract <T> SharedPreferencesStorage<T> getSharePreferenceStorage(int type);

    @Override
    public <T> void save(int type, T t) {
        SharedPreferencesStorage<T> sp = getSharePreferenceStorage(type);
        if (null != sp) {
            sp.put(t);
        }
    }

    @Deprecated
    @Override
    public <T> T get(int type) {
        SharedPreferencesStorage<T> sp = getSharePreferenceStorage(type);
        if (null != sp) {
            return sp.get();
        }
        return null;
    }

    @Override
    public <T> T get(int type, T t) {
        if (t == null) {
            throw new NullPointerException("Parameter t must not be null; please pass a non-null default value.");
        }
        SharedPreferencesStorage<T> sp = getSharePreferenceStorage(type);
        if (null != sp) {
            T spValue = sp.get();
            if (spValue != null) {
                return spValue;
            }
        }
        return t;
    }

}
