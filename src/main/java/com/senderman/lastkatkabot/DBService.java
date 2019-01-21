package com.senderman.lastkatkabot;

import com.senderman.lastkatkabot.TempObjects.TgUser;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DBService {

    void initStats(int id);

    void incDuelWins(int id);

    void incDuelLoses(int id);

    void incBNCWin(int id);

    Map<String, Integer> getStats(int id, String player);

    void addToBlacklist(int id, String name);

    void removeFromBlacklist(int id);

    Set<TgUser> getBlackListUsers();

    Set<Integer> getBlacklistIds();

    void resetBlackList();

    void addAdmin(int id, String name);

    void removeAdmin(int id);

    Set<TgUser> getAdmins();

    Set<Integer> getAdminsIds();

    Set<Integer> getAllUsersIds();

    void addUserToDB(User user, long chatId);

    void removeUserFromDB(User user, long chatId);

    List<TgUser> getChatMembers(long chatId);

    int getTournamentMessage();

    void setTournamentMessage(int messageId);

    Set<Long> getAllowedChatsSet();

    void addAllowedChat(long chatId, String title);

    void removeAllowedChat(long chatId);

    Map<Long, String> getAllowedChats();

    boolean pairExistsToday(long chatId);

    void setPair(long chatId, String pair, String history);

    String getPairOfTheDay(long chatId);

    String getPairsHistory(long chatId);

}