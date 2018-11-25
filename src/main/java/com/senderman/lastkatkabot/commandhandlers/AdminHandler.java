package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.ServiceHolder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

public class AdminHandler {

    public static void badneko(Message message, LastkatkaBotHandler handler) {
        if (!message.isReply())
            return;
        if (handler.blacklist.contains(message.getReplyToMessage().getFrom().getId())) {
            return;
        }
        ServiceHolder.db().addToBlacklist(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName(),
                handler.blacklist);
        handler.sendMessage(message.getChatId(), "\uD83D\uDE3E " + message.getReplyToMessage().getFrom().getUserName() +
                " - плохая киса!");
    }

    public static void goodneko(Message message, LastkatkaBotHandler handler) {
        if (!message.isReply())
            return;
        ServiceHolder.db().removeFromBlacklist(message.getReplyToMessage().getFrom().getId(),
                handler.blacklist);
        handler.sendMessage(message.getChatId(), "\uD83D\uDE38 " + message.getReplyToMessage().getFrom().getUserName() +
                " хорошая киса!");
    }

    public static void nekos(Message message, LastkatkaBotHandler handler) {
        handler.sendMessage(message.getChatId(), ServiceHolder.db().getBlackList());
    }

    public static void loveneko(Message message, LastkatkaBotHandler handler) {
        ServiceHolder.db().resetBlackList(handler.blacklist);
        handler.sendMessage(message.getChatId(), "❤️ Все кисы - хорошие!");
    }

    public static void owner(Message message, LastkatkaBotHandler handler) {
        if (!message.isReply())
            return;
        if (handler.admins.contains(message.getReplyToMessage().getFrom().getId())) {
            return;
        }
        ServiceHolder.db().addToAdmins(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName(),
                handler.admins);
        handler.sendMessage(message.getChatId(), "✅" + message.getReplyToMessage().getFrom().getFirstName() +
                " теперь мой хозяин!");
    }

    public static void remOwner(Message message, LastkatkaBotHandler handler) {
        if (!message.isReply())
            return;
        ServiceHolder.db().removeFromAdmins(message.getReplyToMessage().getFrom().getId(),
                handler.admins);
        handler.sendMessage(message.getChatId(), "\uD83D\uDEAB " + message.getReplyToMessage().getFrom().getFirstName() +
                " больше не мой хозяин!");
    }

    public static void listOwners(Message message, LastkatkaBotHandler handler) {
        handler.sendMessage(message.getChatId(), ServiceHolder.db().getAdmins());
    }

    public static void update(Message message, LastkatkaBotHandler handler) {
        String[] params = message.getText().split("\n");
        if (params.length < 2) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
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

    public static void getinfo(Message message, LastkatkaBotHandler handler) {
        if (!message.isReply())
            return;
        handler.sendMessage(message.getChatId(), message.getReplyToMessage().toString());
    }

    public static void announce(Message message, LastkatkaBotHandler handler) {
        handler.sendMessage(message.getChatId(), "Рассылка запущена. На время рассылки бот будет недоступен");
        var text = message.getText();
        text = "\uD83D\uDCE3 <b>Объявление</b>\n\n" + text.split("\\s+", 2)[1];
        var players = ServiceHolder.db().getPlayersIds();
        int counter = 0;
        for (long player : players) {
            try {
                handler.execute(new SendMessage(player, text).enableHtml(true));
                counter++;
            } catch (TelegramApiException e) {
                BotLogger.error("ANNOUNCE", e.toString());
            }
        }
        handler.sendMessage(message.getChatId(), "Объявление получили " + counter + " человек");
    }

    public static void setupHelp(Message message, LastkatkaBotHandler handler) {
        Methods.sendMessage(message.getChatId(), handler.botConfig.getSetuphelp()).call(handler);
    }
}
