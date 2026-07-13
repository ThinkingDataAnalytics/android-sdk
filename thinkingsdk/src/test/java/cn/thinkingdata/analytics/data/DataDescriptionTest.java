/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.data;

import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.TDConstants;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DataDescription.get() — the JSON assembly step before data is stored/uploaded.
 *
 * Android stubs (TextUtils, etc.) return default values via returnDefaultValues=true.
 * ThinkingAnalyticsSDK and ITime are mocked to avoid real Android/network initialisation.
 */
public class DataDescriptionTest {

    private ThinkingAnalyticsSDK mockSdk;
    private ITime mockTime;
    private JSONObject properties;

    @Before
    public void setUp() throws Exception {
        mockSdk = mock(ThinkingAnalyticsSDK.class);
        when(mockSdk.getToken()).thenReturn("test_app_id");

        mockTime = mock(ITime.class);
        when(mockTime.getTime()).thenReturn("2024-06-15 10:30:00.000");
        when(mockTime.getZoneOffset()).thenReturn(8.0);

        properties = new JSONObject();
        properties.put("product", "widget");
        properties.put("price", 99);
    }

    // ── track event — required fields ─────────────────────────────────────────

    @Test
    public void get_track_containsTypeField() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_1");
        desc.eventName = "button_click";

        JSONObject result = desc.get();

