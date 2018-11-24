package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import com.annimon.tgbotsmodule.api.methods.Methods;
import com.annimon.tgbotsmodule.api.methods.send.SendMessageMethod;
import com.senderman.lastkatkabot.commandhandlers.*;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LastkatkaBotHandler extends BotHandler {

    public final BotConfig botConfig;
    public final Set<Integer> admins;
    public final Set<Long> allowedChats;
    public final Set<Integer> blacklist;
    private final DuelController duelController;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        sendMessage((long) botConfig.getMainAdmin(), "Инициализация...");

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
        duelController = new DuelController(this);

        // notify main admin about launch
        sendMessage((long) botConfig.getMainAdmin(), "Бот готов к работе!");
    }

    @Override
    public BotApiMethod onUpdate(@NotNull Update update) {

        // first we will handle callbacks
        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String data = query.getData();
            if (data.startsWith(LastkatkaBot.CALLBACK_CAKE_OK)) {
                CallbackHandler.cake(query, this, CallbackHandler.CAKE_ACTIONS.CAKE_OK);
            } else if (data.startsWith(LastkatkaBot.CALLBACK_CAKE_NOT)) {
                CallbackHandler.cake(query, this, CallbackHandler.CAKE_ACTIONS.CAKE_NOT);
            } else {
                switch (data) {
                    case LastkatkaBot.CALLBACK_REGISTER_IN_TOURNAMENT:
                        CallbackHandler.registerInTournament(query, this);
                        break;
                    case LastkatkaBot.CALLBACK_PAY_RESPECTS:
                        CallbackHandler.payRespects(query, this);
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

        Message message = update.getMessage();

        // don't handle old messages
        if (message.getDate() + 120 < System.currentTimeMillis() / 1000) {
            return null;
        }

        final long chatId = message.getChatId();

        // leave from groups that not in list
        if (!message.isUserMessage() && !allowedChats.contains(chatId)) {
            sendMessage(chatId, "Какая-то левая конфа. СЛАВА ЛАСТКАТКЕ!");
            Methods.leaveChat(chatId).call(this);
            return null;
        }

        // restrict any user that not in tournament
        List<User> newMembers = message.getNewChatMembers();
        if (newMembers != null) {
            if (message.getChatId() == botConfig.getTourgroup()) {
                for (User user : newMembers) {
                    if (TournamentHandler.membersIds == null || !TournamentHandler.membersIds.contains(user.getId())) {
                        Methods.Administration.restrictChatMember()
                                .setChatId(botConfig.getTourgroup())
                                .setUserId(user.getId())
                                .setCanSendMessages(false).call(this);
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

        if (!message.isCommand())
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

        if (command.startsWith("/pinlist") && isFromWwBot(message)) {
            UsercommandsHandler.pinlist(message, this);
            return null;

        } else if (command.startsWith("/duel") && !message.isUserMessage() && isNotInBlacklist(message)) {
            Methods.deleteMessage(chatId, message.getMessageId()).call(this);
            duelController.createNewDuel(chatId, message.getFrom());
            return null;

        } else if (command.startsWith("/help")) {
            UsercommandsHandler.help(message, this);
            return null;
        } else if (command.startsWith("/setup") && isFromAdmin(message) && !TournamentHandler.isEnabled) {
            TournamentHandler.setup(message, this);
            return null;
        }

        // users in blacklist are not allowed to use this commands
        if (isNotInBlacklist(message)) {
            switch (command) {
                case "/action":
                    UsercommandsHandler.action(message, this);
                    break;
                case "/f":
                    UsercommandsHandler.payRespects(message, this);
                    break;
                case "/dice":
                    UsercommandsHandler.dice(message, this);
                    break;
                case "/cake":
                    UsercommandsHandler.cake(message, this);
                    break;
                case "/feedback":
                    UsercommandsHandler.feedback(message, this);
                    break;
                case "/dstats":
                    UsercommandsHandler.dstats(message, this);
                    break;
            }
        }

        // commands for main admin only
        if (message.getFrom().getId().equals(botConfig.getMainAdmin())) {
            switch (command) {
                case "/owner":
                    AdminHandler.owner(message, this);
                    break;
                case "/remowner":
                    AdminHandler.remOwner(message, this);
                    break;
                case "/update":
                    AdminHandler.update(message, this);
                    break;
                case "/announce":
                    AdminHandler.announce(message, this);
                    break;
            }
        }

        // commands for all admins
        if (isFromAdmin(message)) {
            switch (command) {
                case "/owners":
                    AdminHandler.listOwners(message, this);
                    break;
                case "/badneko":
                    AdminHandler.badneko(message, this);
                    break;
                case "/goodneko":
                    AdminHandler.goodneko(message, this);
                    break;
                case "/nekos":
                    AdminHandler.nekos(message, this);
                    break;
                case "/loveneko":
                    AdminHandler.loveneko(message, this);
                    break;
                case "/getinfo":
                    AdminHandler.getinfo(message, this);
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
                    TournamentHandler.score(message, this);
                    break;
                case "/win":
                    TournamentHandler.win(message, this);
                    break;
                case "/rt":
                    TournamentHandler.rt(this);
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

    private boolean isNotInBlacklist(Message message) {
        boolean result = blacklist.contains(message.getFrom().getId());
        if (result) {
            Methods.deleteMessage(message.getChatId(), message.getMessageId()).call(this);
        }
        return !result;
    }

    private boolean isFromWwBot(Message message) {
        return botConfig.getWwBots().contains(message.getReplyToMessage().getFrom().getUserName()) &&
                message.getReplyToMessage().getText().contains("#players");
    }

    public void sendMessage(long chatId, String text) {
        sendMessage(Methods.sendMessage(chatId, text));
    }

    public Message sendMessage(SendMessageMethod sm) {
        return sm
                .disableWebPagePreview()
                .enableHtml(true)
                .call(this);
    }
}
