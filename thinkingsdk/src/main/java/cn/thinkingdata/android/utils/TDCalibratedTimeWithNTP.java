package cn.thinkingdata.android.utils;

import android.os.SystemClock;

import java.util.Date;


public class TDCalibratedTimeWithNTP implements ICalibratedTime {

    private final static String TAG = "ThinkingAnalytics.NTP";
    private final static int DEFAULT_TIME_OUT = 3000;

    private static String[] ntpServerHost = new String[]{
            "ntp.aliyun.com",
            "time.apple.com",
            "pool.ntp.org",
    };

    private long startTime;
    private long mSystemElapsedRealtime;

    private String[] ntpServer;

    private final Thread mThread = new Thread(new Runnable() {
        final TDNTPClient ntpClient = new TDNTPClient();
        @Override
        public void run() {
            for (String s : ntpServer) {
                if (ntpClient.requestTime(s, 3000)) {
                    TDLog.i(TAG, "NTP offset from " + s + " is: " + ntpClient.getOffset());
                    startTime = System.currentTimeMillis() + ntpClient.getOffset();
                    mSystemElapsedRealtime = SystemClock.elapsedRealtime();
                    break;
                }
            }

            if (mSystemElapsedRealtime == 0 ) {
                for (String s : ntpServerHost) {
                    TDLog.i(TAG, s);
                    if (ntpClient.requestTime(s, 3000)) {
                        TDLog.i(TAG, "NTP offset from " + s + " is: " + ntpClient.getOffset());
                        startTime = System.currentTimeMillis() + ntpClient.getOffset();
                        mSystemElapsedRealtime = SystemClock.elapsedRealtime();
                        break;
                    }
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

        return mSystemElapsedRealtime == 0 ? new Date() : new Date(elapsedRealtime - this.mSystemElapsedRealtime + startTime);
    }
}
