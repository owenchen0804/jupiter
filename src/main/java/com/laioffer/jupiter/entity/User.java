package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

// POJO Class 没有逻辑功能 主要定义一些 fields
@JsonIgnoreProperties(ignoreUnknown = true)
// 对于 json中有 Game中没有的field 就忽略 而不要抛出异常
@JsonInclude(JsonInclude.Include.NON_NULL)
// 对 null 的数据不convert
@JsonDeserialize(builder = User.Builder.class)
// 创建 Game对象的时候使用 builder class 不要用其默认的 constructor
// @JsonIgnoreProperties(ignoreUnknown = true) indicates that other fields in the response can be safely ignored.
// Without this, you’ll get an exception at runtime.
// @JsonInclude(JsonInclude.Include.NON_NULL) indicates that null fields can be skipped and not included.
// @JsonDeserialize indicates that Jackson needs to use Game.Builder when constructing a Game object from JSON strings.
public class User {
    @JsonProperty("user_id")
    private final String userId;
    @JsonProperty("password")
    private String password;
    // password 不是 final 因为前端传进来的是明文 不会直接把用户传进来的密码存在数据库 我们存在数据库的是加密后的密码
    // 防止数据库被黑  用户隐私泄漏
    // 一般前端加密 比较常见 前端发送请求之前就加密好 后端之用存在数据库中
    @JsonProperty("first_name")
    private final String firstName;
    @JsonProperty("last_name")
    private final String lastName;

    private User(Builder builder) {
        this.userId = builder.userId;
        this.password = builder.password;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    // 对于 json中有 Game中没有的field 就忽略 而不要抛出异常
    @JsonInclude(JsonInclude.Include.NON_NULL)
    // 对 null 的数据不convert
    public static class Builder {
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("password")
        private String password;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
