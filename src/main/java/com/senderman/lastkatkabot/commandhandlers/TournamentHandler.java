package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.Services;
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
    private static Set<String> teams;
    private static boolean isTeamMode;
    private static String roundName;

    public static void setup(Message message, LastkatkaBotHandler handler) {
        if (isEnabled)
            return;

        members = new HashSet<>();
        membersIds = new HashSet<>();
        var params = message.getText().split("\n");
        if (params.length != 4) {
            handler.sendMessage(message.getChatId(), "Ma'lumotlar soni yetarli emas!");
            return;
        }

        isTeamMode = !params[1].startsWith("@"); // Name of commands should not start with @

        roundName = params[3].strip();

        var checkText = new StringBuilder()
                .append("⚠️ Berilga ma'lumotlar to'g'riligini tekshiring\n\n")
                .append("<b>Raund:</b> ").append(roundName);

        if (isTeamMode) {
            teams = new HashSet<>();
            for (int i = 1; i < 3; i++) {
                var paramString = params[i].strip().split("\\s+");
                teams.add(paramString[0].replace("_", " "));
                for (int j = 1; j < paramString.length; j++) {
                    members.add(paramString[j].replace("@", ""));
                }
            }
            checkText.append("\n<b>Guruhlar:</b> ").append(getTeamsAsString());

        } else {
            members.add(params[1].replace("@", "").strip());
            members.add(params[2].replace("@", "").strip());
        }
        checkText.append("\n<b>Qatnashchilar:</b> ").append(getMembersAsString())
                .append("\n\n/go - tasdiqlash, /ct - bekor qilish");

        handler.sendMessage(Methods.sendMessage()
                .setChatId(Services.botConfig().getLastvegan())
                .setText(checkText.toString())
                .setReplyToMessageId(message.getMessageId()));
    }

    public static void startTournament(LastkatkaBotHandler handler) {
        if (members.isEmpty() || isEnabled)
            return;

        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(new InlineKeyboardButton()
                .setText("Cheklovni olish")
                .setCallbackData(LastkatkaBot.CALLBACK_REGISTER_IN_TOURNAMENT)
        );
        var row2 = List.of(new InlineKeyboardButton()
                .setText("Turnir gruppasi")
                .setUrl("https://t.me/" + Services.botConfig().getTourgroupname().replace("@", "")));
        var row3 = List.of(new InlineKeyboardButton()
                .setText("Turnir kanali")
                .setUrl("https://t.me/" + Services.botConfig().getTourchannel().replace("@", "")));
        markup.setKeyboard(List.of(row1, row2, row3));

        var toVegans = Methods.sendMessage()
                .setChatId(Services.botConfig().getLastvegan())
                .setText("\uD83D\uDCE3 <b>Turnir aktivlashdi!</b>\n\n"
                        + getMembersAsString() +
                        ", turnirdan cheklovni olish uchun pastdagi knopkani bosing\n\n")
                .setReplyMarkup(markup);

        Methods.Administration.pinChatMessage()
                .setChatId(Services.botConfig().getLastvegan())
                .setMessageId(handler.sendMessage(toVegans).getMessageId())
                .setNotificationEnabled(false)
                .call(handler);

        var toChannel = Methods.sendMessage()
                .setChatId(Services.botConfig().getTourchannel());
        if (isTeamMode) {
            toChannel.setText("<b>" + roundName + "</b>\n\n"
                    + teams.toArray()[0] + " vs " + teams.toArray()[1]);
        } else {
            toChannel.setText("<b>" + roundName + "</b>\n\n@"
                    + members.toArray()[0] + " vs @" + members.toArray()[1]);
        }
        handler.sendMessage(toChannel);
        isEnabled = true;
    }

    public static void cancelSetup(LastkatkaBotHandler handler) {
        if (isEnabled)
            return;
        members.clear();
        if (isTeamMode)
            teams.clear();
        handler.sendMessage(Methods.sendMessage(Services.botConfig().getLastvegan(), "\uD83D\uDEAB Faoliyat to'xtadi"));
    }

    private static String getScore(String[] params) {
        String player1;
        String player2;
        if (isTeamMode) {
            player1 = params[1].replace("_", " ");
            player2 = params[3].replace("_", " ");
        } else {
            player1 = params[1];
            player2 = params[3];
        }
        return String.format("%1$s - %2$s\n%3$s:%4$s", player1, player2, params[2], params[4]);
    }

    private static void restrictMembers(LastkatkaBotHandler handler) {
        isEnabled = false;
        for (Integer memberId : membersIds) {
            Methods.Administration.restrictChatMember(Services.botConfig().getTourgroup(), memberId).call(handler);
        }
        members.clear();
        membersIds.clear();
        if (isTeamMode)
            teams.clear();
        Methods.Administration.unpinChatMessage(Services.botConfig().getLastvegan()).call(handler);
        int tournamentMessage = Services.db().getTournamentMessage();
        if (tournamentMessage != 0) {
            Methods.Administration.pinChatMessage()
                    .setChatId(Services.botConfig().getLastvegan())
                    .setMessageId(tournamentMessage)
                    .setNotificationEnabled(false)
                    .call(handler);
        }
    }

    public static void score(Message message, LastkatkaBotHandler handler) {
        var params = message.getText().split("\\s+");
        if (params.length != 5) {
            handler.sendMessage(message.getChatId(), "Argumentlar soni yetarli emas!");
            return;
        }
        String score = getScore(params);
        handler.sendMessage(Methods.sendMessage(Services.botConfig().getTourchannel(), score));
    }

    public static void win(Message message, LastkatkaBotHandler handler) {
        var params = message.getText().split("\\s+");
        if (params.length != 6) {
            handler.sendMessage(message.getChatId(), "Argumentlar soni yetarli emas!");
            return;
        }
        var score = getScore(params);
        restrictMembers(handler);

        String goingTo;
        if (isTeamMode)
            goingTo = (params[5].equals("over")) ? " turnirda g'alaba qilishda" : " chiqishdi " + params[5].replace("_", " ");
        else
            goingTo = (params[5].equals("over")) ? " turnirda g'alaba qilishda" : " chiqishdi " + params[5].replace("_", " ");

        handler.sendMessage(Methods.sendMessage()
                .setChatId(Services.botConfig().getTourchannel())
                .setText(score + "\n\n" + params[1].replace("_", " ") + "<b>" + goingTo + "!</b>"));

        handler.sendMessage(Methods.sendMessage()
                .setChatId(Services.botConfig().getLastvegan())
                .setText("\uD83D\uDCE3 <b>Raund tugadi.\n\nChempion:</b> "
                        + params[1] + "\nMuhlislar, ma'lumot uchun"
                        + Services.botConfig().getTourchannel() + "ga kiringlar"));
    }

    public static void rt(LastkatkaBotHandler handler) {
        restrictMembers(handler);
        handler.sendMessage(Services.botConfig().getLastvegan(),
                "\uD83D\uDEAB <b>Raund ayrim sabablarga ko'ra to'xtatildi!</b>");
    }

    public static void tourmessage(LastkatkaBotHandler handler, Message message) {
        if (!message.getChatId().equals(Services.botConfig().getLastvegan()) || !message.isReply())
            return;
        Services.db().setTournamentMessage(message.getReplyToMessage().getMessageId());
        handler.sendMessage(message.getChatId(), "✅ Turnirni asosiy xati o'rnatildi!");
    }

    private static String getMembersAsString() {
        var memberList = new StringBuilder();
        for (String member : members) {
            memberList.append("@").append(member).append(", ");
        }
        memberList.delete(memberList.length() - 2, memberList.length() - 1); //remove trailing ", "
        return memberList.toString();
    }

    private static String getTeamsAsString() {
        var teamList = new StringBuilder();
        for (String team : teams) {
            teamList.append(team).append(", ");
        }
        teamList.delete(teamList.length() - 2, teamList.length() - 1);
        return teamList.toString();
    }
}
