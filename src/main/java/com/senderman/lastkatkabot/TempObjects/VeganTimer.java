package com.senderman.lastkatkabot.TempObjects;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.HashSet;
import java.util.Set;

public class VeganTimer {
    private final long chatId;
    private final LastkatkaBotHandler handler;
    private Set<Integer> vegans;
    private boolean runTimer = true;

    public VeganTimer(long chatId, LastkatkaBotHandler handler) {
        this.chatId = chatId;
        this.handler = handler;
        vegans = new HashSet<>();
        new Thread(this::startVeganTimer).start();
    }

    private void startVeganTimer() {
        for (int i = 299; i > 0 && runTimer; i--) {

            if (i % 60 == 0) {
                handler.sendMessage(chatId,
                        "Qo'shilishga " + (i / 60) + " minut qoldi.\n\nQo'shil --> /join@huntuzbot");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                stop();
                BotLogger.error("THREAD SLEEP", e.toString());
                handler.sendMessage(chatId, "Xatolik, timer to'xtatildi");
            }
        }
        stop();
    }

    public void stop() {
        runTimer = false;
        handler.veganTimers.remove(chatId);
    }

    public void addPlayer(int id, Message message) {
        if (vegans.contains(message.getFrom().getId()))
            return;

        vegans.add(id);
        int count = vegans.size();
        String toSend = " " + count + " ta o'yinchi qo'shildi";
        if (count % 2 != 0 && count > 2) {
            toSend += "\nKalamush bo'ladi o'yinda!";
        }
        handler.sendMessage(chatId, toSend);
    }

    public void removePlayer(int id) {
        if (!vegans.contains(id))
            return;

        vegans.remove(id);
        int count = getVegansAmount();
        String toSend = " " + count + " ta o'yinchi qoldi";
        if (count % 2 != 0 && count > 2) {
            toSend += "\nSichqon bo'ldi!";
        }
        handler.sendMessage(chatId, toSend);
    }

    public int getVegansAmount() {
        return vegans.size();
    }
}
