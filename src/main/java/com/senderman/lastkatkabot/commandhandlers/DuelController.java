package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.Services;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DuelController {

    private final LastkatkaBotHandler handler;
    private final Map<Long, Map<Integer, Duel>> duels;

    public DuelController(LastkatkaBotHandler handler) {
        this.handler = handler;
        duels = new ConcurrentHashMap<>();
    }

    public void createNewDuel(long chatId, Message message) {
        if (message.isUserMessage())
            return;
        var player1 = message.getFrom();
        var sm = Methods.sendMessage()
                .setChatId(chatId)
                .setText("\uD83C\uDFAF Набор на дуэль! Жмите кнопку ниже\nДжойнулись:\n" + player1.getFirstName());
        var duelMessageId = handler.sendMessage(sm).getMessageId();
        var duel = new Duel(chatId, duelMessageId);
        duel.player1 = player1;
        getChatDuels(chatId).put(duelMessageId, duel);
        setReplyMarkup(chatId, duelMessageId);
    }

    public void joinDuel(CallbackQuery query) {
        var duelMessageId = query.getMessage().getMessageId();
        var chatDuels = getChatDuels(query.getMessage().getChatId());
        var player2 = query.getFrom();
        if (!chatDuels.containsKey(duelMessageId)) {
            answerCallbackQuery(query, "⏰ Дуэль устарела", true);
            return;
        }

        var duel = chatDuels.get(duelMessageId);
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
        chatDuels.remove(duelMessageId);
    }

    private void startDuel(Duel duel) {
        int randomInt = ThreadLocalRandom.current().nextInt(100);
        var winner = (randomInt < 50) ? duel.player1 : duel.player2;
        var loser = (randomInt < 50) ? duel.player2 : duel.player1;

        var winnerName = nameOf(winner);
        var loserName = nameOf(loser);

        var duelResult = new StringBuilder();
        duelResult.append(String.format(
                "<b>Дуэль</b>\n" +
                        "%1$s vs %2$s\n\n" +
                        "Противники разошлись в разные стороны, развернулись лицом друг к другу, и %3$s выстрелил первым\n" +
                        "%4$s лежит на земле, истекая кровью!\n\n",
                nameOf(duel.player1), nameOf(duel.player2), winnerName, loserName));

        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            duelResult.append(String.format("Но, умирая, %1$s успевает выстрелить в голову %2$s!\n" +
                            "%2$s падает замертво!\n\n" +
                            "\uD83D\uDC80 <b>Дуэль окончилась ничьей!</b>",
                    loserName, winnerName));
            Services.db().incDuelLoses(winner.getId());
            Services.db().incDuelLoses(loser.getId());

        } else {
            duelResult.append(String.format("\uD83D\uDC51 <b>%1$s выиграл дуэль!</b>", winnerName));
            Services.db().incDuelWins(winner.getId());
            Services.db().incDuelLoses(loser.getId());
        }

        Methods.editMessageText()
                .setChatId(duel.chatId)
                .setMessageId(duel.messageId)
                .setText(duelResult.toString())
                .setParseMode(ParseMode.HTML)
                .call(handler);

    }

    public void critical(long chatId) {
        duels.clear();
        handler.sendMessage(chatId, "✅ Все неначатые дуэли были очищены!");
    }

    private String nameOf(User user) {
        return user.getFirstName()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void setReplyMarkup(long chatId, int duelMessageId) {
        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                .setText("Присоединиться")
                .setCallbackData(LastkatkaBot.CALLBACK_JOIN_DUEL));
        markup.setKeyboard(List.of(row1));
        Methods.editMessageReplyMarkup()
                .setChatId(chatId)
                .setMessageId(duelMessageId)
                .setReplyMarkup(markup)
                .call(handler);
    }

    private void answerCallbackQuery(CallbackQuery query, String text, boolean showAsAlert) {
        Methods.answerCallbackQuery()
                .setText(text)
                .setCallbackQueryId(query.getId())
                .setShowAlert(showAsAlert)
                .call(handler);
    }

    private Map<Integer, Duel> getChatDuels(long chatId) {
        if (duels.containsKey(chatId)) {
            return duels.get(chatId);
        } else {
            var chatDuels = new HashMap<Integer, Duel>();
            duels.put(chatId, chatDuels);
            return chatDuels;
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