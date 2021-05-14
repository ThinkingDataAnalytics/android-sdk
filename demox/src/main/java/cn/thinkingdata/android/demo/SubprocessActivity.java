package cn.thinkingdata.android.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

public class SubprocessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subprocess);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("hh","SubprocessActivity onPause");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("hh","SubprocessActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        TDTracker.getInstance().track("YYYYY");
        Log.i("hh","SubprocessActivity onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("hh","SubprocessActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("hh","SubprocessActivity onDestroy");
    }
}