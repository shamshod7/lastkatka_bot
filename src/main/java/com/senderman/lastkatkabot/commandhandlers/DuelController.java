package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.ServiceHolder;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DuelController {

    private final LastkatkaBotHandler handler;
    private final Map<Long, ChatDuels> duels;

    public DuelController(LastkatkaBotHandler handler) {
        this.handler = handler;
        duels = new ConcurrentHashMap<>();
    }

    public void createNewDuel(long chatId, User player1) {
        var sm = new SendMessage()
                .setChatId(chatId)
                .setText("\uD83C\uDFAF Набор на дуэль! Жмите кнопку ниже\nДжойнулись:\n" + player1.getFirstName());
        var duelMessageId = handler.sendMessage(sm).getMessageId();
        var duel = new Duel(chatId, duelMessageId);
        duel.player1 = player1;
        getChatDuels(chatId).createDuel(duelMessageId, duel);
        setReplyMarkup(chatId, duelMessageId);
    }

    public void joinDuel(CallbackQuery query) {
        var duelMessageId = query.getMessage().getMessageId();
        var chatDuels = getChatDuels(query.getMessage().getChatId());
        var player2 = query.getFrom();
        if (!chatDuels.hasDuel(duelMessageId)) {
            answerCallbackQuery(query, "⏰ Дуэль устарела", true);
            return;
        }

        var duel = chatDuels.getDuel(duelMessageId);
        if (duel.player2 != null) {
            answerCallbackQuery(query, "\uD83D\uDEAB Дуэлянтов уже набрали, увы", true);
            return;
        }
        if (duel.player1.getId().equals(player2.getId())) {
            answerCallbackQuery(query, "\uD83D\uDC7A Я думаю, что тебе стоит сходить к психологу! Ты вызываешь на дуэль самого себя", true);
            return;
        }

        duel.player2 = player2;
        answerCallbackQuery(query, "✅ Вы успешно присоединились к дуэли!", false);
        startDuel(duel);
        chatDuels.removeDuel(duelMessageId);
    }

    private void startDuel(Duel duel) {
        int randomInt = ThreadLocalRandom.current().nextInt(0, 100);
        var winner = (randomInt < 50) ? duel.player1 : duel.player2;
        var loser = (randomInt < 50) ? duel.player2 : duel.player1;

        var winnerName = name(winner);
        var loserName = name(loser);

        var messageText = new StringBuilder();
        messageText.append("<b>Дуэль</b>\n")
                .append(name(duel.player1)).append(" vs ").append(name(duel.player2))
                .append("\n\nПротивники разошлись в разные стороны, развернулись лицом друг к другу, и ")
                .append(winnerName).append(" выстрелил первым!\n")
                .append(loserName).append(" лежит на земле, истекая кровью!\n");
        if (ThreadLocalRandom.current().nextInt(0, 100) < 20) {
            messageText.append("\nНо, умирая, ")
                    .append(loserName).append(" успевает выстрелить в голову ").append(winnerName).append("! ")
                    .append(winnerName).append(" падает замертво!")
                    .append("\n\n\uD83D\uDC80 <b>Дуэль окончилась ничьей!</b>");
            ServiceHolder.db().loserToStats(winner.getId());
            ServiceHolder.db().loserToStats(loser.getId());
        } else {
            messageText
                    .append("\n\n\uD83D\uDC51 <b>")
                    .append(winnerName).append(" выиграл дуэль!</b>");
            ServiceHolder.db().winnerToStats(winner.getId());
            ServiceHolder.db().loserToStats(loser.getId());
        }
        editDuelMessage(duel, messageText.toString());

    }

    public void critical(long chatId) {
        duels.clear();
        handler.sendMessage(chatId, "✅ Все неначатые дуэли были очищены!");
    }

    private String name(User user) {
        return user.getFirstName()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void setReplyMarkup(long chatId, int duelMessageId) {
        var em = new EditMessageReplyMarkup()
                .setChatId(chatId)
                .setMessageId(duelMessageId)
                .setReplyMarkup(getMarkupForDuel());
        try {
            handler.execute(em);
        } catch (TelegramApiException e) {
            BotLogger.error("CREATE DUEL", e.toString());
        }
    }

    private void editDuelMessage(Duel duel, String text) {
        var edt = new EditMessageText()
                .setChatId(duel.chatId)
                .setMessageId(duel.messageId)
                .setText(text)
                .setParseMode(ParseMode.HTML);
        try {
            handler.execute(edt);
        } catch (TelegramApiException e) {
            BotLogger.error("EDIT_DUEL", e.toString());
        }
    }

    private InlineKeyboardMarkup getMarkupForDuel() {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                .setText("Присоединиться")
                .setCallbackData(LastkatkaBot.JOIN_DUEL));
        markup.setKeyboard(List.of(row1));
        return markup;
    }

    private void answerCallbackQuery(CallbackQuery query, String text, boolean showAsAlert) {
        var acq = new AnswerCallbackQuery()
                .setText(text)
                .setCallbackQueryId(query.getId())
                .setShowAlert(showAsAlert);
        try {
            handler.execute(acq);
        } catch (TelegramApiException e) {
            BotLogger.error("ANSWER CALLBACK", e.toString());
        }
    }

    private ChatDuels getChatDuels(long chatId) {
        if (duels.containsKey(chatId)) {
            return duels.get(chatId);
        } else {
            var chatDuels = new ChatDuels();
            duels.put(chatId, chatDuels);
            return chatDuels;
        }
    }

    static class ChatDuels extends HashMap<Integer, Duel> {

        void createDuel(Integer messageId, Duel duel) {
            super.put(messageId, duel);
        }

        boolean hasDuel(Integer messageId) {
            return super.containsKey(messageId);
        }

        Duel getDuel(Integer messageId) {
            return super.get(messageId);
        }

        void removeDuel(Integer messageId) {
            super.remove(messageId);
        }
    }


    static class Duel {
        private final long chatId;
        private final int messageId;

        private User player1;
        private User player2;

        Duel(long chatId, int messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }
    }
}