        assertTrue(result.has(TDConstants.KEY_TYPE));
        assertEquals("track", result.getString(TDConstants.KEY_TYPE));
    }

    @Test
    public void get_track_containsTimeField() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_1");
        desc.eventName = "button_click";

        JSONObject result = desc.get();

        assertTrue(result.has(TDConstants.KEY_TIME));
        assertEquals("2024-06-15 10:30:00.000", result.getString(TDConstants.KEY_TIME));
    }

    @Test
    public void get_track_containsDistinctId() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_42", "account_1");
        desc.eventName = "login";

        JSONObject result = desc.get();

        assertTrue(result.has(TDConstants.KEY_DISTINCT_ID));
        assertEquals("user_42", result.getString(TDConstants.KEY_DISTINCT_ID));
    }

    @Test
    public void get_track_containsEventName() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_1");
        desc.eventName = "purchase";

        JSONObject result = desc.get();

        assertTrue(result.has(TDConstants.KEY_EVENT_NAME));
        assertEquals("purchase", result.getString(TDConstants.KEY_EVENT_NAME));
    }

    @Test
    public void get_track_containsProperties() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_1");
        desc.eventName = "view_item";

        JSONObject result = desc.get();

        assertTrue(result.has(TDConstants.KEY_PROPERTIES));
        JSONObject props = result.getJSONObject(TDConstants.KEY_PROPERTIES);
        assertEquals("widget", props.getString("product"));
        assertEquals(99, props.getInt("price"));
    }

    @Test
    public void get_track_zoneOffsetAddedToProperties() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_1");
        desc.eventName = "scroll";

        JSONObject result = desc.get();

        JSONObject props = result.getJSONObject(TDConstants.KEY_PROPERTIES);
        assertTrue(props.has(TDConstants.KEY_ZONE_OFFSET));
        assertEquals(8.0, props.getDouble(TDConstants.KEY_ZONE_OFFSET), 1e-9);
    }

    @Test
    public void get_track_nullZoneOffset_notAddedToProperties() throws Exception {
        when(mockTime.getZoneOffset()).thenReturn(null);
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_1");
        desc.eventName = "click";

        JSONObject result = desc.get();

        JSONObject props = result.getJSONObject(TDConstants.KEY_PROPERTIES);
        assertFalse(props.has(TDConstants.KEY_ZONE_OFFSET));
    }

    // ── accountId — present vs absent ────────────────────────────────────────

    @Test
    public void get_withAccountId_accountIdIncluded() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", "account_xyz");
        desc.eventName = "click";

        JSONObject result = desc.get();

        assertTrue(result.has(TDConstants.KEY_ACCOUNT_ID));
        assertEquals("account_xyz", result.getString(TDConstants.KEY_ACCOUNT_ID));
    }

    @Test
    public void get_withNullAccountId_accountIdNotIncluded() throws Exception {
        // null value: JSONObject.put(key, null) removes the key in org.json
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", null);
        desc.eventName = "click";

        JSONObject result = desc.get();

        assertFalse(
                "account_id must not appear when accountId is null",
                result.has(TDConstants.KEY_ACCOUNT_ID)
        );
    }

    // ── DataType differences — track vs user property ─────────────────────────

    @Test
    public void get_userSetType_typeStringIsUserSet() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.USER_SET, "user_1", null);

        JSONObject result = desc.get();

        assertEquals("user_set", result.getString(TDConstants.KEY_TYPE));
    }

    @Test
    public void get_userSetType_noEventNameField() throws Exception {
        // isTrack() returns false for USER_SET, so #event_name must not be added
        DataDescription desc = buildTrack(TDConstants.DataType.USER_SET, "user_1", null);

        JSONObject result = desc.get();

        assertFalse(
                "#event_name must not be present for user property types",
                result.has(TDConstants.KEY_EVENT_NAME)
        );
    }

    @Test
    public void get_userSetType_noZoneOffsetInProperties() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.USER_SET, "user_1", null);

        JSONObject result = desc.get();

        JSONObject props = result.getJSONObject(TDConstants.KEY_PROPERTIES);
        assertFalse(
                "#zone_offset must not be in properties for user property types",
                props.has(TDConstants.KEY_ZONE_OFFSET)
        );
    }

    @Test
    public void get_trackUpdateType_isTrack_eventNamePresent() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK_UPDATE, "user_1", "acc_1");
        desc.eventName = "payment";

        JSONObject result = desc.get();

        assertEquals("track_update", result.getString(TDConstants.KEY_TYPE));
        assertTrue(result.has(TDConstants.KEY_EVENT_NAME));
        assertEquals("payment", result.getString(TDConstants.KEY_EVENT_NAME));
    }

    @Test
    public void get_trackOverwriteType_isTrack_eventNamePresent() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK_OVERWRITE, "user_1", "acc_1");
        desc.eventName = "sign_up";

        JSONObject result = desc.get();

        assertEquals("track_overwrite", result.getString(TDConstants.KEY_TYPE));
        assertTrue(result.has(TDConstants.KEY_EVENT_NAME));
    }

    // ── extraFields ───────────────────────────────────────────────────────────

    @Test
    public void get_withExtraFields_fieldsAppendedToOutput() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", null);
        desc.eventName = "click";

        Map<String, String> extras = new HashMap<>();
        extras.put("#app_id", "app_abc");
        extras.put("#uuid", "uuid-1234");
        desc.setExtraFields(extras);

        JSONObject result = desc.get();

        assertEquals("app_abc", result.getString("#app_id"));
        assertEquals("uuid-1234", result.getString("#uuid"));
    }

    @Test
    public void get_noExtraFields_noExtraKeysInOutput() throws Exception {
        DataDescription desc = buildTrack(TDConstants.DataType.TRACK, "user_1", null);
        desc.eventName = "click";

        JSONObject result = desc.get();

        // Only the standard keys should be present
        assertTrue(result.has(TDConstants.KEY_TYPE));
        assertTrue(result.has(TDConstants.KEY_TIME));
        assertTrue(result.has(TDConstants.KEY_DISTINCT_ID));
        assertTrue(result.has(TDConstants.KEY_EVENT_NAME));
        assertTrue(result.has(TDConstants.KEY_PROPERTIES));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private DataDescription buildTrack(TDConstants.DataType type, String distinctId, String accountId)
            throws Exception {
        return new DataDescription(mockSdk, type, new JSONObject(properties.toString()),
                mockTime, distinctId, accountId, false);
    }
}
