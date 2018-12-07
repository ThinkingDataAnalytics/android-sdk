package com.thinking.analyselibrarysv;

import android.content.SharedPreferences;
import android.os.Build;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

abstract class SharedPreferencesStorageSV<T> {
    private T data;
    private final StroageUnit unit;
    private final String stroageKey;
    private final Future<SharedPreferences> loadStoredPreferences;

    interface StroageUnit<T> {
        T create();
        String save(T item);
        T get(final String value);
    }

    SharedPreferencesStorageSV(final Future<SharedPreferences> loadStoredPreferences, StroageUnit unit, final String
            stroageKey) {
        this.loadStoredPreferences = loadStoredPreferences;
        this.unit = unit;
        this.stroageKey = stroageKey;
    }

    public T get() {
        String data = null;
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
                data = sharedPreferences.getString(this.stroageKey, null);
                T t_data = null;
                if (data == null) {
                    t_data = (T) unit.create();
                    if (t_data != null) {
                        put(t_data);
                    }
                } else {
                    t_data = (T) unit.get(data);
                    return t_data;
                }
            }
        }
        return this.data;
    }

    void put(T item) {
        this.data = item;

        synchronized (loadStoredPreferences) {
            final SharedPreferences.Editor editor = getEditor();
            editor.putString(stroageKey, unit.save(this.data));

            if (Build.VERSION.SDK_INT >= 9) {
                editor.apply();
            } else {
                editor.commit();
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
        return sharedPreferences.edit();
    }

}
