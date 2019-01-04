package com.thinking.analyselibrary;

import org.json.JSONException;
import org.json.JSONObject;

public interface ScreenAutoTracker {
    String getScreenUrl();
    JSONObject getTrackProperties() throws JSONException;
}
