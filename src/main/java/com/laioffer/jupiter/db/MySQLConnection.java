package com.laioffer.jupiter.db;

import com.laioffer.jupiter.entity.Item;
import com.laioffer.jupiter.entity.ItemType;
import com.laioffer.jupiter.entity.User;
import com.mysql.cj.jdbc.Driver;

import java.sql.*;
import java.util.*;

public class MySQLConnection {
    private final Connection conn;

    // constructor
    public MySQLConnection() throws MySQLException {
        try {
            // 可以在 运行的时候临时添加 library
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            // 等同于下面这种写法
            // 但下面这种 compile的时候必须 Driver 这个library已经在了
            // com.mysql.cj.jdbc.Driver driver = new Driver();
            // driver.newInstance();

            // 与 MySQL database 建立连接
            conn = DriverManager.getConnection(MySQLDBUtil.getMySQLAddress());
        } catch (Exception e) {
            e.printStackTrace();
            throw new MySQLException("Failed to connect to Database");
        }
    }

    // 断开与数据库的连接
    // 数据库可能只允许有限的程序连接数据库, max maybe 50
    // connection 本身也会耗内存
    // 从数据库读数据 steps 1. connect   step 2. read   step 3. return
    // after step 2 and before step 3 should disconnect database
    // save 内存资源     让程序运行的更快
    // 好处： 1. 不占用数据库端口的资源  2. 释放内存资源，java程序的运行也会更快
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 验证登录: 成功返回username, 不成功返回 ""
    /*public String verifyLogin(String userId, String password) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        String name = "";
        String sql = "SELECT first_name, last_name FROM users WHERE id = ? AND password = ?";
        // PreparedStatement: 1. 方便 2.安全 防止 sql injection
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            // 只能一个 col 一个col 的 set
            statement.setString(1, userId);
            statement.setString(2, password);
            // 执行 sql 语句, 读数据用 executeQuery
            ResultSet rs = statement.executeQuery();    // 结果只有一行 或者没有 所以下面可以用 if
            if (rs.next()) {
                name = rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to save favorite item to Database");
        }
        return name;
    }*/

    // 把新用户加到数据库中
    /*public boolean addUser(User user) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        String sql = "INSERT IGNORE INTO users VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, user.getUserId());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFirstName());
            statement.setString(4, user.getLastName());
            // 执行 sql 语句, 存数据用 executeUpdate
            // 如何知道插入成功还是失败
            return statement.executeUpdate() == 1;  // 是否成功插入一行
            // 此处 == 1 表示插入成功， != 1 表示duplicate
            // executeUpdate 的返回值是说这个 sql语句执行 插入了多少行
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get user information from Database");
        }
    }*/

