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
                    .setText("Siz allaqachon respekt bergansiz! (yoki siz o'zizga o'ziz ovoz beryabsiz)")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }
        Methods.answerCallbackQuery()
                .setCallbackQueryId(query.getId())
                .setText("Siz respekt berdingiz!")
                .setShowAlert(true)
                .call(handler);
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setReplyMarkup(UsercommandsHandler.getMarkupForPayingRespects())
                .setText(query.getMessage().getText() + "\n" + query.getFrom().getFirstName() + " hurmat bildirdi👏🏻")
                .call(handler);
    }

    public void cake(CallbackQuery query, CAKE_ACTIONS actions) {
        if (!query.getFrom().getId().equals(query.getMessage().getReplyToMessage().getFrom().getId())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("Bu tort sizga emas!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }
        if (query.getMessage().getDate() + 2400 < System.currentTimeMillis() / 1000) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("Tort aynib qoldi!")
                    .setShowAlert(true)
                    .call(handler);
            Methods.editMessageText()
                    .setChatId(query.getMessage().getChatId())
                    .setText("\uD83E\uDD22 Tort buzilib qoldi!")
                    .setMessageId(query.getMessage().getMessageId())
                    .setReplyMarkup(null)
                    .call(handler);
        }
        var acq = Methods.answerCallbackQuery()
                .setCallbackQueryId(query.getId());
        var emt = Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setParseMode(ParseMode.HTML)
                .setReplyMarkup(null);
        if (actions == CAKE_ACTIONS.CAKE_OK) {
            acq.setText("y o q i m l i  i sh t a h a");
            emt.setText("\uD83C\uDF82 <b>" + query.getFrom().getFirstName() + "</b> tortni oldi"
                    + query.getData().replace(LastkatkaBot.CALLBACK_CAKE_OK, ""));
        } else {
            acq.setText("Ha mayli");
            emt.setText("\uD83D\uDEAB \uD83C\uDF82 " + query.getFrom().getFirstName() + " tortni rad etdi"
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
                    .setText("⚠️ Ayni damda ochiq raundlar yo'q!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }

        if (TournamentHandler.membersIds.contains(memberId)) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("⚠️ Siz allaqachon xatni jo'natishga ruhsat olgansiz!")
                    .setShowAlert(true)
                    .call(handler);
            return;
        }

        if (!TournamentHandler.members.contains(query.getFrom().getUserName())) {
            Methods.answerCallbackQuery()
                    .setCallbackQueryId(query.getId())
                    .setText("\uD83D\uDEAB Siz ushbu raund o'yinchisi emassiz!")
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
                .setText("✅ Turnir guruhida xat yozishga ruhsat berildi!")
                .setShowAlert(true)
                .call(handler);
        handler.sendMessage(Services.botConfig().getTourgroup(),
                "✅ " + query.getFrom().getUserName() + " o'yin uchun ruhsat oldi!");
    }

    public void addChat(CallbackQuery query) {
        var chatId = Long.parseLong(query.getData()
                .replace(LastkatkaBot.CALLBACK_ALLOW_CHAT, "")
                .replaceAll("title=.*$", ""));
        var title = query.getData().replaceAll("^.*?title=", "");
        Services.db().addAllowedChat(chatId, title);
        handler.allowedChats.add(chatId);
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setText("✅ Chatga ruhsat etildi!")
                .setMessageId(query.getMessage().getMessageId())
                .setReplyMarkup(null)
                .call(handler);
        handler.sendMessage(chatId, "Yaratuvchi chatga ruhsat etdi. Bot faoliyatga tayyor!\n" +
                "Bazi ishlar uchun botga adminlik huquqi zarur.");
    }

    public void denyChat(CallbackQuery query) {
        var chatId = Long.parseLong(query.getData().replace(LastkatkaBot.CALLBACK_DONT_ALLOW_CHAT, ""));
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setText("\uD83D\uDEAB Chat chetlashtiradi!")
                .setReplyMarkup(null)
                .call(handler);
        handler.sendMessage(chatId, "Yaratuvchi ushbu guruhni chetlashtirdi. Hammaga hayr!");
        Methods.leaveChat(chatId).call(handler);
    }

    public void deleteChat(CallbackQuery query) {
        var chatId = Long.parseLong(query.getData().split(" ")[1]);
        Services.db().removeAllowedChat(chatId);
        handler.allowedChats.remove(chatId);
        Methods.answerCallbackQuery()
                .setShowAlert(true)
                .setText("Chat o'chirildi!")
                .setCallbackQueryId(query.getId())
                .call(handler);
        handler.sendMessage(chatId, "Yaratuvchi ushbu chatni o'chirishga qaror qildi. Hammaga hayr!");
        Methods.leaveChat(chatId).call(handler);
        Methods.deleteMessage(query.getMessage().getChatId(), query.getMessage().getMessageId()).call(handler);
    }

    public void deleteAdmin(CallbackQuery query) {
        var adminId = Integer.parseInt(query.getData().split(" ")[1]);
        Services.db().removeAdmin(adminId);
        handler.admins.remove(adminId);
        Methods.answerCallbackQuery()
                .setShowAlert(true)
                .setText("Admin o'chirildi!")
                .setCallbackQueryId(query.getId())
                .call(handler);
        handler.sendMessage(adminId, "Yaratuvchi sizni bot adminlaridan o'chirdi!");
        Methods.deleteMessage(query.getMessage().getChatId(), query.getMessage().getMessageId()).call(handler);
    }

    public void closeMenu(CallbackQuery query) {
        Methods.editMessageText()
                .setChatId(query.getMessage().getChatId())
                .setMessageId(query.getMessage().getMessageId())
                .setText("Menyuni yopish")
                .setReplyMarkup(null)
                .call(handler);
    }

    public enum CAKE_ACTIONS {CAKE_OK, CAKE_NOT}
}
