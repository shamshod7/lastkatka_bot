package com.senderman.lastkatkabot;

public class ServiceHolder {

    private static DBService db;

    public static DBService db() {
        return db;
    }

    static void setDbService(DBService db) {
        ServiceHolder.db = db;
    }
}
