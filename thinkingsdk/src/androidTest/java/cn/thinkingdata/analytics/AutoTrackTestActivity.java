/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.app.Activity;
import android.os.Bundle;

/**
 * Minimal activity used to drive auto-track lifecycle events in instrumented tests.
 */
public class AutoTrackTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
