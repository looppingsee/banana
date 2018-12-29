package com.github.shadowsocks.bean;

public class NetModeBean {
    public String mode;
    public String name;
    public String desc;
    public boolean isCheck;

    public NetModeBean(String mode, String name, String desc,boolean isCheck) {
        this.mode = mode;
        this.name = name;
        this.desc = desc;
        this.isCheck = isCheck;
    }
}
