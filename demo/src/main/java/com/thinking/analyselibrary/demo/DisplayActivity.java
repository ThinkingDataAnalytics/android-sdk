package com.thinking.analyselibrary.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.thinking.analyselibrary.ScreenAutoTracker;

import org.json.JSONException;
import org.json.JSONObject;

public class DisplayActivity extends AppCompatActivity implements HomeFragment.OnFragmentInteractionListener, ScreenAutoTracker {

    private TextView mTextMessage;
    private String mMessage;
    FragmentManager mFragmentManager;
    HomeFragment mHomeFragment;
    NotificationsFragment mNotificationsFragment;
    DashboardFragment mDashboardFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home + ": " + mMessage);
                    if (null != mDashboardFragment){
                        fragmentTransaction.hide(mDashboardFragment);
                    }

                    if (null != mNotificationsFragment){
                        fragmentTransaction.hide(mNotificationsFragment);
                    }

                    if (null == mHomeFragment) {
                        mHomeFragment = new HomeFragment();
                        fragmentTransaction.add(R.id.content_view, mHomeFragment);
                    } else {
                        fragmentTransaction.show(mHomeFragment);
                    }
                    break;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard + ": " + mMessage);
                    if (null != mHomeFragment) {
                        fragmentTransaction.hide(mHomeFragment);
                    }

                    if (null != mNotificationsFragment) {
                        fragmentTransaction.hide(mNotificationsFragment);
                    }

                    if (null == mDashboardFragment) {
                        mDashboardFragment = new DashboardFragment();
                        fragmentTransaction.add(R.id.content_view, mDashboardFragment);
                    } else {
                        fragmentTransaction.show(mDashboardFragment);
                    }

                    break;

                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications + ": " + mMessage);
                    if (null != mHomeFragment) {
                        fragmentTransaction.hide(mHomeFragment);
                    }

                    if (null != mDashboardFragment) {
                        fragmentTransaction.hide(mDashboardFragment);
                    }

                    if (null == mNotificationsFragment) {
                        mNotificationsFragment = new NotificationsFragment();
                        fragmentTransaction.add(R.id.content_view, mNotificationsFragment);
                    } else {
                        fragmentTransaction.show(mNotificationsFragment);
                    }
                    break;
            }

            fragmentTransaction.commit();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        mMessage = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        mFragmentManager = getSupportFragmentManager(); //获取 fragment 管理器

        //默认显示HomeFragment
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction(); //获得 Fragment 事务处理器
        mHomeFragment = new HomeFragment(); //把主页 new 出来
        fragmentTransaction.replace(R.id.content_view, mHomeFragment); //加载fragment
        fragmentTransaction.commit();//提交加载操作
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public String getScreenUrl() {
        return "thinkingdatademo://main/display_activity";
    }

    @Override
    public JSONObject getTrackProperties() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("param1", "ABCD");
        jsonObject.put("param2", "thinkingdata");
        return jsonObject;
    }
}
