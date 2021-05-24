package cn.thinkingdata.android.demo.subprocess;

import android.content.Intent;
import android.view.View;

import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubprocessActivity extends TDListActivity {
    @Override
    public void setData() {
        TDActionModel userIDModel = new TDActionModel("设置用户ID", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubUserIDActivity.class));
            }
        });
        TDActionModel userPropertyModel = new TDActionModel("用户属性设置", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubUserPropertyActivity.class));
            }
        });

        TDActionModel eventModel = new TDActionModel("事件发送", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubEventActivity.class));
            }
        });
        TDActionModel otherModel = new TDActionModel("其他", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubOtherActivity.class));
            }
        });
        models.add(userIDModel);
        models.add(userPropertyModel);
        models.add(eventModel);
        models.add(otherModel);
    }

    @Override
    public void setContentView() {
        super.setContentView();
        mNavigation.setTitle("子进程测试");
        mNavigation.setTitleVisible(View.VISIBLE);
    }
}
