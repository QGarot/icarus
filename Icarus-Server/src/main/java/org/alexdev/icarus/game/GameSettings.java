package org.alexdev.icarus.game;

import org.alexdev.icarus.util.config.Configuration;

public class GameSettings {

    public static int MAX_ROOMS_PER_ACCOUNT;
    public static int MAX_ROOMS_POPULAR;
    public static int MAX_ROOMS_SUB_CATEGORIES;

    public static int CREDITS_INTERVAL_MINUTES;
    public static int CREDITS_INTERVAL_AMOUNT;

    public static int DUCKETS_INTERVAL_MINUTES;
    public static int DUCKETS_INTERVAL_AMOUNT;

    public static int CHAT_FLOOD_SECONDS = 4;
    public static int CHAT_FLOOD_WAIT = 20;
    public static int MAX_CHAT_BEFORE_FLOOD = 8;

    public static boolean BOT_SPAMMERS_ALLOW;
    public static String BOT_SPAMMERS_SSO_PREFIX;

    public static final double FURNITURE_OFFSET = 0.001;

    public static void load() {
        MAX_ROOMS_PER_ACCOUNT = Configuration.getInstance().getGameConfig().get("Navigator", "max.room.per.user", Integer.class);
        MAX_ROOMS_POPULAR = Configuration.getInstance().getGameConfig().get("Navigator", "max.rooms.popular.tab", Integer.class);
        MAX_ROOMS_SUB_CATEGORIES = Configuration.getInstance().getGameConfig().get("Navigator", "max.room.sub.category", Integer.class);
        CREDITS_INTERVAL_MINUTES = Configuration.getInstance().getGameConfig().get("Scheduler", "credits.interval.minutes", Integer.class);
        CREDITS_INTERVAL_AMOUNT = Configuration.getInstance().getGameConfig().get("Scheduler", "credits.interval.amount", Integer.class);
        DUCKETS_INTERVAL_MINUTES = Configuration.getInstance().getGameConfig().get("Scheduler", "duckets.interval.minutes", Integer.class);
        DUCKETS_INTERVAL_AMOUNT = Configuration.getInstance().getGameConfig().get("Scheduler", "duckets.interval.amount", Integer.class);
        BOT_SPAMMERS_ALLOW = Configuration.getInstance().getGameConfig().get("Bots", "bot.spammers.allow", Boolean.class);
        BOT_SPAMMERS_SSO_PREFIX = Configuration.getInstance().getGameConfig().get("Bots", "bot.spammer.sso.ticket.prefix", String.class);
    }
}
