package com.senderman.lastkatkabot;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.TempObjects.TgUser;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.objects.User;

import java.text.SimpleDateFormat;
import java.util.*;

public class MongoDBService implements DBService {
    private final MongoClient client = MongoClients.create(System.getenv("database"));
    private final MongoDatabase lastkatkaDB = client.getDatabase("lastkatka");
    private final MongoDatabase chatMembersDB = client.getDatabase("chatmembers");
    private final MongoCollection<Document> admins = lastkatkaDB.getCollection("admins");
    private final MongoCollection<Document> blacklist = lastkatkaDB.getCollection("blacklist");
    private final MongoCollection<Document> duelstats = lastkatkaDB.getCollection("duelstats");
    private final MongoCollection<Document> settings = lastkatkaDB.getCollection("settings");
    private final MongoCollection<Document> pairs = lastkatkaDB.getCollection("pairs");
    private final MongoCollection<Document> allowedChatsCollection = lastkatkaDB.getCollection("allowedchats");

    private MongoCollection<Document> getChatMembersCollection(long chatId) {
        var collection = chatMembersDB.getCollection(String.valueOf(chatId));
        if (collection == null) {
            chatMembersDB.createCollection(String.valueOf(chatId));
            collection = chatMembersDB.getCollection(String.valueOf(chatId));
        }
        return collection;
    }

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
                "\nВыиграно дуэлей: " +
                wins +
                "\nВсего дуэлей сыграно: " +
                total +
                "\nВинрейт: " +
                winrate +
                "%" +
                "\n\n\uD83D\uDC2E Выиграно в Быки и Коровы: "
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

    public Set<TgUser> getBlackList() {
        Set<TgUser> result = new HashSet<>();
        try (MongoCursor<Document> cursor = blacklist.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                result.add(new TgUser(doc.getInteger("id"), doc.getString("name")));
            }
        }
        return result;
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

    public Set<TgUser> getAdmins() {
        Set<TgUser> result = new HashSet<>();
        try (MongoCursor<Document> cursor = admins.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                result.add(new TgUser(doc.getInteger("id"), doc.getString("name")));
            }
        }
        return result;
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

    public Set<Integer> getPlayersIds() {
        Set<Integer> players = new HashSet<>();
        try (MongoCursor<Document> cursor = duelstats.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                players.add(doc.getInteger("id"));
            }
        }
        return players;
    }

    @Override
    public void addUserToDB(User user, long chatId) {
        var chat = getChatMembersCollection(chatId);
        var doc = chat.find(Filters.eq("id", user.getId())).first();
        if (doc != null)
            return;
        chat.insertOne(new Document()
                .append("name", user.getFirstName())
                .append("id", user.getId()));
    }

    @Override
    public void removeUserFromDB(User user, long chatId) {
        getChatMembersCollection(chatId).deleteOne(Filters.eq("id", user.getId()));
    }

    @Override
    public List<TgUser> getChatMembers(long chatId) {
        var chat = getChatMembersCollection(chatId);
        ArrayList<TgUser> members = new ArrayList<>();
        try (MongoCursor<Document> cursor = chat.find().iterator()) {
            while (cursor.hasNext()) {
                var doc = cursor.next();
                members.add(new TgUser(doc.getInteger("id"), doc.getString("name")));
            }
        }
        return members;
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

    @Override
    public boolean pairExistsToday(long chatId) {
        var doc = pairs.find(Filters.eq("chatId", chatId)).first();
        if (doc == null)
            return false;
        else {
            var format = new SimpleDateFormat("yyyyMMdd");
            var today = format.format(new Date());
            return doc.getLong("date") == Long.parseLong(today);
        }
    }

    @Override
    public void setPair(long chatId, String pair, String history) {
        pairs.deleteOne(Filters.eq("chatId", chatId));
        var format = new SimpleDateFormat("yyyyMMdd");
        pairs.insertOne(new Document()
                .append("chatId", chatId)
                .append("pair", pair)
                .append("history", history)
                .append("date", Long.parseLong(format.format(new Date()))));
    }

    @Override
    public String getPairOfTheDay(long chatId) {
        var doc = pairs.find(Filters.eq("chatId", chatId)).first();
        if (doc != null) {
            return "Пара дня: " + doc.getString("pair");
        } else
            return null;
    }

    @Override
    public String getPairsHistory(long chatId) {
        var doc = pairs.find(Filters.eq("chatId", chatId)).first();
        return (doc != null) ? doc.getString("history") : null;
    }
}