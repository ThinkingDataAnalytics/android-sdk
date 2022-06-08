/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.thirdparty;

/**
 * < AbstractSyncThirdData >.
 *
 * @author thinker
 * @create 2022/01/01
 * @since 1.0.0
 */
public abstract class AbstractSyncThirdData implements ISyncThirdPartyData {

    protected static final String TAG = "ThinkingAnalytics.SyncData";

    protected String distinctId; //ta_distinct_id
    protected String accountId; //ta_account_id

    public AbstractSyncThirdData() {
    }

    public AbstractSyncThirdData(String distinctId) {
        this.distinctId = distinctId;
    }

    public AbstractSyncThirdData(String distinctId, String accountId) {
        this.distinctId = distinctId;
        this.accountId = accountId;
    }

}
