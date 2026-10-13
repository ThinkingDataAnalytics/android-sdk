/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.session;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the current {@code #session_id} using Amplitude's session gap rule.
 * The id is a UUID v7 string. Its first 48 bits are the session start time in milliseconds.
 * A new id is assigned when the gap since the last event reaches the configured timeout,
 * or when the account identity changes. App foreground/background transitions do not affect
 * the event gap.
 */
public class SessionManager {

    /** Default gap before a new session starts: 5 minutes. */
    public static final long DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    static final long NO_SESSION = -1L;

    private static final Map<String, SessionManager> INSTANCES = new HashMap<>();

    private final SessionStore store;
    private String sessionId;
    private long lastEventTime = NO_SESSION;

    /**
     * Creates a session manager. When {@code store} holds a previous session, that state is restored.
     *
     * @param store persistence, or null to keep state in memory only
     */
    public SessionManager(SessionStore store) {
        this.store = store;
        if (store == null) {
            return;
        }
        String storedId = store.loadSessionId();
        long storedTime = store.loadLastEventTime();
        if (hasSession(storedId) && storedTime >= 0) {
            sessionId = storedId;
            lastEventTime = storedTime;
        }
    }

    /**
     * Returns the session manager for an SDK instance. The first caller supplies persistence;
     * later callers with the same key share that instance.
     *
     * @param instanceKey SDK instance name
     * @param store persistence used when this key has no manager yet; ignored when one already exists
     * @return shared session manager
     */
    public static SessionManager getInstance(String instanceKey, SessionStore store) {
        synchronized (INSTANCES) {
            SessionManager existing = INSTANCES.get(instanceKey);
            if (existing != null) {
                return existing;
            }
            SessionManager created = new SessionManager(store);
            INSTANCES.put(instanceKey, created);
            return created;
        }
    }

    /**
     * Assigns or extends the session for an event processed at {@code now}.
     *
     * @param now event processing time in epoch milliseconds
     * @param timeoutMillis session timeout; values &lt;= 0 use {@link #DEFAULT_TIMEOUT_MILLIS}
     * @return current session id, a UUID v7 string
     */
    public synchronized String onEvent(long now, long timeoutMillis) {
        long timeout = normalizeTimeout(timeoutMillis);
        if (!hasSession(sessionId) || lastEventTime < 0 || now - lastEventTime >= timeout) {
            sessionId = UuidV7.generate(now);
        }
        lastEventTime = now;
        persist();
        return sessionId;
    }

    /**
     * Handles an app background transition without changing session state.
     * Lifecycle transitions are not events and therefore must not extend the session timeout.
     *
     * @param now background time in epoch milliseconds; ignored for session timing
     */
    public synchronized void onEnterBackground(long now) {
        // Intentionally empty. Kept for source compatibility with existing integrations.
    }

    /**
     * Handles an app foreground transition without changing session state.
     * A timed-out session is rotated only when the next event is processed by
     * {@link #onEvent(long, long)}.
     *
     * @param now foreground time in epoch milliseconds; ignored for session timing
     * @param timeoutMillis session timeout; ignored until the next event
     * @return current session id, or null when no event has started a session
     */
    public synchronized String onEnterForeground(long now, long timeoutMillis) {
        return sessionId;
    }

    /**
     * Invalidates the current session after the account identity changes.
     * The next event starts a new session; no session is created by the account operation itself.
     */
    public synchronized void onAccountChanged() {
        sessionId = null;
        lastEventTime = NO_SESSION;
        if (store != null) {
            // Persist an empty id because SharedPreferences storage ignores null values.
            store.save("", NO_SESSION);
        }
    }

    /**
     * @return current session id, or null when no session has started
     */
    public synchronized String getSessionId() {
        return sessionId;
    }

    private static boolean hasSession(String sessionId) {
        return sessionId != null && sessionId.length() > 0;
    }

    private void persist() {
        if (store != null) {
            store.save(sessionId, lastEventTime);
        }
    }

    static long normalizeTimeout(long timeoutMillis) {
        return timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }
}
