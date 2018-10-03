package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.*;

public class LastkatkaBotHandler extends BotHandler {

    private static final String CALLBACK_REGISTER_IN_TOURNAMENT = "register_in_tournament";

    private final BotConfig botConfig;
    private Set<Long> allowedChats;
    private Set<String> members;
    private Set<Integer> membersIds;
    private Map<Long, VeganTimer> veganTimers;
    private Set<Integer> blacklist;
    private boolean tournamentEnabled;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        allowedChats = new HashSet<>(List.of(
                botConfig.getLastkatka(),
                botConfig.getLastvegan(),
                botConfig.getTourgroup()
        ));
        veganTimers = new HashMap<>();
        members = new HashSet<>();
        membersIds = new HashSet<>();
        blacklist = new HashSet<>();
        tournamentEnabled = false;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    private void processTournament(Message message, String text) {

        if (text.startsWith("/score") && isFromAdmin(message)) {
            var params = text.split(" ");
            if (params.length != 5) {
                sendMessage(message.getChatId(), "Неверное количество аргументов!");
                return;
            }
            String score = getScore(params);
            sendMessage(new SendMessage()
                    .setChatId(botConfig.getTourchannel())
                    .setText(score));

        } else if (text.startsWith("/win") && isFromAdmin(message)) {
            var params = text.split(" ");
            if (params.length != 6) {
                sendMessage(message.getChatId(), "Неверное количество аргументов!");
                return;
            }
            String score = getScore(params);
            restrictMembers(botConfig.getTourgroup());
            tournamentEnabled = false;
            String goingTo = (params[5].equals("over")) ? " выиграл турнир" : " выходит в " + params[5].replace("_", " ");
            var toChannel = new SendMessage()
                    .setChatId(botConfig.getTourchannel())
                    .setText(score + "\n\n" + params[1] + "<b>" + goingTo + "!</b>");
            sendMessage(toChannel);

            var toVegans = new SendMessage()
                    .setChatId(botConfig.getLastvegan())
                    .setText("<b>Раунд завершен.\n\nПобедитель:</b> "
                            + params[1] + "\nБолельщики, посетите "
                            + botConfig.getTourchannel() + ",  чтобы узнать подробности");
            sendMessage(toVegans);
        } else if (text.startsWith("/rt") && isFromAdmin(message)) {
            restrictMembers(botConfig.getTourgroup());
            tournamentEnabled = false;
            sendMessage(new SendMessage(botConfig.getLastvegan(),
                    "<b>Турнир отменен из-за непредвиденных обстоятельств!</b>"));
        }
    }

    private boolean isFromAdmin(Message message) {
        return botConfig.getAdmins().contains(message.getFrom().getId());
    }

    private boolean isInBlacklist(Message message) {
        return blacklist.contains(message.getFrom().getId());
    }

    private boolean isAllowedChat(Message message) {
        return allowedChats.contains(message.getChatId());
    }

    private boolean isFromWwBot(Message message) {
        return botConfig.getWwBots().contains(message.getReplyToMessage().getFrom().getUserName()) &&
                message.getReplyToMessage().getText().contains("#players");
    }

    private void restrictMembers(long groupId) {
        for (Integer membersId : membersIds) {
            try {
                execute(new RestrictChatMember(groupId, membersId));
            } catch (TelegramApiException e) {
                BotLogger.error("RESTRICT", e);
            }
        }
        members.clear();
        membersIds.clear();
    }

    void removeVeganTimer(long chatId) {
        veganTimers.remove(chatId);
    }

    private String getScore(String[] params) {
        String player1 = params[1];
        String player2 = params[3];
        return player1 + " - " +
                player2 + "\n" +
                params[2] + ":" +
                params[4];
    }

    void sendMessage(Long chatId, String message) {
        sendMessage(new SendMessage(chatId, message));
    }

    void sendMessage(SendMessage message) {
        message.enableHtml(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            BotLogger.error("SEND", e);
        }
    }

    private void delMessage(Long chatId, Integer messageId) {
        delMessage(new DeleteMessage(chatId, messageId));
    }

