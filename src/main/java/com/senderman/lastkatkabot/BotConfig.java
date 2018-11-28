package com.senderman.lastkatkabot;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class BotConfig {

    @JsonProperty(required = true)
    private String token;
    @JsonProperty(required = true)
    private String username;

    @JsonProperty(required = true)
    private int mainAdmin;
    @JsonProperty(required = true)
    private String allowedChats;

    @JsonProperty(required = true)
    private long lastkatka;
    @JsonProperty(required = true)
    private long lastvegan;
    @JsonProperty(required = true)
    private long tourgroup;

    @JsonProperty(required = true)
    private String tourchannel;

    @JsonProperty(required = true)
    private String tourgroupname;

    @JsonProperty
    private Set<String> wwBots;

    @JsonProperty
    private String help;

    @JsonProperty
    private String adminhelp;

    @JsonProperty
    private String setuphelp;

    @JsonProperty
    private Set<String> veganWarsCommands;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getMainAdmin() {
        return mainAdmin;
    }

    public void setMainAdmin(int mainAdmin) {
        this.mainAdmin = mainAdmin;
    }

    public String getAllowedChats() {
        return allowedChats;
    }

    public void setAllowedChats(String allowedChats) {
        this.allowedChats = allowedChats;
    }

    public long getLastkatka() {
        return lastkatka;
    }

    public void setLastkatka(long lastkatka) {
        this.lastkatka = lastkatka;
    }

    public long getLastvegan() {
        return lastvegan;
    }

    public void setLastvegan(long lastvegan) {
        this.lastvegan = lastvegan;
    }

    public long getTourgroup() {
        return tourgroup;
    }

    public void setTourgroup(long tourgroup) {
        this.tourgroup = tourgroup;
    }

    public String getTourgroupname() {
        return tourgroupname;
    }

    public void setTourgroupname(String tourgroupname) {
        this.tourgroupname = tourgroupname;
    }

    public String getTourchannel() {
        return tourchannel;
    }

    public void setTourchannel(String tourchannel) {
        this.tourchannel = tourchannel;
    }

    public Set<String> getWwBots() {
        return wwBots;
    }

    public void setWwBots(Set<String> wwBots) {
        this.wwBots = wwBots;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getAdminhelp() {
        return adminhelp;
    }

    public void setAdminhelp(String adminhelp) {
        this.adminhelp = adminhelp;
    }

    public String getSetuphelp() {
        return setuphelp;
    }

    public void setSetuphelp(String setuphelp) {
        this.setuphelp = setuphelp;
    }

    public Set<String> getVeganWarsCommands() {
        return veganWarsCommands;
    }

    public void setVeganWarsCommands(Set<String> veganWarsCommands) {
        this.veganWarsCommands = veganWarsCommands;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BotConfig{");
        sb.append("token='").append(token).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", mainAdmin=").append(mainAdmin);
        sb.append(", allowedChats='").append(allowedChats).append('\'');
        sb.append(", lastkatka=").append(lastkatka);
        sb.append(", lastvegan=").append(lastvegan);
        sb.append(", tourgroup=").append(tourgroup);
        sb.append(", tourchannel='").append(tourchannel).append('\'');
        sb.append(", tourgroupname='").append(tourgroupname).append('\'');
        sb.append(", wwBots=").append(wwBots);
        sb.append(", help='").append(help).append('\'');
        sb.append(", adminhelp='").append(adminhelp).append('\'');
        sb.append(", setuphelp='").append(setuphelp).append('\'');
        sb.append(", veganWarsCommands=").append(veganWarsCommands);
        sb.append('}');
        return sb.toString();
    }
}
