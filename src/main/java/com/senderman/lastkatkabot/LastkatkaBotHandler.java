package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.senderman.lastkatkabot.commandhandlers.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.*;

public class LastkatkaBotHandler extends BotHandler {

    public static final String CALLBACK_REGISTER_IN_TOURNAMENT = "register_in_tournament";
    public static final String CALLBACK_PAY_RESPECTS = "pay_respects";
    public static final String CALLBACK_CAKE_OK = "cake ok";
    public static final String CALLBACK_CAKE_NOT = "cake not";

    public final BotConfig botConfig;
    public final Set<Integer> admins;
    public final Set<Long> allowedChats;
    public final Set<Integer> blacklist;
    public final Map<Long, Map<Integer, Duel>> duels;
    public final MongoDatabase lastkatkaDatabase;
    private final int mainAdmin = Integer.valueOf(System.getenv("main_admin"));
    private final UsercommandsHandler usercommands;
    private final GamesHandler games;
    public Set<String> members;
    public Set<Integer> membersIds;
    private TournamentHandler tournament;
    private AdminHandler adminPanel;

    private Message message;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;

        // settings

        sendMessage((long) mainAdmin, "Инициализация...");
        admins = new HashSet<>();
        blacklist = new HashSet<>();

        allowedChats = new HashSet<>(List.of(
                botConfig.getLastkatka(),
                botConfig.getLastvegan(),
                botConfig.getTourgroup()
        ));
        var envAllowed = System.getenv("allowed_chats");
        if (envAllowed != null) {
            for (String allowedChat : envAllowed.split(" ")) {
                allowedChats.add(Long.valueOf(allowedChat));
            }
        }

        // database
        var client = MongoClients.create(System.getenv("database"));
        lastkatkaDatabase = client.getDatabase("lastkatka");

        // handlers
        usercommands = new UsercommandsHandler(this);
        games = new GamesHandler(this);
        adminPanel = new AdminHandler(this);

        duels = new HashMap<>();

        //tournament
        //members = new HashSet<>();
        //membersIds = new HashSet<>();