    private void delMessage(DeleteMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            BotLogger.error("DELETE", e);
        }
    }

    @Override
    public BotApiMethod onUpdate(Update update) {

        if (update.hasCallbackQuery()) {
            if ((update.getCallbackQuery().getData().equals(CALLBACK_REGISTER_IN_TOURNAMENT))) {
                int id = update.getCallbackQuery().getFrom().getId();
                if (members.contains(update.getCallbackQuery().getFrom().getUserName()) && !membersIds.contains(id)) {
                    membersIds.add(id);
                    var rcm = new RestrictChatMember()
                            .setChatId(botConfig.getTourgroup())
                            .setUserId(id)
                            .setCanSendMessages(true)
                            .setCanSendMediaMessages(true)
                            .setCanSendOtherMessages(true);
                    var acq = new AnswerCallbackQuery()
                            .setCallbackQueryId(update.getCallbackQuery().getId())
                            .setText("Вам даны права на отправку сообщений в группе турнира!")
                            .setShowAlert(true);
                    sendMessage(botConfig.getTourgroup(),
                            update.getCallbackQuery().getFrom().getFirstName() + " <b>получил доступ к игре</b>");
                    try {
                        execute(acq);
                        execute(rcm);
                    } catch (TelegramApiException e) {
                        BotLogger.error("UNBAN", "Failed to unban member");
                    }
                } else {
                    var acq = new AnswerCallbackQuery()
                            .setCallbackQueryId(update.getCallbackQuery().getId())
                            .setText("Вы не являетесь участником текущего раунда!")
                            .setShowAlert(true);
                    try {
                        execute(acq);
                    } catch (TelegramApiException e) {
                        BotLogger.fine("UNKNOWN MEMBER", "This error means nothing");
                    }
                }
            }
            return null;
        }

        if (!update.hasMessage()) {
            return null;
        }

        final var message = update.getMessage();

        // don't process old messages
        long current = System.currentTimeMillis() / 1000;
        if (message.getDate() + 60 < current) {
            return null;
        }

        final long chatId = message.getChatId();
        final int messageId = message.getMessageId();

        // restrict any user that not in tournament
        if (message.getChatId() == botConfig.getTourgroup() && !isFromAdmin(message)) {
            List<User> news = message.getNewChatMembers();
            if (news != null)
                for (User user : news) {
                    if (!membersIds.contains(user.getId())) {
                        var rcm = new RestrictChatMember()
                                .setChatId(botConfig.getTourgroup())
                                .setUserId(user.getId())
                                .setCanSendMessages(false);
                        try {
                            execute(rcm);
                        } catch (TelegramApiException e) {
                            BotLogger.error("RESTRICT", e);
                        }
                    }
                }

        } else if (message.hasText()) {
            if (!message.isUserMessage() && !isAllowedChat(message)) { // leave from foreign groups
                sendMessage(chatId, "Какая-то левая конфа. СЛАВА ЛАСТКАТКЕ!");
                try {
                    execute(new LeaveChat().setChatId(chatId));
                } catch (TelegramApiException e) {
                    BotLogger.error("LEAVE", e);
                }
                return null;
            }

            String text = message.getText();

            if (text.startsWith("/pinlist") && !message.isUserMessage() && message.isReply() && isFromWwBot(message)) {
                try {
                    execute(new PinChatMessage(chatId, message.getReplyToMessage().getMessageId())
                            .setDisableNotification(true));
                } catch (TelegramApiException e) {
                    BotLogger.error("PINMESSAGE", e);
                }
                delMessage(chatId, messageId);

            } else if (text.startsWith("/bite") && !message.isUserMessage() && message.isReply()) {
                delMessage(chatId, messageId);
                if (isInBlacklist(message))
                    return null;

                String name = message.getFrom().getFirstName();
                String who = (message.getFrom().getId().equals(message.getReplyToMessage().getFrom().getId())) ? "себя" : "тебя";
                sendMessage(new SendMessage(chatId, name + " укусил " + who + " за ушко")
                        .setReplyToMessageId(message.getReplyToMessage().getMessageId()));

            } else if (text.startsWith("/pat") && !message.isUserMessage() && message.isReply()) {
                delMessage(chatId, messageId);
                if (isInBlacklist(message))
                    return null;

                String name = message.getFrom().getFirstName();
                String who = (message.getFrom().getId().equals(message.getReplyToMessage().getFrom().getId())) ? "себя" : "тебя";
                sendMessage(new SendMessage(chatId, name + " погладил " + who + " по голове")
                        .setReplyToMessageId(message.getReplyToMessage().getMessageId()));

            } else if (text.startsWith("/badneko") && isFromAdmin(message) && !message.isUserMessage() && message.isReply()) {
                blacklist.add(message.getReplyToMessage().getFrom().getId());
                sendMessage(chatId, message.getReplyToMessage().getFrom().getFirstName() +
                        " был плохой кошечкой, и теперь не может гладить и кусать!");

            } else if (text.startsWith("/goodneko") && isFromAdmin(message) && !message.isUserMessage() && message.isReply()) {
                blacklist.remove(message.getReplyToMessage().getFrom().getId());
                sendMessage(chatId, message.getReplyToMessage().getFrom().getFirstName() +
                        " вел себя хорошо, и теперь может гладить и кусать!");

            } else if (text.startsWith("/help") && message.isUserMessage()) {
                SendMessage sm = new SendMessage()
                        .setChatId(chatId)
                        .setText(botConfig.getHelp());
                sendMessage(sm);

            } else if (text.startsWith("/announce") && isFromAdmin(message)) {
                String[] params = text.split("\n");
                if (params.length != 6) {
                    sendMessage(chatId, "Неверное количество аргументов!");
                    return null;
                }
                String announce = botConfig.getAnnounce()
                        .replace("DATE", params[1])
                        .replace("UNTIL", params[2])
                        .replace("AWARD", params[3])
                        .replace("LINK", params[4])
                        .replace("VOTE", params[5]);
                sendMessage(botConfig.getLastvegan(), announce);

            } else if (text.startsWith("/setup") && isFromAdmin(message)) {
                var params = text.split(" ");
                if (params.length != 4) {
                    sendMessage(chatId, "Неверное количество аргументов!");
                    return null;
                }
                members.clear();
                membersIds.clear();
                members.add(params[1].replace("@", ""));
                members.add(params[2].replace("@", ""));
                tournamentEnabled = true;

                var markup = new InlineKeyboardMarkup();
                var row1 = List.of(
                        new InlineKeyboardButton()
                                .setText("Снять ограничения")
                                .setCallbackData(CALLBACK_REGISTER_IN_TOURNAMENT)
                );
                var row2 = List.of(
                        new InlineKeyboardButton()
                                .setText("Группа турнира")
                                .setUrl("https://t.me/" + botConfig.getTourgroupname().replace("@", "")));
                markup.setKeyboard(List.of(row1, row2));
                var toVegans = new SendMessage()
                        .setChatId(botConfig.getLastvegan())
                        .setText("<b>Турнир активирован!</b>\n\n"
                                + String.join(", ", params[1], params[2],
                                "нажмите на кнопку ниже для снятия ограничений в группе турнира\n\n"))
                        .setReplyMarkup(markup)
                        .enableMarkdown(true);
                sendMessage(toVegans);

                var toChannel = new SendMessage()
                        .setChatId(botConfig.getTourchannel())
                        .setText("<b>" + params[3].replace("_", " ") + "</b>\n\n"
                                + params[1] + " vs " + params[2]);
                sendMessage(toChannel);

            } else if (botConfig.getVeganCommands().contains(text)) { // start veganwars
                if (!veganTimers.containsKey(chatId)) {
                    veganTimers.put(chatId, new VeganTimer(chatId, this));
                    veganTimers.get(chatId).start();
                }

            } else {
                if (text.startsWith("/join") && veganTimers.containsKey(chatId)) {
                    veganTimers.get(chatId).addPlayer(message.getFrom().getId(), message);

                } else if (text.startsWith("/flee") && veganTimers.containsKey(chatId)) {
                    veganTimers.get(chatId).removePlayer(message.getFrom().getId());

                } else if (text.startsWith("/fight") && veganTimers.containsKey(chatId)) {
                    if (veganTimers.get(chatId).getVegansAmount() > 1) {
                        veganTimers.get(chatId).stop();
                    }

                } else if (text.startsWith("/reset") && veganTimers.containsKey(chatId)) {
                    veganTimers.get(chatId).stop();
                    sendMessage(chatId, "Список игроков сброшен");

                } else if (tournamentEnabled) {
                    processTournament(message, text);
                }
            }
        }
        return null;
    }
}
