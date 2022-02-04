package cn.thinkingdata.android.demo.subprocess;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import cn.thinkingdata.android.demo.R;
import cn.thinkingdata.android.demo.subprocess.model.TDCallBack;


public abstract class TDBaseActivity extends Activity {
    public View view;
    public TDNavigation mNavigation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_td_base);
        mNavigation = findViewById(R.id.navigation);
        mNavigation.mCallback  = new TDCallBack() {
            @Override
            public void invoke() {
                finish();
            }
        };
        LinearLayout layout = findViewById(R.id.content);
        view = LayoutInflater.from(this).inflate(contentViewID(),layout);
        setData();
        setContentView();
    }
    abstract  public  void setContentView();
    abstract public int contentViewID();
    abstract public void setData();
}