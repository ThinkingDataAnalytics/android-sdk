package cn.thinkingdata.android.demo.subprocess;

import android.content.Intent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubOtherActivity extends TDListActivity {
    public int num = 0;
    @Override
    public void setData() {
        super.setData();
        TDActionModel setSuperProperties = new TDActionModel("设置静态公共属性", new TDAction() {
            @Override
            public void doAction() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("sub_SUPER_A","A");
                    jsonObject.put("sub_SUPER_DATE",new Date());

                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
                TDTracker.getInstance().setSuperProperties(jsonObject);
            }
        });
        TDActionModel setDynamicProperties = new TDActionModel("设置动态公共属性", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().setDynamicSuperPropertiesTracker(new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
                    @Override
                    public JSONObject getDynamicSuperProperties() {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("SUPER_DY_num",num++);
                            jsonObject.put("SUPER_DY_date",new Date());
                        } catch (JSONException exception) {
                            exception.printStackTrace();
                        }
                        return jsonObject;
                    }
                });
            }
        });
        TDActionModel clearSuperProperties = new TDActionModel("清空静态公共属性", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().clearSuperProperties();
            }
        });
        TDActionModel delSuperProperties = new TDActionModel("清空部分公共预制属性", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().unsetSuperProperty("sub_SUPER_A");
            }
        });
        TDActionModel flushModel = new TDActionModel("Flush", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().flush();
            }
        });
        models.add(setSuperProperties);
        models.add(setDynamicProperties);
        models.add(clearSuperProperties);
        models.add(delSuperProperties);
        models.add(flushModel);
    }

    @Override
    public void setContentView() {
        super.setContentView();
        mNavigation.setTitleVisible(View.VISIBLE);
        mNavigation.setTitle("其他设置");
        mNavigation.mBackBtn.setVisibility(View.VISIBLE);
    }
}
