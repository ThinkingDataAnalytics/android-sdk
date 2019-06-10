package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

public class TDViewOnClickAppClick {
    private final static String TAG = "TDViewOnClickAppClick";

    public static void onAppClick(final JoinPoint joinPoint) {
        try {
            if (joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                @Override
                public void process(ThinkingAnalyticsSDK instance) {
                    try {
                        if (!instance.isAutoTrackEnabled()) {
                            return;
                        }

                        if (instance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                            return;
                        }

                        View view = (View) joinPoint.getArgs()[0];
                        if (view == null) {
                            return;
                        }

                        long currentOnClickTimestamp = System.currentTimeMillis();
                        String tag = (String) AopUtil.getTag(instance.getToken(), view, R.id.thinking_analytics_tag_view_onclick_timestamp);
                        if (!TextUtils.isEmpty(tag)) {
                            try {
                                long lastOnClickTimestamp = Long.parseLong(tag);
                                if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                                    TDLog.i(TAG, "This onClick maybe extends from super, IGNORE");
                                    return;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        AopUtil.setTag(instance.getToken(), view, R.id.thinking_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

                        Context context = view.getContext();
                        if (context == null) {
//                        return;
                        }

                        Activity activity = AopUtil.getActivityFromContext(context);
                        if (activity != null) {
                            if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                                return;
                            }
                        }

                        if (AopUtil.isViewIgnored(instance, view)) {
                            return;
                        }

                        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                        if (methodSignature == null) {
                            return;
                        } else {
                            Method method = methodSignature.getMethod();
                            if (method != null) {
                                ThinkingDataIgnoreTrackOnClick trackEvent = method.getAnnotation(ThinkingDataIgnoreTrackOnClick.class);
                                if (trackEvent != null && (TextUtils.isEmpty(trackEvent.appId()) || instance.getToken().equals(trackEvent.appId()))) {
                                    return;
                                }

                                ThinkingDataTrackViewOnClick trackViewOnClick = method.getAnnotation(ThinkingDataTrackViewOnClick.class);
                                if (null != trackViewOnClick && !(TextUtils.isEmpty(trackViewOnClick.appId())
                                        || instance.getToken().equals(trackViewOnClick.appId()))) {
                                    return;
                                }
                            }
                        }


                        JSONObject properties = new JSONObject();

                        AopUtil.addViewPathProperties(activity, view, properties);

                        String idString = AopUtil.getViewId(view, instance.getToken());
                        if (!TextUtils.isEmpty(idString)) {
                            properties.put(AopConstants.ELEMENT_ID, idString);
                        }

                        if (activity != null) {
                            properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                            String activityTitle = AopUtil.getActivityTitle(activity);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                properties.put(AopConstants.TITLE, activityTitle);
                            }
                        }


                        Class<?> switchCompatClass = null;
                        try {
                            switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
                        } catch (Exception e) {
                            //ignored
                        }
                        if (switchCompatClass == null) {
                            try {
                                switchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
                            } catch (Exception e) {
                                //ignored
                            }
                        }

                        Class<?> viewPagerClass = null;
                        try {
                            viewPagerClass = Class.forName("android.support.v4.view.ViewPager");
                        } catch (Exception e) {
                            //ignored
                        }
                        if (null == viewPagerClass) {
                            try {
                                viewPagerClass = Class.forName("androidx.viewpager.widget.ViewPager");
                            } catch (Exception e) {
                                //ignored
                            }
                        }

                        String viewType = view.getClass().getCanonicalName();
                        CharSequence viewText = null;
                        if (view instanceof CheckBox) {
                            viewType = "CheckBox";
                            CheckBox checkBox = (CheckBox) view;
                            viewText = checkBox.getText();
                        } else if (switchCompatClass != null && switchCompatClass.isInstance(view)) {
                            viewType = "SwitchCompat";
                            CompoundButton switchCompat = (CompoundButton) view;

                            Method getTextMethod;
                            if (switchCompat.isChecked()) {
                                getTextMethod = view.getClass().getMethod("getTextOn");
                            } else {
                                getTextMethod = view.getClass().getMethod("getTextOff");
                            }
                            viewText = (String) getTextMethod.invoke(view);
                        } else if (viewPagerClass != null && viewPagerClass.isInstance(view)) {
                            viewType = "ViewPager";
                            try {
                                Method getAdapterMethod = view.getClass().getMethod("getAdapter");
                                if (getAdapterMethod != null) {
                                    Object viewPagerAdapter = getAdapterMethod.invoke(view);
                                    Method getCurrentItemMethod = view.getClass().getMethod("getCurrentItem");
                                    if (getCurrentItemMethod != null) {
                                        int currentItem = (int) getCurrentItemMethod.invoke(view);
                                        properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", currentItem));
                                        Method getPageTitleMethod = viewPagerAdapter.getClass().getMethod("getPageTitle", new Class[]{int.class});
                                        if (getPageTitleMethod != null) {
                                            viewText = (String) getPageTitleMethod.invoke(viewPagerAdapter, new Object[]{currentItem});
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (view instanceof RadioButton) {
                            viewType = "RadioButton";
                            RadioButton radioButton = (RadioButton) view;
                            viewText = radioButton.getText();
                        } else if (view instanceof ToggleButton) {
                            viewType = "ToggleButton";
                            ToggleButton toggleButton = (ToggleButton) view;
                            boolean isChecked = toggleButton.isChecked();
                            if (isChecked) {
                                viewText = toggleButton.getTextOn();
                            } else {
                                viewText = toggleButton.getTextOff();
                            }
                        } else if (view instanceof Button) {
                            viewType = "Button";
                            Button button = (Button) view;
                            viewText = button.getText();
                        } else if (view instanceof CheckedTextView) {
                            viewType = "CheckedTextView";
                            CheckedTextView textView = (CheckedTextView) view;
                            viewText = textView.getText();
                        } else if (view instanceof TextView) {
                            viewType = "TextView";
                            TextView textView = (TextView) view;
                            viewText = textView.getText();
                        } else if (view instanceof ImageButton) {
                            viewType = "ImageButton";
                            ImageButton imageButton = (ImageButton) view;
                            if (!TextUtils.isEmpty(imageButton.getContentDescription())) {
                                viewText = imageButton.getContentDescription().toString();
                            }
                        } else if (view instanceof ImageView) {
                            viewType = "ImageView";
                            ImageView imageView = (ImageView) view;
                            if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                                viewText = imageView.getContentDescription().toString();
                            }
                        } else if (view instanceof RatingBar) {
                            viewType = "RatingBar";
                            RatingBar ratingBar = (RatingBar) view;
                            viewText = String.valueOf(ratingBar.getRating());
                        } else if (view instanceof SeekBar) {
                            viewType = "SeekBar";
                            SeekBar seekBar = (SeekBar) view;
                            viewText = String.valueOf(seekBar.getProgress());
                        } else if (view instanceof Spinner) {
                            viewType = "Spinner";
                            try {
                                StringBuilder stringBuilder = new StringBuilder();
                                viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                                if (!TextUtils.isEmpty(viewText)) {
                                    viewText = viewText.toString().substring(0, viewText.length() - 1);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (view instanceof ViewGroup) {
                            try {
                                StringBuilder stringBuilder = new StringBuilder();
                                viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                                if (!TextUtils.isEmpty(viewText)) {
                                    viewText = viewText.toString().substring(0, viewText.length() - 1);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (!TextUtils.isEmpty(viewText)) {
                            properties.put(AopConstants.ELEMENT_CONTENT, viewText.toString());
                        }

                        properties.put(AopConstants.ELEMENT_TYPE, viewType);
                        AopUtil.getFragmentNameFromView(view, properties);

                        JSONObject p = (JSONObject) AopUtil.getTag(instance.getToken(), view,
                                R.id.thinking_analytics_tag_view_properties);
                        if (p != null) {
                            TDUtil.mergeJSONObject(p, properties);
                        }

                        instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                    } catch (Exception e) {
                        e.printStackTrace();
                        TDLog.i(TAG, "onViewClickMethod error: " + e.getMessage());
                    }

                }
            });


        } catch (Exception e) {
        }
    }
}

