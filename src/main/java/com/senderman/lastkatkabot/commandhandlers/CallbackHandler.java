package com.senderman.lastkatkabot.commandhandlers;

import com.annimon.tgbotsmodule.api.methods.Methods;
import com.senderman.lastkatkabot.LastkatkaBot;
import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

public class CallbackHandler {
    private final LastkatkaBotHandler handler;
    private CallbackQuery query;
    private String id;
    private Message message;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public CallbackHandler(LastkatkaBotHandler handler, CallbackQuery query) {
        this.handler = handler;
        this.query = query;
        this.id = query.getId();
        this.message = query.getMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = query.getFrom().getFirstName()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public void payRespects() {
        if (text.contains(query.getFrom().getFirstName())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("You've already payed respects! (or you've tried to pay respects to yourself)")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }
        Methods.answerCallbackQuery()
                .setCallbackQueryId(id)
                .setText("You've payed respects")
                .setShowAlert(true).call(handler);
        Methods.editMessageText()
                .setChatId(chatId)
                .setMessageId(message.getMessageId())
                .setInlineMessageId(query.getInlineMessageId())
                .setReplyMarkup(UsercommandsHandler.getMarkupForPayingRespects())
                .setText(text + "\n" + query.getFrom().getFirstName() + " have payed respects")
                .call(handler);
    }

    public void cake(CAKE_ACTIONS actions) {
        if (!query.getFrom().getId().equals(message.getReplyToMessage().getFrom().getId())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("Этот тортик не вам!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }
        var acq = Methods.answerCallbackQuery()
                .setCallbackQueryId(id);
        var emt = Methods.editMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setReplyMarkup(null);
        if (actions == CAKE_ACTIONS.CAKE_OK) {
            acq.setText("n p u я m н o r o  a n n e m u m a");
            emt.setText("\uD83C\uDF82 " + name + " принял тортик "
                    + query.getData().replace(LastkatkaBot.CALLBACK_CAKE_OK, ""));
        } else {
            acq.setText("Ну и ладно");
            emt.setText("\uD83D\uDEAB \uD83C\uDF82 " + name + " отказался от тортика "
                    + query.getData().replace(LastkatkaBot.CALLBACK_CAKE_NOT, ""));
        }
        acq.call(handler);
        emt.call(handler);
    }

    public void registerInTournament() {
        int memberId = query.getFrom().getId();
        if (handler.members.contains(query.getFrom().getUserName()) && !handler.membersIds.contains(memberId)) {
            handler.membersIds.add(memberId);
            Methods.Administration.restrictChatMember()
                    .setChatId(handler.botConfig.getTourgroup())
                    .setUserId(memberId)
                    .setCanSendMessages(true)
                    .setCanSendMediaMessages(true)
                    .setCanSendOtherMessages(true)
                    .call(handler);
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("✅ Вам даны права на отправку сообщений в группе турнира!")
                    .setShowAlert(true)
                    .call(handler);
            handler.sendMessage(handler.botConfig.getTourgroup(),
                    "✅ " +
                            query.getFrom().getFirstName()
                                    .replace("<", "&lt")
                                    .replace(">", "&gt")
                            + " <b>получил доступ к игре</b>");
        } else {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("\uD83D\uDEAB Вы не являетесь участником текущего раунда!")
                    .setShowAlert(true)
                    .call(handler);
        }
    }

    public enum CAKE_ACTIONS {CAKE_OK, CAKE_NOT}
}
