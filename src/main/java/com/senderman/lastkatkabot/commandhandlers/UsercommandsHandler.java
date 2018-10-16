package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

public class UsercommandsHandler {
    private final LastkatkaBotHandler handler;
    private Message message;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public UsercommandsHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
    }

    private void setCurrentMessage() {
        this.message = handler.getCurrentMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = LastkatkaBotHandler.getValidName(message);
    }

    public void action() { // /action
        setCurrentMessage();
        handler.delMessage(chatId, messageId);
        if (handler.isInBlacklist(message))
            return;

        if (text.split(" ").length == 1) {
            return;
        }

        String action = text.replace("/action", "");
        SendMessage sm = new SendMessage(chatId, name + action);
        if (message.isReply()) {
            sm.setReplyToMessageId(message.getReplyToMessage().getMessageId());
        }
        handler.sendMessage(sm);
    }

    public void payRespects() { // /f
        setCurrentMessage();
        handler.delMessage(chatId, messageId);
        handler.sendMessage(new SendMessage()
                .setChatId(chatId)
                .setText("Press F to pay respects to " + message.getReplyToMessage().getFrom().getFirstName())
                .setReplyMarkup(handler.getMarkupForPayingRespects()));
    }

    public void pinlist() { // /pinlist
        setCurrentMessage();
        try {
            handler.execute(new PinChatMessage(chatId, message.getReplyToMessage().getMessageId())
                    .setDisableNotification(true));
        } catch (TelegramApiException e) {
            BotLogger.error("PINMESSAGE", e);
        }
        handler.delMessage(chatId, messageId);
    }

    public void help() { // /help
        setCurrentMessage();
        SendMessage sm = new SendMessage()
                .setChatId(chatId)
                .setText(handler.botConfig.getHelp());
        handler.sendMessage(sm);
    }
}
