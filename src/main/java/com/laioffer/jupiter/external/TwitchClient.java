package com.laioffer.jupiter.external;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.laioffer.jupiter.entity.Item;
import com.laioffer.jupiter.entity.ItemType;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.entity.Game;

import java.util.*;


public class TwitchClient {
    private static final String TOKEN = "Bearer og8gawqepjzgqxhtkykg2pemg48uen";    // Bearer后面是secret
    private static final String CLIENT_ID = "ztqkrgohrgd4m9tkz6a3dpwqb714nx";
    private static final String TOP_GAME_URL = "https://api.twitch.tv/helix/games/top?first=%s";
    private static final String GAME_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/games?name=%s";
    private static final int DEFAULT_GAME_LIMIT = 20;
    private static final String STREAM_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/streams?game_id=%s&first=%s";
    private static final String VIDEO_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/videos?game_id=%s&first=%s";
    private static final String CLIP_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/clips?game_id=%s&first=%s";
    private static final String TWITCH_BASE_URL = "https://www.twitch.tv/";
    private static final int DEFAULT_SEARCH_LIMIT = 20;


    // Build the request URL which will be used when calling Twitch APIs,
    // e.g. https://api.twitch.tv/helix/games/top when trying to get top games.
    private String buildGameURL(String url, String gameName, int limit) {
        if (gameName.equals("")) {
            // top game
            // https://api.twitch.tv/helix/games/top?first=20
            return String.format(url, limit);
        } else {
            try {
                // Encode special characters in URL, e.g. Rick Sun -> Rick%20Sun
                // 空格在 request header 里代表一段信息的结束
                gameName = URLEncoder.encode(gameName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // getGame
            // 替换占位符
            // https://api.twitch.tv/helix/games?name=among%20us
            return String.format(url, gameName);
        }
    }

    // Similar to buildGameURL, build Search URL that will be used when calling Twitch API.
    // e.g. https://api.twitch.tv/helix/clips?game_id=12924.
    private String buildSearchURL(String url, String gameId, int limit) {
        try {
            gameId = URLEncoder.encode(gameId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // 替换占位符
        // https://api.twitch.tv/helix/streams?game_id=16898&first=20
        return String.format(url, gameId, limit);
    }


    // Send HTTP request to Twitch Backend based on the given URL
    // and returns the body of the HTTP response returned from Twitch backend.
    private String searchTwitch(String url) throws TwitchException {
        // httpclient 用来帮助发送请求
        // input: response
        // lambda expression like anonymous class
        CloseableHttpClient httpclient = HttpClients.createDefault();
        //  这里是先写好了如何handle一个response，写完了这部分逻辑之后，才是真正建立了client的连接

        // Define the response handler to parse and return HTTP response body returned from Twitch
        ResponseHandler<String> responseHandler = response -> {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                System.out.println("Response status: " + response.getStatusLine().getReasonPhrase());
                throw new TwitchException("Failed to get result from Twitch API");
            }
            // get response body
            HttpEntity entity = response.getEntity();
            // System.out.println(entity);
            if (entity == null) {
                throw new TwitchException("Failed to get result from Twitch API");
            }
            // 把 response body 的整体内容变成一个 JSONObject
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));
            // 我们需要的信息是 data 这个 key 所对应的 array
            return obj.getJSONArray("data").toString();
        };
        //  上面规定好了handler要做的，现在才是建立client, 并发送请求
        //  用 httpclient 发送请求
        try {
            // Define the HTTP request, TOKEN and CLIENT_ID are
            // used for user authentication on Twitch backend
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", TOKEN);
            request.setHeader("Client-Id", CLIENT_ID);
            return httpclient.execute(request, responseHandler);    //  这句话才是建立连接，用responseHandler来接
        } catch (IOException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to get result from Twitch API");
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Convert JSON format data returned from Twitch to an Arraylist of Game objects
    private List<Game> getGameList(String data) throws TwitchException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 把 JSON 格式的array convert 为Java Object
            // 如果无法一一对应会出现JSONException
            // 后面一个参数（Game[].class）的意思是 convert 成 Game 这个 class 组成的 array
            // JSON -> Java Object Game
            Game[] games = mapper.readValue(data, Game[].class);    //  这里就是Deserialize
            return Arrays.asList(games);
            // return Arrays.asList(mapper.readValue(data, Game[].class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to parse game data from Twitch API");
        }
    }

    // Integrate searchTwitch() and getGameList() together, returns the top x popular games from Twitch.
    public List<Game> topGames(int limit) throws TwitchException {
        if (limit <= 0) {
            limit = DEFAULT_GAME_LIMIT;
        }
        return getGameList(searchTwitch(buildGameURL(TOP_GAME_URL, "", limit)));
    }

    // Integrate search() and getGameList() together, returns the dedicated game based on the game name.
    public Game searchGame(String gameName) throws TwitchException {
        List<Game> gameList = getGameList(searchTwitch(buildGameURL(GAME_SEARCH_URL_TEMPLATE, gameName, 0)));
        if (gameList.size() != 0) {
            return gameList.get(0);
        }
        return null;
    }



    // Similar to getGameList, convert the json data returned from Twitch to a list of Item objects.
    private List<Item> getItemList(String data) throws TwitchException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Item[].class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to parse item data from Twitch API");
        }
    }

    // Returns the top x streams based on game ID.
    private List<Item> searchStreams(String gameId, int limit) throws TwitchException {
        List<Item> streams = getItemList(searchTwitch(buildSearchURL(STREAM_SEARCH_URL_TEMPLATE, gameId, limit)));
        // Step 1:
        // String url = buildSearchURL(STREAM_SEARCH_URL_TEMPLATE, gameId, limit);
        // Step 2:
        // String data = searchTwitch(url);
        // Step 3:
        // List<Item> streams = getItemList(data);
        for (Item item : streams) {
            item.setType(ItemType.STREAM);
            item.setUrl(TWITCH_BASE_URL + item.getBroadcasterName());
        }
        return streams;
    }

    // Returns the top x clips based on game ID.
    private List<Item> searchClips(String gameId, int limit) throws TwitchException {
        List<Item> clips = getItemList(searchTwitch(buildSearchURL(CLIP_SEARCH_URL_TEMPLATE, gameId, limit)));
        for (Item item : clips) {
            item.setType(ItemType.CLIP);
        }
        return clips;
    }

    // Returns the top x videos based on game ID.
    private List<Item> searchVideos(String gameId, int limit) throws TwitchException {
        List<Item> videos = getItemList(searchTwitch(buildSearchURL(VIDEO_SEARCH_URL_TEMPLATE, gameId, limit)));
        for (Item item : videos) {
            item.setType(ItemType.VIDEO);
        }
        return videos;
    }

    // 只需调用一个 method  传入不同的参数即可
    // 不用去 分别调用 三个不同的 method
    public List<Item> searchByType(String gameId, ItemType type, int limit) throws TwitchException {
        List<Item> items = Collections.emptyList();

        switch (type) {
            case STREAM:
                items = searchStreams(gameId, limit);
                break;
            case VIDEO:
                items = searchVideos(gameId, limit);
                break;
            case CLIP:
                items = searchClips(gameId, limit);
                break;
        }

        // Update gameId for all items. GameId is used by recommendation function
        for (Item item : items) {
            item.setGameId(gameId); // why setting gameId gain? 不是input吗？？
            //  返回的item本身不带gameId，需要通过item自己的setter来设置出来
        }
        return items;
    }

    // ["Stream" : ... ; "Video" : ... ; "Clip" : ...]
    public Map<String, List<Item>> searchItems(String gameId) throws TwitchException {
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), searchByType(gameId, type, DEFAULT_SEARCH_LIMIT));
        }
        return itemMap;
    }


}
