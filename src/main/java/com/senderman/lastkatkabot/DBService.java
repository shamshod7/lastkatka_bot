package com.senderman.lastkatkabot;

import java.util.Set;

public interface DBService {

    void initStats(int id);

    void winnerToStats(int id);

    void loserToStats(int id);

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

    Set<Long> getPlayersIds();

    int getTournamentMessage();

    void setTournamentMessage(int messageId);

    void updateAllowedChats(Set<Long> allowedChats);

    void addToAllowedChats(long chatId, Set<Long> allowedChats);

    void removeFromAllowedChats(long chatId, Set<Long> allowedChats);

}