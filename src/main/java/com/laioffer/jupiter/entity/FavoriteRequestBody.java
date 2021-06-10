package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

public class FavoriteRequestBody {
    private final Item favoriteItem;
    // 这里我们只想 把 JSON request body convert 为 Java Object
    // 并不需要把 Java Object convert成 JSON  如果需要双向 convert 用下面这种
    // @JsonProperty("favorite")
    // private final Item favoriteItem;

    // convertor
    // 把 JSON string 变成 FavoriteRequestBody 的对象
    // convert 的时候 调用下面这个 constructor
    @JsonCreator
    public FavoriteRequestBody(@JsonProperty("favorite") Item favoriteItem) {
        this.favoriteItem = favoriteItem;
    }
    // 把 JSON 中 "favorite" 对应的部分 convert 成 Item 这个class type 的 favoriteItem



    public Item getFavoriteItem() {
        return favoriteItem;
    }
}

