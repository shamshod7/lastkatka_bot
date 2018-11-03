package com.senderman.lastkatkabot.commandhandlers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.concurrent.ThreadLocalRandom;

public class GamesHandler {
    private final LastkatkaBotHandler handler;
    private long chatId;
    private Message message;
    private int messageId;
    private String text;
    private String name;

    private MongoCollection<Document> duelstats;

    public GamesHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
        duelstats = handler.lastkatkaDatabase.getCollection("duelstats");
    }

    private void setCurrentMessage() {
        message = handler.getCurrentMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = LastkatkaBotHandler.getValidName(message);
    }

    private void initStats(int id) {
        var doc = new Document("id", id)
                .append("total", 0)
                .append("wins", 0);
        duelstats.insertOne(doc);
    }

    public void dice() {
        setCurrentMessage();
        int random = ThreadLocalRandom.current().nextInt(1, 7);
        handler.sendMessage(new SendMessage()
                .setChatId(chatId)
                .setText("Кубик брошен. Результат: " + random)
                .setReplyToMessageId(messageId));
    }

    public void dstats() {
        setCurrentMessage();
        var playername = message.getFrom().getFirstName();
        int total = 0, wins = 0, winrate = 0;
        var doc = duelstats.find(Filters.eq("id", message.getFrom().getId())).first();
        if (doc == null) {
            initStats(message.getFrom().getId());
        } else {
            total = doc.getInteger("total");
            wins = doc.getInteger("wins");
            winrate = 100 * wins / total;
        }
        String stats = playername +
                "\nВыиграно игр: " +
                wins +
                "\nВсего игр: " +
                total +
                "\nВинрейт: " +
                winrate +
                "%";
        handler.sendMessage(chatId, stats);

    }
}
