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
     * 获取账号ID
     * @return
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
     * 保存账号ID
     * @param loginId
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
     * 清除账号ID
     */
    public void clearLoginId(){
        synchronized (mLoginId){
            storagePlugin.save(LocalStorageType.LOGIN_ID,null);
        }
    }

    /**
     * 清除账号ID
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
     * 获取访客ID
     * @return
     */
    public String getIdentifyId(){
        synchronized (mIdentifyId){
            return storagePlugin.get(LocalStorageType.IDENTIFY);
        }
    }

    /**
     * 保存访客ID
     * @param identify
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
     * 清除访客ID
     */
    public void clearIdentify(){
        synchronized (mIdentifyId) {
            storagePlugin.save(LocalStorageType.IDENTIFY,null);
        }
    }

    /**
     * 获取发送暂停状态开关
     * @return
     */
    public boolean getEnableFlag(){
        return storagePlugin.get(LocalStorageType.ENABLE);
    }

    /**
     * 保存发送暂停状态开关
     * @param flag
     */
    public void saveEnableFlag(boolean flag){
        storagePlugin.save(LocalStorageType.ENABLE,flag);
    }

    /**
     * 获取发送停止状态开关
     * @return
     */
    public boolean getOptOutFlag(){
        return storagePlugin.get(LocalStorageType.OPT_OUT);
    }

    /**
     * 保存发送停止状态开关
     * @param flag
     */
    public void saveOptOutFlag(boolean flag){
        storagePlugin.save(LocalStorageType.OPT_OUT,flag);
    }

    /**
     * 获取发送saveOnly状态开关
     * @return
     */
    public boolean getPausePostFlag(){
        return storagePlugin.get(LocalStorageType.PAUSE_POST);
    }

    /**
     * 保存发送saveOnly状态开关
     * @param flag
     */
    public void savePausePostFlag(boolean flag){
        storagePlugin.save(LocalStorageType.PAUSE_POST,flag);
    }

    /**
     * 获取静态公共属性
     * @return
     */
    public JSONObject getSuperProperties(){
        synchronized (mSuperProperties){
            return storagePlugin.get(LocalStorageType.SUPER_PROPERTIES);
        }
    }

    /**
     * 保存静态公共属性
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
     * 取消某一个公共属性
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

    /**
     * 清除公共属性
     */
    public void clearSuperProperties(){
        synchronized (mSuperProperties) {
            storagePlugin.save(LocalStorageType.SUPER_PROPERTIES,new JSONObject());
        }
    }

}
