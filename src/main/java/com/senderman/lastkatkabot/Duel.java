package com.senderman.lastkatkabot;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Duel {

    private final long chatId;
    private final int messageId;
    private final LastkatkaBotHandler handler;
    private final MongoCollection<Document> duelstats;
    private String messageText;
    private DuelPlayer player1;
    private DuelPlayer player2;

    public Duel(Message message, LastkatkaBotHandler handler, MongoCollection<Document> duelstats) {
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.messageText = message.getText();
        this.handler = handler;
        this.duelstats = duelstats;
    }

    public void addPlayer(int id, String name) { //TODO убрать костыли
        if (player1 != null && player1.id == id) {
            return;
        }
        if (player1 == null) {
            player1 = new DuelPlayer(id, name);
            editMessage(messageText + "\n" + name, getMarkupForDuel());
        } else {
            player2 = new DuelPlayer(id, name);
            handler.duels.get(chatId).remove(messageId);
            start();
        }
    }

    private void start() {
        int randomInt = ThreadLocalRandom.current().nextInt(0, 100);
        var winner = (randomInt < 50) ? player1 : player2;
        var loser = (randomInt < 50) ? player2 : player1;
        var messageText = new StringBuilder();
        messageText.append("<b>Дуэль</b>\n")
                .append(player1.name).append(" vs ").append(player2.name)
                .append("\n\nПротивники разошлись в разные стороны, развернулись лицом друг к другу, и ")
                .append(winner.name).append(" выстрелил первым!\n")
                .append(loser.name).append(" лежит на земле, истекая кровью!\n");
        if (ThreadLocalRandom.current().nextInt(0, 100) < 25) {
            messageText.append("\nНо, умирая, ")
                    .append(loser.name).append(" успевает выстрелить в голову ").append(winner.name).append("! ")
                    .append(winner.name).append(" падает замертво!")
                    .append("\n\n<b>Дуэль окончилась ничьей!</b<");
            loserToStats(player1.id);
            loserToStats(player1.id);
        } else {
            messageText
                    .append("\n\n<b>")
                    .append(winner.name).append(" выиграл дуэль!</b>");
            winnerToStats(winner.id);
            loserToStats(loser.id);
        }
        editMessage(messageText.toString(), null);
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

    private InlineKeyboardMarkup getMarkupForDuel() {
        var markup = new InlineKeyboardMarkup();
        String url = "https://t.me/" +
                handler.getBotUsername() +
                "?start=duel" +
                " " +
                chatId +
                " " +
                messageId;
        var row1 = List.of(new InlineKeyboardButton()
                .setText("Присоединиться")
                .setUrl(url));
        markup.setKeyboard(List.of(row1));
        return markup;
    }

    private void initStats(int id) {
        var doc = new Document("id", id)
                .append("total", 0)
                .append("wins", 0);
        duelstats.insertOne(doc);
    }

    private void winnerToStats(int id) {
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

    private void loserToStats(int id) {
        var doc = duelstats.find(Filters.eq("id", id)).first();
        if (doc == null) {
            initStats(id);
        }
        var updateDoc = new Document()
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
