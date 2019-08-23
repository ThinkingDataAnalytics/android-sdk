package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * ListView 与 GridView 可以通过Adapter实现本接口，来为点击某item时触发的控件点击事件增加自定义属性。
 */
public interface ThinkingAdapterViewItemTrackProperties {
    /**
     * 增加点击 position 处 item 时的属性
     * @param position int
     * @return JSONObject 自定义属性
     * @throws JSONException
     */
    JSONObject getThinkingItemTrackProperties(int position) throws JSONException;
}