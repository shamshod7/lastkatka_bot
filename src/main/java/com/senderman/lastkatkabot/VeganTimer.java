package com.senderman.lastkatkabot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.HashSet;
import java.util.Set;

class VeganTimer {
    private final long chatId;
    private final LastkatkaBotHandler handler;
    private Set<Integer> vegans;
    private boolean runTimer = true;

    VeganTimer(long chatId, LastkatkaBotHandler handler) {
        this.chatId = chatId;
        this.handler = handler;
        vegans = new HashSet<>();
    }

    private void veganTimer() {
        for (int i = 300; i > 0; i--) {
            if (!runTimer) break;

            if (i % 60 == 0 && i != 300) {
                handler.sendMessage(new SendMessage(
                        chatId,
                        "Осталось " + (i / 60) + " минуты чтобы джойнуться\n\nДжоин --> /join@veganwarsbot"));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                BotLogger.error("TIMER", e);
            }
        }
        stop();
    }

    void start() {
        new Thread(this::veganTimer).start();
    }

    void stop() {
        runTimer = false;
        handler.removeVeganTimer(chatId);
    }

    void addPlayer(int id, Message message) {
        if (isValidJoin(message)) {
            vegans.add(id);
            int count = vegans.size();
            String toSend = "Джойнулось " + count + " игроков";
            if (count % 2 != 0 && count > 2) {
                toSend += "\nБудет крыса!";
            }
            handler.sendMessage(chatId, toSend);
        }
    }

    void removePlayer(int id) {
        if (vegans.contains(id)) {
            vegans.remove(id);
            int count = vegans.size();
            String toSend = "Осталось " + count + " игроков";
            if (count % 2 != 0 && count > 2) {
                toSend += "\nБудет крыса!";
            }
            handler.sendMessage(chatId, toSend);
        }
    }

    int getVegansAmount() {
        return vegans.size();
    }

    private boolean isValidJoin(Message message) {
        return (message.isReply() && message.getReplyToMessage().getFrom().getUserName().equals("veganwarsbot") ||
                message.getText().startsWith("/join@veganwarsbot")) && !vegans.contains(message.getFrom().getId());
    }

}
