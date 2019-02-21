package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import com.annimon.tgbotsmodule.api.methods.Methods;
import com.annimon.tgbotsmodule.api.methods.send.SendMessageMethod;
import com.senderman.lastkatkabot.TempObjects.BullsAndCowsGame;
import com.senderman.lastkatkabot.TempObjects.VeganTimer;
import com.senderman.lastkatkabot.commandhandlers.*;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class LastkatkaBotHandler extends BotHandler {

    public final Set<Integer> admins;
    public final Set<Long> allowedChats;
    public final Set<Integer> blacklist;
    private final AdminHandler adminHandler;
    private final UsercommandsHandler usercommandsHandler;
    private final CallbackHandler callbackHandler;
    private final DuelController duelController;
    private final BotConfig botConfig;
    public Map<Long, VeganTimer> veganTimers;
    public Map<Long, BullsAndCowsGame> bullsAndCowsGames;

    LastkatkaBotHandler(BotConfig botConfig) {
        this.botConfig = botConfig;
        sendMessage(botConfig.getMainAdmin(), "Yuklanmoqda...");

        // settings
        Services.setBotConfig(botConfig);
        Services.setDBService(new MongoDBService());

        admins = Services.db().getAdminsIds();
        blacklist = Services.db().getBlacklistIds();

        allowedChats = Services.db().getAllowedChatsSet();
        allowedChats.add(botConfig.getLastvegan());
        allowedChats.add(botConfig.getTourgroup());

        adminHandler = new AdminHandler(this);
        usercommandsHandler = new UsercommandsHandler(this);
        callbackHandler = new CallbackHandler(this);
        duelController = new DuelController(this);
        veganTimers = new HashMap<>();
        bullsAndCowsGames = new HashMap<>();

        // notify main admin about launch
        sendMessage(botConfig.getMainAdmin(), "Бот готов к работе!");
    }

    @Override
    public BotApiMethod onUpdate(@NotNull Update update) {

        // first we will handle callbacks
        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String data = query.getData();

            if (data.startsWith(LastkatkaBot.CALLBACK_CAKE_OK)) {
                callbackHandler.cake(query, CallbackHandler.CAKE_ACTIONS.CAKE_OK);
            } else if (data.startsWith(LastkatkaBot.CALLBACK_CAKE_NOT)) {
                callbackHandler.cake(query, CallbackHandler.CAKE_ACTIONS.CAKE_NOT);
            } else if (data.startsWith(LastkatkaBot.CALLBACK_ALLOW_CHAT)) {
                callbackHandler.addChat(query);
            } else if (data.startsWith(LastkatkaBot.CALLBACK_DONT_ALLOW_CHAT)) {
                callbackHandler.denyChat(query);
            } else if (data.startsWith(LastkatkaBot.CALLBACK_DELETE_CHAT)) {
                callbackHandler.deleteChat(query);
                adminHandler.chats(query.getMessage());
            } else if (data.startsWith(LastkatkaBot.CALLBACK_DELETE_ADMIN)) {
                callbackHandler.deleteAdmin(query);
                adminHandler.listOwners(query.getMessage());
            } else {

                switch (data) {
                    case LastkatkaBot.CALLBACK_REGISTER_IN_TOURNAMENT:
                        callbackHandler.registerInTournament(query);
                        break;
                    case LastkatkaBot.CALLBACK_PAY_RESPECTS:
                        callbackHandler.payRespects(query);
                        break;
                    case LastkatkaBot.CALLBACK_CLOSE_MENU:
                        callbackHandler.closeMenu(query);
                        break;
                    case LastkatkaBot.CALLBACK_JOIN_DUEL:
                        duelController.joinDuel(query);
                        break;
                }
            }
            return null;
        }

        if (!update.hasMessage())
            return null;

        final var message = update.getMessage();

        // don't handle old messages
        if (message.getDate() + 120 < System.currentTimeMillis() / 1000)
            return null;

        final var chatId = message.getChatId();

        var newMembers = message.getNewChatMembers();

        if (newMembers != null) {
            if (chatId == Services.botConfig().getTourgroup()) { // restrict any user who isn't in tournament
                for (User user : newMembers) {
                    if (TournamentHandler.membersIds == null || !TournamentHandler.membersIds.contains(user.getId())) {
                        Methods.Administration.restrictChatMember()
                                .setChatId(Services.botConfig().getTourgroup())
                                .setUserId(user.getId())
                                .setCanSendMessages(false).call(this);
                    }
                }
                return null;
            }

            if (newMembers.get(0).getUserName().equalsIgnoreCase(getBotUsername())) {
                if (allowedChats.contains(chatId)) {// Say hello to new group if chat is allowed
                    sendMessage(chatId, "Этот чат находится в списке разрешенных. Бот готов к работе здесь.");
                } else {
                    sendMessage(chatId, "Чата нет в списке разрешенных. Дождитесь решения разработчика");

                    var row1 = List.of(new InlineKeyboardButton()
                            .setText("Добавить")
                            .setCallbackData(LastkatkaBot.CALLBACK_ALLOW_CHAT + chatId + "title=" + message.getChat().getTitle()));
                    var row2 = List.of(new InlineKeyboardButton()
                            .setText("Отклонить")
                            .setCallbackData(LastkatkaBot.CALLBACK_DONT_ALLOW_CHAT + chatId));
                    var markup = new InlineKeyboardMarkup();
                    markup.setKeyboard(List.of(row1, row2));
                    sendMessage(Methods.sendMessage((long) Services.botConfig().getMainAdmin(),
                            "Добавить чат " + message.getChat().getTitle() +
                                    " (" + chatId + ") "
                                    + "в список разрешенных?")
                            .setReplyMarkup(markup));
                }
            }
            return null;
        }

        if (!allowedChats.contains(chatId) && !message.isUserMessage()) // do not respond in not allowed chats
            return null;

        if (message.getLeftChatMember() != null) {
            Methods.sendDocument()
                    .setChatId(chatId)
                    .setFile(Services.botConfig().getLeavesticker())
                    .setReplyToMessageId(message.getMessageId())
                    .call(this);
            Services.db().removeUserFromDB(message.getLeftChatMember(), chatId);
        }

        if (message.isGroupMessage() || message.isSuperGroupMessage()) // add user to DB
            Services.db().addUserToDB(message.getFrom(), chatId);

        if (!message.hasText())
            return null;

        var text = message.getText();

        // for bulls and cows
        if (text.matches("\\d{4}") && bullsAndCowsGames.containsKey(chatId)) {
            bullsAndCowsGames.get(chatId).check(message);
            return null;
        }

        if (!message.isCommand())
            return null;

        /* bot should only trigger on general commands (like /command) or on commands for this bot (/command@mybot),
         * and NOT on commands for another bots (like /command@notmybot) except for @veganwarsbot
         */
        final var command = text.split("\\s+", 2)[0].toLowerCase(Locale.ENGLISH).replace("@" + getBotUsername(), "");

        if (Services.botConfig().getVeganWarsCommands().contains(text) && !veganTimers.containsKey(chatId)) { // start veganwars timer
            veganTimers.put(chatId, new VeganTimer(chatId, this));

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

        if (command.startsWith("/reset") && veganTimers.containsKey(chatId)) {
            veganTimers.get(chatId).stop();
            sendMessage(chatId, "Список игроков сброшен");

        }

        // users in blacklist are not allowed to use this commands
        if (isNotInBlacklist(message)) {
            switch (command) {
                case "/pinlist":
                    usercommandsHandler.pinlist(message);
                    break;
                case "/pair":
                    usercommandsHandler.pair(chatId);
                    break;
                case "/lastpairs":
                    usercommandsHandler.lastpairs(chatId);
                    break;
                case "/action":
                    usercommandsHandler.action(message);
                    break;
                case "/f":
                    usercommandsHandler.payRespects(message);
                    break;
                case "/dice":
                    usercommandsHandler.dice(message);
                    break;
                case "/cake":
                    usercommandsHandler.cake(message);
                    break;
                case "/duel":
                    duelController.createNewDuel(chatId, message);
                    break;
                case "/stats":
                    usercommandsHandler.dstats(message);
                    break;
                case "/bnc":
                    if (!bullsAndCowsGames.containsKey(chatId))
                        bullsAndCowsGames.put(chatId, new BullsAndCowsGame(this, chatId));
                    else
                        sendMessage(chatId, "В этом чате игра уже идет!");
                    break;
                case "/bnchelp":
                    usercommandsHandler.bnchelp(message);
                    break;
                case "/feedback":
                    usercommandsHandler.feedback(message);
                    break;
                case "/help":
                    usercommandsHandler.help(message);
                    break;
            }
        }

        // commands for main admin only
        if (message.getFrom().getId().equals(Services.botConfig().getMainAdmin())) {
            switch (command) {
                case "/owner":
                    adminHandler.owner(message);
                    break;
                case "/update":
                    adminHandler.update(message);
                    break;
                case "/announce":
                    adminHandler.announce(message);
                    break;
                case "/owners":
                    adminHandler.listOwners(message);
                    break;
                case "/chats":
                    adminHandler.chats(message);
                    break;
            }
        }

        // commands for all admins
        if (isFromAdmin(message)) {
            switch (command) {
                case "/badneko":
                    adminHandler.badneko(message);
                    break;
                case "/goodneko":
                    adminHandler.goodneko(message);
                    break;
                case "/nekos":
                    adminHandler.nekos(message);
                    break;
                case "/getinfo":
                    adminHandler.getinfo(message);
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
                    adminHandler.setupHelp(message);
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

    public Message sendMessage(long chatId, String text) {
        return sendMessage(Methods.sendMessage(chatId, text));
    }

    public Message sendMessage(SendMessageMethod sm) {
        return sm
                .disableWebPagePreview()
                .enableHtml(true)
                .call(this);
    }
}
