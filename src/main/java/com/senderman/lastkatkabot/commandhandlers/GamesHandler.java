package com.senderman.lastkatkabot.commandhandlers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.Duel;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GamesHandler {
    private final LastkatkaBotHandler handler;
    private long chatId;
    private Message message;
    private int messageId;
    private String text;
    private String name;

    private MongoCollection duelstats;

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

    public InlineKeyboardMarkup getMarkupForDuel() {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                .setText("Присоединиться")
                .setCallbackData(LastkatkaBotHandler.CALLBACK_JOIN_DUEL));
        markup.setKeyboard(List.of(row1));
        return markup;
    }

    public void dice() {
        setCurrentMessage();
        int random = ThreadLocalRandom.current().nextInt(1, 7);
        handler.sendMessage(new SendMessage()
                .setChatId(chatId)
                .setText("Кубик брошен. Результат: " + random)
                .setReplyToMessageId(messageId));
    }

    public void duel() {
        setCurrentMessage();
        var sm = new SendMessage()
                .setChatId(chatId)
                .setText("Набор на дуэль! Жмите кнопку ниже\nДжойнулись:")
                .setReplyMarkup(getMarkupForDuel());

        var sentMessage = handler.sendMessage(sm);
        int duelMessageId = sentMessage.getMessageId();
        var duel = new Duel(sentMessage, handler, duelstats);
        if (handler.duels.containsKey(chatId)) {
            handler.duels.get(chatId).put(duelMessageId, duel);
        } else {
            Map<Integer, Duel> duelMap = new HashMap<>();
            duelMap.put(duelMessageId, duel);
            handler.duels.put(chatId, duelMap);
        }
    }

    public void dstats() {
        setCurrentMessage();
        String playername = message.getFrom().getFirstName();
        int total = 0, wins = 0, winrate = 0;
        Document doc = (Document) duelstats.find(Filters.eq("id", message.getFrom().getId())).first();
        if (doc == null) {
            initStats(message.getFrom().getId());
        } else {
            total = doc.getInteger("total");
            wins = doc.getInteger("wins");
            winrate = 100 * wins / total;
        }
            var stats = new StringBuilder()
                    .append(playername)
                    .append("\nВыиграно игр: ")
                    .append(wins)
                    .append("\nВсего игр: ")
                    .append(total)
                    .append("\nВинрейт: ")
                    .append(winrate)
                    .append("%");
            handler.sendMessage(chatId, stats.toString());

    }
}
