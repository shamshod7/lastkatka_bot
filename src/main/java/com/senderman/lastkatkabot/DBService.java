package com.senderman.lastkatkabot;

import com.senderman.lastkatkabot.TempObjects.TgUser;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Set;

public interface DBService {

    void initStats(int id);

    void incDuelWins(int id);

    void incDuelLoses(int id);

    void incBNCWin(int id);

    String getStats(int id, String player);

    void addToBlacklist(int id, String name, Set<Integer> blacklistSet);

    void removeFromBlacklist(int id, Set<Integer> blacklistSet);

    String getBlackList();

    void updateBlacklist(Set<Integer> blacklistSet);

    void resetBlackList(Set<Integer> blacklistSet);

    void addToAdmins(int id, String name, Set<Integer> adminsSet);

    void removeFromAdmins(int id, Set<Integer> adminsSet);

    String getAdmins();

    void updateAdmins(Set<Integer> adminsSet);

    Set<Integer> getPlayersIds();

    void addUserToDB(User user, long chatId);

    void removeUserFromDB(User user, long chatId);

    List<TgUser> getChatMembers(long chatId);

    int getTournamentMessage();

    void setTournamentMessage(int messageId);

    void updateAllowedChats(Set<Long> allowedChats);

    void addToAllowedChats(long chatId, Set<Long> allowedChats);

    void removeFromAllowedChats(long chatId, Set<Long> allowedChats);

    boolean pairExistsToday(long chatId);

    void setPair(long chatId, String name1, String name2);

    String getPairOfTheDay(long chatId);

}