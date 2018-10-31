package com.senderman.lastkatkabot.commandhandlers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.senderman.lastkatkabot.Duel;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

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

    private InlineKeyboardMarkup getMarkupForDuel(long chatId, int messageId) {
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
                .setText("Набор на дуэль! Жмите кнопку ниже\nДжойнулись:");

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
        joinDuel(chatId, duelMessageId);
        var em = new EditMessageReplyMarkup()
                .setChatId(chatId)
                .setMessageId(duelMessageId)
                .setReplyMarkup(getMarkupForDuel(chatId, duelMessageId));
        try {
            handler.execute(em);
        } catch (TelegramApiException e) {
            BotLogger.error("CREATE DUEL", e.toString());
        }
    }

    public void joinDuel(long duelchat, int msgDuel) {
        setCurrentMessage();
        try {
            handler.duels.get(duelchat).get(msgDuel).addPlayer(message.getFrom().getId(), name);
        } catch (Exception e) {
            handler.sendMessage(chatId, "Эта дуэль устарела!");
        }
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
