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
    private static String team1;
    private static String team2;
    private static boolean isTeamMode;
    private static String roundName;

    public static void setup(Message message, LastkatkaBotHandler handler) {
        members = new HashSet<>();
        membersIds = new HashSet<>();
        var params = message.getText().split("\n");
        if (params.length != 4) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
            return;
        }

        isTeamMode = !params[1].startsWith("@");

        var checkMessage = Methods.sendMessage()
                .setChatId(handler.botConfig.getLastvegan());

        roundName = params[3];

        if (isTeamMode) {
            team1 = params[1].split("\\s+", 2)[0];
            team2 = params[2].split("\\s+", 2)[0];
            params[1] = params[1].replace(team1, "");
            params[2] = params[2].replace(team2, "");
            for (int i = 1; i < 3; i++) {
                for (String member : params[i].split("\\s+")) {
                    members.add(member.replace("@", ""));
                }
            }
            checkMessage.setText("⚠️ Проверьте правильность веденных данных\n" +
                    "Тип игры: Командный\nРаунд: " + roundName + "\nКоманды: " +
                    team1 + ", " + team2 + "\nУчастники: " + String.join(", ", members +
                    "\n\n/go - подтвердить, /ct - отменить"));
        } else {
            members.add(params[1].replace("@", ""));
            members.add(params[2].replace("@", ""));
            checkMessage.setText("⚠️ Проверьте правильность веденных данных\n" +
                    "Тип игры: Дуэль\nРаунд: " + roundName + "\nУчастники: " + String.join(", ", members +
                    "\n\n/go - подтвердить, /ct - отменить"));
        }
        checkMessage.setReplyToMessageId(message.getMessageId()).call(handler);
    }

    public static void startTournament(LastkatkaBotHandler handler) {
        if (members.isEmpty())
            return;

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
                        + "@" + String.join(", @", members) +
                        ", нажмите на кнопку ниже для снятия ограничений в группе турнира\n\n")
                .setReplyMarkup(markup);
        handler.sendMessage(toVegans);

        var toChannel = Methods.sendMessage()
                .setChatId(handler.botConfig.getTourchannel());
        if (isTeamMode) {
            toChannel.setText("<b>" + roundName + "</b>\n\n"
                    + team1 + " vs " + team2);
        } else {
            toChannel.setText("<b>" + roundName + "</b>\n\n"
                    + members.toArray()[0] + " vs " + members.toArray()[1]);
        }
        handler.sendMessage(toChannel);
        isEnabled = true;
    }

    public static void cancelTournament(LastkatkaBotHandler handler) {
        restrictMembers(handler);
        Methods.sendMessage(handler.botConfig.getLastvegan(), "\uD83D\uDEAB Действие отменено");
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
