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

import java.util.*;

public class LastkatkaBotHandler extends BotHandler {

    public final BotConfig botConfig;
    public final Set<Integer> admins;
    public final Set<Long> allowedChats;
    public final Set<Integer> blacklist;
    private final DuelController duelController;
    Map<Long, VeganTimer> veganTimers;
    Map<Long, BullsAndCowsGame> bullsAndCowsGames;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        sendMessage((long) botConfig.getMainAdmin(), "Инициализация...");

        // settings
        ServiceHolder.setDBService(new MongoDBService());

        admins = new HashSet<>();
        ServiceHolder.db().updateAdmins(admins);

        blacklist = new HashSet<>();
        ServiceHolder.db().updateBlacklist(blacklist);

        allowedChats = new HashSet<>();
        allowedChats.add(botConfig.getLastvegan());
        allowedChats.add(botConfig.getTourgroup());
        ServiceHolder.db().updateAllowedChats(allowedChats);

        duelController = new DuelController(this);
        veganTimers = new HashMap<>();
        bullsAndCowsGames = new HashMap<>();

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
            sendMessage(chatId, "Какая-то левая конфа (id " + chatId + "). СЛАВА ЛАСТКАТКЕ!");
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

        var text = message.getText();

        // for bulls and cows
        if (text.matches("\\d{4}") && bullsAndCowsGames.containsKey(chatId)) {
            bullsAndCowsGames.get(chatId).check(Integer.parseInt(text));
            return null;
        }

        if (!message.isCommand())
            return null;

        // we dont need to handle messages that are not commands
        if (!text.startsWith("/"))
            return null;

        /* bot should only trigger on general commands (like /command) or on commands for this bot (/command@mybot),
         * and NOT on commands for another bots (like /command@notmybot) except @veganwarsbot
         */
        var command = text.split("\\s+", 2)[0].toLowerCase(Locale.ENGLISH).replace("@" + getBotUsername(), "");

        if (botConfig.getVeganWarsCommands().contains(text) && !veganTimers.containsKey(chatId)) { // start veganwars timer
            veganTimers.put(chatId, new VeganTimer(chatId, this));
            veganTimers.get(chatId).start();

        } else if (text.startsWith("/join") && veganTimers.containsKey(chatId)) {
            veganTimers.get(chatId).addPlayer(message.getFrom().getId(), message);

        } else if (text.startsWith("/flee") && veganTimers.containsKey(chatId)) {
            veganTimers.get(chatId).removePlayer(message.getFrom().getId());

        } else if (text.startsWith("/fight") && veganTimers.containsKey(chatId)) {
            if (veganTimers.get(chatId).getVegansAmount() > 1) {
                veganTimers.get(chatId).stop();
            }

        } else if (command.contains("@"))
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
        } else if (command.startsWith("/reset") && veganTimers.containsKey(chatId)) {
            veganTimers.get(chatId).stop();
            sendMessage(chatId, "Список игроков сброшен");

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
                case "/bnc":
                    if (!bullsAndCowsGames.containsKey(chatId))
                        bullsAndCowsGames.put(chatId, new BullsAndCowsGame(this, chatId));
                    else
                        sendMessage(chatId, "В этом чате игра уже идет!");
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
                case "/addchat": // /addchat chatId
                    ServiceHolder.db().addToAllowedChats(Long.parseLong(text.replace("/addchat ", "")),
                            allowedChats);
                    sendMessage(chatId, "✅ Чат добавлен!");
                    break;
                case "/remchat":
                    ServiceHolder.db().removeFromAllowedChats(chatId, allowedChats);
                    break;
                case "/newfield":
                    sendMessage(chatId, "Инит");
                    ServiceHolder.db().updStats();
                    sendMessage(chatId, "done");
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
                case "/setup":
                    TournamentHandler.setup(message, this);
                    break;
                case "/go":
                    TournamentHandler.startTournament(this);
                    break;
                case "/ct":
                    TournamentHandler.cancelSetup(this);
                    break;
                case "/tourhelp":
                    AdminHandler.setupHelp(message, this);
                    break;
                case "/tourmessage":
                    TournamentHandler.tourmessage(this, message);
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