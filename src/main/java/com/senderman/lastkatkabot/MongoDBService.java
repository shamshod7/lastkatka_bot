package com.senderman.lastkatkabot;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.TempObjects.TgUser;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.objects.User;

import java.text.SimpleDateFormat;
import java.util.*;

public class MongoDBService implements DBService {
    private final TimeZone timeZone = TimeZone.getTimeZone("Europe/Moscow");
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
        return chatMembersDB.getCollection(String.valueOf(chatId));
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
        var updateDoc = new Document() // maybe works
                .append("$inc", new Document("wins", 1).append("total", 1));

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

    public Map<String, Integer> getStats(int id, String player) {
        int total = 0, wins = 0, bncwins = 0;
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        } else {
            total = doc.getInteger("total");
            wins = doc.getInteger("wins");
            bncwins = doc.getInteger("bncwins");
        }
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("wins", wins);
        stats.put("bncwins", bncwins);
        return stats;
    }

    public void addToBlacklist(int id, String name) {
        blacklist.insertOne(new Document("id", id)
                .append("name", name));
    }

    public void removeFromBlacklist(int id) {
        blacklist.deleteOne(Filters.eq("id", id));
    }

    public Set<TgUser> getBlackListUsers() {
        Set<TgUser> result = new HashSet<>();
        for (Document doc : blacklist.find()) {
            result.add(new TgUser(doc.getInteger("id"), doc.getString("name")));
        }
        return result;
    }

    public Set<Integer> getBlacklistIds() {
        Set<Integer> blacklistSet = new HashSet<>();
        for (Document doc : blacklist.find()) {
            blacklistSet.add(doc.getInteger("id"));
        }
        return blacklistSet;
    }

    public void addAdmin(int id, String name) {
        admins.insertOne(new Document("id", id)
                .append("name", name));
    }

    public void removeAdmin(int id) {
        admins.deleteOne(Filters.eq("id", id));
    }

    public Set<TgUser> getAdmins() {
        Set<TgUser> result = new HashSet<>();
        for (Document doc : admins.find()) {
            result.add(new TgUser(doc.getInteger("id"), doc.getString("name")));
        }
        return result;
    }

    public Set<Integer> getAdminsIds() {
        Set<Integer> adminsSet = new HashSet<>();
        for (Document doc : admins.find()) {
            adminsSet.add(doc.getInteger("id"));
        }
        return adminsSet;
    }

    public Set<Integer> getAllUsersIds() {
        Set<Integer> userIds = new HashSet<>();
        for (String collName : chatMembersDB.listCollectionNames()) {
            for (Document doc : chatMembersDB.getCollection(collName).find()) {
                userIds.add(doc.getInteger("id"));
            }
        }
        for (Document doc : duelstats.find()) {
            userIds.add(doc.getInteger("id"));
        }
        return userIds;
    }

    @Override
    public void addUserToDB(User user, long chatId) {
        var chat = getChatMembersCollection(chatId);
        var doc = chat.find(Filters.eq("id", user.getId())).first();
        if (doc != null) {
            if (!doc.getString("name").equals(user.getFirstName()))
            chat.updateOne(Filters.eq("id", user.getId()),
                    new Document("$set", new Document("name", user.getFirstName())));
        } else {
            chat.insertOne(new Document()
                    .append("name", user.getFirstName())
                    .append("id", user.getId()));
        }
    }

    @Override
    public void removeUserFromDB(User user, long chatId) {
        getChatMembersCollection(chatId).deleteOne(Filters.eq("id", user.getId()));
    }

    @Override
    public List<TgUser> getChatMembers(long chatId) {
        var chat = getChatMembersCollection(chatId);
        ArrayList<TgUser> members = new ArrayList<>();
        for (Document doc : chat.find()) {
            members.add(new TgUser(doc.getInteger("id"), doc.getString("name")));
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
    public Set<Long> getAllowedChatsSet() {
        Set<Long> allowedChats = new HashSet<>();
        for (Document doc : allowedChatsCollection.find()) {
            allowedChats.add(doc.getLong("chatId"));
        }
        return allowedChats;
    }

    @Override
    public void addAllowedChat(long chatId, String title) {
        allowedChatsCollection.insertOne(new Document("chatId", chatId)
                .append("title", title));
    }

    @Override
    public void removeAllowedChat(long chatId) {
        allowedChatsCollection.deleteOne(Filters.eq("chatId", chatId));
    }

    @Override
    public Map<Long, String> getAllowedChats() {
        Map<Long, String> chats = new HashMap<>();
        for (Document doc : allowedChatsCollection.find()) {
            chats.put(doc.getLong("chatId"), doc.getString("title"));
        }
        return chats;
    }

    @Override
    public boolean pairExistsToday(long chatId) {
        var doc = pairs.find(Filters.eq("chatId", chatId)).first();
        if (doc == null)
            return false;
        else {
            var dateFormat = new SimpleDateFormat("yyyyMMdd");
            dateFormat.setTimeZone(timeZone);
            var date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).getTime();
            var today = dateFormat.format(date);
            if (doc.getLong("date") < Long.parseLong(today))
                return false;
            else {
                var hoursFormat = new SimpleDateFormat("HH");
                hoursFormat.setTimeZone(timeZone);
                var hours = Integer.parseInt(hoursFormat.format(date));
                hours = (hours >= 0 && hours < 12) ? 0 : 12;
                return doc.getInteger("hours") == hours;
            }
        }
    }

    @Override
    public void setPair(long chatId, String pair, String history) {
        pairs.deleteOne(Filters.eq("chatId", chatId));
        var date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).getTime();
        var dateFormat = new SimpleDateFormat("yyyyMMdd");
        var hoursFormat = new SimpleDateFormat("HH");
        dateFormat.setTimeZone(timeZone);
        hoursFormat.setTimeZone(timeZone);
        var hours = Integer.parseInt(hoursFormat.format(date));
        hours = (hours >= 0 && hours < 12) ? 0 : 12;
        pairs.insertOne(new Document()
                .append("chatId", chatId)
                .append("pair", pair)
                .append("history", history)
                .append("date", Long.parseLong(dateFormat.format(date)))
                .append("hours", hours));
    }

    @Override
    public String getPairOfTheDay(long chatId) {
        var doc = pairs.find(Filters.eq("chatId", chatId)).first();
        if (doc != null) {
            return "Kun juftligi: " + doc.getString("pair");
        } else
            return null;
    }

    @Override
    public String getPairsHistory(long chatId) {
        var doc = pairs.find(Filters.eq("chatId", chatId)).first();
        return (doc != null) ? doc.getString("history") : null;
    }
}
