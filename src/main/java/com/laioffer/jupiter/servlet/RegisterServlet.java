package com.laioffer.jupiter.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entity.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 读 request 数据
        // request.getReader()  返回 request body 对应的 stream 数据
        // step 1: 读取 request body: request.getReader()
        // step 2: 用 ObjectMapper 将 JSON对象 convert 为 Java Object（class类型：FavoriteRequestBody.class）：mapper.readValue()
        // request.getReader() --> Reader 是读取 stream的interface

        //    ObjectMapper mapper = new ObjectMapper();
        //    User user = mapper.readValue(request.getReader(), User.class);

        User user = ServletUtil.readRequestBody(User.class, request);
        if (user == null || user.getUserId().isEmpty() || user.getPassword().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean isUserAdded = false;

        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            // update password: 密码加密  再存数据库
            user.setPassword(ServletUtil.encryptPassword(user.getUserId(), user.getPassword()));
            isUserAdded = connection.addUser(user);
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        if (!isUserAdded) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}

