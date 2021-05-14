package cn.thinkingdata.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDUtils;


public class TDReceiver extends BroadcastReceiver {
    static  TDReceiver receiver;
    public static TDReceiver getInstance()
    {
        if(receiver == null)
        {
            receiver = new TDReceiver();
        }
        return receiver;
    }
    public static void registerReceiver(Context context)
    {
        IntentFilter filter = new IntentFilter();
        String mainProcessName = TDUtils.getMainProcessName(context);
        filter.addAction(mainProcessName);
        context.registerReceiver(getInstance(),filter);
    }
    public static void unregisterReceiver(Context context)
    {
        context.unregisterReceiver(getInstance());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra(TDConstants.TD_ACTION,0);
        Log.i("hh","收到子进程数据");
        switch (type)
        {
            case TDConstants.TD_ACTION_TRACK:

                break;
        }
    }
}