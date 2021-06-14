package com.laioffer.jupiter.recommendation;

import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entity.Game;
import com.laioffer.jupiter.entity.Item;
import com.laioffer.jupiter.entity.ItemType;
import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.external.TwitchException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemRecommender {
    // 最多选3个game
    private static final int DEFAULT_GAME_LIMIT = 3;
    // 每个game最多推荐10个
    private static final int DEFAULT_PER_GAME_RECOMMENDATION_LIMIT = 10;
    // 总共最多推荐20个
    private static final int DEFAULT_TOTAL_RECOMMENDATION_LIMIT = 20;

    // recommendByTopGames method to ItemRecommender to handle recommendation when the user is not logged in.
    // The recommendation is purely based-on top games returned by Twitch.
    private List<Item> recommendByTopGames(ItemType type, List<Game> topGames) throws RecommendationException {
        List<Item> recommendedItems = new ArrayList<>();
        TwitchClient client = new TwitchClient();

        outerloop:
        for (Game game : topGames) {
            List<Item> items;
            try {
                items = client.searchByType(game.getId(), type, DEFAULT_TOTAL_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result");
            }
            // check 是否已经达到 recommendation 的上限
            // 15 + 8
            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    break outerloop;
                }
                recommendedItems.add(item);
            }
        }
        return recommendedItems;
    }

    // ItemType: maybe video
    private List<Item> recommendByFavoriteHistory(Set<String> favoriteItemIdx, List<String> favoriteGameIdx,
                                                  ItemType type/*video*/) throws RecommendationException {
        // Step 1: 将 list convert 成 map
        // favoriteGameIdx -> [2345, 1234, 1234] -> {1234 : 2, 2345 : 1}
        // Function.identity() -> 定义 key
        // same as:  str -> str
        // String someFunctions(String str) { return str;}
        // Collectors.counting() -> 定义 value
        // Collectors.groupingBy() -> 相同的元素分成一组
        Map<String, Long> favoriteGameIdByCount = favoriteGameIdx.parallelStream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        // favoriteGameIds -> [game, game, game] -> {1234 : 2, 2345 : 1}
        // game -> id, price, url, link  (can use sum/average/counting ... as value)
        // 1234 -> 0.5
        // 1234 -> 0.8

        // Step 2: 排序
        // 2.1 先转换成 list 可以排序
        List<Map.Entry<String, Long>> sortedFavoriteGameIdListByCount = new ArrayList<>(
                favoriteGameIdByCount.entrySet()
        );
        // 2.2 排序
//        sortedFavoriteGameIdListByCount.sort((Map.Entry<String, Long> e1, Map.Entry<String, Long> e2)
//                -> Long.compare(e2.getValue(), e1.getValue()));
        sortedFavoriteGameIdListByCount.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

        // preprocessing 只保留三个 gameID
        if (sortedFavoriteGameIdListByCount.size() > DEFAULT_GAME_LIMIT) {
            sortedFavoriteGameIdListByCount = sortedFavoriteGameIdListByCount.subList(0, DEFAULT_GAME_LIMIT);
        }

        // Step 2: searchByType with top gameIDs collected from user favorite list
        List<Item> recommendItems = new ArrayList<>();
        TwitchClient client = new TwitchClient();
        outerloop:
        for (Map.Entry<String, Long> favoriteGame : sortedFavoriteGameIdListByCount) {
            List<Item> items;
            try {
                items = client.searchByType(favoriteGame.getKey(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result");
            }

            for (Item item : items) {
                if (recommendItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    break outerloop;
                }
                if (!favoriteItemIdx.contains(item.getId())) {
                    recommendItems.add(item);
                }
            }
        }
        return recommendItems;
    }

    public Map<String, List<Item>> recommendItemByUser(String userId) throws RecommendationException {
        Map<String, List<Item>> recommendItemMap = new HashMap<>();
        Set<String> favoriteItemIds;    // [aaaa, bbbb, cccc, dddd]
        Map<String, List<String>> favoriteGameIds;  // key: itemtype  value: gameIds   {"video": 1234,1234,2345; "stream": 3456; "clip": 1232}
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            favoriteItemIds = connection.getFavoriteItemIds(userId);
            favoriteGameIds = connection.getFavoriteGameIds(favoriteItemIds);
        } catch (MySQLException e) {
            throw new RecommendationException("Failed to get user favorite history for recommendation");
        } finally {
            connection.close();
        }
        for (Map.Entry<String, List<String>> entry : favoriteGameIds.entrySet()) {
            if (entry.getValue().size() == 0) {
                TwitchClient client = new TwitchClient();
                List<Game> topGames;
                try {
                    topGames = client.topGames(DEFAULT_GAME_LIMIT);
                } catch (TwitchException e) {
                    throw new RecommendationException("Failed to get game data for recommendation");
                }
                recommendItemMap.put(entry.getKey(), recommendByTopGames(ItemType.valueOf(entry.getKey()), topGames));
            } else {
                recommendItemMap.put(entry.getKey(), recommendByFavoriteHistory(favoriteItemIds, entry.getValue(), ItemType.valueOf(entry.getKey())));
            }
        }
        return recommendItemMap;
    }

    public Map<String, List<Item>> recommendItemsByDefault() throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        TwitchClient client = new TwitchClient();
        List<Game> topGames;
        try {
            topGames = client.topGames(DEFAULT_GAME_LIMIT);
        } catch (TwitchException e) {
            throw new RecommendationException("Failed to get game data for recommendation");
        }

        for (ItemType type : ItemType.values()) {
            recommendedItemMap.put(type.toString(), recommendByTopGames(type, topGames));
        }
        return recommendedItemMap;
    }
}
