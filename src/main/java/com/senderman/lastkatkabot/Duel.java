package com.senderman.lastkatkabot;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.concurrent.ThreadLocalRandom;

public class Duel {

    private final long chatId;
    private final int messageId;
    private final LastkatkaBotHandler handler;
    private String messageText;
    private DuelPlayer player1;
    private DuelPlayer player2;

    public Duel(Message message, LastkatkaBotHandler handler) {
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.messageText = message.getText();
        this.handler = handler;
    }

    public void addPlayer(int id, String name) {
        if (player1 != null && player1.id == id) {
            return;
        }
        if (player1 == null) {
            player1 = new DuelPlayer(id, name);
            editMessage(messageText + "\n" + name, handler.getMarkupForDuel());
        } else {
            player2 = new DuelPlayer(id, name);
            new Thread(this::start).start();
        }
    }

    private void start() {
        var random = ThreadLocalRandom.current();
        int randomInt = random.nextInt(0, 100);
        String winner = (randomInt < 50) ? player1.name : player2.name;
        String loser = (randomInt < 50) ? player2.name : player1.name;
        StringBuilder messageText = new StringBuilder();
        messageText.append("<b>Дуэль</b>\n")
                .append(player1.name).append(" vs ").append(player2.name)
                .append("\n\nПротивники разошлись в разные стороны, развернулись лицом друг к другу, и ")
                .append(winner).append(" выстрелил первым! ")
                .append(loser).append(" лежит на земле, истекая кровью!");
        if (random.nextInt(0, 100) < 21) {
            messageText.append("\nНо, умирая, ")
                    .append(loser).append(" успевает выстрелить в голову ").append(winner).append("! ")
                    .append(winner).append(" падает замертво!")
                    .append("\n\n<b>Дуэль окончилась ничьей!</b<");
        } else {
            messageText.append(winner).append(" выиграл дуэль!");
        }
        editMessage(messageText.toString(), null);

        handler.duels.get(chatId).remove(messageId);
    }

    private void editMessage(String text, InlineKeyboardMarkup markup) {
        EditMessageText edt = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text);
        if (markup != null) {
            edt.setReplyMarkup(markup);
        }
        try {
            handler.execute(edt);
        } catch (TelegramApiException e) {
            BotLogger.error("EDIT_DUEL", e.toString());
        }
    }
}

class DuelPlayer {
    final String name;
    final int id;

    DuelPlayer(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
