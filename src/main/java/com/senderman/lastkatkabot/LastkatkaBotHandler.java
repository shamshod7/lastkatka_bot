package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LastkatkaBotHandler extends BotHandler {

    private static final String CALLBACK_REGISTER_IN_TOURNAMENT = "register_in_tournament";

    private final BotConfig botConfig;
    private Set<Long> allowedChats;
    private Set<String> members;
    private Set<Integer> membersIds;
    private Set<String> vegans;
    private boolean isCollectingVegans;
    private boolean tournamentEnabled;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        allowedChats = new HashSet<>(List.of(
                botConfig.getLastkatka(),
                botConfig.getLastvegan(),
                botConfig.getTourgroup()
        ));
        vegans = new HashSet<>();
        members = new HashSet<>();
        membersIds = new HashSet<>();

        isCollectingVegans = false;
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
            if (params.length == 5) {
                String score = getScore(params);
                sendMessage(new SendMessage()
                        .setChatId(botConfig.getTourchannel())
                        .setText(score)
                        .enableHtml(true));
            }

        } else if (text.startsWith("/win") && isFromAdmin(message)) {
            var params = text.split(" ");
            if (params.length == 6) {
                String score = getScore(params);
                restrictMembers(botConfig.getTourgroup());
                tournamentEnabled = false;

                var toChannel = new SendMessage()
                        .setChatId(botConfig.getTourchannel())
                        .setText(score + "\n\n<b>" + params[1] + " выходит в " + params[5] + "!</b>")
                        .enableHtml(true);
                sendMessage(toChannel);

                var toVegans = new SendMessage()
                        .setChatId(botConfig.getLastvegan())
                        .setText("<b>Раунд завершен.</b>\n\n<b>Победитель:</b> "
                                + params[1] + "\nБолельщики, посетите "
                                + botConfig.getTourchannel() + ",  чтобы узнать подробности")
                        .enableHtml(true);
                sendMessage(toVegans);
            }
        } else if (text.startsWith("/rt") && isFromAdmin(message)) {
            restrictMembers(botConfig.getTourgroup());
            tournamentEnabled = false;
            sendMessage(new SendMessage(botConfig.getLastvegan(),
                    "<b>Турнир отменен из-за непредвиденных обстоятельств!</b>")
                    .enableHtml(true));
        }
    }

    private boolean isFromAdmin(Message message) {
        return botConfig.getAdmins().contains(message.getFrom().getId());
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

    private String getScore(String[] params) {
        String player1 = "<b>" + params[1] + "</b>";
        String player2 = "<b>" + params[3] + "</b>";
        return player1 + " - " +
                player2 + "\n" +
                params[2] + ":" +
                params[4];
    }

    private void veganTimer() {
        for (int i = 300; i > 0; i--) {
            if (!isCollectingVegans) break;

            if (i % 60 == 0 && i != 300) {
                sendMessage(new SendMessage(
                        botConfig.getLastvegan(),
                        "Осталось " + (i / 60) + " минуты чтобы джойнуться\n/join@veganwarsbot"));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                BotLogger.error("TIMER", e);
            }
        }
        isCollectingVegans = false;
        vegans.clear();
    }

    private void sendMessage(Long chatId, String message) {
        sendMessage(new SendMessage(chatId, message));
    }

    private void sendMessage(SendMessage message) {
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
                if (members.contains(update.getCallbackQuery().getFrom().getUserName())) {
                    membersIds.add(update.getCallbackQuery().getFrom().getId());
                    var rcm = new RestrictChatMember()
                            .setChatId(botConfig.getTourgroup())
                            .setUserId(update.getCallbackQuery().getFrom().getId())
                            .setCanSendMessages(true)
                            .setCanAddWebPagePreviews(true)
                            .setCanSendMediaMessages(true)
                            .setCanSendOtherMessages(true);
                    AnswerCallbackQuery acq = new AnswerCallbackQuery()
                            .setCallbackQueryId(update.getCallbackQuery().getId())
                            .setText("Вам даны права на отправку сообщений в группе турнира!");
                    try {
                        execute(rcm);
                        execute(acq);
                    } catch (TelegramApiException e) {
                        BotLogger.fine("UNBAN", "Some error, but this doesn't matter");
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

        long chatId = message.getChatId();
        int messageId = message.getMessageId();

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
                sendMessage(chatId, "Какая-то левая конфа, ну её нафиг");
                try {
                    execute(new LeaveChat().setChatId(chatId));
                } catch (TelegramApiException e) {
                    BotLogger.error("LEAVE", e);
                }
                return null;
            }

            String text = message.getText();

            if (text.startsWith("/pinthis") && !message.isUserMessage() && message.isReply() && isFromWwBot(message)) {
                try {
                    execute(new PinChatMessage(chatId, message.getReplyToMessage().getMessageId())
                            .setDisableNotification(true));
                } catch (TelegramApiException e) {
                    BotLogger.error("PINMESSAGE", e);
                }
                delMessage(chatId, messageId);

            } else if (text.startsWith("/bite") && !message.isUserMessage() && message.isReply()) {
                String name = message.getFrom().getFirstName();
                sendMessage(new SendMessage(chatId, "Вы были укушены за ушко юзером " + name)
                        .setReplyToMessageId(message.getReplyToMessage().getMessageId()));
                delMessage(chatId, messageId);

            } else if (text.startsWith("/pat") && !message.isUserMessage() && message.isReply()) {
                String name = message.getFrom().getFirstName();
                sendMessage(new SendMessage(chatId, "Юзер " + name + " погладил вас по голове")
                        .setReplyToMessageId(message.getReplyToMessage().getMessageId()));
                delMessage(chatId, messageId);

            } else if (text.startsWith("/help") && message.isUserMessage()) {
                SendMessage sm = new SendMessage()
                        .setChatId(chatId)
                        .setText(botConfig.getHelp())
                        .enableMarkdown(true);
                sendMessage(sm);

            } else if (text.startsWith("/tourgroup") && isFromAdmin(message)) {
                // Replace old tour group
                var params = text.split(" ");
                if (params.length == 2) {
                    allowedChats.remove(botConfig.getTourgroup());
                    botConfig.setTourgroup(Long.valueOf(params[1]));
                    allowedChats.add(botConfig.getTourgroup());
                    sendMessage(chatId, "Группа турнира успешно назначена!");
                }

            } else if (text.startsWith("/tourchannel") && isFromAdmin(message)) {
                // Replace old tour channel
                var params = text.split(" ");
                if (params.length == 2) {
                    botConfig.setTourchannel(params[1]);
                    sendMessage(chatId, "Канал успешно назначен!");
                }

            } else if (text.startsWith("/setup") && isFromAdmin(message)) {
                var params = text.split(" ");
                if (params.length != 4) {
                    sendMessage(chatId, "Неверное количество аргументов!");
                } else {
                    members.clear();
                    membersIds.clear();
                    members.add(params[1].replace("@", ""));
                    members.add(params[2].replace("@", ""));
                    tournamentEnabled = true;

                    var markup = new InlineKeyboardMarkup();
                    var row1 = List.of(
                            new InlineKeyboardButton()
                                    .setText("На турнир!")
                                    .setCallbackData(CALLBACK_REGISTER_IN_TOURNAMENT)
                    );
                    markup.setKeyboard(List.of(row1));
                    var toVegans = new SendMessage()
                            .setChatId(botConfig.getLastvegan())
                            .setText("<b>Турнир активирован!</b>\n\n"
                                    + String.join(", ", params[1], params[2],
                                    "нажмите на кнопку ниже для снятия ограничения в группе турнира\n\n")
                                    + "Группа турнира (болельщикам read-only) - " + botConfig.getTourgroupname())
                            .setReplyMarkup(markup)
                            .enableHtml(true);
                    sendMessage(toVegans);

                    var toChannel = new SendMessage()
                            .setChatId(botConfig.getTourchannel())
                            .setText("<b>" + params[3] + "</b>\n"
                                    + params[1] + " vs " + params[2])
                            .enableHtml(true);
                    sendMessage(toChannel);
                }
            } else if (botConfig.getVeganCommands().contains(text)) {
                if (!isCollectingVegans) {
                    isCollectingVegans = true;
                    vegans.clear();
                    new Thread(this::veganTimer).start();
                }

            } else {
                if (text.startsWith("/join@veganwarsbot") && chatId == botConfig.getLastvegan() && isCollectingVegans) {
                    var userName = message.getFrom().getUserName();
                    if (!vegans.contains(userName)) {
                        vegans.add(userName);
                        int count = vegans.size();
                        String toSend = "Джойнулось " + count + " игроков";
                        if (count % 2 != 0 && count > 2) {
                            toSend += "\nГарантированно будет крыса!";
                        }
                        sendMessage(botConfig.getLastvegan(), toSend);
                    }

                } else if (text.startsWith("/flee@veganwarsbot") && chatId == botConfig.getLastvegan() && isCollectingVegans) {
                    var userName = message.getFrom().getUserName();
                    if (vegans.contains(userName)) {
                        vegans.remove(userName);
                        int count = vegans.size();
                        String toSend = "Осталось " + count + " игроков";
                        if (count % 2 != 0 && count > 2) {
                            toSend += "\nГарантированно будет крыса!";
                        }
                        sendMessage(botConfig.getLastvegan(), toSend);
                    }

                } else if (text.startsWith("/fight@veganwarsbot") && chatId == botConfig.getLastvegan() && isCollectingVegans) {
                    if (vegans.size() > 1) {
                        isCollectingVegans = false;
                        vegans.clear();
                    } else {
                        sendMessage(botConfig.getLastvegan(), "Чото вас, веганов, маловато");
                    }

                } else if (text.startsWith("/reset") && chatId == botConfig.getLastvegan()) {
                    isCollectingVegans = false;
                    vegans.clear();
                    sendMessage(botConfig.getLastvegan(), "Список игроков сброшен");

                } else if (tournamentEnabled) {
                    processTournament(message, text);
                }
            }
        }
        return null;
    }
}
