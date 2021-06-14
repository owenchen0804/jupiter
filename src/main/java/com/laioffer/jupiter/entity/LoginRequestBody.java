package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// Json -> Java obj
public class LoginRequestBody {
    private final String userId;
    private final String password;

    @JsonCreator
    // JsonCreator annotation： 让 Jackson 用这个constructor 把JSON 格式的对象 转化为 Java Obj
    // JsonProperty annotation： 传进来的 key 如何 map 成 constructor里的参数
    public LoginRequestBody(@JsonProperty("user_id") String userId, @JsonProperty("password") String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }
}
