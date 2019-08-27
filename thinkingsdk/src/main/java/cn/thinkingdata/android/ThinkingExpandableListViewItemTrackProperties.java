package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * ExpandableListView 可以通过 Adapter 实现本接口，来为点击某 item 时触发的控件点击事件增加自定义属性.
 */
public interface ThinkingExpandableListViewItemTrackProperties {
    /**
     * 点击 childPosition 处 item 时的属性
     * @param groupPosition The child's parent group's position.
     * @param childPosition The child position within the group.
     * @return 自定义属性
     */
    JSONObject getThinkingChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 时的属性
     * @param groupPosition the group position
     * @return 自定义属性
     */
    JSONObject getThinkingGroupItemTrackProperties(int groupPosition) throws JSONException;
}