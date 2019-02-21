package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.Services;
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

    private final LastkatkaBotHandler handler;

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

    public void action(Message message) {
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

    public void payRespects(Message message) { // /f
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("\uD83D\uDD6F respekt berish uchun f knopkasini bosing - " + message.getReplyToMessage().getFrom().getFirstName())
                .setReplyMarkup(getMarkupForPayingRespects()));
    }

    public void cake(Message message) {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                        .setText("Qabul qilish")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_OK + message.getText().replace("/cake", "")),
                new InlineKeyboardButton()
                        .setText("Inkor etish")
                        .setCallbackData(LastkatkaBot.CALLBACK_CAKE_NOT + message.getText().replace("/cake", "")));
        markup.setKeyboard(List.of(row1));
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("\uD83C\uDF82 " + message.getReplyToMessage().getFrom().getFirstName()
                        + ", foydalanuvchi " + message.getFrom().getFirstName()
                        + " sizga tort sovg'a qildi" + message.getText().replace("/cake", ""))
                .setReplyToMessageId(message.getReplyToMessage().getMessageId())
                .setReplyMarkup(markup));
    }

    public void dice(Message message) {
        int random;
        var args = message.getText().split("\\s+", 3);
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
                .setText("\uD83C\uDFB2 Kubik tashlandi. Natija: " + random)
                .setReplyToMessageId(message.getMessageId()));
    }

    public void dstats(Message message) {
        var player = message.getFrom().getFirstName();
        var stats = Services.db().getStats(message.getFrom().getId(), player);
        var wins = stats.get("wins");
        var total = stats.get("total");
        var text = "\uD83D\uDCCA Statistika " +
                player +
                "\nG'alaba qilingan duellar soni: " +
                wins +
                "\nUmumiy o'ynalgan duellar soni: " +
                total +
                "\nYutuq: " +
                ((total == 0) ? 0 : 100 * wins / total) + "%" +
                "\n\n\uD83D\uDC2E *Bir sonni o'yladim* o'yinidagi g'alabalar soni: "
                + stats.get("bncwins");
        handler.sendMessage(message.getChatId(), text);

    }

    public void pinlist(Message message) {
        if (!isFromWwBot(message))
            return;
        Methods.Administration.pinChatMessage(message.getChatId(), message.getReplyToMessage().getMessageId())
                .setNotificationEnabled(false).call(handler);
        Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(handler);
    }

    public void feedback(Message message) {
        String bugreport = "⚠️ <b>BagReport</b>\n\nОт: " +
                "<a href=\"tg://user?id=" +
                message.getFrom().getId() +
                "\">" +
                message.getFrom().getFirstName() +
                "</a>\n\n" +
                message.getText().replace("/feedback ", "");
        handler.sendMessage((long) Services.botConfig().getMainAdmin(), bugreport);
        handler.sendMessage(Methods.sendMessage()
                .setChatId(message.getChatId())
                .setText("✅ Bot xo'jayiniga jo'natildi")
                .setReplyToMessageId(message.getMessageId()));
    }

    public void bnchelp(Message message) {
        var sendPhoto = Methods.sendPhoto()
                .setChatId(message.getChatId())
                .setFile(Services.botConfig().getBncphoto());
        if (message.isReply())
            sendPhoto.setReplyToMessageId(message.getReplyToMessage().getMessageId());
        else
            sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.call(handler);
    }

    public void help(Message message) {
        var sb = new StringBuilder(Services.botConfig().getHelp());
        if (handler.admins.contains(message.getFrom().getId())) { // admins want to get extra help
            sb.append(Services.botConfig().getAdminhelp());
        }
        if (message.getFrom().getId().equals(Services.botConfig().getMainAdmin())) {
            sb.append(Services.botConfig().getMainadminhelp());
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
                handler.sendMessage(Methods.sendMessage(message.getChatId(), "Iltimos sizga yordam bera olishim uchun menga lichkada start bering.")
                        .setReplyToMessageId(message.getMessageId()));
                return;
            }
            handler.sendMessage(Methods.sendMessage(message.getChatId(), "✅ Yordam lichkaga jo'natildi.")
                    .setReplyToMessageId(message.getMessageId()));
        }
    }

    public void pair(long chatId) {
        if (Services.db().pairExistsToday(chatId)) {
            var pair = Services.db().getPairOfTheDay(chatId);
            pair = (pair != null) ? pair : "Xatolik ertaga yana urinib ko'ring.";
            handler.sendMessage(chatId, pair);
            return;
        }

        var users = Services.db().getChatMembers(chatId);
        if (users.size() < 3) {
            handler.sendMessage(chatId, "Juftlarni yaratilishi uchun azolar soni yetarli emas! Yana kimdir guruhga yozishini kuting!");
            return;
        }
        var lovearray = Services.botConfig().getLovearray();
        var loveStrings = lovearray[ThreadLocalRandom.current().nextInt(lovearray.length)].split("\n");

        try {
            for (int i = 0; i < loveStrings.length - 1; i++) {
                handler.sendMessage(chatId, loveStrings[i]);
                Thread.sleep(1500);
            }
        } catch (InterruptedException e) {
            BotLogger.error("PAIR", "Taymer xatoligi");
        }
        int random1 = ThreadLocalRandom.current().nextInt(users.size());
        int random2;
        do {
            random2 = ThreadLocalRandom.current().nextInt(users.size());
        } while (random1 == random2);

        var user1 = users.get(random1);
        var user2 = users.get(random2);
        var pair = user1.getName() + " ❤ " + user2.getName();
        var history = Services.db().getPairsHistory(chatId);
        if (history == null) {
            history = pair;
        } else { // update history
            history = pair + "\n" + history;
            history = history.lines()
                    .limit(10)
                    .collect(Collectors.joining("\n"));
        }
        Services.db().setPair(chatId, pair, history);
        handler.sendMessage(chatId, String.format(loveStrings[loveStrings.length - 1], user1.getLink(), user2.getLink()));
    }

    public void lastpairs(long chatId) {
        var history = Services.db().getPairsHistory(chatId);
        if (history == null)
            handler.sendMessage(chatId, "Bu chatda holi /pair buyurig'i ishlatilmagan!");
        else
            handler.sendMessage(chatId, "<b>Ohirgi 10 ta juftlik:</b>\n\n" + history);
    }

    private boolean isFromWwBot(Message message) {
        return Services.botConfig().getWwBots().contains(message.getReplyToMessage().getFrom().getUserName()) &&
                message.getReplyToMessage().getText().contains("#players");
    }
}