        // notify main admin about launch
        sendMessage((long) mainAdmin, "Бот готов к работе!");
    }

    public static String getValidName(Message message) {
        return message.getFrom().getFirstName()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    public String getBotUsername() {
        return System.getenv("username");
    }

    @Override
    public String getBotToken() {
        return System.getenv("token");
    }

    private boolean isFromAdmin(Message message) {
        return admins.contains(message.getFrom().getId());
    }

    public boolean isInBlacklist(Message message) {
        return blacklist.contains(message.getFrom().getId());
    }

    private boolean isFromWwBot(Message message) {
        return botConfig.getWwBots().contains(message.getReplyToMessage().getFrom().getUserName()) &&
                message.getReplyToMessage().getText().contains("#players");
    }

    public void sendMessage(Long chatId, String message) {
        sendMessage(new SendMessage(chatId, message));
    }

    public Message sendMessage(SendMessage message) {
        message.enableHtml(true);
        message.disableWebPagePreview();
        Message result = null;
        try {
            result = execute(message);
        } catch (TelegramApiException e) {
            BotLogger.error("SEND", e);
        }
        return result;
    }

    public void delMessage(Long chatId, Integer messageId) {
        delMessage(new DeleteMessage(chatId, messageId));
    }

    private void delMessage(DeleteMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            BotLogger.error("DELETE", e);
        }
    }

    public Message getCurrentMessage() {
        return message;
    }

    @Override
    public BotApiMethod onUpdate(Update update) {

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String data = query.getData();
            if (data.startsWith(CALLBACK_CAKE_OK)) {
                new CallbackHandler(this, query).cake(CallbackHandler.CAKE_ACTIONS.CAKE_OK);
            } else if (data.startsWith(CALLBACK_CAKE_NOT)) {
                new CallbackHandler(this, query).cake(CallbackHandler.CAKE_ACTIONS.CAKE_NOT);
            } else {
                switch (data) {
                    case CALLBACK_REGISTER_IN_TOURNAMENT:
                        new CallbackHandler(this, query).registerInTournament();
                        break;
                    case CALLBACK_PAY_RESPECTS:
                        new CallbackHandler(this, query).payRespects();
                        break;
                }
            }
            return null;
        }

        if (!update.hasMessage())
            return null;

        message = update.getMessage();

        // don't handle old messages
        long currentTime = System.currentTimeMillis() / 1000;
        if (message.getDate() + 60 < currentTime) {
            return null;
        }

        final long chatId = message.getChatId();

        // leave from groups that not in list
        if (!message.isUserMessage() && !allowedChats.contains(chatId)) {
            sendMessage(chatId, "Какая-то левая конфа. СЛАВА ЛАСТКАТКЕ!");
            try {
                execute(new LeaveChat().setChatId(chatId));
            } catch (TelegramApiException e) {
                BotLogger.error("LEAVE", e);
            }
            return null;
        }

        // restrict any user that not in tournament, and say hello to new groups
        List<User> newMembers = message.getNewChatMembers();
        if (newMembers != null) {
            if (message.getChatId() == botConfig.getTourgroup()) {
                for (User user : newMembers) {
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
            } else {
                if (newMembers.get(0).getUserName().equalsIgnoreCase(getBotUsername())) {
                    sendMessage(chatId, "Этот чат находится в списке разрешенных. Бот готов к работе здесь.");
                }
            }
            return null;
        }

        if (!message.hasText())
            return null;

        var text = message.getText();

        // Handle user commands
        if (text.startsWith("/pinlist") && message.isReply() && isFromWwBot(message)) {
            usercommands.pinlist();

        } else if (text.startsWith("/action") && !message.isUserMessage()) {
            usercommands.action();

        } else if (text.startsWith("/f@" + getBotUsername()) && message.isReply()) {
            usercommands.payRespects();

        } else if (text.startsWith("/help")) {
            usercommands.help();

            // handle games
        } else if (text.startsWith("/dice") && !isInBlacklist(message)) {
            games.dice();

        } else if (text.startsWith("/cake") && !isInBlacklist(message)) {
            usercommands.cake();

        } else if (text.startsWith("/dstats")) {
            games.dstats();

        } else if (text.startsWith("/duel") && !message.isUserMessage() && !isInBlacklist(message)) {
            games.duel();

        } else if (text.startsWith("/start duel")) {
            String[] params = text.split(" ");
            games.joinDuel(Long.valueOf(params[2]), Integer.valueOf(params[3]));

            // handle admin commands
        } else if (text.startsWith("/owner") && message.getFrom().getId() == mainAdmin) {
            adminPanel.owner();

        } else if (text.startsWith("/remowner") && message.getFrom().getId() == mainAdmin) {
            adminPanel.remOwner();

        } else if (text.startsWith("/listowners") && message.getFrom().getId() == mainAdmin) {
            adminPanel.listOwners();

        } else if (text.startsWith("/badneko") && isFromAdmin(message) && !message.isUserMessage() && message.isReply()) {
            adminPanel.badneko();

        } else if (text.startsWith("/goodneko") && isFromAdmin(message) && !message.isUserMessage() && message.isReply()) {
            adminPanel.goodneko();

        } else if (text.startsWith("/nekos") && isFromAdmin(message)) {
            adminPanel.nekos();

        } else if (text.startsWith("/loveneko") && isFromAdmin(message)) {
            adminPanel.loveneko();

        } else if (text.startsWith("/critical") && isFromAdmin(message)) {
            adminPanel.critical();

        } else if (text.startsWith("/update") && isFromAdmin(message)) {
            adminPanel.update();

        } else if (text.startsWith("/announce") && isFromAdmin(message)) {
            adminPanel.announce();

        } else if (text.startsWith("/setup") && isFromAdmin(message)) {
            tournament = new TournamentHandler(this);

        } else if (TournamentHandler.isEnabled && isFromAdmin(message)) {
            if (text.startsWith("/score")) {
                tournament.score();
            } else if (text.startsWith("/win")) {
                tournament.win();
            } else if (text.startsWith("/rt")) {
                tournament.rt();
            }
        }

        return null;
    }
}
