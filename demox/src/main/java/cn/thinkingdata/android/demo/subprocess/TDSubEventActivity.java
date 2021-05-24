package cn.thinkingdata.android.demo.subprocess;

import android.os.Handler;
import android.os.Message;
import android.view.View;


import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cn.thinkingdata.android.TDFirstEvent;
import cn.thinkingdata.android.TDOverWritableEvent;
import cn.thinkingdata.android.TDUpdatableEvent;
import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubEventActivity extends TDListActivity {
    Handler handler = new Handler();
    @Override
    public void setData() {
        super.setData();
        TDActionModel eventModel = new TDActionModel("普通事件", new TDAction() {
            @Override
            public void doAction() {
                JSONObject properties = new JSONObject();
                JSONArray  array = new JSONArray();
                try {
                    properties.put("sub_date",new Date());
                    array.put(new Date());
                    array.put(1);
                    array.put(true);
                    properties.put("sub_arr",array);
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDTracker.getInstance().track("sub_event",properties);
            }
        });
        TDActionModel timeEventModel = new TDActionModel("计时事件", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().timeEvent("sub_time_event");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject properties = new JSONObject();
                        JSONArray  array = new JSONArray();
                        try {
                            properties.put("sub_date",new Date());
                            array.put(new Date());
                            array.put(1);
                            array.put(true);
                            properties.put("sub_arr",array);
                        } catch (JSONException exception) {
                            exception.printStackTrace();
                        }
                        TDTracker.getInstance().track("sub_time_event",properties);
                    }
                },3 * 1000);

            }
        });

        TDActionModel firstModel = new TDActionModel("首次事件", new TDAction() {
            @Override
            public void doAction() {

                JSONObject properties = new JSONObject();
                JSONArray  array = new JSONArray();
                try {
                    properties.put("sub_date",new Date());
                    array.put(new Date());
                    array.put(1);
                    array.put(true);
                    properties.put("sub_arr",array);
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDFirstEvent firstEvent = new TDFirstEvent("sub_first",properties);
                TDTracker.getInstance().track(firstEvent);
            }
        });

        TDActionModel updateModel = new TDActionModel("可更新时间", new TDAction() {
            @Override
            public void doAction() {
                JSONObject properties = new JSONObject();
                JSONArray  array = new JSONArray();
                try {
                    properties.put("sub_date",new Date());
                    array.put(new Date());
                    array.put(1);
                    array.put(true);
                    properties.put("sub_arr",array);
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDUpdatableEvent updatableEvent = new TDUpdatableEvent("sub_updatable_event",properties,"sub_event_id");
                TDTracker.getInstance().track(updatableEvent);
            }
        });

        TDActionModel overwriteModel = new TDActionModel("可重写事件", new TDAction() {
            @Override
            public void doAction() {
                JSONObject properties = new JSONObject();
                JSONArray  array = new JSONArray();
                try {
                    properties.put("sub_date",new Date());
                    array.put(new Date());
                    array.put(1);
                    array.put(true);
                    properties.put("sub_arr",array);
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDOverWritableEvent overWritableEvent = new TDOverWritableEvent("sub_writable_event",properties,"sub_event_id");
                TDTracker.getInstance().track(overWritableEvent);
            }
        });

        models.add(eventModel);
        models.add(timeEventModel);
        models.add(firstModel);
        models.add(updateModel);
        models.add(overwriteModel);
    }

    @Override
    public void setContentView() {
        super.setContentView();
        mNavigation.setTitleVisible(View.VISIBLE);
        mNavigation.setTitle("发送事件");
        mNavigation.mBackBtn.setVisibility(View.VISIBLE);
    }
}
