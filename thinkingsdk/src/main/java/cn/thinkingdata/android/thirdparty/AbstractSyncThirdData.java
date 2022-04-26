package cn.thinkingdata.android.thirdparty;

public abstract class AbstractSyncThirdData implements ISyncThirdPartyData {

    protected final String TAG = "ThinkingAnalytics.SyncData";

    protected String distinctId;//ta_distinct_id
    protected String accountId;//ta_account_id

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
