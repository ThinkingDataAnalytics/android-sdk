/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Adds page URL information for page browsing events, as well as other custom properties.
 * The ta_app_view event for the Activity that implements this interface will carry url information and custom properties
 */
public interface ScreenAutoTracker {

    /**
     * Returns the value as the Url information for the current page
     * Serves as the URL for that page and the Referrer for the next page.
     *
     * @return String The value of the preset attribute #url
     */
    String getScreenUrl();

    /**
     * The return value is the added custom attribute.
     *
     * @return JSONObject Page browsing event custom properties
     * @throws JSONException JSONException
     */
    JSONObject getTrackProperties() throws JSONException;
}
