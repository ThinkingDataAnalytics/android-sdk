package com.thinking.analyselibrary;

import org.json.JSONException;
import org.json.JSONObject;

public interface ThinkingExpandableListViewItemTrackProperties {

    JSONObject getThinkingChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    JSONObject getThinkingGroupItemTrackProperties(int groupPosition) throws JSONException;
}