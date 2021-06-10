package com.laioffer.jupiter.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class MySQLDBUtil {
    private static final String INSTANCE = "laiproject-instance-2.cmccrypesdie.us-east-2.rds.amazonaws.com";
    // 如果是本机的 MySQL
    // private static final String INSTANCE = "localhost"；
    // http://localhost:3306
    private static final String PORT_NUM = "3306";
    private static final String DB_NAME = "jupiter"; //  这里叫什么名字都可以 不需要跟AWS上的名字一致
    public static String getMySQLAddress() throws IOException {
        Properties prop = new Properties();
        String propFileName = "config.properties";

        // 找到propFileName文件  然后打开文件  读里面的内容
        // 为什么是 stream? 万一文件很大
        // 读网络传输数据的时候 如果不确定有多长 需要用 stream 来读 一段一段来处理  fileIO
        InputStream inputStream = MySQLDBUtil.class.getClassLoader().getResourceAsStream(propFileName);
        // 这里用的class是MySQLDBUtil，但其实随便任何class比如Item也是可以的，这里只需要
        // specify一个class就可以
        prop.load(inputStream);


        String username = prop.getProperty("user");
        String password = prop.getProperty("password");
        return String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s&autoReconnect=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                INSTANCE, PORT_NUM, DB_NAME, username, password);
    }

}

