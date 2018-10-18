package com.senderman.lastkatkabot.commandhandlers;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdminHandler {
    private final MongoClient client;
    private final MongoDatabase lastkatkaDatabase;
    private final MongoCollection blacklistCollection;

    private final LastkatkaBotHandler handler;
    private Message message;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public AdminHandler(LastkatkaBotHandler handler) {
        this.handler = handler;

        client = MongoClients.create(System.getenv("database"));
        lastkatkaDatabase = client.getDatabase("lastkatka");
        blacklistCollection = lastkatkaDatabase.getCollection("blacklist");
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

    public void shell() {
        setCurrentMessage();
        String cmd = text.replace("/shell ", "");
        Runtime run = Runtime.getRuntime();
        try {
            Process pr = run.exec(cmd);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            var result = new StringBuilder();
            String line = "";
            while ((line = buffer.readLine()) != null) {
                result.append(line).append("\n");
            }
            handler.sendMessage(chatId, result.toString());
        } catch (IOException e) {
            handler.sendMessage(chatId, "Фиговый из тебя линуксоид");
        }

    }

    public void getFileFromServer() {
        setCurrentMessage();
        var sm = new SendDocument()
                .setChatId(chatId)
                .setDocument(new File(text.replace("/getfile ", "")));
        try {
            handler.execute(sm);
        } catch (TelegramApiException e) {
            handler.sendMessage(chatId, "Не могу отправить файл, у меня лапки!");
        }
    }
}
