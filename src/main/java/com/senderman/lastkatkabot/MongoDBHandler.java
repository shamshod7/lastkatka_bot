package com.senderman.lastkatkabot;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.HashSet;
import java.util.Set;

public class MongoDBHandler implements DBService {
    private static final MongoClient client = MongoClients.create(System.getenv("database"));
    private static final MongoDatabase database = client.getDatabase("lastkatka");
    private static final MongoCollection<Document> admins = database.getCollection("admins");
    private static final MongoCollection<Document> blacklist = database.getCollection("blacklist");
    private static final MongoCollection<Document> duelstats = database.getCollection("duelstats");

    public void initStats(int id) {
        var doc = new Document("id", id)
                .append("total", 0)
                .append("wins", 0);
        duelstats.insertOne(doc);
    }

    public void winnerToStats(int id) {
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
                .append("$inc", new Document("wins", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
        updateDoc = new Document()
                .append("$inc", new Document("total", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
    }

    public void loserToStats(int id) {
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
                .append("$inc", new Document("total", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
    }

    public String getStats(int id, String player) {
        int total = 0, wins = 0, winrate = 0;
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        } else {
            total = doc.getInteger("total");
            wins = doc.getInteger("wins");
            winrate = 100 * wins / total;
        }
        return player +
                "\nВыиграно игр: " +
                wins +
                "\nВсего игр: " +
                total +
                "\nВинрейт: " +
                winrate +
                "%";
    }

    public void addToBlacklist(int id, String name, Set<Integer> blacklistSet) {
        blacklist.insertOne(new Document("id", id)
                .append("name", name));
        updateBlacklist(blacklistSet);
    }

    public void removeFromBlacklist(int id, Set<Integer> blacklistSet) {
        blacklist.deleteOne(Filters.eq("id", id));
        updateBlacklist(blacklistSet);
    }

    public String getBlackList() {
        var result = new StringBuilder("<b>Список плохих кис:</b>\n\n");
        try (MongoCursor<Document> cursor = blacklist.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                result.append("<a href=\"tg://user?id=")
                        .append(doc.getInteger("id"))
                        .append("\">")
                        .append(doc.getString("name")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;"))
                        .append("</a>\n");
            }
        }
        return result.toString();
    }

    public void updateBlacklist(Set<Integer> blacklistSet) {
        blacklistSet.clear();
        try (MongoCursor<Document> cursor = blacklist.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                blacklistSet.add(doc.getInteger("id"));
            }
        }
    }

    public void resetBlackList(Set<Integer> blacklistSet) {
        blacklistSet.clear();
        blacklist.deleteMany(new Document());
    }

    public void addToAdmins(int id, String name, Set<Integer> adminsSet) {
        admins.insertOne(new Document("id", id)
                .append("name", name));
        updateAdmins(adminsSet);
    }

    public void removeFromAdmins(int id, Set<Integer> adminsSet) {
        admins.deleteOne(Filters.eq("id", id));
        updateAdmins(adminsSet);
    }

    public String getAdmins() {
        var result = new StringBuilder("<b>Админы бота:</b>\n\n");
        try (MongoCursor<Document> cursor = admins.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                result.append("<a href=\"tg://user?id=")
                        .append(doc.getInteger("id"))
                        .append("\">")
                        .append(doc.getString("name")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;"))
                        .append("</a>\n");
            }
        }
        return result.toString();
    }

    public void updateAdmins(Set<Integer> adminsSet) {
        adminsSet.clear();
        try (MongoCursor<Document> cursor = admins.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                adminsSet.add(doc.getInteger("id"));
            }
        }
    }

    public HashSet<Long> getPlayersIds() {
        HashSet<Long> players = new HashSet<>();
        try (MongoCursor<Document> cursor = duelstats.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                players.add(doc.getInteger("id").longValue());
            }
        }
        return players;
    }
}
