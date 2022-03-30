package com.laioffer.jupiter.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entity.LoginRequestBody;
import com.laioffer.jupiter.entity.LoginResponseBody;
import com.laioffer.jupiter.entity.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginServlet",  urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // ObjectMapper mapper = new ObjectMapper();
        // LoginRequestBody body = mapper.readValue(request.getReader(), LoginRequestBody.class);
        LoginRequestBody body = ServletUtil.readRequestBody(LoginRequestBody.class, request);
        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String username;
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            String userId = body.getUserId();
            // 这里也需要加密  因为存在数据库重的是加密后的password
            // 此处是与存在数据库中 encrypt password 进行比对 所以此处也需要加密
            String password = ServletUtil.encryptPassword(body.getUserId(), body.getPassword());
            username = connection.verifyLogin(userId, password);
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        // generate session
        if (!username.isEmpty()) {
            // getSession either get current session or create a session
            // 此处login，为create session
            HttpSession session = request.getSession();
            // 将 user_id 存为 session 的一个 attribute
            session.setAttribute("user_id", body.getUserId());
            // 过期时间
            session.setMaxInactiveInterval(600);
            // tomcat 把session绑定在 request 和 response 上
            // 将response返回给前端的时候 session在header里一起返回给前端了
            LoginResponseBody loginResponseBody = new LoginResponseBody(body.getUserId(), username);
            // 将 loginResponseBody 返回给前端，也就是拿到的userId还有verifyLogin之后得到的username
            response.setContentType("application/json;charset=UTF-8");
            //  ObjectMapper mapper = new ObjectMapper();
            //  mapper.writeValue(response.getWriter(), loginResponseBody);
            response.getWriter().print(new ObjectMapper().writeValueAsString(loginResponseBody));

        } else {    // 如果返回的 username为 "", 表示登陆失败，无法验证身份
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}

