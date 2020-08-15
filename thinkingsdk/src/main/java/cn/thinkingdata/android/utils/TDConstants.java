package cn.thinkingdata.android.utils;

import java.util.HashMap;
import java.util.Map;

public class TDConstants {

    public enum DataType {
        TRACK("track"),
        TRACK_UPDATE("track_update"),
        TRACK_OVERWRITE("track_overwrite"),
        USER_ADD("user_add"),
        USER_SET("user_set"),
        USER_SET_ONCE("user_setOnce"),
        USER_UNSET("user_unset"),
        USER_APPEND("user_append"),
        USER_DEL("user_del");

        private final String type;

        DataType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public boolean isTrack() {
            return this == TRACK || this == TRACK_OVERWRITE || this == TRACK_UPDATE;
        }

        //****** Reverse Lookup Implementation************//

        //Lookup table
        private static final Map<String, DataType> lookup = new HashMap<>();

        //Populate the lookup table on loading time
        static
        {
            for(DataType type : DataType.values())
            {
                lookup.put(type.getType(), type);
            }
        }

        //This method can be used for reverse lookup purpose
        public static DataType get(String type)
        {
            return lookup.get(type);
        }
    }

    // AOP Constants
    public static final String APP_CLICK_EVENT_NAME = "ta_app_click";
    public static final String APP_VIEW_EVENT_NAME = "ta_app_view";
    public static final String APP_START_EVENT_NAME = "ta_app_start";
    public static final String APP_END_EVENT_NAME = "ta_app_end";
    public static final String APP_CRASH_EVENT_NAME = "ta_app_crash";
    public static final String APP_INSTALL_EVENT_NAME = "ta_app_install";

    public static final String KEY_CRASH_REASON = "#app_crashed_reason";
    public static final String KEY_RESUME_FROM_BACKGROUND = "#resume_from_background";

    public static final String KEY_EVENT_ID  = "#event_id";
    public static final String KEY_FIRST_CHECK_ID = "#first_check_id";

    public static final String ELEMENT_ID = "#element_id";
    public static final String ELEMENT_TYPE = "#element_type";
    public static final String ELEMENT_CONTENT = "#element_content";
    public static final String ELEMENT_POSITION = "#element_position";
    public static final String ELEMENT_SELECTOR = "#element_selector";

    public static final String SCREEN_NAME = "#screen_name";
    public static final String TITLE = "#title";

    // Main data constants
    public static final String KEY_TYPE = "#type";
    public static final String KEY_TIME = "#time";
    public static final String KEY_DISTINCT_ID = "#distinct_id";
    public static final String KEY_ACCOUNT_ID = "#account_id";
    public static final String KEY_EVENT_NAME = "#event_name";
    public static final String KEY_PROPERTIES = "properties";

    public static final String KEY_URL = "#url";
    public static final String KEY_REFERRER = "#referrer";
    public static final String KEY_NETWORK_TYPE = "#network_type";
    public static final String KEY_APP_VERSION = "#app_version";
    public static final String KEY_DURATION = "#duration";
    public static final String KEY_ZONE_OFFSET = "#zone_offset";

    // System Information Constants
    public static final String KEY_OS_VERSION = "#os_version";
    public static final String KEY_MANUFACTURER = "#manufacturer";
    public static final String KEY_DEVICE_MODEL = "#device_model";
    public static final String KEY_SCREEN_HEIGHT = "#screen_height";
    public static final String KEY_SCREEN_WIDTH = "#screen_width";
    public static final String KEY_CARRIER = "#carrier";
    public static final String KEY_DEVICE_ID = "#device_id";
    public static final String KEY_SYSTEM_LANGUAGE = "#system_language";

    // Others
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    // 用于服务端去重
    public static final String DATA_ID = "#uuid";
}

