/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.session;

/**
 * Persistence for the current session id and the time of the last event.
 */
public interface SessionStore {

    /**
     * Saves the session state.
     *
     * @param sessionId UUID v7 session id, or empty when the session is invalidated
     * @param lastEventTime last event time in epoch milliseconds
     */
    void save(String sessionId, long lastEventTime);

    /**
     * @return stored session id, or null when none has been saved
     */
    String loadSessionId();

    /**
     * @return stored last event time, or -1 when none has been saved
     */
    long loadLastEventTime();
}