    public void setFavoriteItem(String userId, Item item) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        // Need to make sure item is added to the database first because the foreign key
        // restriction on item_id(favorite_records) -> id(items)
        saveItem(item);
        // 需要声明一下 加在哪个 cols
        // INSERT IGNORE INTO favorite_records (user_id, item_id) VALUES ('1111', '@#RE2S')
        // 1. 用 string format 替换
        // String template = "INSERT IGNORE INTO favorite_records (user_id, item_id) VALUES (%s, %s)";
        // String sql = String.format(template, userId, item.getId());
        // String template = "INSERT IGNORE INTO favorite_records (user_id) VALUES ('1111; DROP TABLES;')";
        // 数据被恶意删除
        // SELECT * from users WHERE USER_ID = 1111 OR 1 = 1;
        // 等同于 SELECT * from users; 数据泄漏
        // IGNORE 对于 duplicate 数据 ignore  不要抛出异常 也不会 duplicate
        // 不加 IGNORE 会出现 Exception
        String sql = "INSERT IGNORE INTO favorite_records (user_id, item_id) VALUES (?, ?)";
        // PreparedStatement statement = null;
        try {
            // statement = conn.prepareStatement(sql);
            // PreparedStatement 是 sql library 提供的功能 安全
            // can avoid sql injection 恶意注入
            PreparedStatement statement = conn.prepareStatement(sql);
            // 只能一个 col 一个col 的 set
            statement.setString(1, userId);
            statement.setString(2, item.getId());
            // 执行 sql 语句
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to save favorite item to Database");
        }
    }

    public void unsetFavoriteItem(String userId, String itemId) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        // Maybe remove item into items table??
        // removeItem(item);
        // 如果没有别人 favor 这个item 那么有两种选择：
        // 1.立刻删除（延迟用户请求的处理时间 省空间） 2.定期删除（使得用户请求的处理时间更快）
        String sql = "DELETE FROM favorite_records WHERE user_id = ? AND item_id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, itemId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to delete favorite item to Database");
        }
    }

    public void saveItem(Item item) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        // 插入 如果出现 duplicate就 IGNORE
        // 也可以先 check 在不在，在的话才插入
        String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
        // PreparedStatement statement;
        try {
            // statement = conn.prepareStatement(sql);
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, item.getId());
            statement.setString(2, item.getTitle());
            statement.setString(3, item.getUrl());
            statement.setString(4, item.getThumbnailUrl());
            statement.setString(5, item.getBroadcasterName());
            statement.setString(6, item.getGameId());
            statement.setString(7, item.getType().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to add item to Database");
        }
    }

    // 返回这个 user 收藏的所有 item 的 id
    // recommendation helper method  如果已经收藏过了的就不推荐了
    public Set<String> getFavoriteItemIds(String userId) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }

        Set<String> favoriteItems = new HashSet<>();
        String sql = "SELECT item_id FROM favorite_records WHERE user_id = ?";
        // 如果很多 cols 怎么办？
        // orm: sql table <--> java object
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            // Java从DB读数据的方法, 用executeQury()就会返回ResultSet
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String itemId = rs.getString("item_id");
                favoriteItems.add(itemId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite item ids from Database");
        }

        return favoriteItems;
    }

    // 返回这个 user 收藏的所有 item 的 全部信息
    // { video: [ item1, item2, ... ]; clip: [ , , ]; stream: [ , , ] }
    // 1. user --> iid
    // 2. iid --> item
    // 一步走：
    // SELECT * FROM favorite_records INNER JOIN items ON favorite_records.item_id = item.id WHERE user_id = ?
    public Map<String, List<Item>> getFavoriteItems(String userId) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }
        Set<String> favoriteItemIds = getFavoriteItemIds(userId);
        // 返回所有 cols
        String sql = "SELECT * FROM items WHERE id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            for (String itemId : favoriteItemIds) {
                statement.setString(1, itemId);
                // 读操作 用 executeQuery，返回 ResultSet -> 相当于表格
                // 表格只有一列  行数取决于 user 收藏了多少个 item
                ResultSet rs = statement.executeQuery();
                // rs 默认指向 -1 行， 类似 iterator
                // rs 跳到下一行 同时结果不是 null 的话 说明还没有读完
                if (rs.next()) {  // 每次 .next() 跳到下一行
                    // string convert 成 enum 的 ItemType
                    // STREAM (string)  --> STREAM (ItemType) ....
                    ItemType itemType = ItemType.valueOf(rs.getString("type"));
                    Item item = new Item.Builder().id(rs.getString("id")).title(rs.getString("title"))
                            .url(rs.getString("url")).thumbnailUrl(rs.getString("thumbnail_url"))
                            .broadcasterName(rs.getString("broadcaster_name")).gameId(rs.getString("game_id")).type(itemType).build();
                    itemMap.get(rs.getString("type")).add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite items from Database");
        }
        return itemMap;
    }

    // recommendation helper method  推荐 game 给user
    // { video: [ id1, id2, ..]; clip: [ , , , ...]; stream: [ , , , ...] }
    public Map<String, List<String>> getFavoriteGameIds(Set<String> favoriteItemIds) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        Map<String, List<String>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }
        String sql = "SELECT game_id, type FROM items WHERE id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            for (String itemId : favoriteItemIds) {
                statement.setString(1, itemId);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    itemMap.get(rs.getString("type")).add(rs.getString("game_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite game ids from Database");
        }
        return itemMap;
    }
}
