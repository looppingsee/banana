package com.github.shadowsocks.utils;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 保存信息配置类
 */
public class SharedPreferencesHelper {
    private SharedPreferences sharedPreferences;
    private Context mContext;
    /*
     * 保存手机里面的名字
     */private SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("banana",
                Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        mContext = context;
    }

    /**
     * 存储
     */
    public void put(String key, String value) {
        editor.putString(key, SafeStorageUtil.encrypt(mContext, value));
        editor.commit();
    }

    /**
     * 获取保存的数据
     */
    public String getSharedPreference(String key) {
        String tem = sharedPreferences.getString(key, "");
        if(!TextUtils.isEmpty(tem)){
            tem = SafeStorageUtil.decrypt(mContext, tem);
        }
        return tem;
    }

    /**
     * 移除某个key值已经对应的值
     */
    public void remove(String key) {
        editor.remove(key);
        editor.commit();
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        editor.clear();
        editor.commit();
    }

    /**
     * 查询某个key是否存在
     */
    public Boolean contain(String key) {
        return sharedPreferences.contains(key);
    }

    /**
     * 返回所有的键值对
     */
    public Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }
}