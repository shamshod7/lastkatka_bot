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

    @JsonProperty
    private String mainadminhelp;

    @JsonProperty
    private String bncphoto;

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

    public String getMainadminhelp() {
        return mainadminhelp;
    }

    public void setMainadminhelp(String mainadminhelp) {
        this.mainadminhelp = mainadminhelp;
    }

    public String getBncphoto() {
        return bncphoto;
    }

    public void setBncphoto(String bncphoto) {
        this.bncphoto = bncphoto;
    }
}