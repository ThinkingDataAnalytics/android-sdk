/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The ListView and GridView can implement this interface through Adapter to add custom properties
 * to the control click event that is triggered when an item is clicked.
 */
public interface ThinkingAdapterViewItemTrackProperties {
    /**
     * Adds properties when clicking on an item at position.
     *
     * @param position int
     * @return JSONObject
     */
    JSONObject getThinkingItemTrackProperties(int position) throws JSONException;
}