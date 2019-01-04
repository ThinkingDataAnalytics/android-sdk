package com.thinking.analyselibrarysv;

import org.json.JSONException;
import org.json.JSONObject;

public interface ScreenAutoTrackerSV {
    String getScreenUrl();
    JSONObject getTrackProperties() throws JSONException;
}
