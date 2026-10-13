/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SessionManagerTest {

    private static final long TIMEOUT = 1_000L;

    @Test
    public void onEvent_firstEvent_usesUuidV7() {
        SessionManager session = new SessionManager(null);

        assertNull(session.getSessionId());
        String sessionId = session.onEvent(1_700_000_000_000L, TIMEOUT);
        assertTrue(sessionId.matches(UuidV7.PATTERN));
        assertEquals(1_700_000_000_000L, timestampMillis(sessionId));
    }

    @Test
    public void uuidV7_sameMillisecond_isNotRepeated() {
        long now = 1_700_000_000_000L;
        String first = new SessionManager(null).onEvent(now, TIMEOUT);
        String second = new SessionManager(null).onEvent(now, TIMEOUT);
        assertNotEquals(first, second);
    }

    @Test
    public void onEvent_withinTimeout_reusesSessionId() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(0L, TIMEOUT);

        assertEquals(first, session.onEvent(TIMEOUT - 1, TIMEOUT));
    }

    @Test
    public void onEvent_atTimeout_startsNewSession() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(0L, TIMEOUT);
        String next = session.onEvent(TIMEOUT, TIMEOUT);

        assertNotEquals(first, next);
        assertEquals(TIMEOUT, timestampMillis(next));
    }

    @Test
    public void lifecycleTransitions_doNotExtendSession() {
        SessionManager session = new SessionManager(null);
        long timeout = 15_000L;
        String first = session.onEvent(0L, timeout);

        session.onEnterBackground(100L);
        assertEquals(first, session.onEnterForeground(9_000L, timeout));
        String next = session.onEvent(17_953L, timeout);

        assertNotEquals(first, next);
        assertEquals(17_953L, timestampMillis(next));
    }

    @Test
    public void lifecycleTransitions_withoutEvents_doNotCreateSession() {
        SessionManager session = new SessionManager(null);

        session.onEnterBackground(100L);
        assertNull(session.onEnterForeground(100L + TIMEOUT, TIMEOUT));
        assertNull(session.getSessionId());
    }

    @Test
    public void onAccountChanged_invalidatesMemoryAndPersistedSession() {
        MemorySessionStore store = new MemorySessionStore();
        SessionManager session = new SessionManager(store);
        String first = session.onEvent(5_000L, TIMEOUT);

        session.onAccountChanged();

        assertNull(session.getSessionId());
        assertEquals("", store.sessionId);
        assertEquals(SessionManager.NO_SESSION, store.lastEventTime);

        SessionManager restored = new SessionManager(store);
        assertNull(restored.getSessionId());
        String next = restored.onEvent(5_001L, TIMEOUT);
        assertNotEquals(first, next);
        assertEquals(5_001L, timestampMillis(next));
    }

    @Test
    public void onEvent_nonPositiveTimeout_usesFiveMinutes() {
        SessionManager withinWindow = new SessionManager(null);
        String first = withinWindow.onEvent(0L, 0L);
        assertEquals(first, withinWindow.onEvent(SessionManager.DEFAULT_TIMEOUT_MILLIS - 1, -1L));

        SessionManager expired = new SessionManager(null);
        expired.onEvent(0L, 0L);
        String next = expired.onEvent(SessionManager.DEFAULT_TIMEOUT_MILLIS, 0L);
        assertEquals(SessionManager.DEFAULT_TIMEOUT_MILLIS, timestampMillis(next));
    }

    @Test
    public void store_reloadsSessionWithinTimeout() {
        MemorySessionStore store = new MemorySessionStore();
        SessionManager first = new SessionManager(store);
        String sessionId = first.onEvent(5_000L, TIMEOUT);

        SessionManager restored = new SessionManager(store);

        assertEquals(sessionId, restored.getSessionId());
        assertEquals(sessionId, restored.onEvent(5_000L + TIMEOUT - 1, TIMEOUT));
        assertFalse(sessionId.isEmpty());
    }

    private static long timestampMillis(String uuid) {
        return Long.parseLong(uuid.replace("-", "").substring(0, 12), 16);
    }

    private static final class MemorySessionStore implements SessionStore {
        private String sessionId;
        private long lastEventTime = -1L;

        @Override
        public void save(String sessionId, long lastEventTime) {
            this.sessionId = sessionId;
            this.lastEventTime = lastEventTime;
        }

        @Override
        public String loadSessionId() {
            return sessionId;
        }

        @Override
        public long loadLastEventTime() {
            return lastEventTime;
        }
    }
}
