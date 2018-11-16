package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LastkatkaBotHandler extends BotHandler {

    public final BotConfig botConfig;
    public final Set<Integer> admins;
    public final Set<Long> allowedChats;
    public final Set<Integer> blacklist;
    private final UsercommandsHandler usercommands;
    private final DuelController duelController;
    public Set<String> members;
    public Set<Integer> membersIds;
    private TournamentHandler tournament;
    private AdminHandler adminPanel;

    private Message message;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        sendMessage((long) LastkatkaBot.mainAdmin, "Инициализация...");

        // settings
        admins = new HashSet<>();
        blacklist = new HashSet<>();
        ServiceHolder.setDBService(new MongoDBHandler());
        ServiceHolder.db().updateAdmins(admins);
        ServiceHolder.db().updateBlacklist(blacklist);

        allowedChats = new HashSet<>(List.of(
                botConfig.getLastkatka(),
                botConfig.getLastvegan(),
                botConfig.getTourgroup()
        ));
        var envAllowed = botConfig.getAllowedChats();
        if (envAllowed != null) {
            for (String allowedChat : envAllowed.split(" ")) {
                allowedChats.add(Long.parseLong(allowedChat));
            }
        }

        // handlers
        adminPanel = new AdminHandler(this);
        usercommands = new UsercommandsHandler(this);
        duelController = new DuelController(this);

        // notify main admin about launch
        sendMessage((long) LastkatkaBot.mainAdmin, "Бот готов к работе!");
    }

    public static String getValidName(Message message) {
        return message.getFrom().getFirstName()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    public BotApiMethod onUpdate(Update update) {

        // first we will handle callbacks
        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String data = query.getData();
            if (data.startsWith(LastkatkaBot.CALLBACK_CAKE_OK)) {
                new CallbackHandler(this, query).cake(CallbackHandler.CAKE_ACTIONS.CAKE_OK);
            } else if (data.startsWith(LastkatkaBot.CALLBACK_CAKE_NOT)) {
                new CallbackHandler(this, query).cake(CallbackHandler.CAKE_ACTIONS.CAKE_NOT);
            } else {
                switch (data) {
                    case LastkatkaBot.CALLBACK_REGISTER_IN_TOURNAMENT:
                        new CallbackHandler(this, query).registerInTournament();
                        break;
                    case LastkatkaBot.CALLBACK_PAY_RESPECTS:
                        new CallbackHandler(this, query).payRespects();
                        break;
                    case LastkatkaBot.JOIN_DUEL:
                        duelController.joinDuel(query);
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
        if (message.getDate() + 120 < currentTime) {
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

        // restrict any user that not in tournament
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
            } else { // Say hello to new groups
                if (newMembers.get(0).getUserName().equalsIgnoreCase(getBotUsername())) {
                    sendMessage(chatId, "Этот чат находится в списке разрешенных. Бот готов к работе здесь.");
                }
            }
            return null;
        }

        if (!message.hasText())
            return null;

        var text = message.getText();

        // we dont need to handle messages that are not commands
        if (!text.startsWith("/"))
            return null;

        /* bot should only trigger on general commands (like /command) or on commands for this bot (/command@mybot),
         * and NOT on commands for another bots (like /command@notmybot)
         */
        var command = text.split("\\s+", 2)[0].toLowerCase(Locale.ENGLISH).replace("@" + getBotUsername(), "");
        if (command.contains("@"))
            return null;
        // TODO: убрать все названия команд из методов
        if (command.startsWith("/pinlist") && isFromWwBot(message)) {
            usercommands.pinlist();
            return null;

        } else if (command.startsWith("/duel") && !message.isUserMessage() && !isInBlacklist(message)) {
            delMessage(chatId, message.getMessageId());
            duelController.createNewDuel(chatId, message.getFrom());
            return null;

        } else if (command.startsWith("/help")) {
            usercommands.help();
            return null;
        } else if (command.startsWith("/setup") && isFromAdmin(message) && !TournamentHandler.isEnabled) {
            tournament = new TournamentHandler(this);
            return null;
        }

        // users in blacklist are not allowed to use this commands
        if (!isInBlacklist(message)) {
            switch (command) {
                case "/action":
                    usercommands.action();
                    break;
                case "/f":
                    usercommands.payRespects();
                    break;
                case "/dice":
                    usercommands.dice();
                    break;
                case "/cake":
                    usercommands.cake();
                    break;
                case "/feedback":
                    usercommands.feedback();
                    break;
                case "/dstats":
                    usercommands.dstats();
                    break;
            }
        }

        // commands for main admin only
        if (message.getFrom().getId().equals(LastkatkaBot.mainAdmin)) {
            switch (command) {
                case "/owner":
                    adminPanel.owner();
                    break;
                case "/remowner":
                    adminPanel.remOwner();
                    break;
                case "/update":
                    adminPanel.update();
                    break;
                case "/announce":
                    adminPanel.announce();
                    break;
            }
        }

        // commands for all admins
        if (isFromAdmin(message)) {
            switch (command) {
                case "/owners":
                    adminPanel.listOwners();
                    break;
                case "/badneko":
                    adminPanel.badneko();
                    break;
                case "/goodneko":
                    adminPanel.goodneko();
                    break;
                case "/nekos":
                    adminPanel.nekos();
                    break;
                case "/loveneko":
                    adminPanel.loveneko();
                    break;
                case "/getinfo":
                    adminPanel.getinfo();
                    break;
                case "/critical":
                    duelController.critical(chatId);
                    break;
            }
        }

        // commands for tournament
        if (TournamentHandler.isEnabled && isFromAdmin(message)) {
            switch (command) {
                case "/score":
                    tournament.score();
                    break;
                case "/win":
                    tournament.win();
                    break;
                case "/rt":
                    tournament.win();
            }
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    private boolean isFromAdmin(Message message) {
        return admins.contains(message.getFrom().getId());
    }

    public boolean isInBlacklist(Message message) {
        boolean result = blacklist.contains(message.getFrom().getId());
        if (result) {
            delMessage(message.getChatId(), message.getMessageId());
        }
        return result;
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
}
