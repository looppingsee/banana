package com.github.shadowsocks.utils;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

public class SafeStorageUtil {

    private static String AESKEY;

    /**
     * 获得应用程序包名
     * @param context
     * @return
     */
    private static String getPackageName(Context context) {
        return context.getPackageName();
    }

    /**
     * 设备IMEI号
     * @param mContext
     * @return
     */
    private static String getDeviceID(Context mContext) {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 获取唯一标识
     * @param mContext
     * @return
     */
    private static String getAppUnique(Context mContext){
        return getDeviceID(mContext)+getPackageName(mContext);
    }

    private static String getAESKey(Context context) {
        if (!TextUtils.isEmpty(AESKEY))return AESKEY;
        //取16位作为密钥key
        AESKEY = HashUtils.getHash(getAppUnique(context), "MD5").substring(0, 16);
        return AESKEY;
    }

    public static String encrypt(Context context,String data){
        return SafeAESTool.encrypt(getAESKey(context), data);
    }

    public static String decrypt(Context context,String data){
        return SafeAESTool.decrypt(getAESKey(context), data);
    }


}