package cn.thinkingdata.android.demo.subprocess;

import android.content.Intent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubUserPropertyActivity extends TDListActivity {
    @Override
    public void setData() {
        super.setData();
        TDActionModel userSetModel = new TDActionModel("UserSet", new TDAction() {
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
                TDTracker.getInstance().user_set(properties);
            }
        });
        TDActionModel userSetOnceModel = new TDActionModel("UserSetOnce", new TDAction() {
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
                TDTracker.getInstance().user_setOnce(properties);

            }
        });
        TDActionModel userAddModel = new TDActionModel("UserAdd", new TDAction() {
            @Override
            public void doAction() {
                JSONObject properties = new JSONObject();
                try {
                    properties.put("sub_pro_int",1);
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDTracker.getInstance().user_add(properties);
            }
        });
        TDActionModel userUnsetModel = new TDActionModel("UserUnset", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().user_unset("sub_pro_int");
            }
        });
        TDActionModel userDeleteModel = new TDActionModel("UserDelete", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().user_delete();
            }
        });
        TDActionModel userAppendModel = new TDActionModel("UserAppend", new TDAction() {
            @Override
            public void doAction() {
                JSONObject properties = new JSONObject();
                JSONArray arr = new JSONArray();
                try {
                    arr.put("XXX");
                    arr.put(new Date());
                    properties.put("sub_list",arr);
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDTracker.getInstance().user_append(properties);
            }
        });
        models.add(userSetModel);
        models.add(userSetOnceModel);
        models.add(userAddModel);
        models.add(userUnsetModel);
        models.add(userDeleteModel);
        models.add(userAppendModel);
    }

    @Override
    public void setContentView() {
        super.setContentView();
        mNavigation.setTitleVisible(View.VISIBLE);
        mNavigation.setTitle("用户属性设置");
        mNavigation.mBackBtn.setVisibility(View.VISIBLE);
    }
}
