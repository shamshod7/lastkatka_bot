package com.senderman.lastkatkabot;

import com.annimon.tgbotsmodule.BotHandler;
import com.annimon.tgbotsmodule.BotModule;
import com.annimon.tgbotsmodule.beans.Config;
import com.annimon.tgbotsmodule.services.YamlConfigLoaderService;
import org.jetbrains.annotations.NotNull;

public class LastkatkaBot implements BotModule {

    public static final String CALLBACK_REGISTER_IN_TOURNAMENT = "register_in_tournament";
    public static final String CALLBACK_PAY_RESPECTS = "pay_respects";
    public static final String CALLBACK_CAKE_OK = "cake ok";
    public static final String CALLBACK_CAKE_NOT = "cake not";
    public static final String JOIN_DUEL = "join_duel";

    @NotNull
    @Override
    public BotHandler botHandler(@NotNull Config config) {
        final var configLoader = new YamlConfigLoaderService<BotConfig>();
        final var configFile = configLoader.configFile("lastkatkabot", config.getProfile());
        final var botConfig = configLoader.load(configFile, BotConfig.class);
        return new LastkatkaBotHandler(botConfig);
    }
}
