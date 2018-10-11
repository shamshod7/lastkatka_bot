package com.senderman.lastkatkabot;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

class Duel {
    private final long chatId;
    private final int messageId;
    private final Map<Integer, String> players;
    private final LastkatkaBotHandler handler;
    private String messageText;

    Duel(Message message, LastkatkaBotHandler handler) {
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.messageText = message.getText();
        this.handler = handler;
        players = new HashMap<>();
    }

    void addPlayer(int id, String name) {
        if (players.containsKey(id) || players.size() == 2) {
            return;
        }
        players.put(id, name);
        editMessage(messageText + "\n" + name, handler.getMarkupForDuel(messageId));

        if (players.size() == 2) {
            new Thread(this::start).start();
        }
    }

    private void start() {
        Iterator<String> i = players.values().iterator();
        String player1 = i.next();
        String player2 = i.next();
        messageText = player1 + " vs " + player2;
        editMessage("Дуэль началась!\n" + messageText, null);
        sleep();
        editMessage(messageText + "\n\nПротивники взяли пистолеты и расходятся в разные стороны...", null);
        sleep();
        editMessage(messageText + "\n\nПротивники встали лицом к лицу...", null);
        sleep();
        int random = ThreadLocalRandom.current().nextInt(0, 100);
        String winner = (random < 50) ? player1 : player2;
        String loser = (random < 50) ? player2 : player1;
        editMessage(messageText + "\n\nВыстрел! " + winner + " победно смотрит на медленно умирающего " + loser + "!", null);
        sleep();
        if (ThreadLocalRandom.current().nextInt(0, 100) < 21) {
            editMessage(messageText + "\n\nНо, умирая, " + loser +
                    " успевает выстрелить в голову " + winner + "! Оба противника мертвы!", null);
        } else {
            editMessage(messageText + "\n\n" + winner + " выиграл дуэль!", null);
        }
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

    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            BotLogger.error("DUEL THREAD", e.toString());
        }
    }
}
