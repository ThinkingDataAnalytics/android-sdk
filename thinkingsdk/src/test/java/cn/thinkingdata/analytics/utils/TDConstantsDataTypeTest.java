/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class TDConstantsDataTypeTest {

    // ── isTrack — track types ─────────────────────────────────────────────────

    @Test
    public void isTrack_track_returnsTrue() {
        assertTrue(TDConstants.DataType.TRACK.isTrack());
    }

    @Test
    public void isTrack_trackUpdate_returnsTrue() {
        assertTrue(TDConstants.DataType.TRACK_UPDATE.isTrack());
    }

    @Test
    public void isTrack_trackOverwrite_returnsTrue() {
        assertTrue(TDConstants.DataType.TRACK_OVERWRITE.isTrack());
    }

    // ── isTrack — user property types ────────────────────────────────────────

    @Test
    public void isTrack_userSet_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_SET.isTrack());
    }

    @Test
    public void isTrack_userAdd_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_ADD.isTrack());
    }

    @Test
    public void isTrack_userSetOnce_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_SET_ONCE.isTrack());
    }

    @Test
    public void isTrack_userUnset_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_UNSET.isTrack());
    }

    @Test
    public void isTrack_userAppend_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_APPEND.isTrack());
    }

    @Test
    public void isTrack_userDel_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_DEL.isTrack());
    }

    @Test
    public void isTrack_userUniqAppend_returnsFalse() {
        assertFalse(TDConstants.DataType.USER_UNIQ_APPEND.isTrack());
    }

    // ── getType — string representation ──────────────────────────────────────

    @Test
    public void getType_track_returnsTrackString() {
        assertEquals("track", TDConstants.DataType.TRACK.getType());
    }

    @Test
    public void getType_trackUpdate_returnsCorrectString() {
        assertEquals("track_update", TDConstants.DataType.TRACK_UPDATE.getType());
    }

    @Test
    public void getType_trackOverwrite_returnsCorrectString() {
        assertEquals("track_overwrite", TDConstants.DataType.TRACK_OVERWRITE.getType());
    }

    @Test
    public void getType_userSet_returnsCorrectString() {
        assertEquals("user_set", TDConstants.DataType.USER_SET.getType());
    }

    @Test
    public void getType_userAdd_returnsCorrectString() {
        assertEquals("user_add", TDConstants.DataType.USER_ADD.getType());
    }

    @Test
    public void getType_userSetOnce_returnsCorrectString() {
        assertEquals("user_setOnce", TDConstants.DataType.USER_SET_ONCE.getType());
    }

    @Test
    public void getType_userUnset_returnsCorrectString() {
        assertEquals("user_unset", TDConstants.DataType.USER_UNSET.getType());
    }

    @Test
    public void getType_userAppend_returnsCorrectString() {
        assertEquals("user_append", TDConstants.DataType.USER_APPEND.getType());
    }

    @Test
    public void getType_userDel_returnsCorrectString() {
        assertEquals("user_del", TDConstants.DataType.USER_DEL.getType());
    }

    @Test
    public void getType_userUniqAppend_returnsCorrectString() {
        assertEquals("user_uniq_append", TDConstants.DataType.USER_UNIQ_APPEND.getType());
    }

    // ── reverse lookup ────────────────────────────────────────────────────────

    @Test
    public void reverseLookup_track_returnsTrackEnum() {
        assertEquals(TDConstants.DataType.TRACK, TDConstants.DataType.get("track"));
    }

    @Test
    public void reverseLookup_trackUpdate_returnsEnum() {
        assertEquals(TDConstants.DataType.TRACK_UPDATE, TDConstants.DataType.get("track_update"));
    }

    @Test
    public void reverseLookup_trackOverwrite_returnsEnum() {
        assertEquals(TDConstants.DataType.TRACK_OVERWRITE, TDConstants.DataType.get("track_overwrite"));
    }

    @Test
    public void reverseLookup_userSet_returnsEnum() {
        assertEquals(TDConstants.DataType.USER_SET, TDConstants.DataType.get("user_set"));
    }

    @Test
    public void reverseLookup_userSetOnce_returnsEnum() {
        // Note: the string is "user_setOnce" (mixed case)
        assertEquals(TDConstants.DataType.USER_SET_ONCE, TDConstants.DataType.get("user_setOnce"));
    }

    @Test
    public void reverseLookup_userDel_returnsEnum() {
        assertEquals(TDConstants.DataType.USER_DEL, TDConstants.DataType.get("user_del"));
    }

    @Test
    public void reverseLookup_unknownString_returnsNull() {
        assertNull(TDConstants.DataType.get("nonexistent_type"));
    }

    @Test
    public void reverseLookup_emptyString_returnsNull() {
        assertNull(TDConstants.DataType.get(""));
    }

    @Test
    public void reverseLookup_null_returnsNull() {
        assertNull(TDConstants.DataType.get(null));
    }

    @Test
    public void reverseLookup_wrongCase_returnsNull() {
        // Lookup is case-sensitive: "Track" != "track"
        assertNull(TDConstants.DataType.get("Track"));
        assertNull(TDConstants.DataType.get("TRACK"));
    }

    // ── round-trip consistency ────────────────────────────────────────────────

    @Test
    public void getType_isConsistentWithReverseLookup_allValues() {
        for (TDConstants.DataType type : TDConstants.DataType.values()) {
            TDConstants.DataType found = TDConstants.DataType.get(type.getType());
            assertEquals("Reverse lookup must round-trip for " + type, type, found);
        }
    }
}
