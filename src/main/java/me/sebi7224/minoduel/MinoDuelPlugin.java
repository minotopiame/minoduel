package me.sebi7224.minoduel;

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.Console;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.SimpleInjector;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import me.sebi7224.minoduel.cmd.CommandsAdmin;
import me.sebi7224.minoduel.cmd.CommandsArena;
import me.sebi7224.minoduel.cmd.CommandsPlayer;
import me.sebi7224.minoduel.queue.DuelWaitingQueue;
import me.sebi7224.minoduel.util.IconMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.xxyy.common.util.inventory.InventoryHelper;

import java.lang.reflect.Method;

public class MinoDuelPlugin extends JavaPlugin {
    private static class PlayerOnlyCommandException extends RuntimeException { //Hack to work around hasPermission(...) not declaring the checked CommandException exception
        public PlayerOnlyCommandException(String desc) {
            super(desc);
        }
    }

    public static final String PREFIX = "§6[§a1vs1§6] ";
    private static MinoDuelPlugin instance;
    private long teleportDelayTicks;
    private CommandsManager<CommandSender> commandsManager = new CommandsManager<CommandSender>() {
        @Override
        public boolean hasPermission(CommandSender sender, String perm) {
            return sender instanceof ConsoleCommandSender || sender.hasPermission(perm);
        }

        @Override
        protected boolean hasPermission(Method method, CommandSender player) { //sneaky hack bcuz @Console has no effect by default
            if(method.isAnnotationPresent(Console.class) && !(player instanceof Player)) {
                throw new PlayerOnlyCommandException("Du kannst diesen Befehl nur als Spieler ausführen!");
            }

            return super.hasPermission(method, player);
        }
    };
    private IconMenu arenaMenu;

    @Override
    public void onEnable() {
        instance = this;

        //Initialize config
        saveDefaultConfig(); //Doesn't save if exists
        getConfig().options().header("MinoDuel config file! Any changes in here will get overridden on reload - Use the ingame config editing commands.");
        getConfig().options().copyHeader(true);
        teleportDelayTicks = getConfig().getLong("tp-delay-seconds") * 20L;

        //Load arenas
        Arenas.reloadArenas(getConfig());
        initArenaMenu();

        //Register dem commands
        commandsManager.setInjector(new SimpleInjector(this));
        CommandsManagerRegistration reg = new CommandsManagerRegistration(this, commandsManager);
        reg.register(CommandsPlayer.class);
        reg.register(CommandsAdmin.class);
        reg.register(CommandsArena.class);

        //Register Bukkit API stuffs
        getServer().getPluginManager().registerEvents(new MainListener(), this);

        //Automagically save config every 5 minutes to minimize data-loss on crash
        getServer().getScheduler().runTaskTimer(this, this::saveConfig,
                5L * 60L * 20L, 5L * 60L * 20L); //And yes, the compiler does actually optimize that calculation away so quit complaining kthnx
    }

    @Override
    public void onDisable() {
        //Make sure we save the config and make it impossible to override by editing manually.
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { //sk89q command framework - https://forums.bukkit.org/threads/tut-using-sk89qs-command-framework.185423/
        try {
            commandsManager.execute(command.getName(), args, sender, sender);
        } catch (MissingNestedCommandException mnce) {
            sender.sendMessage("§c" + mnce.getUsage());
        } catch (CommandUsageException cue) {
            sender.sendMessage("§c" + cue.getMessage());
            sender.sendMessage("§c" + cue.getUsage());
        } catch (WrappedCommandException wce) {
            if (wce.getCause() instanceof NumberFormatException) {
                sender.sendMessage("§cZahl benötigt, Zeichenkette übergeben.");
            } else {
                sender.sendMessage("§cEin Fehler ist aufgetreten, wir bitten um Verzeihung. " +
                        "Sollte dies erneut passieren, bitte melde das im Forum: http://minotopia.me/forum/");
                wce.printStackTrace();
            }
        } catch (CommandPermissionsException cpe) {
            sender.sendMessage(getPrefix() + "§cDu bist nicht berechtigt, diese Aktion auszuführen.");
        } catch (CommandException | PlayerOnlyCommandException e) {
            sender.sendMessage("§c" + e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException ise) {
            sender.sendMessage("§cInterner Fehler: "+ise.getMessage()+" - Sollte dies öfter auftreten, melde dies bitte im Forum!");
            ise.printStackTrace();
        }

        return true;
    }

    private void initArenaMenu() {
        arenaMenu = new IconMenu("§8Wähle eine Arena!", //Title
                InventoryHelper.validateInventorySize(Arenas.all().size() + 1), //Round arena amount up to next valid inv size - Need +1 so that at least one "any arena" option is included
                event -> {
                    Player player = event.getPlayer();
                    Arena arena = event.getArena();

                    player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
                    DuelWaitingQueue.enqueue(player, arena); //This takes care of teleportation etc if a match is found
                    player.sendMessage(getPrefix() + "Du bist nun in der Warteschlange" +
                            (arena == null ? "" : " für die Arena §e" + arena.getName() + "§6") +
                            "!");
                }, this);

        int i = 0;
        for (Arena arena : Arenas.all()) {
            arenaMenu.setArena(i, arena);
            i++;
        }
    }

    public long getTeleportDelayTicks() {
        return teleportDelayTicks;
    }

    public static MinoDuelPlugin inst() {
        return MinoDuelPlugin.getInstance();
    }

    public String getPrefix() {
        return PREFIX;
    }

    public static MinoDuelPlugin getInstance() {
        return instance;
    }

    public IconMenu getArenaMenu() {
        return arenaMenu;
    }
}
