package me.minotopia.minoduel;

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.Console;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.SimpleInjector;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import me.minotopia.minoduel.arena.ArenaManager;
import me.minotopia.minoduel.cmd.CommandsAdmin;
import me.minotopia.minoduel.cmd.CommandsArena;
import me.minotopia.minoduel.cmd.CommandsPlayer;
import me.minotopia.minoduel.hook.EssentialsHook;
import me.minotopia.minoduel.hook.MTCHook;
import me.minotopia.minoduel.hook.XLoginHook;
import me.minotopia.minoduel.listener.DefaultLocationListener;
import me.minotopia.minoduel.listener.IllegalItemListener;
import me.minotopia.minoduel.listener.MainListener;
import me.minotopia.minoduel.queue.WaitingQueueManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.xxyy.common.chat.XyComponentBuilder;
import io.github.xxyy.common.version.PluginVersion;

import java.lang.reflect.Method;

public class MinoDuelPlugin extends JavaPlugin {
    private static class PlayerOnlyCommandException extends RuntimeException { //Hack to work around hasPermission(...) not declaring the checked CommandException exception

        public PlayerOnlyCommandException(String desc) {
            super(desc);
        }
    }

    public static final String PREFIX = "§6[§a1vs1§6] ";
    private static final XyComponentBuilder PREFIX_BUILDER = new XyComponentBuilder("[").color(ChatColor.GOLD)
            .append("1vs1", ChatColor.GREEN)
            .append("] ", ChatColor.GOLD);
    public static final PluginVersion VERSION = PluginVersion.ofClass(MinoDuelPlugin.class);
    private long teleportDelayTicks;
    private CommandsManager<CommandSender> commandsManager = new CommandsManager<CommandSender>() {
        @Override
        public boolean hasPermission(CommandSender sender, String perm) {
            return sender instanceof ConsoleCommandSender || sender.hasPermission(perm);
        }

        @Override
        protected boolean hasPermission(Method method, CommandSender player) { //sneaky hack bcuz @Console has no effect by default
            if (method.isAnnotationPresent(Console.class) && !(player instanceof Player)) {
                throw new PlayerOnlyCommandException("Du kannst diesen Befehl nur als Spieler ausführen!");
            }

            return super.hasPermission(method, player);
        }
    };
    private DuelRequestManager requestManager = new DuelRequestManager();
    private CommandHelpHelper helpHelper = new CommandHelpHelper(commandsManager);
    private ArenaManager arenaManager = new ArenaManager(this);
    private WaitingQueueManager queueManager = new WaitingQueueManager(arenaManager);
    private MTCHook mtcHook;
    private EssentialsHook essentialsHook;
    private InventorySaver inventorySaver;
    private LocationSaver locationSaver;

    @Override
    public void onEnable() {
        //Initialize config
        saveDefaultConfig(); //Doesn't save if exists
        getConfig().options().header("MinoDuel config file! Any changes in here will get overridden on reload - Use the ingame config editing commands.")
                .copyHeader(true)
                .copyDefaults(true);

        teleportDelayTicks = getConfig().getLong("tp-delay-seconds") * 20L;
        getConfig().addDefault("find-item-markers", false);

        inventorySaver = new InventorySaver(this);
        locationSaver = new LocationSaver(this);

        //Load arenas
        arenaManager.initialise();
        arenaManager.reloadArenas(getConfig());

        //Register dem commands
        commandsManager.setInjector(new SimpleInjector(this));
        CommandsManagerRegistration reg = new CommandsManagerRegistration(this, commandsManager);
        reg.register(CommandsPlayer.class);
        reg.register(CommandsAdmin.class);
        reg.register(CommandsArena.class);

        //Register Bukkit API stuffs
        getServer().getPluginManager().registerEvents(new MainListener(this), this);
        if (getConfig().getBoolean("find-item-markers", false)) {
            getServer().getPluginManager().registerEvents(new IllegalItemListener(this), this);
        }

        //Automagically save config every 5 minutes to minimize data-loss on crash
        getServer().getScheduler().runTaskTimer(this, this::saveConfig,
                5L * 60L * 20L, 5L * 60L * 20L); //And yes, the compiler does actually optimize that calculation away so quit complaining kthnx

        //Hook stuffs
        mtcHook = new MTCHook(this).tryHook();
        essentialsHook = new EssentialsHook(this).tryHook();

        if (!new XLoginHook(this).tryHook().isHooked()) {
            getServer().getPluginManager().registerEvents(new DefaultLocationListener(this), this);
        }

        //Give players their items back if their game was killed due to a reload
        getServer().getOnlinePlayers().forEach(getInventorySaver()::loadInventoryWithMessage);
        getServer().getOnlinePlayers().forEach(getLocationSaver()::loadLocationWithMessage);

        getLogger().info("Enabled " + VERSION.toString() + "!");
    }

    @Override
    public void onDisable() {
        //Make sure we save the config and make it impossible to override by editing manually.
        saveConfig();
        queueManager.notifyReload();
        arenaManager.onReload();
        inventorySaver.onReload();
        locationSaver.onReload();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { //sk89q command framework - https://forums.bukkit.org/threads/tut-using-sk89qs-command-framework.185423/
        try {
            commandsManager.execute(command.getName(), args, sender, /* method args: */ sender);
        } catch (MissingNestedCommandException mnce) {
            sender.sendMessage("§c" + mnce.getUsage());
            helpHelper.sendNestedHelp(sender, command, args);
        } catch (CommandUsageException cue) {
            sender.sendMessage("§c" + cue.getMessage());
            sender.sendMessage("§c" + cue.getUsage());
        } catch (WrappedCommandException wce) {
            if (wce.getCause() instanceof NumberFormatException) {
                sender.sendMessage("§cZahl benötigt, Zeichenkette übergeben.");
            } else {
                sender.sendMessage("§cEin Fehler ist aufgetreten, wir bitten um Verzeihung. " +
                        "Sollte dies erneut passieren, bitte melde das im Forum (Mit der aktuellen Uhrzeit): http://minotopia.me/forum/");
                wce.printStackTrace();
            }
        } catch (CommandPermissionsException cpe) {
            sender.sendMessage(getPrefix() + "§cDu bist nicht berechtigt, diese Aktion auszuführen.");
        } catch (CommandException | PlayerOnlyCommandException e) {
            sender.sendMessage("§c" + e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException ise) {
            sender.sendMessage("§cInterner Fehler: " + ise.getMessage() + " - Sollte dies öfter auftreten, melde dies bitte im Forum!");
            ise.printStackTrace();
        }

        return true;
    }

    public long getTeleportDelayTicks() {
        return teleportDelayTicks;
    }

    public String getPrefix() {
        return PREFIX;
    }

    public DuelRequestManager getRequestManager() {
        return requestManager;
    }

    public WaitingQueueManager getQueueManager() {
        return queueManager;
    }

    public XyComponentBuilder getPrefixBuilder() {
        return new XyComponentBuilder(PREFIX_BUILDER);
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public MTCHook getMtcHook() {
        return mtcHook;
    }

    public EssentialsHook getEssentialsHook() {
        return essentialsHook;
    }

    public InventorySaver getInventorySaver() {
        return inventorySaver;
    }

    public LocationSaver getLocationSaver() {
        return locationSaver;
    }
}
