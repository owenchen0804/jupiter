package com.laioffer.jupiter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

// 用来创建 table in MySQL database
public class MySQLTableCreator {
    // Run this as a Java application to reset the database.
    // 项目就是这样设计的：会把之前所有的数据都删掉  创建出新的 table 用来  reset 数据库，
    // 所以每次run main function 都要清理数据库把之前的table drop
    // 此处 用 main function  使得这里可以独立的运行 不依赖于 tomcat 其它部分的运行依赖于Tomcat
    public static void main(String[] args) {
        try {
            // Step 1 Connect to MySQL.
            System.out.println("Connecting to " + MySQLDBUtil.getMySQLAddress());
            // 解决 corner case  防止出错
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            // same with:
            // step 1:
            // com.mysql.cj.jdbc.Driver driver = new com.mysql.cj.jdbc.Driver();
            // (but the library must be available when compile, cannot wait to load the library in runtime)
            // step 2:
            // driver.newInstance();
            // 与 MySQL database 建立连接
            Connection conn = DriverManager.getConnection(MySQLDBUtil.getMySQLAddress());

            if (conn == null) {
                return;
            }

            // Step 2 Drop tables in case they exist.
            // DROP TABLE IF EXISTS table_name;
            Statement statement = conn.createStatement();
            // 这里DROP TABLE的顺序  必须先删除 favorite  因为favorite 有foreign key 指向 user 和 item
            String sql = "DROP TABLE IF EXISTS favorite_records";
            statement.executeUpdate(sql);   // update是写操作  还有insert 写操作提倡用 executeUpdate()
            // executeQuery() 读操作 并且有返回值

            sql = "DROP TABLE IF EXISTS items";
            statement.executeUpdate(sql);

            sql = "DROP TABLE IF EXISTS users";
            statement.executeUpdate(sql);

            // Step 3 Create new tables.
            // 这里的顺序  也跟table之间的dependency有关系 必须后创建 favorite
            // 因为favorite 有foreign key 指向 user 和 item
            // 所以drop table也要先删有foreign key的再删它指向的
            // item table 跟 item class(POJO) 里的fields 对应  一共有7个
            // VARCHAR(255) 最大长度为 255个字符的 string
            sql = "CREATE TABLE items ("
                    + "id VARCHAR(255) NOT NULL,"
                    + "title VARCHAR(255),"
                    + "url VARCHAR(255),"
                    + "thumbnail_url VARCHAR(255),"
                    + "broadcaster_name VARCHAR(255),"
                    + "game_id VARCHAR(255),"
                    + "type VARCHAR(255) NOT NULL,"
                    + "PRIMARY KEY (id)"
                    + ")";
            statement.executeUpdate(sql);

            sql = "CREATE TABLE users ("
                    + "id VARCHAR(255) NOT NULL,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "first_name VARCHAR(255),"
                    + "last_name VARCHAR(255),"
                    + "PRIMARY KEY (id)"
                    + ")";
            statement.executeUpdate(sql);

            // 用组合键 "PRIMARY KEY (user_id, item_id) 作为 primary key
            // "last_favor_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            // 自己提供时间 或者用默认的 插入数据的时间
            sql = "CREATE TABLE favorite_records ("
                    + "user_id VARCHAR(255) NOT NULL,"
                    + "item_id VARCHAR(255) NOT NULL,"
                    + "last_favor_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (user_id, item_id),"
                    + "FOREIGN KEY (user_id) REFERENCES users(id),"
                    + "FOREIGN KEY (item_id) REFERENCES items(id)"
                    + ")";
            statement.executeUpdate(sql);

            // Step 4 insert fake user
            sql = "INSERT INTO users VALUES('1111', '3229c1097c00d497a0fd282d586be050', 'John', 'Smith')";
            // 这个 长string 代表加密之后的密码 3229c1097c00d497a0fd282d586be050，防止隐私泄漏
            statement.executeUpdate(sql);
            // 断开与数据库的连接
            conn.close();
            System.out.println("Import done successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// ORM (Object Relation Mode)
// 把一个 java class map 成 MySQL 的table
// Java 内用 JPA（interface）实现
// hibernate 是一种具体的实现思路
// 这里 也可以改成 用 hibernate
