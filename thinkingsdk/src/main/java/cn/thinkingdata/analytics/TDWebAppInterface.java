/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.thinkingdata.analytics.data.DataDescription;
import cn.thinkingdata.analytics.tasks.TrackTaskManager;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDTimeConstant;
import cn.thinkingdata.core.preset.TDPresetUtils;
import cn.thinkingdata.core.utils.TDLog;

/**
 * Web interface class.
 */
public class TDWebAppInterface {
    private static final String TAG = "ThinkingAnalytics.TDWebAppInterface";

    // if no exist instance has the same token with H5 data, the data will be tracked to default instance.
    private final ThinkingAnalyticsSDK defaultInstance;

    private Map<String, Object> deviceInfoMap;

    // for internal use to identify whether the data has been tracked.
    private class TrackFlag {
        private boolean tracked;

        void tracked() {
            tracked = true;
        }

        boolean shouldTrack() {
            return !tracked;
        }
    }

    TDWebAppInterface(ThinkingAnalyticsSDK instance, Map<String, Object> deviceInfoMap) {
        defaultInstance = instance;
        this.deviceInfoMap = deviceInfoMap;
    }

    /**
     * call native
     *
     * @param event Event
     */
    @JavascriptInterface
    public void thinkingdata_track(final String event) {
        if (TextUtils.isEmpty(event)) {
            return;
        }

        TDLog.d(TAG, event);

        try {
            JSONObject eventData = new JSONObject(event);
            final String token = eventData.getString("#app_id");
            final TrackFlag flag = new TrackFlag();

            ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                @Override
                public void process(ThinkingAnalyticsSDK instance) {
                    if (instance.getToken().equals(token)) {
                        flag.tracked();
                        //instance.trackFromH5(event);
                        trackFromH5(event, instance);
                    }
                }
            });

            // if the H5 data could is not match with any instance, track trough default instance
            if (flag.shouldTrack()) {
                //defaultInstance.trackFromH5(event);
                trackFromH5(event, defaultInstance);
            }
        } catch (JSONException e) {
            TDLog.w(TAG, "Unexpected exception occurred: " + e.toString());
        }

    }

    private void trackFromH5(final String event, final ThinkingAnalyticsSDK instance) {

        if (TextUtils.isEmpty(event)) {
            return;
        }

        try {
            JSONArray data = new JSONObject(event).getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject eventObject = data.getJSONObject(i);

                String timeString = eventObject.getString(TDConstants.KEY_TIME);

                Double zoneOffset = null;
                if (eventObject.has(TDConstants.KEY_ZONE_OFFSET)) {
                    zoneOffset = eventObject.getDouble(TDConstants.KEY_ZONE_OFFSET);
                }

                final ITime time = new TDTimeConstant(timeString, zoneOffset);

                String eventType = eventObject.getString(TDConstants.KEY_TYPE);

                final TDConstants.DataType type = TDConstants.DataType.get(eventType);
                if (null == type) {
                    TDLog.w(TAG, "Unknown data type from H5. ignoring...");
                    return;
                }

                final JSONObject properties = eventObject.getJSONObject(TDConstants.KEY_PROPERTIES);

                for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                    String key = ( String ) iterator.next();
                    if (key.equals(TDConstants.KEY_ACCOUNT_ID) || key.equals(TDConstants.KEY_DISTINCT_ID) || key.equals(TDPresetUtils.KEY_DEVICE_ID) || deviceInfoMap.containsKey(key)) {
                        iterator.remove();
                    }
                }

                if (type.isTrack()) {
                    String eventName = eventObject.getString(TDConstants.KEY_EVENT_NAME);

                    Map<String, String> extraFields = new HashMap<>();
                    if (eventObject.has(TDConstants.KEY_FIRST_CHECK_ID)) {
                        extraFields.put(TDConstants.KEY_FIRST_CHECK_ID, eventObject.getString(TDConstants.KEY_FIRST_CHECK_ID));
                    }
                    if (eventObject.has(TDConstants.KEY_EVENT_ID)) {
                        extraFields.put(TDConstants.KEY_EVENT_ID, eventObject.getString(TDConstants.KEY_EVENT_ID));
                    }

                    instance.track(eventName, properties, time, false, extraFields, type, 0);
                } else {
                    // 用户属性
                    final String accountId = instance.getLoginId();
                    final String distinctId = instance.getDistinctId();
                    final boolean isSaveOnly = instance.isStatusTrackSaveOnly();

                    TrackTaskManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            DataDescription dataDescription;
                            dataDescription = new DataDescription(instance, type, properties, time, distinctId, accountId, isSaveOnly);
                            instance.trackInternal(dataDescription);
                        }
                    });
                }
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Exception occurred when track data from H5.");
            e.printStackTrace();
        }
    }
}
