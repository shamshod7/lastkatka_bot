package com.senderman.lastkatkabot;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.HashSet;
import java.util.Set;

public class MongoDBService implements DBService {
    private final MongoClient client = MongoClients.create(System.getenv("database"));
    private final MongoDatabase database = client.getDatabase("lastkatka");
    private final MongoCollection<Document> admins = database.getCollection("admins");
    private final MongoCollection<Document> blacklist = database.getCollection("blacklist");
    private final MongoCollection<Document> duelstats = database.getCollection("duelstats");
    private final MongoCollection<Document> settings = database.getCollection("settings");
    private final MongoCollection<Document> allowedChatsCollection = database.getCollection("allowedchats");

    public void initStats(int id) {
        var doc = new Document("id", id)
                .append("total", 0)
                .append("wins", 0)
                .append("bncwins", 0);
        duelstats.insertOne(doc);
    }

    public void incDuelWins(int id) {
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

    public void incDuelLoses(int id) {
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
                .append("$inc", new Document("total", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
    }

    @Override
    public void incBNCWin(int id) {
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
                .append("$inc", new Document("bncwins", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
    }

    public String getStats(int id, String player) {
        int total = 0, wins = 0, winrate = 0, bncwins = 0;
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        } else {
            total = doc.getInteger("total");
            wins = doc.getInteger("wins");
            winrate = (total == 0) ? 0 : 100 * wins / total;
            bncwins = doc.getInteger("bncwins");
        }
        return "\uD83D\uDCCA Статистика " +
                player +
                "\nВыиграно игр: " +
                wins +
                "\nВсего игр: " +
                total +
                "\nВинрейт: " +
                winrate +
                "%" +
                "Выиграно в Быки и Коровы: "
                + bncwins;
    }

    public void addToBlacklist(int id, String name, Set<Integer> blacklistSet) {
        blacklist.insertOne(new Document("id", id)
                .append("name", name));
        blacklistSet.add(id);
    }

    public void removeFromBlacklist(int id, Set<Integer> blacklistSet) {
        blacklist.deleteOne(Filters.eq("id", id));
        blacklistSet.remove(id);
    }

    public String getBlackList() {
        var result = new StringBuilder("\uD83D\uDE3E <b>Список плохих кис:</b>\n\n");
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
        adminsSet.add(id);
    }

    public void removeFromAdmins(int id, Set<Integer> adminsSet) {
        admins.deleteOne(Filters.eq("id", id));
        adminsSet.remove(id);
    }

    public String getAdmins() {
        var result = new StringBuilder("\uD83D\uDE0E <b>Админы бота:</b>\n\n");
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

    public Set<Long> getPlayersIds() {
        Set<Long> players = new HashSet<>();
        try (MongoCursor<Document> cursor = duelstats.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                players.add(doc.getInteger("id").longValue());
            }
        }
        return players;
    }

    @Override
    public int getTournamentMessage() {
        var doc = settings.find(Filters.exists("messageId", true)).first();
        if (doc == null)
            return 0;
        return doc.getInteger("messageId");
    }

    @Override
    public void setTournamentMessage(int messageId) {
        var doc = settings.find(Filters.exists("messageId", true)).first();
        if (doc == null)
            settings.insertOne(new Document("messageId", messageId));
        else
            settings.updateOne(Filters.exists("messageId", true),
                    new Document(
                            "$set", new Document("messageId", messageId)
                    ));
    }

    @Override
    public void updateAllowedChats(Set<Long> allowedChats) {
        try (MongoCursor<Document> cursor = allowedChatsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                allowedChats.add(doc.getLong("chatId"));
            }
        }
    }

    @Override
    public void addToAllowedChats(long chatId, Set<Long> allowedChats) {
        allowedChatsCollection.insertOne(new Document("chatId", chatId));
        allowedChats.add(chatId);
    }

    @Override
    public void removeFromAllowedChats(long chatId, Set<Long> allowedChats) {
        allowedChatsCollection.deleteOne(Filters.eq("chatId", chatId));
        allowedChats.remove(chatId);
    }
}