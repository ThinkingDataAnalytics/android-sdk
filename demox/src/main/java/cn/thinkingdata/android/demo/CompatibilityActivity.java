package cn.thinkingdata.android.demo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.TDFirstEvent;
import cn.thinkingdata.android.TDOverWritableEvent;
import cn.thinkingdata.android.TDUpdatableEvent;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;

public class CompatibilityActivity extends Activity {

    public static final String TA_APP_ID = "e40482f8edbb4a6189ccdee5bd94fc96";
    public static final String TA_SERVER_URL = "https://receiver-ta-demo.thinkingdata.cn";

    private Context mContext;
    public static ThinkingAnalyticsSDK instance;
    public static TDConfig config;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_compatibility);
        initSDK();
        setAccountID();
        setDynamicProperties();
        normalTrack();
        trackWithTimeZone();
        trackFirstEvent();
        trackUpdatableEvent();
        trackWriteableEvent();
        trackTimeEvent();
        userSet();
        userUnSet();
        userSetOnce();
        userAdd();
    }

    private void initSDK() {
        config = TDConfig.getInstance(mContext, TA_APP_ID, TA_SERVER_URL);
        instance = ThinkingAnalyticsSDK.sharedInstance(config);
        ThinkingAnalyticsSDK.enableTrackLog(true);
        setDistinctID();
        List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        instance.enableAutoTrack(typeList);
    }

    private void setDistinctID() {
        instance.identify("distinctId");
        instance.track("eventName");
    }

    private void setAccountID() {
        instance.login("accountId");
        instance.track("eventName");
    }

    private void setDynamicProperties() {
        instance.setDynamicSuperPropertiesTracker(() -> {
            JSONObject properties = new JSONObject();
            try {
                properties.put("dyldkey1", "dyldvalue1");
                properties.put("dyldkey2", "dyldvalue2");
                properties.put("dyldkey3", "dyldvalue3");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return properties;
        });
        instance.track("eventName");
    }

    private void normalTrack() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("key1", "value1");
            properties.put("key2", "value2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        instance.track("event_track_property");
    }

    private void trackWithTimeZone() {
        config.setDefaultTimeZone(TimeZone.getTimeZone("GMT+07:00"));
        instance.track("event_track_time", null, new Date());
    }

    private void trackFirstEvent() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("key1", "value1");
            properties.put("key2", "value2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TDFirstEvent firstEvent = new TDFirstEvent("event_first", properties);
        firstEvent.setFirstCheckId("firstCheckID");
        instance.track(firstEvent);
    }

    private void trackUpdatableEvent() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("key1", "value1");
            properties.put("key2", "value2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TDUpdatableEvent updatableEvent = new TDUpdatableEvent("event_update", properties, "event_update_eventid");
        instance.track(updatableEvent);
    }

    private void trackWriteableEvent() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("key1", "value1");
            properties.put("key2", "value2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TDOverWritableEvent overWritableEvent = new TDOverWritableEvent("event_overwrite", properties, "event_overwrite_eventid");
        instance.track(overWritableEvent);
    }

    private void trackTimeEvent() {
        instance.timeEvent("event_time");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        instance.track("event_time");
    }

    private void userSet() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("userkey1", "uservalue1");
            properties.put("userkey2", "uservalue2");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        instance.user_set(properties);
    }

    private void userUnSet() {
        instance.user_unset("userkey2");
    }

    private void userSetOnce() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("onceEventKey1", "onceEventValue1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        instance.user_setOnce(properties);
    }

    private void userAdd() {
        instance.user_add("useradd1", 1);
    }


}
