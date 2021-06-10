package com.laioffer.jupiter.servlet;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.entity.Item;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ServletUtil {
    public static void writeItemMap(HttpServletResponse response, Map<String, List<Item>> itemMap) throws IOException {
        // 返回 json 格式
        // UTF-8 （unicode）支持各种语言 英文中文韩文拉丁文....
        // 如果不加 UTF-8 后端返回给前端的数据 可能会是乱码 无法成功解析
        // （比如 get favorite response 中无法解析的字符 会变成 ？？？？）
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(new ObjectMapper().writeValueAsString(itemMap));
        // ObjectMapper().writeValueAsString(): Java Object  -->  JSON
        // Step 1:
        // Map<String, List<Item>> items = client.searchItems(gameId)
        // Step 2:
        // ObjectMapper().writeValueAsString()   item -> JSON
        // String output = new ObjectMapper().writeValueAsString(items);
        // Step 3:
        // response.getWriter().print(output);
    }

    // md5Hex单线加密 可加密 不好解密: Decrypt ---> Encrypt
    // HS256 HS512 既可加密 亦可解密: Decrypt <--> Encrypt
    public static String encryptPassword(String userId, String password) throws IOException {
        return DigestUtils.md5Hex(userId + DigestUtils.md5Hex(password).toLowerCase());     // 两步加密
        // step1: password 加密
        // DigestUtils.md5Hex(password)
        // step2: (username +  加密后的password) 再一次加密
        // DigestUtils.md5Hex(userId + DigestUtils.md5Hex(password).toLowerCase())
    }

    public static <T> T readRequestBody(Class<T> cl, HttpServletRequest request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(request.getReader(), cl);
        } catch (JsonParseException | JsonMappingException e) {
            return null;
        }
    }
}
