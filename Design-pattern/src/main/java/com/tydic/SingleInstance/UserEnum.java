package com.tydic.SingleInstance;

public enum UserEnum {

    HTTP_200(200, "请求成功！"), HTTP_500(500, "请求失败！");

    private Integer code;
    private String msg;


    UserEnum(int i, String s) {
        this.code = i;
        this.msg = s;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
