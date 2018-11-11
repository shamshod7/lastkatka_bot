package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import com.annimon.tgbotsmodule.BotModule;
import com.annimon.tgbotsmodule.Runner;
import com.annimon.tgbotsmodule.beans.Config;
import com.annimon.tgbotsmodule.services.YamlConfigLoaderService;

import java.util.List;

public class LastkatkaBot implements BotModule {

    public static final String CALLBACK_REGISTER_IN_TOURNAMENT = "register_in_tournament";
    public static final String CALLBACK_PAY_RESPECTS = "pay_respects";
    public static final String CALLBACK_CAKE_OK = "cake ok";
    public static final String CALLBACK_CAKE_NOT = "cake not";
    public static final String JOIN_DUEL = "join_duel";
    public static int mainAdmin;

    public static void main(String[] args) {
        final var profile = (args.length >= 1 && !args[0].isEmpty()) ? args[0] : "";
        Runner.run(profile, List.of(new LastkatkaBot()));
    }

    @Override
    public BotHandler botHandler(Config config) {
        final var configLoader = new YamlConfigLoaderService<BotConfig>();
        final var configFile = configLoader.configFile("lastkatkabot", config.getProfile());
        final var botConfig = configLoader.load(configFile, BotConfig.class);
        mainAdmin = botConfig.getMainAdmin();
        return new LastkatkaBotHandler(botConfig);
    }
}
