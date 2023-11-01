/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.encrypt;

import android.text.TextUtils;

import cn.thinkingdata.analytics.TDConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * Data encryption class.
 * */
public class ThinkingDataEncrypt {

    private static final String TAG = "ThinkingAnalytics.ThinkingDataEncrypt";

    private ITDEncrypt mTAEncrypt;

    private final List<ITDEncrypt> mEncrypts = new ArrayList<>();

    private static final Map<String, ThinkingDataEncrypt> sInstances = new HashMap<>();

    private final TDConfig mConfig;

    /**
     * createInstance
     *
     * @param token App ID
     * @param mConfig TDConfig
     * @return {@link ThinkingDataEncrypt}
     */
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

    /**
     * getInstance
     *
     * @param token App ID
     * @return {@link ThinkingDataEncrypt}
     */
    public static ThinkingDataEncrypt getInstance(String token) {
        synchronized (sInstances) {
            return sInstances.get(token);
        }
    }

    private ThinkingDataEncrypt(TDConfig mConfig) {
        this.mConfig = mConfig;
        mEncrypts.add(new TDRSAEncrypt());
    }

    /**
     * Encrypt the reported data
     *
     * @param json JSONObject
     * @return JSONObject
     */
    public JSONObject encryptTrackData(JSONObject json) {

        try {
            if (mConfig == null) {
                return json;
            }
            TDSecreteKey secreteKey = mConfig.getSecreteKey();

            if (isSecretKeyNull(secreteKey)) {
                return json;
            }

            if (!isMatchEncryptType(mTAEncrypt, secreteKey)) {
                mTAEncrypt = getEncryptListener(secreteKey);
            }

            if (mTAEncrypt == null) {
                return json;
            }

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
            //ignored
        }
        return json;
    }

    /**
     * Check whether the public key is empty.
     *
     * @return boolean
     */
    private boolean isSecretKeyNull(TDSecreteKey secreteKey) {
        return secreteKey == null || TextUtils.isEmpty(secreteKey.publicKey);
    }

    /**
     * Whether the plug-in matches.
     *
     * @param listener ITDEncrypt
     * @param secreteKey TDSecreteKey
     * @return is match
     */
    boolean isMatchEncryptType(ITDEncrypt listener, TDSecreteKey secreteKey) {
        return listener != null && !isSecretKeyNull(secreteKey) && !isEncryptorTypeNull(listener) && listener.asymmetricEncryptType().equals(secreteKey.asymmetricEncryption)
                && listener.symmetricEncryptType().equals(secreteKey.symmetricEncryption);
    }

    /**
     * Whether the plug-in encryption type is null.
     *
     * @param mEncrypt ITDEncrypt
     * @return Whether the encryption type is null
     */
    private boolean isEncryptorTypeNull(ITDEncrypt mEncrypt) {
        return TextUtils.isEmpty(mEncrypt.asymmetricEncryptType())
                || TextUtils.isEmpty(mEncrypt.symmetricEncryptType());
    }

    /**
     * Find a matching plug-in.
     *
     * @param secreteKey TDSecreteKey
     * @return ITDEncrypt
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
