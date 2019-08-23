package cn.thinkingdata.android.demo;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import cn.thinkingdata.android.ThinkingDataIgnoreTrackAppViewScreenAndAppClick;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;


@ThinkingDataIgnoreTrackAppViewScreenAndAppClick
public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        WebView myWebView = (WebView) findViewById(R.id.webview);

        TDTracker.getInstance().setJsBridge(myWebView);
        myWebView.loadUrl("file:///android_asset/hello.html");
    }

}
