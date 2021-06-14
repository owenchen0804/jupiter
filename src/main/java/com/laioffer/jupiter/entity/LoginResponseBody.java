package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

// Java obj -> Json
public class LoginResponseBody {
    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("name")
    private final String name;  // 主要需要返回 user 的名字 给前端 （前端要求）
    // 在LoginServlet成功建立session后通过new LoginResponseBody给constructor传username，然后mapper打印显示到前端成JSON

    // session ID


    public LoginResponseBody(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}
