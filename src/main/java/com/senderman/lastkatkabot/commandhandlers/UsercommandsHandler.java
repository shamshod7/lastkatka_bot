package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;

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

    static InlineKeyboardMarkup getMarkupForPayingRespects() {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                .setText("F")
                .setCallbackData(LastkatkaBotHandler.CALLBACK_PAY_RESPECTS));
        markup.setKeyboard(List.of(row1));
        return markup;
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

        var action = text.replace("/action", "");
        var sm = new SendMessage(chatId, name + action);
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
                .setReplyMarkup(getMarkupForPayingRespects()));
    }

    public void cake() {
        setCurrentMessage();
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                        .setText("Принять")
                        .setCallbackData(LastkatkaBotHandler.CALLBACK_CAKE_OK + text.replace("/cake ", "")),
                new InlineKeyboardButton()
                        .setText("Отказаться")
                        .setCallbackData(LastkatkaBotHandler.CALLBACK_CAKE_NOT + text.replace("/cake ", "")));
        markup.setKeyboard(List.of(row1));
        handler.delMessage(chatId, messageId);
        handler.sendMessage(new SendMessage()
                .setChatId(chatId)
                .setText("\uD83C\uDF82 " + message.getReplyToMessage().getFrom().getFirstName()
                        + ", пользователь " + message.getFrom().getFirstName()
                        + " подарил вам тортик" + text.replace("/cake", ""))
                .setReplyToMessageId(message.getReplyToMessage().getMessageId())
                .setReplyMarkup(markup));
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
    
    public void feedback() {
    	setCurrentMessage();
    	var sb = new StringBuilder()
    		.append("<b>Багрепорт</b>\n\nОт: ")
    		.append("<a href=\"tg://user?id=")
    		.append(message.getFrom().getId())
            .append("\">")
            .append(name)
            .append("</a>\n\n")
            .append(text.replace("/feedback", ""));
        handler.sendMessage((long) LastkatkaBotHandler.mainAdmin, sb.toString());
    }

    public void help() { // /help
        setCurrentMessage();
        if (message.isUserMessage()) {
            var sm = new SendMessage()
                    .setChatId(chatId)
                    .setText(handler.botConfig.getHelp());
            handler.sendMessage(sm);
        } else { // attempt to send help to PM
            try {
                handler.execute(new SendMessage((long) message.getFrom().getId(), handler.botConfig.getHelp())
                        .setParseMode(ParseMode.HTML));
            } catch (TelegramApiException e) {
                handler.sendMessage(new SendMessage(chatId, "Пожалуйста, начните диалог со мной в лс, чтобы я мог отправить вам помощь")
                        .setReplyToMessageId(messageId));
                return;
            }
            handler.sendMessage(new SendMessage(chatId, "Помощь была отправлена вам в лс")
                    .setReplyToMessageId(messageId));
        }
    }
}
