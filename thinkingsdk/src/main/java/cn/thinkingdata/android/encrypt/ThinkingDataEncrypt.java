package cn.thinkingdata.android.encrypt;

import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.thinkingdata.android.TDConfig;

public class ThinkingDataEncrypt {

    private static final String TAG = "ThinkingAnalytics.ThinkingDataEncrypt";

    private ITDEncrypt mTAEncrypt;

    private final List<ITDEncrypt> mEncrypts = new ArrayList<>();

    private static final Map<String, ThinkingDataEncrypt> sInstances = new HashMap<>();

    private final TDConfig mConfig;

    public static ThinkingDataEncrypt createInstance(String token, TDConfig mConfig) {
        synchronized (sInstances) {
            ThinkingDataEncrypt mInstance = sInstances.get(token);
            if (mInstance == null) {
                mInstance = new ThinkingDataEncrypt(mConfig);
                sInstances.put(token, mInstance);
            }
            return mInstance;
        }
    }

    public static ThinkingDataEncrypt getInstance(String token){
        synchronized (sInstances) {
            return sInstances.get(token);
        }
    }

    private ThinkingDataEncrypt(TDConfig mConfig) {
        this.mConfig = mConfig;
        mEncrypts.add(new TDRSAEncrypt());
    }

    /**
     * 对上报数据进行加密
     *
     * @param json
     * @return
     */
    public JSONObject encryptTrackData(JSONObject json) {

        try {
            if (mConfig == null) return json;
            TDSecreteKey secreteKey = mConfig.getSecreteKey();

            if (isSecretKeyNull(secreteKey)) {
                return json;
            }

            if (!isMatchEncryptType(mTAEncrypt, secreteKey)) {
                mTAEncrypt = getEncryptListener(secreteKey);
            }

            if (mTAEncrypt == null) return json;

            String pKey = secreteKey.publicKey;
            if (pKey.startsWith("EC:")) {
                pKey = pKey.substring(pKey.indexOf(":") + 1);
            }

            String encryptedKey = mTAEncrypt.encryptSymmetricKey(pKey);

            if (TextUtils.isEmpty(encryptedKey)) {
                return json;
            }

            String encryptData = mTAEncrypt.encryptDataEvent(json.toString());
            if (TextUtils.isEmpty(encryptData)) {
                return json;
            }
            JSONObject dataJson = new JSONObject();
            dataJson.put("ekey", encryptedKey);
            dataJson.put("pkv", secreteKey.version);
            dataJson.put("payload", encryptData);
            return dataJson;
        } catch (Exception e) {

        }
        return json;
    }

    /**
     * 判断公钥是否为空
     *
     * @return
     */
    private boolean isSecretKeyNull(TDSecreteKey secreteKey) {
        return secreteKey == null || TextUtils.isEmpty(secreteKey.publicKey);
    }

    /**
     * 是否匹配到插件
     *
     * @param listener
     * @param secreteKey
     * @return
     */
    boolean isMatchEncryptType(ITDEncrypt listener, TDSecreteKey secreteKey) {
        return listener != null && !isSecretKeyNull(secreteKey) && !isEncryptorTypeNull(listener) && listener.asymmetricEncryptType().equals(secreteKey.asymmetricEncryption)
                && listener.symmetricEncryptType().equals(secreteKey.symmetricEncryption);
    }

    /**
     * 插件加密类型是否为空
     *
     * @param mEncrypt
     * @return
     */
    private boolean isEncryptorTypeNull(ITDEncrypt mEncrypt) {
        return TextUtils.isEmpty(mEncrypt.asymmetricEncryptType())
                || TextUtils.isEmpty(mEncrypt.symmetricEncryptType());
    }

    /**
     * 找到匹配的插件
     *
     * @param secreteKey
     * @return
     */
    ITDEncrypt getEncryptListener(TDSecreteKey secreteKey) {
        if (!isSecretKeyNull(secreteKey)) {
            for (ITDEncrypt encrypt : mEncrypts) {
                if (encrypt != null && isMatchEncryptType(encrypt, secreteKey)) {
                    return encrypt;
                }
            }
        }
        return null;
    }

}
