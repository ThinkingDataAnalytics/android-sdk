package cn.thinkingdata.android.demo.subprocess;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;


import cn.thinkingdata.android.demo.R;
import cn.thinkingdata.android.demo.subprocess.model.TDCallBack;

public class TDNavigation extends RelativeLayout {
    public TextView mTitleTV;
    public ImageButton mBackBtn;
    public TDCallBack mCallback;
    public TDNavigation(Context context) {
        super(context);

    }

    public TDNavigation(final Context context, AttributeSet attrs) {
        super(context, attrs);
        View view = inflate(context, R.layout.td_navigation,this);
        mTitleTV = view.findViewById(R.id.tv_title);
        mBackBtn = view.findViewById(R.id.btn_back);
        mBackBtn.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view) {
                if(mCallback != null)
                {
                    mCallback.invoke();
                }
            }
        });
    }

    public void setTitleVisible(int visible)
    {
        mTitleTV.setVisibility(visible);
    }
    public void setBackVisible(int visible)
    {
        mBackBtn.setVisibility(visible);
    }
    public void setTitle(String title)
    {
        mTitleTV.setText(title);
    }

    public TDNavigation(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

}
