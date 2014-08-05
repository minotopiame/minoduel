package me.sebi7224.minoduel.cmd;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Console;
import com.sk89q.minecraft.util.commands.NestedCommand;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.xxyy.common.util.CommandHelper;

import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RED;

/**
 * Provides commands for managing arenas.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 4.8.14 // 1.0
 */
public class CommandsArena {
    @Command(aliases = {"mdarena", "mda"}, desc = "Arenamanagement für MinoDuel")
    @NestedCommand(SubCommands.class)
    public void mdaMain() {
        //body is ignored
    }

    public class SubCommands {
        @Command(aliases = {"create", "new"},
                desc = "Erstellt eine neue Arena.", min = 1)
        @CommandPermissions({"minoduel.arena.create"})
        public void create(CommandContext args, Player player) throws CommandException {
            String arenaName = args.getString(0);

            if (Arenas.existsByName(arenaName)) {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§cDie Arena §4" + arenaName + " §cexistiert bereits!");
                return;
            }

            Arenas.createArena(arenaName, MinoDuelPlugin.inst().getConfig());
            player.sendMessage(MinoDuelPlugin.getPrefix() + "§aDu hast die Arena §2" + arenaName + " §aerfolgreich erstellt!");
        }

        @Command(aliases = {"remove", "delete", "rem"},
                desc = "Löscht eine Arena.",
                usage = "[Arena]",
                flags = "y", min = 1)
        @CommandPermissions({"minoduel.arena.remove"})
        public void remove(CommandContext args, Player player) throws CommandException {
            String arenaName = args.getString(0);

            if(!args.hasFlag('y')) {
                new FancyMessage("Möchtest du die Arena ")
                            .color(RED)
                        .then(arenaName)
                            .color(DARK_RED)
                        .then("wirklich entfernen? ")
                            .color(RED)
                        .then("[Js (klick)]")
                            .color(GOLD)
                            .command("/mdarena remove -y " + arenaName)
                            .tooltip("Dies entfernt die Arena und alle Optionen permanent!")
                        .send(player);
                return;
            }

            Arena arena = Arenas.byName(arenaName);

            if (arena == null) {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§cDie Arena §4" + arenaName + " §cexistiert nicht!");
                return;
            }

            arena.remove();
            player.sendMessage(MinoDuelPlugin.getPrefix() + "§aDie Arena §2" + arenaName + " §awurde erfolgreich entfernt!");
        }

        @Command(aliases = {"status", "checklist"},
                desc = "Zeigt den Status einer Arena an.",
                usage = "[Arena]", min = 1)
        @CommandPermissions({"minoduel.arena.status"})
        @Console
        public void checklist(CommandContext args, CommandSender player) throws CommandException {
            String arenaName = args.getString(0);
            Arena arena = Arenas.byName(arenaName);

            if (arena == null) {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§cDie Arena §4" + arenaName + " §cexistiert nicht!");
                return;
            }

            arena.sendChecklist(player);
        }

        @Command(aliases = {"set", "s"},
                desc = "Setzt Arenaoptionen.")
        @NestedCommand(SetCommands.class)
        @CommandPermissions({"minoduel.arena.set"})
        public void set() {

        }

