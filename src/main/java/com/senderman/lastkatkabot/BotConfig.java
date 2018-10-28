package com.senderman.lastkatkabot;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class BotConfig {

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BotConfig{");
        sb.append("lastkatka=").append(lastkatka);
        sb.append(", lastvegan=").append(lastvegan);
        sb.append(", tourgroup=").append(tourgroup);
        sb.append(", tourchannel='").append(tourchannel).append('\'');
        sb.append(", tourgroupname='").append(tourgroupname).append('\'');
        sb.append(", wwBots=").append(wwBots);
        sb.append(", help='").append(help).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
