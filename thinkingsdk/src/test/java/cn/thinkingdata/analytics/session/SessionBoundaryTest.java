/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Boundary cases for one session staying stable, and a new session opening
 * after a 5-minute gap with no events. Time is injected, so the 5-minute
 * edges are exact milliseconds rather than a wall-clock sleep.
 */
public class SessionBoundaryTest {

    /** 2023-11-14T22:13:20Z, far from zero so timestamps stay positive. */
    private static final long T0 = 1_700_000_000_000L;
    private static final long FIVE_MINUTES = SessionManager.DEFAULT_TIMEOUT_MILLIS;

    @Test
    public void round1_defaultTimeout_isFiveMinutes() {
        assertEquals(5 * 60 * 1000L, FIVE_MINUTES);
        assertEquals(300_000L, FIVE_MINUTES);
    }

    @Test
    public void round1_burstInsideSession_keepsSameId() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);
        long[] offsets = {0L, 1L, 1_000L, 60_000L, 4 * 60_000L};

        for (long offset : offsets) {
            assertEquals(first, session.onEvent(T0 + offset, FIVE_MINUTES));
        }
        assertEquals(first, session.getSessionId());
        assertTrue(first.matches(UuidV7.PATTERN));
    }

    @Test
    public void round1_sameMillisecond_sameManager_keepsId() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);

        assertEquals(first, session.onEvent(T0, FIVE_MINUTES));
        assertEquals(first, session.onEvent(T0, FIVE_MINUTES));
    }

    @Test
    public void round1_slidingWindow_spanExceedsFiveMinutes_keepsId() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);
        long step = FIVE_MINUTES - 60_000L;
        long now = T0;
        for (int i = 0; i < 4; i++) {
            now += step;
            assertEquals(first, session.onEvent(now, FIVE_MINUTES));
        }
        assertTrue(now - T0 > FIVE_MINUTES);
        assertEquals(T0, timestampMillis(first));
    }

    @Test
    public void round1_oneMillisecondBeforeFiveMinutes_keepsId() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);

        assertEquals(first, session.onEvent(T0 + FIVE_MINUTES - 1, FIVE_MINUTES));
        assertEquals(first, session.getSessionId());
    }

    @Test
    public void round2_exactlyFiveMinutesIdle_startsNewSession() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);
        long nextAt = T0 + FIVE_MINUTES;

        String next = session.onEvent(nextAt, FIVE_MINUTES);

        assertNotEquals(first, next);
        assertEquals(next, session.getSessionId());
        assertEquals(nextAt, timestampMillis(next));
        assertEquals(T0, timestampMillis(first));
    }

    @Test
    public void round2_oneMillisecondPastFiveMinutes_startsNewSession() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);
        long nextAt = T0 + FIVE_MINUTES + 1;

        String next = session.onEvent(nextAt, FIVE_MINUTES);

        assertNotEquals(first, next);
        assertEquals(nextAt, timestampMillis(next));
    }

    @Test
    public void round2_afterRotation_newSessionStaysStable() {
        SessionManager session = new SessionManager(null);
        session.onEvent(T0, FIVE_MINUTES);
        long rotatedAt = T0 + FIVE_MINUTES;
        String rotated = session.onEvent(rotatedAt, FIVE_MINUTES);

        assertEquals(rotated, session.onEvent(rotatedAt + 1, FIVE_MINUTES));
        assertEquals(rotated, session.onEvent(rotatedAt + FIVE_MINUTES - 1, FIVE_MINUTES));

        long thirdAt = rotatedAt + FIVE_MINUTES - 1 + FIVE_MINUTES;
        String third = session.onEvent(thirdAt, FIVE_MINUTES);
        assertNotEquals(rotated, third);
        assertEquals(thirdAt, timestampMillis(third));
    }

    @Test
    public void round2_explicitZeroTimeout_stillUsesFiveMinuteBoundary() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, 0L);

        assertEquals(first, session.onEvent(T0 + FIVE_MINUTES - 1, 0L));
        String next = session.onEvent(T0 + FIVE_MINUTES - 1 + FIVE_MINUTES, 0L);
        assertNotEquals(first, next);
    }

    @Test
    public void round2_restoredWithinFiveMinutes_keepsId() {
        MemorySessionStore store = new MemorySessionStore();
        String first = new SessionManager(store).onEvent(T0, FIVE_MINUTES);

        SessionManager restored = new SessionManager(store);

        assertEquals(first, restored.getSessionId());
        assertEquals(first, restored.onEvent(T0 + FIVE_MINUTES - 1, FIVE_MINUTES));
    }

    @Test
    public void round2_restoredAfterExactlyFiveMinutes_startsNewSessionOnNextEvent() {
        MemorySessionStore store = new MemorySessionStore();
        String first = new SessionManager(store).onEvent(T0, FIVE_MINUTES);

        SessionManager restored = new SessionManager(store);
        assertEquals(first, restored.getSessionId());

        long nextAt = T0 + FIVE_MINUTES;
        String next = restored.onEvent(nextAt, FIVE_MINUTES);
        assertNotEquals(first, next);
        assertEquals(nextAt, timestampMillis(next));
        assertEquals(next, store.sessionId);
        assertEquals(nextAt, store.lastEventTime);
    }

    @Test
    public void round3_lifecycleTransitions_doNotRestartFiveMinuteWindow() {
        MemorySessionStore store = new MemorySessionStore();
        SessionManager session = new SessionManager(store);
        String first = session.onEvent(T0, FIVE_MINUTES);

        session.onEnterBackground(T0 + 2 * 60_000L);
        assertEquals(first, session.onEnterForeground(T0 + 4 * 60_000L, FIVE_MINUTES));
        assertEquals(T0, store.lastEventTime);

        String next = session.onEvent(T0 + FIVE_MINUTES, FIVE_MINUTES);
        assertNotEquals(first, next);
        assertEquals(T0 + FIVE_MINUTES, timestampMillis(next));
    }

    @Test
    public void round3_foregroundAfterTimeout_rotatesOnlyOnNextEvent() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);
        long backgroundAt = T0 + 60_000L;
        session.onEnterBackground(backgroundAt);

        long foregroundAt = T0 + FIVE_MINUTES + 1;
        assertEquals(first, session.onEnterForeground(foregroundAt, FIVE_MINUTES));
        assertEquals(first, session.getSessionId());

        String next = session.onEvent(foregroundAt, FIVE_MINUTES);
        assertNotEquals(first, next);
        assertEquals(foregroundAt, timestampMillis(next));
    }

    @Test
    public void round3_backgroundWithoutSession_doesNotCreateOne() {
        SessionManager session = new SessionManager(null);

        session.onEnterBackground(T0);

        assertNull(session.getSessionId());
    }

    @Test
    public void round3_clockMovedBackward_doesNotRotate() {
        SessionManager session = new SessionManager(null);
        String first = session.onEvent(T0, FIVE_MINUTES);

        assertEquals(first, session.onEvent(T0 - 5_000L, FIVE_MINUTES));
        assertEquals(first, session.getSessionId());
    }

    @Test
    public void round4_distinctInstanceKeys_keepIndependentSessions() {
        String keyA = "boundary-instance-a";
        String keyB = "boundary-instance-b";
        SessionManager first = SessionManager.getInstance(keyA, null);
        SessionManager second = SessionManager.getInstance(keyB, null);

        String idA = first.onEvent(T0, FIVE_MINUTES);
        String idB = second.onEvent(T0, FIVE_MINUTES);

        assertNotEquals(idA, idB);
        assertEquals(idA, first.onEvent(T0 + FIVE_MINUTES - 1, FIVE_MINUTES));
        assertEquals(idB, second.getSessionId());
    }

    @Test
    public void round4_sameInstanceKey_sharesSession_andIgnoresLaterStore() {
        String key = "boundary-instance-shared";
        MemorySessionStore ignored = new MemorySessionStore();
        SessionManager first = SessionManager.getInstance(key, null);
        String id = first.onEvent(T0, FIVE_MINUTES);

        SessionManager again = SessionManager.getInstance(key, ignored);

        assertEquals(id, again.getSessionId());
        assertEquals(id, again.onEvent(T0 + 1_000L, FIVE_MINUTES));
        assertNull(ignored.loadSessionId());
    }

    @Test
    public void round3_incompleteStore_startsFreshSession() {
        MemorySessionStore store = new MemorySessionStore();
        String staleId = "01900000-0000-7000-8000-000000000001";
        store.sessionId = staleId;
        store.lastEventTime = -1L;

        SessionManager restored = new SessionManager(store);
        assertNull(restored.getSessionId());

        String created = restored.onEvent(T0, FIVE_MINUTES);
        assertEquals(T0, timestampMillis(created));
        assertNotEquals(staleId, created);
        assertEquals(created, store.sessionId);
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
