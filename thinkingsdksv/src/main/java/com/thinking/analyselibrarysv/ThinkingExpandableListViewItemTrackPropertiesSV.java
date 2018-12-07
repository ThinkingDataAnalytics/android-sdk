package com.thinking.analyselibrarysv;

import org.json.JSONException;
import org.json.JSONObject;

public interface ThinkingExpandableListViewItemTrackPropertiesSV {

    JSONObject getThinkingChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    JSONObject getThinkingGroupItemTrackProperties(int groupPosition) throws JSONException;
}