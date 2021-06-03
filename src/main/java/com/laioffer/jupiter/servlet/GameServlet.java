package com.laioffer.jupiter.servlet;

import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.external.TwitchException;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.laioffer.jupiter.entity.Game;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet(name = "GameServlet", urlPatterns = {"/game"})
public class GameServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 利用前端传入的数据 来初始化 JSONObject
        // 前端发送的是 JSON --> String --> stream --> String --> JSON
        // 互联网传输过程中 前端发送的 JSON 格式文件被转化为 stream 格式，可能一段JSON 需要几个 steam 才能收到
        // Step 1: request.getReader() 获取 request的body, request的body本来是 stream 格式，流数据
        // Step 2: IOUtils.toString() 将 request body(stream)转化为String
        // Step 3: JSONObject 将String 转化为 JSON 格式的 Object
        JSONObject jsonRequest = new JSONObject(IOUtils.toString(request.getReader()));
        String name = jsonRequest.getString("name");
        String developer = jsonRequest.getString("developer");
        String releaseTime = jsonRequest.getString("release_time");
        String website = jsonRequest.getString("website");
        float price = jsonRequest.getFloat("price");

        // 此处把结果打印在console里
        // 后面应该实现 save game to database
        System.out.println("Name is: " + name);
        System.out.println("Developer is: " + developer);
        System.out.println("Release time is: " + releaseTime);
        System.out.println("Website is: " + website);
        System.out.println("Price is: " + price);

        response.setContentType("application/json");    // 给前端的注释 数据的返回格式
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", "ok");   // 这里不是必须的
        response.getWriter().print(jsonResponse);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // response.getWriter().print("Hello World!");

        // 解析 URL 里的 gamename parameter
        // String gamename = request.getParameter("gamename");
        // request.getParameter can only read parameter in url
        // request.getParameter can only be used in doGet method
        // response.getWriter().print("Game is: " + gamename);

        // Method 1:
        // 返回 JSON 格式的数据给前端
        //        response.setContentType("application/json");
        //        JSONObject obj = new JSONObject();
        //        obj.put("name", "World of Warcraft");
        //        obj.put("developer", "Blizzard Entertainment");
        //        obj.put("release_time", "Feb 11, 2005");
        //        obj.put("website", "https://www.worldofwarcraft.com");
        //        obj.put("price", 49.99);
        //        response.getWriter().print(obj);  // 隐含调用了 game.toString()

        // Method 2:
        // 把Java格式的对象 convert成JSONObject的数据
        //        Game game = new Game("World of Warcraft", "Blizzard Entertainment",
        //                "Feb 11, 2005", "https://www.worldofwarcraft.com", 49.99);
        //        // Jackson library 里的 ObjectMapper class 提供了转化 Java Object
        //        ObjectMapper mapper = new ObjectMapper();
        //        response.getWriter().print(mapper.writeValueAsString(game));


        // Get gameName from request URL.
        String gameName = request.getParameter("game_name");
        TwitchClient client = new TwitchClient();

        // Let the client know the returned data is in JSON format.
        response.setContentType("application/json;charset=UTF-8");
        try {
            // Return the dedicated game information if gameName is provided in the request URL, otherwise return the top x games.
            if (gameName != null) {
                response.getWriter().print(new ObjectMapper().writeValueAsString(client.searchGame(gameName)));
            } else {
                response.getWriter().print(new ObjectMapper().writeValueAsString(client.topGames(0)));
            }
        } catch (TwitchException e) {
            throw new ServletException(e);
        }


    }
}