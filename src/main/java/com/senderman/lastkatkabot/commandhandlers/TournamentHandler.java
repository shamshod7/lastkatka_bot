package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;

public class TournamentHandler {
    public static boolean isEnabled = false;
    private final LastkatkaBotHandler handler;
    private Message message;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public TournamentHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
    }

    private void setCurrentMessage() {
        this.message = handler.getCurrentMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = LastkatkaBotHandler.getValidName(message);
    }

    private String getScore(String[] params) {
        String player1 = params[1];
        String player2 = params[3];
        return player1 + " - " +
                player2 + "\n" +
                params[2] + ":" +
                params[4];
    }

    private void restrictMembers(long groupId) {
        for (Integer membersId : handler.membersIds) {
            try {
                handler.execute(new RestrictChatMember(groupId, membersId));
            } catch (TelegramApiException e) {
                BotLogger.error("RESTRICT", e);
            }
        }
        handler.members.clear();
        handler.membersIds.clear();
    }

    public void setup() {
        setCurrentMessage();
        var params = text.split(" ");
        if (params.length != 4) {
            handler.sendMessage(chatId, "Неверное количество аргументов!");
            return;
        }
        handler.members.clear();
        handler.membersIds.clear();
        handler.members.add(params[1].replace("@", ""));
        handler.members.add(params[2].replace("@", ""));
        isEnabled = true;

        var markup = new InlineKeyboardMarkup();
        var row1 = List.of(
                new InlineKeyboardButton()
                        .setText("Снять ограничения")
                        .setCallbackData(LastkatkaBotHandler.CALLBACK_REGISTER_IN_TOURNAMENT)
        );
        var row2 = List.of(
                new InlineKeyboardButton()
                        .setText("Группа турнира")
                        .setUrl("https://t.me/" + handler.botConfig.getTourgroupname().replace("@", "")));
        markup.setKeyboard(List.of(row1, row2));
        var toVegans = new SendMessage()
                .setChatId(handler.botConfig.getLastvegan())
                .setText("<b>Турнир активирован!</b>\n\n"
                        + String.join(", ", params[1], params[2],
                        "нажмите на кнопку ниже для снятия ограничений в группе турнира\n\n"))
                .setReplyMarkup(markup);
        handler.sendMessage(toVegans);

        var toChannel = new SendMessage()
                .setChatId(handler.botConfig.getTourchannel())
                .setText("<b>" + params[3].replace("_", " ") + "</b>\n\n"
                        + params[1] + " vs " + params[2]);
        handler.sendMessage(toChannel);
    }

    public void score() {
        setCurrentMessage();
        var params = text.split(" ");
        if (params.length != 5) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
            return;
        }
        String score = getScore(params);
        handler.sendMessage(new SendMessage()
                .setChatId(handler.botConfig.getTourchannel())
                .setText(score));
    }

    public void win() {
        setCurrentMessage();
        var params = text.split(" ");
        if (params.length != 6) {
            handler.sendMessage(message.getChatId(), "Неверное количество аргументов!");
            return;
        }
        String score = getScore(params);
        restrictMembers(handler.botConfig.getTourgroup());
        isEnabled = false;
        String goingTo = (params[5].equals("over")) ? " выиграл турнир" : " выходит в " + params[5].replace("_", " ");
        var toChannel = new SendMessage()
                .setChatId(handler.botConfig.getTourchannel())
                .setText(score + "\n\n" + params[1] + "<b>" + goingTo + "!</b>");
        handler.sendMessage(toChannel);

        var toVegans = new SendMessage()
                .setChatId(handler.botConfig.getLastvegan())
                .setText("<b>Раунд завершен.\n\nПобедитель:</b> "
                        + params[1] + "\nБолельщики, посетите "
                        + handler.botConfig.getTourchannel() + ",  чтобы узнать подробности");
        handler.sendMessage(toVegans);
    }

    public void rt() {
        setCurrentMessage();
        restrictMembers(handler.botConfig.getTourgroup());
        isEnabled = false;
        handler.sendMessage(new SendMessage(handler.botConfig.getLastvegan(),
                "<b>Турнир отменен из-за непредвиденных обстоятельств!</b>"));
    }
}
