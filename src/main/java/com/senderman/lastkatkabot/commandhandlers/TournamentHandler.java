package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TournamentHandler {
    public static boolean isEnabled = false;
    public static Set<Integer> membersIds;
    static Set<String> members;

    public static void setup(Message message, LastkatkaBotHandler handler) {
        members = new HashSet<>();
        membersIds = new HashSet<>();
        var params = message.getText().split("\\s+");
        if (params.length != 4) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
            return;
        }
        members.clear();
        membersIds.clear();
        members.add(params[1].replace("@", ""));
        members.add(params[2].replace("@", ""));

        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(
                new InlineKeyboardButton()
                        .setText("Снять ограничения")
                        .setCallbackData(LastkatkaBot.CALLBACK_REGISTER_IN_TOURNAMENT)
        );
        var row2 = List.of(
                new InlineKeyboardButton()
                        .setText("Группа турнира")
                        .setUrl("https://t.me/" + handler.botConfig.getTourgroupname().replace("@", "")));
        markup.setKeyboard(List.of(row1, row2));
        var toVegans = Methods.sendMessage()
                .setChatId(handler.botConfig.getLastvegan())
                .setText("\uD83D\uDCE3 <b>Турнир активирован!</b>\n\n"
                        + String.join(", ", params[1], params[2],
                        "нажмите на кнопку ниже для снятия ограничений в группе турнира\n\n"))
                .setReplyMarkup(markup);
        handler.sendMessage(toVegans);

        var toChannel = Methods.sendMessage()
                .setChatId(handler.botConfig.getTourchannel())
                .setText("<b>" + params[3].replace("_", " ") + "</b>\n\n"
                        + params[1] + " vs " + params[2]);
        handler.sendMessage(toChannel);
        isEnabled = true;
    }

    private static String getScore(String[] params) {
        String player1 = params[1];
        String player2 = params[3];
        return player1 + " - " +
                player2 + "\n" +
                params[2] + ":" +
                params[4];
    }

    private static void restrictMembers(LastkatkaBotHandler handler) {
        for (Integer memberId : membersIds) {
            Methods.Administration.restrictChatMember(handler.botConfig.getTourgroup(), memberId).call(handler);
        }
        members.clear();
        membersIds.clear();
    }

    public static void score(Message message, LastkatkaBotHandler handler) {
        var params = message.getText().split("\\s+");
        if (params.length != 5) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
            return;
        }
        String score = getScore(params);
        handler.sendMessage(Methods.sendMessage(handler.botConfig.getTourchannel(), score));
    }

    public static void win(Message message, LastkatkaBotHandler handler) {
        var params = message.getText().split("\\s+");
        if (params.length != 6) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
            return;
        }
        var score = getScore(params);
        restrictMembers(handler);
        isEnabled = false;
        String goingTo = (params[5].equals("over")) ? " выиграл турнир" : " выходит в " + params[5].replace("_", " ");
        handler.sendMessage(Methods.sendMessage()
                .setChatId(handler.botConfig.getTourchannel())
                .setText(score + "\n\n" + params[1] + "<b>" + goingTo + "!</b>"));

        handler.sendMessage(Methods.sendMessage()
                .setChatId(handler.botConfig.getLastvegan())
                .setText("\uD83D\uDCE3 <b>Раунд завершен.\n\nПобедитель:</b> "
                        + params[1] + "\nБолельщики, посетите "
                        + handler.botConfig.getTourchannel() + ",  чтобы узнать подробности"));
    }

    public static void rt(LastkatkaBotHandler handler) {
        restrictMembers(handler);
        isEnabled = false;
        handler.sendMessage(handler.botConfig.getLastvegan(),
                "\uD83D\uDEAB <b>Турнир отменен из-за непредвиденных обстоятельств!</b>");
    }
}
