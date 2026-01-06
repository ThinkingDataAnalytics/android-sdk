/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.TimeZone;

import cn.thinkingdata.analytics.utils.TDDebugException;
import cn.thinkingdata.analytics.utils.PropertyUtils;
import cn.thinkingdata.core.utils.TDLog;
import cn.thinkingdata.analytics.utils.TDUtils;

/**
 * @author liulongbing
 * @since 2022/9/8
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
     *
     * @param enableTrackOldData enable track old data
     * @param context context
     * @return login id
     */
    public String getLoginId(boolean enableTrackOldData,Context context){
        synchronized (mLoginId) {
            String loginId = storagePlugin.get(LocalStorageType.LOGIN_ID, "");
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
     *
     * @param loginId save loginId
     */
    public void saveLoginId(String loginId){
        try {
            if (TextUtils.isEmpty(loginId)) {
                TDLog.d(TAG, "The account id cannot be empty.");
                return;
            }
            synchronized (mLoginId) {
                if (!loginId.equals(storagePlugin.get(LocalStorageType.LOGIN_ID, ""))) {
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
            storagePlugin.save(LocalStorageType.LOGIN_ID,"");
        }
    }

    /**
     * Clearing an Account ID
     * @param enableTrackOldData enable track old data
     * @param context context
     */
    public void logout(boolean enableTrackOldData,Context context){
        try {
            synchronized (mLoginId) {
                storagePlugin.save(LocalStorageType.LOGIN_ID,"");
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
            return storagePlugin.get(LocalStorageType.IDENTIFY, "");
        }
    }

    /**
     * @param identify save identify id
     */
    public void setIdentifyId(String identify){
        if (TextUtils.isEmpty(identify)) {
            TDLog.w(TAG, "The identity cannot be empty.");
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
            storagePlugin.save(LocalStorageType.IDENTIFY,"");
        }
    }

    /**
     * Gets the send pause status switch
     * @return enable flag
     */
    public boolean getEnableFlag(){
        return storagePlugin.get(LocalStorageType.ENABLE, false);
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
     * @return switch of opt_out
     */
    public boolean getOptOutFlag(){
        return storagePlugin.get(LocalStorageType.OPT_OUT, false);
    }

    /**
     * Save the switch of sending stop status
     * @param flag switch of opt_out
     */
    public void saveOptOutFlag(boolean flag){
        storagePlugin.save(LocalStorageType.OPT_OUT,flag);
    }

    /**
     * Gets the saveOnly status switch
     * @return switch of pause post
     */
    public boolean getPausePostFlag(){
        return storagePlugin.get(LocalStorageType.PAUSE_POST, false);
    }

    /**
     * save the saveOnly status switch
     * @param flag switch of pause post
     */
    public void savePausePostFlag(boolean flag){
        storagePlugin.save(LocalStorageType.PAUSE_POST,flag);
    }

    /**
     * Gets the static public property
     * @return JSONObject super properties
     */
    public JSONObject getSuperProperties(){
        synchronized (mSuperProperties){
            return storagePlugin.get(LocalStorageType.SUPER_PROPERTIES, new JSONObject());
        }
    }

    /**
     * Save static public properties
     * @param superProperties super properties
     * @param timeZone time zone
     * @param shouldThrowException should throw exception
     */
    public void setSuperProperties(JSONObject superProperties, TimeZone timeZone){
        try {
            if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                return;
            }
            synchronized (mSuperProperties){
                JSONObject properties = storagePlugin.get(LocalStorageType.SUPER_PROPERTIES, new JSONObject());
                TDUtils.mergeJSONObject(superProperties, properties, timeZone);
                storagePlugin.save(LocalStorageType.SUPER_PROPERTIES,properties);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Cancels a public property
     * @param superPropertyName property name
     */
    public void unsetSuperProperty(String superPropertyName){
        try{
            if (superPropertyName == null) {
                return;
            }
            synchronized (mSuperProperties){
                JSONObject superProperties = storagePlugin.get(LocalStorageType.SUPER_PROPERTIES, new JSONObject());
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
        return storagePlugin.get(LocalStorageType.SESSION_ID, 0);
    }

}
