/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.utils.broadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import cn.thinkingdata.analytics.TDFirstEvent;
import cn.thinkingdata.analytics.TDOverWritableEvent;
import cn.thinkingdata.analytics.TDUpdatableEvent;
import cn.thinkingdata.analytics.ThinkingAnalyticsEvent;
import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.utils.CommonUtil;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDUtils;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Broadcast reception, processing process communication.
 * */
public class TDReceiver extends BroadcastReceiver {
    private static  volatile TDReceiver receiver;

    /**
     * getInstance
     *
     * @return {@link TDReceiver}
     */
    public static synchronized  TDReceiver getInstance() {
        if (receiver == null) {
            synchronized (TDReceiver.class) {
                if (null == receiver) {
                    receiver = new TDReceiver();
                }
            }
        }
        return receiver;
    }

    /**
     * registerReceiver
     *
     * @param context context
     */
    @SuppressLint("WrongConstant")
    public static void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        String mainProcessName = CommonUtil.getMainProcessName(context);
        if (mainProcessName.length() == 0) {
            mainProcessName = TDConstants.TD_RECEIVER_FILTER;
        } else {
            mainProcessName = mainProcessName + "." + TDConstants.TD_RECEIVER_FILTER;
        }
        filter.addAction(mainProcessName);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(getInstance(), filter, 0x00000002);
        } else {
            context.registerReceiver(getInstance(), filter);
        }
    }

    public static void unregisterReceiver(Context context) {
        context.unregisterReceiver(getInstance());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra(TDConstants.TD_ACTION, 0);
        String appid = intent.getStringExtra(TDConstants.KEY_APP_ID);
        String propertiesString = null;
        long timestamp = 0;
        JSONObject properties = null;
        Date date = null;
        if (appid != null && appid.length() > 0) {
            ThinkingAnalyticsSDK instance = ThinkingAnalyticsSDK.sharedInstance(context, appid);
            if (instance != null) {
                switch (type) {
                    case TDConstants.TD_ACTION_TRACK: {
                        propertiesString = intent.getStringExtra(TDConstants.KEY_PROPERTIES);
                        timestamp = intent.getLongExtra(TDConstants.TD_KEY_DATE, 0);
                        String timezoneID = intent.getStringExtra(TDConstants.TD_KEY_TIMEZONE);
                        if (propertiesString != null) {
                            try {
                                properties = new JSONObject(propertiesString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (timestamp != 0) {
                            date = new Date(timestamp);
                        }
                        TimeZone timeZone = instance.mConfig.getDefaultTimeZone();
                        if (timezoneID != null) {
                            timeZone = TimeZone.getTimeZone(timezoneID);
                        }
                        String eventName = intent.getStringExtra(TDConstants.KEY_EVENT_NAME);
                        if(null == date){
                            instance.track(eventName,properties);
                        }else {
                            instance.track(eventName, properties, date, timeZone);
                        }
                    }
                    break;
                    case TDConstants.TD_ACTION_USER_PROPERTY_SET: {

                        propertiesString = intent.getStringExtra(TDConstants.KEY_PROPERTIES);
                        timestamp = intent.getLongExtra(TDConstants.TD_KEY_DATE, 0);
                        if (propertiesString != null) {
                            try {
                                properties = new JSONObject(propertiesString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (timestamp != 0) {
                            date = new Date(timestamp);
                        }
                        String dataType = intent.getStringExtra(TDConstants.TD_KEY_USER_PROPERTY_SET_TYPE);
                        instance.user_operations(TDConstants.DataType.get(dataType), properties, date);
                    }
                    break;
                    case TDConstants.TD_ACTION_SET_SUPER_PROPERTIES: {
                        propertiesString = intent.getStringExtra(TDConstants.KEY_PROPERTIES);
                        if (propertiesString != null) {
                            try {
                                properties = new JSONObject(propertiesString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        instance.setSuperProperties(properties);
                    }
                    break;
                    case TDConstants.TD_ACTION_FLUSH: {
                        instance.flush();
                    }
                    break;
                    case TDConstants.TD_ACTION_LOGIN: {
                        String accountID = intent.getStringExtra(TDConstants.KEY_ACCOUNT_ID);
                        instance.login(accountID);
                    }
                    break;
                    case TDConstants.TD_ACTION_LOGOUT: {
                        instance.logout();
                    }
                    break;
                    case TDConstants.TD_ACTION_IDENTIFY: {
                        String distinctID = intent.getStringExtra(TDConstants.KEY_DISTINCT_ID);
                        instance.identify(distinctID);
                    }
                    break;
                    case TDConstants.TD_ACTION_TRACK_UPDATABLE_EVENT:
                    case TDConstants.TD_ACTION_TRACK_OVERWRITE_EVENT:
                    case TDConstants.TD_ACTION_TRACK_FIRST_EVENT: {
                        String eventName = intent.getStringExtra(TDConstants.KEY_EVENT_NAME);
                        propertiesString = intent.getStringExtra(TDConstants.KEY_PROPERTIES);
                        timestamp = intent.getLongExtra(TDConstants.TD_KEY_DATE, 0);
                        String timezoneID = intent.getStringExtra(TDConstants.TD_KEY_TIMEZONE);
                        if (propertiesString != null) {
                            try {
                                properties = new JSONObject(propertiesString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (timestamp != 0) {
                            date = new Date(timestamp);
                        }
                        TimeZone timeZone = null;
                        if (timezoneID != null) {
                            timeZone = TimeZone.getTimeZone(timezoneID);
                        }
                        String extraString = intent.getStringExtra(TDConstants.TD_KEY_EXTRA_FIELD);
                        ThinkingAnalyticsEvent event = null;
                        if (type == TDConstants.TD_ACTION_TRACK_FIRST_EVENT) {
                            event = new TDFirstEvent(eventName, properties);
                            if (extraString != null && extraString.length() > 0) {
                                (( TDFirstEvent ) event).setFirstCheckId(extraString);
                            }
                        } else if (type == TDConstants.TD_ACTION_TRACK_OVERWRITE_EVENT) {
                            event = new TDOverWritableEvent(eventName, properties, extraString);
                        } else if (type == TDConstants.TD_ACTION_TRACK_UPDATABLE_EVENT) {
                            event = new TDUpdatableEvent(eventName, properties, extraString);
                        }
                        if (event != null) {
                            event.setEventTime(date, timeZone);
                            instance.track(event);
                        }
                    }
                        break;
                    case TDConstants.TD_ACTION_TRACK_AUTO_EVENT: {
                        propertiesString = intent.getStringExtra(TDConstants.KEY_PROPERTIES);
                        if (propertiesString != null) {
                            try {
                                properties = new JSONObject(propertiesString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        String eventName = intent.getStringExtra(TDConstants.KEY_EVENT_NAME);
                        instance.autoTrack(eventName, properties);
                    }
                    break;
                    case TDConstants.TD_ACTION_CLEAR_SUPER_PROPERTIES: {
                        instance.clearSuperProperties();
                    }
                    break;
                    case TDConstants.TD_ACTION_UNSET_SUPER_PROPERTIES: {
                        propertiesString = intent.getStringExtra(TDConstants.KEY_PROPERTIES);
                        instance.unsetSuperProperty(propertiesString);
                    }
                    break;
                    default:
                        break;
                }
            }

        }

    }

}