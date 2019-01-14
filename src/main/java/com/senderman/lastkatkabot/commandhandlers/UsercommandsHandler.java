package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.annimon.tgbotsmodule.api.methods.send.SendPhotoMethod;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.ServiceHolder;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class UsercommandsHandler {

    static InlineKeyboardMarkup getMarkupForPayingRespects() {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                .setText("F")
                .setCallbackData(LastkatkaBot.CALLBACK_PAY_RESPECTS));
        markup.setKeyboard(List.of(row1));
        return markup;
    }

    public static void action(Message message, LastkatkaBotHandler handler) {
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
        if (message.getText().split("\\s+").length == 1) {
            return;
        }

        var action = message.getText().split("\\s+", 2)[1];
        var sm = Methods.sendMessage(message.getChatId(), message.getFrom().getFirstName() + " " + action);
        if (message.isReply()) {
            sm.setReplyToMessageId(message.getReplyToMessage().getMessageId());
        }
        handler.sendMessage(sm);
    }

    public static void payRespects(Message message, LastkatkaBotHandler handler) { // /f
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("\uD83D\uDD6F Press F to pay respects to " + message.getReplyToMessage().getFrom().getFirstName())
                .setReplyMarkup(getMarkupForPayingRespects()));
    }

    public static void cake(Message message, LastkatkaBotHandler handler) {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                        .setText("Принять")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_OK + message.getText().replace("/cake", "")),
                new InlineKeyboardButton()
                        .setText("Отказаться")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_NOT + message.getText().replace("/cake", "")));
        markup.setKeyboard(List.of(row1));
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("\uD83C\uDF82 " + message.getReplyToMessage().getFrom().getFirstName()
                        + ", пользователь " + message.getFrom().getFirstName()
                        + " подарил вам тортик" + message.getText().replace("/cake", ""))
                .setReplyToMessageId(message.getReplyToMessage().getMessageId())
                .setReplyMarkup(markup));
    }

    public static void dice(Message message, LastkatkaBotHandler handler) {
        int random;
        String[] args = message.getText().split("\\s+", 3);
        if (args.length == 3) {
            try {
                int min = Integer.parseInt(args[1]);
                int max = Integer.parseInt(args[2]);
                random = ThreadLocalRandom.current().nextInt(min, max + 1);
            } catch (NumberFormatException nfe) {
                random = ThreadLocalRandom.current().nextInt(1, 7);
            }
        } else if (args.length == 2) {
            int max = Integer.parseInt(args[1]);
            random = ThreadLocalRandom.current().nextInt(1, max + 1);
        } else
            random = ThreadLocalRandom.current().nextInt(1, 7);

        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("\uD83C\uDFB2 Кубик брошен. Результат: " + random)
                .setReplyToMessageId(message.getMessageId()));
    }

    public static void dstats(Message message, LastkatkaBotHandler handler) {
        var player = message.getFrom().getFirstName();
        handler.sendMessage(message.getChatId(), ServiceHolder.db().getStats(message.getFrom().getId(), player));

    }

    public static void pinlist(Message message, LastkatkaBotHandler handler) {
        Methods.Administration.pinChatMessage(message.getChatId(), message.getReplyToMessage().getMessageId())
                .setNotificationEnabled(false).call(handler);
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
    }

    public static void feedback(Message message, LastkatkaBotHandler handler) {
        String bugreport = "⚠️ <b>Багрепорт</b>\n\nОт: " +
                "<a href=\"tg://user?id=" +
                message.getFrom().getId() +
                "\">" +
                message.getFrom().getFirstName() +
                "</a>\n\n" +
                message.getText().replace("/feedback ", "");
        handler.sendMessage((long) handler.botConfig.getMainAdmin(), bugreport);
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("✅ Отправлено разрабу бота")
                .setReplyToMessageId(message.getMessageId()));
    }

    public static void bnchelp(Message message, LastkatkaBotHandler handler) {
        SendPhotoMethod sendPhoto = Methods.sendPhoto()
                .setChatId(message.getChatId())
                .setFile(handler.botConfig.getBncphoto());
        if (message.isReply())
            sendPhoto.setReplyToMessageId(message.getReplyToMessage().getMessageId());
        else
            sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.call(handler);
    }

    public static void help(Message message, LastkatkaBotHandler handler) {
        var sb = new StringBuilder(handler.botConfig.getHelp());
        if (handler.admins.contains(message.getFrom().getId())) { // admins want to get extra help
            sb.append(handler.botConfig.getAdminhelp());
        }
        if (message.getFrom().getId().equals(handler.botConfig.getMainAdmin())) {
            sb.append(handler.botConfig.getMainadminhelp());
        }
        if (message.isUserMessage()) {
            var sm = Methods.sendMessage()
                    .setChatId(message.getChatId())
                    .setText(sb.toString());
            handler.sendMessage(sm);
        } else { // attempt to send help to PM
            try {
                handler.execute(new SendMessage((long) message.getFrom().getId(), sb.toString())
                        .setParseMode(ParseMode.HTML));
            } catch (TelegramApiException e) {
                handler.sendMessage(Methods.sendMessage(message.getChatId(), "Пожалуйста, начните диалог со мной в лс, чтобы я мог отправить вам помощь")
                        .setReplyToMessageId(message.getMessageId()));
                return;
            }
            handler.sendMessage(Methods.sendMessage(message.getChatId(), "✅ Помощь была отправлена вам в лс")
                    .setReplyToMessageId(message.getMessageId()));
        }
    }

    public static void pair(long chatId, LastkatkaBotHandler handler) {
        if (ServiceHolder.db().pairExistsToday(chatId)) {
            var pair = ServiceHolder.db().getPairOfTheDay(chatId);
            pair = (pair != null) ? pair : "Ошибка, попробуйте завтра";
            handler.sendMessage(chatId, pair);
            return;
        }

        var users = ServiceHolder.db().getChatMembers(chatId);
        if (users.size() < 3) {
            handler.sendMessage(chatId, "Недостаточно пользователей для создания пары! Подождите, пока кто-то еще напишет в чат!");
            return;
        }
        var lovearray = handler.botConfig.getLovearray();
        var loveStrings = lovearray[ThreadLocalRandom.current().nextInt(lovearray.length)].split("\n");

        try {
            for (int i = 0; i < loveStrings.length - 1; i++) {
                handler.sendMessage(chatId, loveStrings[i]);
                Thread.sleep(1500);
            }
        } catch (InterruptedException e) {
            BotLogger.error("PAIR", "Ошибка таймера");
        }
        int random1 = ThreadLocalRandom.current().nextInt(users.size());
        int random2;
        do {
            random2 = ThreadLocalRandom.current().nextInt(users.size());
        } while (random1 == random2);

        var user1 = users.get(random1);
        var user2 = users.get(random2);
        var pair = user1.getName() + " ❤ " + user2.getName();
        var history = ServiceHolder.db().getPairsHistory(chatId);
        if (history == null) {
            history = pair;
        } else { // update history
            history = pair + "\n" + history;
            history = history.lines()
                    .limit(10)
                    .collect(Collectors.joining("\n"));
        }
        ServiceHolder.db().setPair(chatId, pair, history);
        handler.sendMessage(chatId, String.format(loveStrings[loveStrings.length - 1], user1.getLink(), user2.getLink()));
    }

    public static void lastpairs(long chatId, LastkatkaBotHandler handler) {
        var history = ServiceHolder.db().getPairsHistory(chatId);
        if (history == null)
            handler.sendMessage(chatId, "В этом чате еще никогда не запускали команду /pair!");
        else
            handler.sendMessage(chatId, "<b>Последние 10 пар:</b>\n\n" + history);
    }
}