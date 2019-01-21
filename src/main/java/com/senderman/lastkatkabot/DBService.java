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

    void addToBlacklist(int id, String name, Set<Integer> blacklistSet);

    void removeFromBlacklist(int id, Set<Integer> blacklistSet);

    Set<TgUser> getBlackList();

    void updateBlacklist(Set<Integer> blacklistSet);

    void resetBlackList(Set<Integer> blacklistSet);

    void addAdmin(int id, String name, Set<Integer> adminsSet);

    void removeAdmin(int id, Set<Integer> adminsSet);

    Set<TgUser> getAdmins();

    void updateAdmins(Set<Integer> adminsSet);

    Set<Integer> getPlayersIds();

    void addUserToDB(User user, long chatId);

    void removeUserFromDB(User user, long chatId);

    List<TgUser> getChatMembers(long chatId);

    int getTournamentMessage();

    void setTournamentMessage(int messageId);

    void updateAllowedChats(Set<Long> allowedChats);

    void addAllowedChat(long chatId, String title, Set<Long> allowedChats);

    void removeAllowedChat(long chatId, Set<Long> allowedChats);

    Map<Long, String> getAllowedChats();

    boolean pairExistsToday(long chatId);

    void setPair(long chatId, String pair, String history);

    String getPairOfTheDay(long chatId);

    String getPairsHistory(long chatId);

}