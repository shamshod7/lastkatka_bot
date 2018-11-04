package com.senderman.lastkatkabot;

public class ServiceHolder {

    private static DBService db;

    public static DBService db() {
        return db;
    }

    static void setDBService(DBService db) {
        ServiceHolder.db = db;
    }
}
