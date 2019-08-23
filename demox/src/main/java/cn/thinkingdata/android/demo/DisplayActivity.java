package cn.thinkingdata.android.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import cn.thinkingdata.android.ScreenAutoTracker;
import cn.thinkingdata.android.demo.fragment.ExpandableListFragment;
import cn.thinkingdata.android.demo.fragment.ListViewFragment;
import cn.thinkingdata.android.demo.fragment.RecyclerViewFragment;

import org.json.JSONObject;

public class DisplayActivity extends AppCompatActivity implements RecyclerViewFragment.OnFragmentInteractionListener, ScreenAutoTracker {

    private TextView mTextMessage;
    private String mMessage;
    FragmentManager mFragmentManager;
    RecyclerViewFragment mRecyclerViewFragment;
    ExpandableListFragment mExpandableListFragment;
    ListViewFragment mListViewFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

            switch (item.getItemId()) {
                case R.id.navigation_recycler:
                    //mTextMessage.setText(R.string.title_recycler + ": " + mMessage);
                    if (null != mListViewFragment){
                        fragmentTransaction.hide(mListViewFragment);
                    }

                    if (null != mExpandableListFragment){
                        fragmentTransaction.hide(mExpandableListFragment);
                    }

                    if (null == mRecyclerViewFragment) {
                        mRecyclerViewFragment = new RecyclerViewFragment();
                        fragmentTransaction.add(R.id.content_view, mRecyclerViewFragment);
                    } else {
                        fragmentTransaction.show(mRecyclerViewFragment);
                    }
                    break;
                case R.id.navigation_list:
                    //mTextMessage.setText(R.string.title_listview + ": " + mMessage);
                    if (null != mRecyclerViewFragment) {
                        fragmentTransaction.hide(mRecyclerViewFragment);
                    }

                    if (null != mExpandableListFragment) {
                        fragmentTransaction.hide(mExpandableListFragment);
                    }

                    if (null == mListViewFragment) {
                        mListViewFragment = new ListViewFragment();
                        fragmentTransaction.add(R.id.content_view, mListViewFragment);
                    } else {
                        fragmentTransaction.show(mListViewFragment);
                    }

                    break;

                case R.id.navigation_expandable:
                    //mTextMessage.setText(R.string.title_expandable + ": " + mMessage);
                    if (null != mRecyclerViewFragment) {
                        fragmentTransaction.hide(mRecyclerViewFragment);
                    }

                    if (null != mListViewFragment) {
                        fragmentTransaction.hide(mListViewFragment);
                    }

                    if (null == mExpandableListFragment) {
                        mExpandableListFragment = new ExpandableListFragment();
                        fragmentTransaction.add(R.id.content_view, mExpandableListFragment);
                    } else {
                        fragmentTransaction.show(mExpandableListFragment);
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

        //mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        //mMessage = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        mFragmentManager = getSupportFragmentManager(); //获取 fragment 管理器

        //默认显示HomeFragment
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction(); //获得 Fragment 事务处理器
        mRecyclerViewFragment = RecyclerViewFragment.newInstance(); //把主页 new 出来
        fragmentTransaction.replace(R.id.content_view, mRecyclerViewFragment); //加载fragment
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
    public JSONObject getTrackProperties() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("param1", "ABCD");
            jsonObject.put("param2", "thinkingdata");
            return jsonObject;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
