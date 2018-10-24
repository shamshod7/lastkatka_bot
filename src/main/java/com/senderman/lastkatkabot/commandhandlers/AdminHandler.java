package com.senderman.lastkatkabot.commandhandlers;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AdminHandler {

    private final LastkatkaBotHandler handler;
    private Message message;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    private final MongoCollection adminsCollection;
    public final MongoCollection blacklistCollection;

    public AdminHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
        adminsCollection = handler.lastkatkaDatabase.getCollection("admins");
        blacklistCollection = handler.lastkatkaDatabase.getCollection("blacklist");
        updateAdmins();
        updateBlacklist();
    }

    private void addToBlacklist(int id, String name) {
        blacklistCollection.insertOne(new Document("id", id)
                .append("name", name));
        updateBlacklist();
    }

    private void removeFromBlacklist(int id) {
        blacklistCollection.deleteOne(Filters.eq("id", id));
        updateBlacklist();
    }

    private String getBlackList() {
        StringBuilder result = new StringBuilder("<b>Список плохих кис:</b>\n\n");
        try (MongoCursor cursor = blacklistCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = (Document) cursor.next();
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

    private void updateBlacklist() {
        handler.blacklist.clear();
        try (MongoCursor cursor = blacklistCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = (Document) cursor.next();
                handler.blacklist.add(doc.getInteger("id"));
            }
        }
    }

    private void resetBlackList() {
        handler.blacklist.clear();
        blacklistCollection.deleteMany(new Document());
    }

    private void addToAdmins(int id, String name) {
        adminsCollection.insertOne(new Document("id", id)
                .append("name", name));
        updateAdmins();
    }

    private void removeFromAdmins(int id) {
        adminsCollection.deleteOne(Filters.eq("id", id));
        updateAdmins();
    }

    private String getAdmins() {
        StringBuilder result = new StringBuilder("<b>Админы бота:</b>\n\n");
        try (MongoCursor cursor = adminsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = (Document) cursor.next();
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

    private void updateAdmins() {
        handler.admins.clear();
        try (MongoCursor cursor = adminsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = (Document) cursor.next();
                handler.admins.add(doc.getInteger("id"));
            }
        }
    }

    private void setCurrentMessage() {
        this.message = handler.getCurrentMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = LastkatkaBotHandler.getValidName(message);
    }

    public void badneko() {
        setCurrentMessage();
        addToBlacklist(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName());
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getUserName() +
                " - плохая киса!");
    }

    public void goodneko() {
        setCurrentMessage();
        removeFromBlacklist(message.getReplyToMessage().getFrom().getId());
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getUserName() +
                " хорошая киса!");
    }

    public void nekos() {
        setCurrentMessage();
        handler.sendMessage(chatId, getBlackList());
    }

    public void loveneko() {
        setCurrentMessage();
        resetBlackList();
        handler.sendMessage(chatId, "Все кисы - хорошие!");
    }

    public void owner() {
        setCurrentMessage();
        addToAdmins(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName());
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getFirstName() +
                " теперь мой хозяин!");
    }

    public void remOwner() {
        setCurrentMessage();
        removeFromAdmins(message.getReplyToMessage().getFrom().getId());
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getFirstName() +
                " больше не мой хозяин!");
    }

    public void listOwners() {
        setCurrentMessage();
        handler.sendMessage(chatId, getAdmins());
    }

    public void announce() {
        setCurrentMessage();
        String[] params = text.split("\n");
        if (params.length != 6) {
            handler.sendMessage(chatId, "Неверное количество аргументов!");
            return;
        }
        String announce = handler.botConfig.getAnnounce()
                .replace("DATE", params[1])
                .replace("UNTIL", params[2])
                .replace("AWARD", params[3])
                .replace("LINK", params[4])
                .replace("VOTE", params[5]);
        handler.sendMessage(handler.botConfig.getLastvegan(), announce);
    }

    public void critical() {
        setCurrentMessage();
        handler.duels.clear();
        handler.sendMessage(chatId, "Все неначатые дуэли были очищены!");
    }
}
