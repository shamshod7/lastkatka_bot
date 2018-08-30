package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LastkatkaBotHandler extends BotHandler {

    private final BotConfig botConfig;
    private ArrayList<Long> allowedChats;
    private HashSet<String> members;
    private HashSet<Integer> membersIds;
    private HashSet<String> vegans;
    private boolean isCollectingVegans = false;
    private boolean tournamentEnabled = false;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        allowedChats = new ArrayList<>();
        allowedChats.add(botConfig.getLastkatka());
        allowedChats.add(botConfig.getLastvegan());
        allowedChats.add(botConfig.getTourgroup());
        vegans = new HashSet<>();
        members = new HashSet<>();
        membersIds = new HashSet<>();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    private void processTournament(Message message, String text) { // TODO

        if (text.startsWith("/score") && botConfig.getAdmins().contains(message.getFrom().getId())) {
            if (text.split(" ").length == 5) {
                String score = getScore(text.split(" "));
                SendMessage sm = new SendMessage()
                        .setChatId(botConfig.getTourchannel())
                        .setText(score)
                        .setParseMode(ParseMode.HTML);
                sendMessage(sm);
            }

        } else if (text.startsWith("/win") && botConfig.getAdmins().contains(message.getFrom().getId())) {
            String[] params = text.split(" ");
            if (params.length == 6) {
                String score = getScore(params);
                for (Integer membersId : membersIds) {
                    RestrictChatMember rcm = new RestrictChatMember(botConfig.getTourgroup(), membersId);
                    try {
                        execute(rcm);
                    } catch (TelegramApiException e) {
                        BotLogger.error("RESTRICT", e);
                    }
                }
                members.clear();
                membersIds.clear();
                tournamentEnabled = false;

                SendMessage toChannel = new SendMessage()
                        .setChatId(botConfig.getTourchannel())
                        .setText(score + "\n\n<b>" + params[1] + " выходит в " + params[5] + "!</b>")
                        .setParseMode(ParseMode.HTML);
                sendMessage(toChannel);

                SendMessage toVegans = new SendMessage()
                        .setChatId(botConfig.getLastvegan())
                        .setText("<b>Раунд завершен.</b>\n\n<b>Победитель:</b> "
                                + params[1] + "\nБолельщики, посетите "
                                + botConfig.getTourchannel() + ",  чтобы узнать подробности")
                        .setParseMode(ParseMode.HTML);
                sendMessage(toVegans);
            }
        } else if (text.startsWith("/rt") && botConfig.getAdmins().contains(message.getFrom().getId())) {
            for (Integer membersId : membersIds) {
                RestrictChatMember rcm = new RestrictChatMember(botConfig.getTourgroup(), membersId);
                try {
                    execute(rcm);
                } catch (TelegramApiException e) {
                    BotLogger.error("RESTRICT", e);
                }
            }
            members.clear();
            membersIds.clear();
            tournamentEnabled = false;
            SendMessage sm = new SendMessage(botConfig.getLastvegan(),
                    "<b>Турнир отменен из-за непредвиденных обстоятельств!</b>")
                    .setParseMode(ParseMode.HTML);
            sendMessage(sm);
        }
    }

    private String getScore(String... params) {
        String player1 = "<b>" + params[1] + "</b>";
        String player2 = "<b>" + params[3] + "</b>";
        return player1 + " - " +
                player2 + "\n" +
                params[2] + ":" +
                params[4];
    }

    private void veganTimer() {
        for (int i = 300; i > 0; i--) {
            if (isCollectingVegans) {
                if (i % 60 == 0 && i != 300) {
                    SendMessage sm = new SendMessage(botConfig.getLastvegan(), "Осталось " + i / 60
                            + " минуты чтобы джойнуться\n/join@veganwarsbot");
                    sendMessage(sm);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    BotLogger.error("TIMER", e);
                }

            } else {
                break;
            }
        }
        isCollectingVegans = false;
        vegans.clear();
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            BotLogger.error("SEND", e);
        }
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
            if ((update.getCallbackQuery().getData().equals("register_in_tournament"))) {
                if (members.contains(update.getCallbackQuery().getFrom().getUserName())) {
                    membersIds.add(update.getCallbackQuery().getFrom().getId());
                    RestrictChatMember rcm = new RestrictChatMember()
                            .setChatId(botConfig.getTourgroup())
                            .setUserId(update.getCallbackQuery().getFrom().getId())
                            .setCanSendMessages(true)
                            .setCanAddWebPagePreviews(true)
                            .setCanSendMediaMessages(true)
                            .setCanSendOtherMessages(true);
                    try {
                        execute(rcm);
                    } catch (TelegramApiException e) {
                        BotLogger.fine("UNBAN", "Some error, but this doesn't matter");
                    }
                }
            }
            return null;
        }

        Message message = update.getMessage();
        // don't process old messages
        long current = System.currentTimeMillis() / 1000;
            if (message.getDate() + 60 >= current) {
            long chatId = message.getChatId();
            int messageId = message.getMessageId();

            // restrict any user that not in tournament
            if (message.getChatId() == botConfig.getTourgroup() && !botConfig.getAdmins().contains(message.getFrom().getId())) {
                List<User> news = message.getNewChatMembers();
                if (news != null)
                    for (User user : news) {
                        if (!membersIds.contains(user.getId())) {
                            RestrictChatMember rcm = new RestrictChatMember()
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

            }  else if (update.getMessage().hasText()) { // leave from foreign groups
                if (!message.isUserMessage() && !allowedChats.contains(message.getChatId())) {
                    SendMessage sm = new SendMessage(chatId, "Какая-то левая конфа, ну её нафиг");
                    sendMessage(sm);
                    LeaveChat lc = new LeaveChat().setChatId(chatId);
                    try {
                        execute(lc);
                    } catch (TelegramApiException e) {
                        BotLogger.error("LEAVE", e);
                    }

                } else {

                    String text = message.getText();

                    if (text.startsWith("/pinthis") && !message.isUserMessage() && message.isReply()) {
                        PinChatMessage pcm = new PinChatMessage(chatId, message.getReplyToMessage().getMessageId())
                                .setDisableNotification(true);
                        try {
                            execute(pcm);
                        } catch (TelegramApiException e) {
                            BotLogger.error("PINMESSAGE", e);
                        }
                        DeleteMessage dm = new DeleteMessage(chatId, messageId);
                        delMessage(dm);

                    } else if (text.startsWith("/unpin") && !message.isUserMessage()) {
                        UnpinChatMessage ucm = new UnpinChatMessage(chatId);
                        try {
                            execute(ucm);
                        } catch (TelegramApiException e) {
                            BotLogger.error("UNPIN", e);
                        }
                        DeleteMessage dm = new DeleteMessage(chatId, messageId);
                        delMessage(dm);

                    } else if (text.startsWith("/bite") && !message.isUserMessage() && message.isReply()) {
                        String name = message.getFrom().getFirstName();
                        SendMessage sm = new SendMessage(chatId, "Вы были укушены за ушко юзером " + name)
                                .setReplyToMessageId(message.getReplyToMessage().getMessageId());
                        sendMessage(sm);
                        DeleteMessage dm = new DeleteMessage(chatId, messageId);
                        delMessage(dm);

                    } else if (text.startsWith("/pat") && !message.isUserMessage() && message.isReply()) {
                        String name = message.getFrom().getFirstName();
                        SendMessage sm = new SendMessage(chatId, "Юзер " + name + " погладил вас по голове")
                                .setReplyToMessageId(message.getReplyToMessage().getMessageId());
                        sendMessage(sm);
                        DeleteMessage dm = new DeleteMessage(chatId, messageId);
                        delMessage(dm);

                    } else if (text.startsWith("/tourgroup") && botConfig.getAdmins().contains(message.getFrom().getId())) {
                        botConfig.setTourgroup(Long.valueOf(text.split(" ")[1]));
                        allowedChats.remove(2);
                        allowedChats.add(botConfig.getTourgroup());
                        SendMessage sm = new SendMessage(chatId, "Группа турнира успешно назначена!");
                        sendMessage(sm);

                    } else if (text.startsWith("/tourchannel") && botConfig.getAdmins().contains(message.getFrom().getId())) {
                        botConfig.setTourchannel(text.split(" ")[1]);
                        SendMessage sm = new SendMessage(chatId, "Канал успешно назначен!");
                        sendMessage(sm);

                    } else if (text.startsWith("/setup") && botConfig.getAdmins().contains(message.getFrom().getId())) {
                        String[] params = text.split(" ");
                        if (params.length != 4) {
                            SendMessage sm = new SendMessage(chatId, "Неверное количество аргументов!");
                            sendMessage(sm);
                        } else { // TODO
                            members = new HashSet<>();
                            membersIds = new HashSet<>();
                            members.add(params[1].replace("@", ""));
                            members.add(params[2].replace("@", ""));
                            tournamentEnabled = true;

                            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                            List<InlineKeyboardButton> row1 = new ArrayList<>();
                            row1.add(new InlineKeyboardButton().setText("На турнир!")
                                    .setCallbackData("register_in_tournament"));
                            rows.add(row1);
                            markup.setKeyboard(rows);
                            SendMessage toVegans = new SendMessage()
                                    .setChatId(botConfig.getLastvegan())
                                    .setText("<b>Турнир активирован!</b>\n\n"
                                    + String.join(", ", params[1], params[2],
                                            "нажмите на кнопку ниже для снятия ограничения в группе турнира\n\n")
                                    + "Группа турнира (болельщикам read-only) - " + botConfig.getTourgroupname())
                                    .setReplyMarkup(markup)
                                    .setParseMode(ParseMode.HTML);
                            sendMessage(toVegans);

                            SendMessage toChannel = new SendMessage()
                                    .setChatId(botConfig.getTourchannel())
                                    .setText("<b>" + params[3] + "</b>\n"
                                            + params[1] + " vs " + params[2])
                                    .setParseMode(ParseMode.HTML);
                            sendMessage(toChannel);
                        }
                    } else if (botConfig.getVeganCommands().contains(text)) {
                        if (!isCollectingVegans) {
                            isCollectingVegans = true;
                            vegans = new HashSet<>();
                            new Thread(this::veganTimer).start();
                        }

                    } else if (text.startsWith("/join@veganwarsbot") && chatId == botConfig.getLastvegan() && isCollectingVegans) {
                        if (!vegans.contains(message.getFrom().getUserName())) {
                            vegans.add(message.getFrom().getUserName());
                            int count = vegans.size();
                            String toSend = "Джойнулось " + count + " игроков";
                            if (count % 2 != 0 && count > 2) {
                                toSend += "\nГарантированно будет крыса!";
                            }
                            SendMessage sm = new SendMessage(botConfig.getLastvegan(), toSend);
                            sendMessage(sm);
                        }

                    } else if (text.startsWith("/flee@veganwarsbot") && chatId == botConfig.getLastvegan() && isCollectingVegans) {
                        if (vegans.contains(message.getFrom().getUserName())) {
                            vegans.remove(message.getFrom().getUserName());
                            int count = vegans.size();
                            String toSend = "Осталось " + count + " игроков";
                            if (count % 2 != 0 && count > 2) {
                                toSend += "\nГарантированно будет крыса!";
                            }
                            SendMessage sm = new SendMessage(botConfig.getLastvegan(), toSend);
                            sendMessage(sm);
                        }

                    } else if (text.startsWith("/fight@veganwarsbot") && chatId == botConfig.getLastvegan() && isCollectingVegans) {
                        if (vegans.size() > 1) {
                            isCollectingVegans = false;
                            vegans.clear();
                        } else {
                            SendMessage sm = new SendMessage(botConfig.getLastvegan(), "Чото вас, веганов, маловато");
                            sendMessage(sm);
                        }

                    } else if (text.startsWith("/reset") && chatId == botConfig.getLastvegan()) {
                        isCollectingVegans = false;
                        vegans.clear();

                    } else if (tournamentEnabled) {
                        processTournament(message, text);
                    }
                }
            }
        }
        return null;
    }
}