        public class SetCommands {
            @Command(aliases = {"join", "new"},
                    desc = "Lässt dich eine Arena auswählen, um ein 1vs1 zu beginnen! (Verwende -a, um das Menü zu unterdrücken)",
                    usage = "<-a [Arena|'egal']>",
                    flags = ":a") //FIXME
            public void spawn() {
                if (args.length < 3) {
                    player.sendMessage("§cÜberprüfe bitte deine Argumente!");
                    return true;
                }
                if (args[1].equalsIgnoreCase("1")) {
                    Arenas.saveLocation("arenas." + args[2] + ".Spawn1", player.getLocation());
                    player.sendMessage(MainClass.getPrefix() + "§7Du hast den §61. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                    return true;
                } else if (args[1].equalsIgnoreCase("2")) {
                    Arenas.saveLocation("arenas." + args[2] + ".Spawn2", player.getLocation());
                    player.sendMessage(MainClass.getPrefix() + "§7Du hast den §62. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                    return true;
                } else {
                    player.sendMessage(MainClass.getPrefix() + "§cDu kannst nur den Spawn 1 und 2 setzen!");
                    return true;
                }
            }

            @Command(aliases = {"join", "new"},
                    desc = "Lässt dich eine Arena auswählen, um ein 1vs1 zu beginnen! (Verwende -a, um das Menü zu unterdrücken)",
                    usage = "<-a [Arena|'egal']>",
                    flags = ":a") //FIXME
            public void icon() {
                if (args.length < 2) {
                    player.sendMessage("§cDu hast keinen Arenanamen angegeben!");
                    return true;
                }
                if (player.getItemInHand().getType() == Material.AIR || player.getItemInHand() == null) {
                    player.sendMessage(MainClass.getPrefix() + "§cDu hast kein §4Item §cin deiner §4Hand§c!");
                    return true;
                }
                Arenas.setArenaIconItem(args[1], player.getItemInHand().getType());
                player.sendMessage(MainClass.getPrefix() + "§7Du hast das §6Icon§7 der Arena §6" + args[1] + " §7gesetzt!");
                return true;
            }

            @Command(aliases = {"join", "new"},
                    desc = "Lässt dich eine Arena auswählen, um ein 1vs1 zu beginnen! (Verwende -a, um das Menü zu unterdrücken)",
                    usage = "<-a [Arena|'egal']>",
                    flags = ":a") //FIXME
            public void kit() {
                if (args.length < 2) {
                    player.sendMessage("§cDu hast keinen Arenanamen angegeben!");
                    return true;
                }
                if (player.getInventory().getContents().length == 0) {
                    player.sendMessage(MainClass.getPrefix() + "§cDu hast keine §4Items §cin deinem §4Inventar§c!");
                    return true;
                }
                for (int i = 0; player.getInventory().getContents().length < i; i++) {
                    if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                        MainClass.inst().getConfig().set("arenas." + args[1] + ".items", player.getInventory().getContents()[i]);
                    }
                }
                for (int i = 0; player.getInventory().getArmorContents().length < i; i++) {
                    if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                        MainClass.inst().getConfig().set("arenas." + args[1] + ".armor", player.getInventory().getArmorContents()[i]);
                    }
                }
                MainClass.inst().saveConfig();
                player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich die Items in der Arena §6" + args[1] + " §7gesetzt!");
                return true;
            }

            @Command(aliases = {"join", "new"},
                    desc = "Lässt dich eine Arena auswählen, um ein 1vs1 zu beginnen! (Verwende -a, um das Menü zu unterdrücken)",
                    usage = "<-a [Arena|'egal']>",
                    flags = ":a")
            public void reward() {
                if (args.length < 2) {
                    player.sendMessage("§cBitte überprüfe deine Argumente!");
                    return true;
                }
                if (player.getInventory().getContents().length == 0) {
                    player.sendMessage(MainClass.getPrefix() + "§cDu hast keine §4Items §cin deinem §4Inventar§c!");
                    return true;
                }
                if (args[1].equalsIgnoreCase("global")) {
                    for (int i = 0; player.getInventory().getContents().length < i; i++) {
                        if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                            MainClass.inst().getConfig().set("globalRewards", player.getInventory().getContents()[i]);
                        }
                    }
                    MainClass.inst().saveConfig();
                    player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich den globalen Reward gesetzt!");
                    return true;
                } else {
                    if (Arenas.exists(args[1])) {
                        for (int i = 0; player.getInventory().getContents().length < i; i++) {
                            if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                                MainClass.inst().getConfig().set("arenas" + args[1] + ".rewards", player.getInventory().getContents()[i]);
                            }
                        }
                        MainClass.inst().saveConfig();
                        player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich den Reward in Arena §e" + args[1] + "§7gesetzt!");
                        return true;
                    } else {
                        player.sendMessage(MainClass.getPrefix() + "§7Die Arena §e" + args[1] + " §7existiert nicht!");
                        return true;
                    }
                }
            }
        }
    }
}
