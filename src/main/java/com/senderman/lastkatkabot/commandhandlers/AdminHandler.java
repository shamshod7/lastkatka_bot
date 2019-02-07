package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.Services;
import com.senderman.lastkatkabot.TempObjects.TgUser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.ArrayList;
import java.util.List;

public class AdminHandler {

    private final LastkatkaBotHandler handler;

    public AdminHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
    }

    public void badneko(Message message) {
        if (!message.isReply())
            return;
        if (handler.blacklist.contains(message.getReplyToMessage().getFrom().getId())) {
            return;
        }
        Services.db().addToBlacklist(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName());
        handler.blacklist.add(message.getReplyToMessage().getFrom().getId());
        handler.sendMessage(message.getChatId(), "\uD83D\uDE3E " + message.getReplyToMessage().getFrom().getUserName() +
                " - плохая киса!");
    }

    public void goodneko(Message message) {
        if (!message.isReply())
            return;
        Services.db().removeFromBlacklist(message.getReplyToMessage().getFrom().getId());
        handler.blacklist.remove(message.getReplyToMessage().getFrom().getId());
        handler.sendMessage(message.getChatId(), "\uD83D\uDE38 " + message.getReplyToMessage().getFrom().getUserName() +
                " - хорошая киса!");
    }

    public void nekos(Message message) {
        var badnekos = new StringBuilder().append("\uD83D\uDE3E <b>Список плохих кис:</b>\n\n");
        var nekoSet = Services.db().getBlackListUsers();
        for (TgUser neko : nekoSet) {
            badnekos.append(neko.getLink()).append("\n");
        }
        handler.sendMessage(Methods.sendMessage(message.getChatId(), badnekos.toString())
                .disableNotification());
    }

    public void loveneko(Message message) {
        Services.db().resetBlackList();
        handler.blacklist.clear();
        handler.sendMessage(message.getChatId(), "❤️ Все кисы - хорошие!");
    }

    public void owner(Message message) {
        if (!message.isReply())
            return;
        if (handler.admins.contains(message.getReplyToMessage().getFrom().getId())) {
            return;
        }
        Services.db().addAdmin(message.getReplyToMessage().getFrom().getId(),
                message.getReplyToMessage().getFrom().getFirstName());
        handler.admins.add(message.getReplyToMessage().getFrom().getId());
        handler.sendMessage(message.getChatId(), "✅" + message.getReplyToMessage().getFrom().getFirstName() +
                " теперь мой хозяин!");
    }

    public void listOwners(Message message) {
        if (!message.isUserMessage()) {
            handler.sendMessage(message.getChatId(), "Команду можно использовать только в лс бота!");
            return;
        }
        var ownersSet = Services.db().getAdmins();
        var markup = new InlineKeyboardMarkup();
        ArrayList<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (TgUser owner : ownersSet) {
            rows.add(List.of(new InlineKeyboardButton()
                    .setText(owner.getName())
                    .setCallbackData(LastkatkaBot.CALLBACK_DELETE_ADMIN + owner.getId())));
        }
        rows.add(List.of(new InlineKeyboardButton()
                .setText("Закрыть меню")
                .setCallbackData(LastkatkaBot.CALLBACK_CLOSE_MENU)));
        markup.setKeyboard(rows);
        handler.sendMessage(Methods.sendMessage(message.getChatId(), "Для удаления админа нажмите на него")
                .setReplyMarkup(markup));
    }

    public void update(Message message) {
        var params = message.getText().split("\n");
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

    public void chats(Message message) {
        if (!message.isUserMessage()) {
            handler.sendMessage(message.getChatId(), "Команду можно использовать только в лс бота!");
            return;
        }
        var markup = new InlineKeyboardMarkup();
        ArrayList<List<InlineKeyboardButton>> rows = new ArrayList<>();
        var chats = Services.db().getAllowedChats();
        for (long chatId : chats.keySet()) {
            rows.add(List.of(new InlineKeyboardButton()
                    .setText(chats.get(chatId))
                    .setCallbackData(LastkatkaBot.CALLBACK_DELETE_CHAT + chatId)));
        }
        rows.add(List.of(new InlineKeyboardButton()
                .setText("Закрыть меню")
                .setCallbackData(LastkatkaBot.CALLBACK_CLOSE_MENU)));
        markup.setKeyboard(rows);
        handler.sendMessage(Methods.sendMessage(message.getChatId(), "Для удаления чата нажите на него")
                .setReplyMarkup(markup));
    }

    public void getinfo(Message message) {
        if (!message.isReply())
            return;
        handler.sendMessage(message.getChatId(), message.getReplyToMessage()
                .toString().replaceAll("[ ,]*\\w+='?null'?", ""));
    }

    public void announce(Message message) {
        handler.sendMessage(message.getChatId(), "Рассылка запущена. На время рассылки бот будет недоступен");
        var text = message.getText();
        text = "\uD83D\uDCE3 <b>Объявление</b>\n\n" + text.split("\\s+", 2)[1];
        var usersIds = Services.db().getAllUsersIds();
        int counter = 0;
        for (int userId : usersIds) {
            try {
                handler.execute(new SendMessage((long) userId, text).enableHtml(true));
                counter++;
            } catch (TelegramApiException e) {
                BotLogger.error("ANNOUNCE", e.toString());
            }
        }
        handler.sendMessage(message.getChatId(), String.format("Объявление получили %1$d/%2$d человек", counter, usersIds.size()));
    }

    public void setupHelp(Message message) {
        handler.sendMessage(Methods.sendMessage(message.getChatId(), Services.botConfig().getSetuphelp()));
    }
}
