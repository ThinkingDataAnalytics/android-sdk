package cn.thinkingdata.android.demo.subprocess;

import android.content.Intent;
import android.view.View;

import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubUserIDActivity extends TDListActivity {
    @Override
    public void setData() {
        super.setData();
        TDActionModel identifyDistinctIdModel = new TDActionModel("设置访客ID", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().identify("sub_distinct");
            }
        });
        TDActionModel loginModel = new TDActionModel("设置账号ID", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().login("sub_account");
            }
        });

        TDActionModel logoutModel = new TDActionModel("清除账号ID", new TDAction() {
            @Override
            public void doAction() {
                TDTracker.getInstance().logout();
            }
        });

        models.add(identifyDistinctIdModel);
        models.add(loginModel);
        models.add(logoutModel);

    }

    @Override
    public void setContentView() {
        super.setContentView();
        mNavigation.setTitleVisible(View.VISIBLE);
        mNavigation.setTitle("设置用户ID");
        mNavigation.mBackBtn.setVisibility(View.VISIBLE);
    }
}
