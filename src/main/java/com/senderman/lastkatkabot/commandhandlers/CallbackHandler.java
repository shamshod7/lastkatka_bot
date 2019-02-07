package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import com.senderman.lastkatkabot.Services;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public class CallbackHandler {

    private final LastkatkaBotHandler handler;

    public CallbackHandler(LastkatkaBotHandler handler) {
        this.handler = handler;
    }

    public void payRespects(CallbackQuery query) {
        if (query.getMessage().getText().contains(query.getFrom().getFirstName())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("You've already payed respects! (or you've tried to pay respects to yourself)")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }
        Methods.answerCallbackQuery()
                .setCallbackQueryId(query.getId())
                .setText("You've payed respects")
                .setShowAlert(true)
                .call(handler);
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setReplyMarkup(UsercommandsHandler.getMarkupForPayingRespects())
                .setText(query.getMessage().getText() + "\n" + query.getFrom().getFirstName() + " have payed respects")
                .call(handler);
    }

    public void cake(CallbackQuery query, CAKE_ACTIONS actions) {
        if (!query.getFrom().getId().equals(query.getMessage().getReplyToMessage().getFrom().getId())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("Этот тортик не вам!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }
        if (query.getMessage().getDate() + 2400 < System.currentTimeMillis() / 1000) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("Тортик испорчен!")
                    .setShowAlert(true)
                    .call(handler);
            Methods.editMessageText()
                    .setChatId(query.getMessage().getChatId())
                    .setText("\uD83E\uDD22 Тортик испортился!")
                    .setMessageId(query.getMessage().getMessageId())
                    .setReplyMarkup(null)
                    .call(handler);
        }
        var acq = Methods.answerCallbackQuery()
                .setCallbackQueryId(query.getId());
        var emt = Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setReplyMarkup(null);
        if (actions == CAKE_ACTIONS.CAKE_OK) {
            acq.setText("n p u я m н o r o  a n n e m u m a");
            emt.setText("\uD83C\uDF82 " + query.getFrom().getFirstName() + " принял тортик"
                    + query.getData().replace(LastkatkaBot.CALLBACK_CAKE_OK, ""));
        } else {
            acq.setText("Ну и ладно");
            emt.setText("\uD83D\uDEAB \uD83C\uDF82 " + query.getFrom().getFirstName() + " отказался от тортика"
                    + query.getData().replace(LastkatkaBot.CALLBACK_CAKE_NOT, ""));
        }
        acq.call(handler);
        emt.call(handler);
    }

    public void registerInTournament(CallbackQuery query) {
        int memberId = query.getFrom().getId();

        if (!TournamentHandler.isEnabled) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("⚠️ На данный момент нет открытых раундов!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }

        if (TournamentHandler.membersIds.contains(memberId)) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("⚠️ Вы уже получили разрешение на отправку сообщений!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }

        if (!TournamentHandler.members.contains(query.getFrom().getUserName())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("\uD83D\uDEAB Вы не являетесь участником текущего раунда!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }

        TournamentHandler.membersIds.add(memberId);
        Methods.Administration.restrictChatMember()
                .setChatId(Services.botConfig().getTourgroup())
                .setUserId(memberId)
                .setCanSendMessages(true)
                .setCanSendMediaMessages(true)
                .setCanSendOtherMessages(true)
                .call(handler);
        Methods.answerCallbackQuery()
                .setCallbackQueryId(query.getId())
                .setText("✅ Вам даны права на отправку сообщений в группе турнира!")
                .setShowAlert(true)
                .call(handler);
        handler.sendMessage(Services.botConfig().getTourgroup(),
                "✅ " + query.getFrom().getUserName() + " получил доступ к игре!");
    }

    public void addChat(CallbackQuery query) {
        var chatId = Long.parseLong(query.getData().replace(LastkatkaBot.CALLBACK_ALLOW_CHAT, ""));
        var title = query.getData().replaceAll("^.*?title=", "");
        Services.db().addAllowedChat(chatId, title);
        handler.allowedChats.add(chatId);
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setText("✅ Чат добавлен в разрешенные!")
                .setMessageId(query.getMessage().getMessageId())
                .setReplyMarkup(null)
                .call(handler);
        handler.sendMessage(chatId, "Разработчик принял данный чат. Бот готов к работе здесь!\n" +
                "Для некоторых фичей бота требуются права админа на удаление и закреп сообщений.");
    }

    public void denyChat(CallbackQuery query) {
        var chatId = Long.parseLong(query.getData().replace(LastkatkaBot.CALLBACK_DONT_ALLOW_CHAT, ""));
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setText("\uD83D\uDEAB Чат отклонен!")
                .setReplyMarkup(null)
                .call(handler);
        handler.sendMessage(chatId, "Разработчик отклонил данный чат. Всем пока!");
        Methods.leaveChat(chatId).call(handler);
    }

    public void deleteChat(CallbackQuery query) {
        var chatId = Long.parseLong(query.getData().replace(LastkatkaBot.CALLBACK_DELETE_CHAT, ""));
        Services.db().removeAllowedChat(chatId);
        handler.allowedChats.remove(chatId);
        Methods.answerCallbackQuery()
                .setShowAlert(true)
                .setText("Чат удален!")
                .setCallbackQueryId(query.getId())
                .call(handler);
        handler.sendMessage(chatId, "Разработчик решил удалить бота из данного чата. Всем пока!");
        Methods.leaveChat(chatId).call(handler);
        Methods.deleteMessage(query.getMessage().getChatId(), query.getMessage().getMessageId()).call(handler);
    }

    public void deleteAdmin(CallbackQuery query) {
        var adminId = Integer.parseInt(query.getData().replace(LastkatkaBot.CALLBACK_DELETE_ADMIN, ""));
        Services.db().removeAdmin(adminId);
        handler.admins.remove(adminId);
        Methods.answerCallbackQuery()
                .setShowAlert(true)
                .setText("Админ удален!")
                .setCallbackQueryId(query.getId())
                .call(handler);
        handler.sendMessage(adminId, "Разработчик удалил вас из админов бота!");
        Methods.deleteMessage(query.getMessage().getChatId(), query.getMessage().getMessageId()).call(handler);
    }

    public void closeMenu(CallbackQuery query) {
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setText("Меню закрыто")
                .setReplyMarkup(null)
                .call(handler);
    }

    public enum CAKE_ACTIONS {CAKE_OK, CAKE_NOT}
}