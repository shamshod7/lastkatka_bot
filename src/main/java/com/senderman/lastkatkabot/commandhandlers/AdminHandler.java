package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.ServiceHolder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

public class AdminHandler {

    private final LastkatkaBotHandler handler;
    private Message message;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public AdminHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
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
        if (handler.blacklist.contains(message.getReplyToMessage().getFrom().getId())) {
            return;
        }
        ServiceHolder.db().addToBlacklist(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName(),
                handler.blacklist);
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getUserName() +
                " - плохая киса!");
    }

    public void goodneko() {
        setCurrentMessage();
        ServiceHolder.db().removeFromBlacklist(message.getReplyToMessage().getFrom().getId(),
                handler.blacklist);
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getUserName() +
                " хорошая киса!");
    }

    public void nekos() {
        setCurrentMessage();
        handler.sendMessage(chatId, ServiceHolder.db().getBlackList());
    }

    public void loveneko() {
        setCurrentMessage();
        ServiceHolder.db().resetBlackList(handler.blacklist);
        handler.sendMessage(chatId, "Все кисы - хорошие!");
    }

    public void owner() {
        setCurrentMessage();
        if (handler.admins.contains(message.getReplyToMessage().getFrom().getId())) {
            return;
        }
        ServiceHolder.db().addToAdmins(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName(),
                handler.admins);
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getFirstName() +
                " теперь мой хозяин!");
    }

    public void remOwner() {
        setCurrentMessage();
        ServiceHolder.db().removeFromAdmins(message.getReplyToMessage().getFrom().getId(),
                handler.admins);
        handler.sendMessage(chatId, message.getReplyToMessage().getFrom().getFirstName() +
                " больше не мой хозяин!");
    }

    public void listOwners() {
        setCurrentMessage();
        handler.sendMessage(chatId, ServiceHolder.db().getAdmins());
    }

    public void update() {
        setCurrentMessage();
        String[] params = text.split("\n");
        if (params.length < 2) {
            handler.sendMessage(chatId, "Неверное количество аргументов!");
            return;
        }
        var update = new StringBuilder().append("\uD83D\uDCE3 <b>ВАЖНОЕ ОБНОВЛЕНИЕ:</b> \n\n");
        for (int i = 1; i < params.length; i++) {
            update.append("* ").append(params[i]).append("\n");
        }
        for (long chat : handler.allowedChats) {
            handler.sendMessage(chat, update.toString());
        }
    }

    public void getinfo() {
        setCurrentMessage();
        handler.sendMessage(chatId, message.getReplyToMessage().toString());
    }

    public void announce() {
        setCurrentMessage();
        handler.sendMessage(chatId, "Рассылка запущена. На время рассылки бот будет недоступен");
        text = "\uD83D\uDCE3 <b>Объявление</b>\n\n" + text.replace("/announce ", "");
        var players = ServiceHolder.db().getPlayersIds();
        int counter = 0;
        for (long player : players) {
            try {
                handler.execute(new SendMessage(player, text));
                counter++;
            } catch (TelegramApiException e) {
                BotLogger.error("ANNOUNCE", e.toString());
            }
        }
        handler.sendMessage(chatId, "Объявление получили " + counter + " человек");
    }
}
