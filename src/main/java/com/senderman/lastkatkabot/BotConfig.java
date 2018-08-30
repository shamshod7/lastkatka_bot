package com.senderman.lastkatkabot;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;

public class BotConfig {

    @NotBlank
    @JsonProperty(required = true)
    private String token;

    @NotBlank
    @JsonProperty(required = true)
    private String username;

    private long lastkatka;
    private long lastvegan;
    private long tourgroup;

    private String tourchannel;

    private String tourgroupname;
    private ArrayList<String> veganCommands;

    private ArrayList<Integer> admins;

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

    public ArrayList<Integer> getAdmins() {
        return admins;
    }

    public void setAdmins(ArrayList<Integer> admins) {
        this.admins = admins;
    }

    public String getTourchannel() {
        return tourchannel;
    }

    public void setTourchannel(String tourchannel) {
        this.tourchannel = tourchannel;
    }

    public ArrayList<String> getVeganCommands() {
        return veganCommands;
    }

    public void setVeganCommands(ArrayList<String> veganCommands) {
        this.veganCommands = veganCommands;
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
                ", veganCommands=" + veganCommands +
                ", admins=" + admins +
                '}';
    }
}
