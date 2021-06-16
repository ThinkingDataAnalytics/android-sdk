package cn.thinkingdata.android.demo.subprocess;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cn.thinkingdata.android.demo.TDTracker;

public class TDSubService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        Log.i("hh","服务启动成功");
        JSONObject supperProperties = new JSONObject();
        try {
            supperProperties.put("service_date",new Date());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        TDTracker.getInstance().setSuperProperties(supperProperties);
        TDTracker.getInstance().track("TEST");
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       return  1;
    }


    @Override
    public void onDestroy() {

    }

}
