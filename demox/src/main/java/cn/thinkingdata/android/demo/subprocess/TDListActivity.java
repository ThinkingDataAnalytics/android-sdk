package cn.thinkingdata.android.demo.subprocess;


import android.opengl.Visibility;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;


import androidx.appcompat.widget.ViewUtils;

import java.util.ArrayList;
import cn.thinkingdata.android.demo.R;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;


public class TDListActivity extends TDBaseActivity {
    ListView mLV;
    ArrayList<TDActionModel> models = new ArrayList<>();
    BaseAdapter mAdapter;

    @Override
    public void setData() {

    }

    @Override
    public void setContentView() {
        mLV = view.findViewById(R.id.lv);
        mAdapter = new TDBaseAdapter(this, models);
        mLV.setAdapter(mAdapter);
        mNavigation.setTitleVisible(View.INVISIBLE);
        mNavigation.setBackVisible(View.INVISIBLE);
        mLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TDActionModel actionModel = models.get(i);
                actionModel.mAction.doAction();
            }
        });


    }


    @Override
    public int contentViewID() {
        return R.layout.activity_list;
    }

}