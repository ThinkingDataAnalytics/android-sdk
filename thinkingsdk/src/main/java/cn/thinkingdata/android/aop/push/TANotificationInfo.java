/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.aop.push;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/6/1
 * @since
 */
public class TANotificationInfo {
    String title;
    String content;
    long time;

    TANotificationInfo(String title, String content, long time) {
        this.title = title;
        this.content = content;
        this.time = time;
    }

    public static TANotificationInfo fromJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return new TANotificationInfo(jsonObject.optString("title"),
                    jsonObject.optString("content"),
                    jsonObject.optLong("time"));
        } catch (JSONException e) {

        }
        return null;
    }

    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", title);
            jsonObject.put("content", content);
            jsonObject.put("time", time);
            return jsonObject.toString();
        } catch (JSONException e) {

        }
        return null;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
