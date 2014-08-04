package me.sebi7224.onevsone;

import me.sebi7224.onevsone.arena.Arenas;
import me.sebi7224.onevsone.cmd.Command1vs1;
import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin {

    public static final String PREFIX = "§6[§a1vs1§6] ";
    private static MainClass instance;
    private long teleportDelayTicks;

    @Override
    public void onEnable() {
        instance = this;

        //Initialize config
        saveDefaultConfig(); //Doesn't save it it exists
        getConfig().options().header("1vs1 config file! Any changes in here will get overridden on reload - Use the ingame config editing commands.");
        getConfig().options().copyHeader(true);
        teleportDelayTicks = getConfig().getLong("tp-delay-seconds") * 20L;

        //Load arenas
        Arenas.reloadArenas(getConfig());

        //Register Bukkit API stuffs
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getCommand("1vs1").setExecutor(new Command1vs1(this));

        //Automagically save config every 5 minutes to minimize data-loss on crash
        getServer().getScheduler().runTaskTimer(this, this::saveConfig, 5L * 60L * 20L, 5L * 60L * 20L); //And yes, the compiler does actually optimize that calculation away so quit complaining kthnx
    }

    @Override
    public void onDisable() {
        //Make sure we save the config and make it impossible to override by editing manually.
        saveConfig();
    }

    public long getTeleportDelayTicks() {
        return teleportDelayTicks;
    }

    public static MainClass instance() {
        return MainClass.getInstance();
    }

    public static String getPrefix() {
        return PREFIX;
    }

    public static MainClass getInstance() {
        return instance;
    }
}
