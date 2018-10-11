package com.senderman.lastkatkabot;

import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

class Duel {
    private final long chatId;
    private final Map<Integer, String> players;
    private final LastkatkaBotHandler handler;

    Duel(long chatId, LastkatkaBotHandler handler) {
        this.chatId = chatId;
        this.handler = handler;
        players = new HashMap<>();
    }

    void addPlayer(int id, String name) {
        if (players.containsKey(id)) {
            return;
        }
        players.put(id, name);
        handler.sendMessage(chatId, name + " успешно присоединился");

        if (players.size() == 2) {
            new Thread(this::start).start();
        }
    }

    private void start() {
        Iterator<String> i = players.values().iterator();
        String player1 = i.next();
        String player2 = i.next();
        handler.sendMessage(chatId, "Дуэль началась!\n" + player1 + " vs " + player2);
        handler.sendMessage(chatId, "Противники взяли пистолеты и расходятся в разные стороны...");
        sleep();
        handler.sendMessage(chatId, "Противники встали лицом к лицу...");
        sleep();
        int random = ThreadLocalRandom.current().nextInt(1, 3);
        String winner = (random == 1) ? player1 : player2;
        String loser = (random == 1) ? player2 : player1;
        handler.sendMessage(chatId, "Выстрел! " + winner +
                " победно смотрит на медленно умирающего " + loser + "!");
        sleep();
        if (ThreadLocalRandom.current().nextInt(0, 100) < 21) {
            handler.sendMessage(chatId, "Но, умирая, " + loser +
                    " успевает выстрелить в голову " + winner + "! Оба противника мертвы!");
        } else {
            handler.sendMessage(chatId, winner + " выиграл дуэль!");
        }
        handler.duels.remove(chatId);
    }

    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            BotLogger.error("DUEL THREAD", e.toString());
        }
    }
}
