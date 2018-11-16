package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.ServiceHolder;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
                .setCallbackData(LastkatkaBot.CALLBACK_PAY_RESPECTS));
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

    public void action() {
        setCurrentMessage();
        handler.delMessage(chatId, messageId);
        if (handler.isInBlacklist(message))
            return;

        if (text.split("\\s+").length == 1) {
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
                .setText("\uD83D\uDD6F Press F to pay respects to " + message.getReplyToMessage().getFrom().getFirstName())
                .setReplyMarkup(getMarkupForPayingRespects()));
    }

    public void cake() {
        setCurrentMessage();
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                        .setText("Принять")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_OK + text.replace("/cake", " ")),
                new InlineKeyboardButton()
                        .setText("Отказаться")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_NOT + text.replace("/cake", " ")));
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

    public void dice() {
        setCurrentMessage();
        int random;
        String[] args = text.split("\\s+", 3);
        if (args.length == 3) {
            try {
                int min = Integer.parseInt(args[1]);
                int max = Integer.parseInt(args[2]);
                random = ThreadLocalRandom.current().nextInt(min, max + 1);
            } catch (NumberFormatException nfe) {
                random = ThreadLocalRandom.current().nextInt(1, 7);
            }
        } else
            random = ThreadLocalRandom.current().nextInt(1, 7);
        handler.sendMessage(new SendMessage()
                .setChatId(chatId)
                .setText("\uD83C\uDFB2 Кубик брошен. Результат: " + random)
                .setReplyToMessageId(messageId));
    }

    public void dstats() {
        setCurrentMessage();
        var player = message.getFrom().getFirstName();
        handler.sendMessage(chatId, ServiceHolder.db().getStats(message.getFrom().getId(), player));

    }

    public void pinlist() {
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
        String sb = "<b>Багрепорт</b>\n\nОт: " +
                "<a href=\"tg://user?id=" +
                message.getFrom().getId() +
                "\">" +
                name +
                "</a>\n\n" +
                text.replace("/feedback ", "");
        handler.sendMessage((long) LastkatkaBot.mainAdmin, sb);
    }

    public void help() {
        setCurrentMessage();
        var sb = new StringBuilder(handler.botConfig.getHelp());
        if (handler.admins.contains(message.getFrom().getId())) { // admins want to get extra help
            sb.append(handler.botConfig.getAdminhelp());
        }
        if (message.isUserMessage()) {
            var sm = new SendMessage()
                    .setChatId(chatId)
                    .setText(sb.toString());
            handler.sendMessage(sm);
        } else { // attempt to send help to PM
            try {
                handler.execute(new SendMessage((long) message.getFrom().getId(), sb.toString())
                        .setParseMode(ParseMode.HTML));
            } catch (TelegramApiException e) {
                handler.sendMessage(new SendMessage(chatId, "Пожалуйста, начните диалог со мной в лс, чтобы я мог отправить вам помощь")
                        .setReplyToMessageId(messageId));
                return;
            }
            handler.sendMessage(new SendMessage(chatId, "✅ Помощь была отправлена вам в лс")
                    .setReplyToMessageId(messageId));
        }
    }
}
