/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

import cn.thinkingdata.android.TDDebugException;
import cn.thinkingdata.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.TDLog;
import cn.thinkingdata.android.utils.TDUtils;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/9/8
 * @since
 */
public class CommonStorageManager {

    private static final String TAG = "ThinkingAnalytics.Storage";
    private final CommonStoragePlugin storagePlugin;

    private final Object mLoginId = new Object();
    private final Object mIdentifyId = new Object();
    private final Object mSuperProperties = new Object();

    public CommonStorageManager(Context context, String name) {
        storagePlugin = new CommonStoragePlugin(context, name);
    }

    /**
     * @return login id
     */
    public String getLoginId(boolean enableTrackOldData,Context context){
        synchronized (mLoginId) {
            String loginId = storagePlugin.get(LocalStorageType.LOGIN_ID);
            if (TextUtils.isEmpty(loginId) && enableTrackOldData) {
                loginId = GlobalStorageManager.getInstance(context).getOldLoginId();
                if(!TextUtils.isEmpty(loginId)){
                    storagePlugin.save(LocalStorageType.LOGIN_ID,loginId);
                    GlobalStorageManager.getInstance(context).clearOldLoginId();
                }
            }
            return loginId;
        }
    }

    /**
     * @param loginId save loginId
     */
    public void saveLoginId(String loginId,boolean shouldThrowException){
        try {
            if (TextUtils.isEmpty(loginId)) {
                TDLog.d(TAG, "The account id cannot be empty.");
                if (shouldThrowException) {
                    throw new TDDebugException("account id cannot be empty");
                }
                return;
            }
            synchronized (mLoginId) {
                if (!loginId.equals(storagePlugin.get(LocalStorageType.LOGIN_ID))) {
                    storagePlugin.save(LocalStorageType.LOGIN_ID,loginId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clearing an Account ID
     */
    public void clearLoginId(){
        synchronized (mLoginId){
            storagePlugin.save(LocalStorageType.LOGIN_ID,null);
        }
    }

    /**
     * Clearing an Account ID
     */
    public void logout(boolean enableTrackOldData,Context context){
        try {
            synchronized (mLoginId) {
                storagePlugin.save(LocalStorageType.LOGIN_ID,null);
                if (enableTrackOldData) {
                    if (!TextUtils.isEmpty(GlobalStorageManager.getInstance(context).getOldLoginId())) {
                        GlobalStorageManager.getInstance(context).clearOldLoginId();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return identify id
     */
    public String getIdentifyId(){
        synchronized (mIdentifyId){
            return storagePlugin.get(LocalStorageType.IDENTIFY);
        }
    }

    /**
     * @param identify save identify id
     */
    public void setIdentifyId(String identify,boolean shouldThrowException){
        if (TextUtils.isEmpty(identify)) {
            TDLog.w(TAG, "The identity cannot be empty.");
            if (shouldThrowException) {
                throw new TDDebugException("distinct id cannot be empty");
            }
            return;
        }

        synchronized (mIdentifyId) {
            storagePlugin.save(LocalStorageType.IDENTIFY,identify);
        }
    }

    /**
     * clear identify id
     */
    public void clearIdentify(){
        synchronized (mIdentifyId) {
            storagePlugin.save(LocalStorageType.IDENTIFY,null);
        }
    }

    /**
     * Gets the send pause status switch
     * @return enable flag
     */
    public boolean getEnableFlag(){
        return storagePlugin.get(LocalStorageType.ENABLE);
    }

    /**
     * Save the switch of sending pause status
     * @param flag sending status
     */
    public void saveEnableFlag(boolean flag){
        storagePlugin.save(LocalStorageType.ENABLE,flag);
    }

    /**
     * Gets the send stop status switch
     * @return
     */
    public boolean getOptOutFlag(){
        return storagePlugin.get(LocalStorageType.OPT_OUT);
    }

    /**
     * Save the switch of sending stop status
     * @param flag
     */
    public void saveOptOutFlag(boolean flag){
        storagePlugin.save(LocalStorageType.OPT_OUT,flag);
    }

    /**
     * Gets the saveOnly status switch
     * @return
     */
    public boolean getPausePostFlag(){
        return storagePlugin.get(LocalStorageType.PAUSE_POST);
    }

    /**
     * save the saveOnly status switch
     * @param flag
     */
    public void savePausePostFlag(boolean flag){
        storagePlugin.save(LocalStorageType.PAUSE_POST,flag);
    }

    /**
     * Gets the static public property
     * @return
     */
    public JSONObject getSuperProperties(){
        synchronized (mSuperProperties){
            return storagePlugin.get(LocalStorageType.SUPER_PROPERTIES);
        }
    }

    /**
     * Save static public properties
     * @param superProperties
     */
    public void setSuperProperties(JSONObject superProperties, TimeZone timeZone,boolean shouldThrowException){
        try {
            if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                if (shouldThrowException) {
                    throw new TDDebugException("Set super properties failed. Please refer to the SDK debug log for details.");
                }
                return;
            }
            synchronized (mSuperProperties){
                JSONObject properties = storagePlugin.get(LocalStorageType.SUPER_PROPERTIES);
                TDUtils.mergeJSONObject(superProperties, properties, timeZone);
                storagePlugin.save(LocalStorageType.SUPER_PROPERTIES,properties);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Cancels a public property
     * @param superPropertyName
     */
    public void unsetSuperProperty(String superPropertyName){
        try{
            if (superPropertyName == null) {
                return;
            }
            synchronized (mSuperProperties){
                JSONObject superProperties = storagePlugin.get(LocalStorageType.SUPER_PROPERTIES);
                superProperties.remove(superPropertyName);
                storagePlugin.save(LocalStorageType.SUPER_PROPERTIES,superProperties);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void clearSuperProperties(){
        synchronized (mSuperProperties) {
            storagePlugin.save(LocalStorageType.SUPER_PROPERTIES,new JSONObject());
        }
    }

    public void saveSessionIndex(int index) {
        storagePlugin.save(LocalStorageType.SESSION_ID, index);
    }

    public int getSessionIndex() {
        return storagePlugin.get(LocalStorageType.SESSION_ID);
    }

}
