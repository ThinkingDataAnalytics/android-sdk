/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.Future;
import cn.thinkingdata.android.SharedPreferencesLoader;

/**
 * @author liulongbing
 * @create 2022/9/7
 * @since
 */
public abstract class AbstractStoragePlugin implements StoragePlugin {

    protected SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    protected Future<SharedPreferences> storedSharedPrefs;

    public AbstractStoragePlugin(Context context, String name) {
        storedSharedPrefs = sPrefsLoader.loadPreferences(context, name);
        createStorage();
    }

    protected abstract void createStorage();

    protected abstract <T> SharedPreferencesStorage<T> getSharePreferenceStorage(LocalStorageType type);

    @Override
    public <T> void save(LocalStorageType type, T t) {
        SharedPreferencesStorage<T> sp = getSharePreferenceStorage(type);
        if (null != sp) {
            sp.put(t);
        }
    }

    @Override
    public <T> T get(LocalStorageType type) {
        SharedPreferencesStorage<T> sp = getSharePreferenceStorage(type);
        if (null != sp) {
            return sp.get();
        }
        return null;
    }

}
