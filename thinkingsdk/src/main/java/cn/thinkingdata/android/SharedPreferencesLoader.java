package cn.thinkingdata.android;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

class SharedPreferencesLoader {

    private final Executor executor;

    public SharedPreferencesLoader() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Future<SharedPreferences> loadPreferences(Context context, String name) {
        Loader loadSharedPrefs = new Loader(context, name);
        FutureTask<SharedPreferences> future = new FutureTask<>(loadSharedPrefs);
        executor.execute(future);
        return future;
    }

    private static class Loader implements Callable<SharedPreferences> {
        private final Context context;
        private final String spName;

        public Loader(Context context, String name) {
            this.context = context;
            this.spName = name;
        }

        @Override
        public SharedPreferences call() {
            return context.getSharedPreferences(spName, Context.MODE_PRIVATE);
        }
    }
}
