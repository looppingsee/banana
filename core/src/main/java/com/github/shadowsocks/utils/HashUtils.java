package com.github.shadowsocks.utils;

import java.security.MessageDigest;

public class HashUtils {

    private static final char[] TARCODE = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z' };

    public static String getHash(String data, String type) {
        String str = "";
        if (data != null) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(type);
                messageDigest.update(data.getBytes());
                str = handleBytes(messageDigest.digest());
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        return str;

    }
    //生成key，这一步是重点
    private static String handleBytes(byte[] data) {
        int i = data.length;
        StringBuilder sb = new StringBuilder(i * 2);
        for (int j = 0; j < i; j++) {
            //对每个字节位运算并且和16进制62做&运算，保证产生的索引在TARCODE的内
            sb.append(TARCODE[(data[j] >> 4 & 0x3D)]);
            sb.append(TARCODE[(data[j] & 0x3D)]);
        }
        return sb.toString();
    }
}