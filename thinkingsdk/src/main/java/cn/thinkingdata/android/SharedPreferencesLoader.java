package cn.thinkingdata.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;

class SharedPreferencesLoader {

    private final Executor mExecutor;

    public SharedPreferencesLoader() {
        mExecutor = Executors.newSingleThreadExecutor();
    }

    public Future<SharedPreferences> loadPreferences(Context context, String name) {
        final LoadSharedPreferences loadSharedPrefs =
                new LoadSharedPreferences(context, name);
        final FutureTask<SharedPreferences> future = new FutureTask<SharedPreferences>(loadSharedPrefs);
        mExecutor.execute(future);
        return future;
    }

    private static class LoadSharedPreferences implements Callable<SharedPreferences> {
        private final Context mContext;
        private final String mPrefsName;

        public LoadSharedPreferences(Context context, String prefsName) {
            mContext = context;
            mPrefsName = prefsName;
        }

        @Override
        public SharedPreferences call() {
            final SharedPreferences ret = mContext.getSharedPreferences(mPrefsName, Context.MODE_PRIVATE);
            return ret;
        }
    }
}
