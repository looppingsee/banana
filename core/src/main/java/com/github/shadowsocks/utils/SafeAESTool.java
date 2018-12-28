package com.github.shadowsocks.utils;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SafeAESTool {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static String encrypt(String key,String data){
        // TODO Auto-generated method stub
        String str = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] ret = cipher.doFinal(data.getBytes());
            str = Base64.encodeToString(ret, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String decrypt(String key,String data){
        // TODO Auto-generated method stub
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] retByte = cipher.doFinal(Base64.decode(data,Base64.NO_WRAP));
            return new String(retByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}