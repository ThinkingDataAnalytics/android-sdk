package cn.thinkingdata.android.utils;

import android.os.SystemClock;

import java.util.Date;


public class TDCalibratedTimeWithNTP implements ICalibratedTime {

    private final static String TAG = "ThinkingAnalytics.NTP";
    private final static int DEFAULT_TIME_OUT = 3000;

    private long startTime;
    private long mSystemElapsedRealtime;

    private String[] ntpServer;

    private final Thread mThread = new Thread(new Runnable() {
        final TDNTPClient ntpClient = new TDNTPClient();
        @Override
        public void run() {
            for (String s : ntpServer) {
                if (ntpClient.requestTime(s, DEFAULT_TIME_OUT)) {
                    TDLog.i(TAG, "NTP offset from " + s + " is: " + ntpClient.getOffset());
                    startTime = System.currentTimeMillis() + ntpClient.getOffset();
                    mSystemElapsedRealtime = SystemClock.elapsedRealtime();
                    break;
                }
            }
        }
    });

    public TDCalibratedTimeWithNTP(final String... ntpServer) {
        this.ntpServer = ntpServer;
        mThread.start();
    }

    @Override
    public Date get(long elapsedRealtime) {
        try {
            mThread.join(DEFAULT_TIME_OUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mSystemElapsedRealtime == 0 ? new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime() + elapsedRealtime)
                : new Date(elapsedRealtime - this.mSystemElapsedRealtime + startTime);
    }
}
