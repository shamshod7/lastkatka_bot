package com.senderman.lastkatkabot.commandhandlers;

import com.senderman.lastkatkabot.LastkatkaBotHandler;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

public class CallbackHandler {
    private final LastkatkaBotHandler handler;
    private CallbackQuery query;
    private String id;
    private long chatId;
    private int messageId;
    private String text;
    private String name;

    public CallbackHandler(LastkatkaBotHandler handler, CallbackQuery query) {
        this.handler = handler;
        this.query = query;
        this.id = query.getId();
        Message message = query.getMessage();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.text = message.getText();
        this.name = query.getFrom().getFirstName()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public void payRespects() {
        if (text.contains(query.getFrom().getFirstName())) {
            var acq = new AnswerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("You've already payed respects! (or you've tried to pay respects to yourself)")
                    .setShowAlert(true);
            try {
                handler.execute(acq);
            } catch (TelegramApiException e) {
                BotLogger.error("PAY_RESPECTS", e.toString());
            }
            return;
        }
        var acq = new AnswerCallbackQuery()
                .setCallbackQueryId(id)
                .setText("You've payed respects")
                .setShowAlert(true);
        var emt = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(query.getMessage().getMessageId())
                .setInlineMessageId(query.getInlineMessageId())
                .setReplyMarkup(handler.getMarkupForPayingRespects())
                .setText(text + "\n" + query.getFrom().getFirstName() + " have payed respects");
        try {
            handler.execute(emt);
            handler.execute(acq);
        } catch (TelegramApiException e) {
            BotLogger.error("PAY_RESPECTS", e.toString());
        }
    }

    public void joinDuel() {
        try {
            handler.duels.get(chatId).get(messageId).addPlayer(query.getFrom().getId(), name);
        } catch (Exception e) {
            var acq = new AnswerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("Эта дуэль устарела!");
            try {
                handler.execute(acq);
            } catch (TelegramApiException tae) {
                BotLogger.error("FAILED DUEL", tae.toString());
            }
        }
    }

    public void registerInTournament() {
        int memberId = query.getFrom().getId();
        if (handler.members.contains(query.getFrom().getUserName()) && !handler.membersIds.contains(memberId)) {
            handler.membersIds.add(memberId);
            var rcm = new RestrictChatMember()
                    .setChatId(handler.botConfig.getTourgroup())
                    .setUserId(memberId)
                    .setCanSendMessages(true)
                    .setCanSendMediaMessages(true)
                    .setCanSendOtherMessages(true);
            var acq = new AnswerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("Вам даны права на отправку сообщений в группе турнира!")
                    .setShowAlert(true);
            handler.sendMessage(handler.botConfig.getTourgroup(),
                    query.getFrom().getFirstName()
                            .replace("<", "&lt")
                            .replace(">", "&gt")
                            + " <b>получил доступ к игре</b>");
            try {
                handler.execute(acq);
                handler.execute(rcm);
            } catch (TelegramApiException e) {
                BotLogger.error("UNBAN", "Failed to unban member");
            }
        } else {
            var acq = new AnswerCallbackQuery()
                    .setCallbackQueryId(id)
                    .setText("Вы не являетесь участником текущего раунда!")
                    .setShowAlert(true);
            try {
                handler.execute(acq);
            } catch (TelegramApiException e) {
                BotLogger.fine("UNKNOWN MEMBER", "This error means nothing");
            }
        }
    }
}
