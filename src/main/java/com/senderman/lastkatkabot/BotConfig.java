package com.senderman.lastkatkabot;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.util.Set;

public class BotConfig {

    @NotBlank
    @JsonProperty(required = true)
    private String token;

    @NotBlank
    @JsonProperty(required = true)
    private String username;

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
    private Set<String> veganCommands;

    @JsonProperty
    private Set<String> wwBots;

    @JsonProperty
    private Set<Integer> admins;

    @JsonProperty
    private String help;

    @JsonProperty
    private String announce;

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

    public Set<Integer> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<Integer> admins) {
        this.admins = admins;
    }

    public String getTourchannel() {
        return tourchannel;
    }

    public void setTourchannel(String tourchannel) {
        this.tourchannel = tourchannel;
    }

    public Set<String> getVeganCommands() {
        return veganCommands;
    }

    public void setVeganCommands(Set<String> veganCommands) {
        this.veganCommands = veganCommands;
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

    public String getAnnounce() {
        return announce;
    }

    public void setAnnounce(String announce) {
        this.announce = announce;
    }

    @Override
    public String toString() {
        return "BotConfig{" +
                "token='" + token + '\'' +
                ", username='" + username + '\'' +
                ", lastkatka=" + lastkatka +
                ", lastvegan=" + lastvegan +
                ", tourgroup=" + tourgroup +
                ", tourchannel='" + tourchannel + '\'' +
                ", tourgroupname='" + tourgroupname + '\'' +
                ", veganCommands=" + veganCommands +
                ", wwBots=" + wwBots +
                ", admins=" + admins +
                ", help='" + help + '\'' +
                '}';
    }
}