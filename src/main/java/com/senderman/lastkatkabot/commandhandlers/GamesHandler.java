package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.ServiceHolder;
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


    public GamesHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
    }

    private void setCurrentMessage() {
        message = handler.getCurrentMessage();
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

    public void dstats() {
        setCurrentMessage();
        var player = message.getFrom().getFirstName();
        handler.sendMessage(chatId, ServiceHolder.db().getStats(message.getFrom().getId(), player));

    }
}
