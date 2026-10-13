/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

import cn.thinkingdata.analytics.session.SessionStore;

/**
 * {@link SessionStore} backed by the instance {@link CommonStorageManager}.
 */
public class AnalyticsSessionStore implements SessionStore {

    private static final long EMPTY_TIME = -1L;

    private final CommonStorageManager storageManager;

    public AnalyticsSessionStore(CommonStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public void save(String sessionId, long lastEventTime) {
        storageManager.saveAnalyticsSession(sessionId, lastEventTime);
    }

    @Override
    public String loadSessionId() {
        String sessionId = storageManager.getAnalyticsSessionId("");
        if (sessionId == null || sessionId.length() == 0) {
            return null;
        }
        return sessionId;
    }

    @Override
    public long loadLastEventTime() {
        return storageManager.getAnalyticsSessionLastEventTime(EMPTY_TIME);
    }
}
