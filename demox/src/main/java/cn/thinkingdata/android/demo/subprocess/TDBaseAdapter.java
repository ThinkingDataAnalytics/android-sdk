package cn.thinkingdata.android.demo.subprocess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import java.util.ArrayList;

import cn.thinkingdata.android.demo.R;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDBaseAdapter extends BaseAdapter {
    Context context;
    ArrayList<TDActionModel> actionModels = new ArrayList<>();
    public TDBaseAdapter(Context context, ArrayList<TDActionModel> models)
    {
        this.context = context;
        this.actionModels = models;
    }
    @Override
    public int getCount() {
        return actionModels.size();
    }

    @Override
    public Object getItem(int i) {
        return actionModels.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if(view == null)
        {
            view  = LayoutInflater.from(context).inflate(R.layout.td_action_cell,null);
            viewHolder = new ViewHolder();
            viewHolder.tv = view.findViewById(R.id.tv_title);
            view.setTag(viewHolder);
        }else
        {
            viewHolder = (ViewHolder) view.getTag();
        }
        TDActionModel mode = actionModels.get(i);
        viewHolder.tv.setText(mode.mName);
        return view;
    }
    class ViewHolder
    {
        public  TextView tv;
    }
}
