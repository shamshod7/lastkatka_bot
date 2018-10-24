package com.senderman.lastkatkabot;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.concurrent.ThreadLocalRandom;

public class Duel {

    private final long chatId;
    private final int messageId;
    private final LastkatkaBotHandler handler;
    private String messageText;
    private DuelPlayer player1;
    private DuelPlayer player2;
    private final MongoCollection duelstats;

    public Duel(Message message, LastkatkaBotHandler handler, MongoCollection duelstats) {
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.messageText = message.getText();
        this.handler = handler;
        this.duelstats = duelstats;
    }

    public void addPlayer(int id, String name) {
        if (player1 != null && player1.id == id) {
            return;
        }
        if (player1 == null) {
            player1 = new DuelPlayer(id, name);
            editMessage(messageText + "\n" + name, handler.getMarkupForDuel());
        } else {
            player2 = new DuelPlayer(id, name);
            new Thread(this::start).start();
        }
    }

    private void start() {
        var random = ThreadLocalRandom.current();
        int randomInt = random.nextInt(0, 100);
        var winner = (randomInt < 50) ? player1 : player2;
        var loser = (randomInt < 50) ? player2 : player1;
        var messageText = new StringBuilder();
        messageText.append("<b>Дуэль</b>\n")
                .append(player1.name).append(" vs ").append(player2.name)
                .append("\n\nПротивники разошлись в разные стороны, развернулись лицом друг к другу, и ")
                .append(winner.name).append(" выстрелил первым!\n")
                .append(loser.name).append(" лежит на земле, истекая кровью!\n");
        if (random.nextInt(0, 100) < 21) {
            messageText.append("\nНо, умирая, ")
                    .append(loser.name).append(" успевает выстрелить в голову ").append(winner.name).append("! ")
                    .append(winner.name).append(" падает замертво!")
                    .append("\n\n<b>Дуэль окончилась ничьей!</b<");
            loserToStats(player1.id, player1.name);
            loserToStats(player1.id, player2.name);
        } else {
            messageText.append(winner.name).append(" выиграл дуэль!");
            winnerToStats(winner.id, winner.name);
            loserToStats(loser.id, loser.name);
        }
        editMessage(messageText.toString(), null);

        handler.duels.get(chatId).remove(messageId);
    }

    private void editMessage(String text, InlineKeyboardMarkup markup) {
        var edt = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .setParseMode(ParseMode.HTML);
        if (markup != null) {
            edt.setReplyMarkup(markup);
        }
        try {
            handler.execute(edt);
        } catch (TelegramApiException e) {
            BotLogger.error("EDIT_DUEL", e.toString());
        }
    }

    private void initStats(int id) {
        var doc = new Document("id", id)
                .append("total", 0)
                .append("wins", 0);
        duelstats.insertOne(doc);
    }

    private void winnerToStats(int id, String name) {
        Document doc = (Document) duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
                .append("$set", new Document("name", name))
                .append("$inc", new Document("wins", 1))
                .append("$inc", new Document("total", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
    }

    private void loserToStats(int id, String name) {
        Document doc = (Document) duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
                .append("$set", new Document("name", name))
                .append("$inc", new Document("total", 1));
        duelstats.updateOne(Filters.eq("id", id), updateDoc);
    }
}

class DuelPlayer {
    final String name;
    final int id;

    DuelPlayer(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
