package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.Duel;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GamesHandler {
    private final LastkatkaBotHandler handler;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public GamesHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
    }

    private void setCurrentMessage() {
        Message message = handler.getCurrentMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = LastkatkaBotHandler.getValidName(message);
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
                .setReplyMarkup(handler.getMarkupForDuel());

        var sentMessage = handler.sendMessage(sm);
        int duelMessageId = sentMessage.getMessageId();
        var duel = new Duel(sentMessage, handler);
        if (handler.duels.containsKey(chatId)) {
            handler.duels.get(chatId).put(duelMessageId, duel);
        } else {
            Map<Integer, Duel> duelMap = new HashMap<>();
            duelMap.put(duelMessageId, duel);
            handler.duels.put(chatId, duelMap);
        }
    }
}
