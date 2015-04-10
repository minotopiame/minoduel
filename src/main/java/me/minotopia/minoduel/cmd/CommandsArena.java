package me.minotopia.minoduel.cmd;

import com.google.common.collect.Lists;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Console;
import com.sk89q.minecraft.util.commands.NestedCommand;
import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.arena.Arena;
import mkremins.fanciful.FancyMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.xxyy.common.util.LocationHelper;

import java.util.List;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.DARK_BLUE;
import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.YELLOW;

/**
 * Provides commands for managing arenas.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 4.8.14 // 1.0
 */
public class CommandsArena {
    @SuppressWarnings("UnusedParameters")
    public CommandsArena(MinoDuelPlugin plugin) {

    }

    @Command(aliases = {"mda", "mdarena"}, desc = "Arenamanagement für MinoDuel.")
    @NestedCommand(SubCommands.class)
    public static void mdaMain() {
        //body is ignored
    }

    public static class SubCommands {
        private final MinoDuelPlugin plugin;

        public SubCommands(MinoDuelPlugin plugin) {
            this.plugin = plugin;
        }

        @Command(aliases = {"create", "new"},
                usage = "<Name>",
                desc = "Erstellt eine neue Arena.", min = 1)
        @CommandPermissions({"minoduel.arena.create"})
        public void arenaCreate(CommandContext args, Player player) throws CommandException {
            String arenaName = args.getString(0);

            if (plugin.getArenaManager().existsByName(arenaName)) {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§cDie Arena §4" + arenaName + " §cexistiert bereits!");
                return;
            }

            plugin.getArenaManager().createArena(arenaName, plugin.getConfig());
            player.sendMessage(MinoDuelPlugin.PREFIX + "§aDu hast die Arena §2" + arenaName + " §aerfolgreich erstellt!");
        }

        @Command(aliases = {"remove"},
                desc = "Löscht eine Arena.",
                usage = "<Arena>",
                flags = "y", min = 1)
        @CommandPermissions({"minoduel.arena.remove"})
        public void arenaRemove(CommandContext args, Player player) throws CommandException {
            String arenaName = args.getString(0);

            if (!args.hasFlag('y')) {
                //@formatter:off
                new FancyMessage("Möchtest du die Arena ")
                            .color(RED)
                        .then(arenaName)
                            .color(DARK_RED)
                        .then("wirklich entfernen? ")
                            .color(RED)
                        .then("[Ja (klick)]")
                            .color(GOLD)
                            .command("/mdarena remove -y " + arenaName)
                            .tooltip("Dies entfernt die Arena und alle Optionen permanent!")
                        .send(player);
                return;
                //@formatter:on
            }

            Arena arena = CmdValidate.getArenaChecked(arenaName, plugin.getArenaManager());

            arena.remove();
            player.sendMessage(MinoDuelPlugin.PREFIX + "§aDie Arena §2" + arenaName + " §awurde erfolgreich entfernt!");
        }

        @Command(aliases = {"status", "info"},
                desc = "Zeigt den Status einer Arena an.",
                usage = "<Arena>", min = 1)
        @CommandPermissions({"minoduel.arena.status"})
        @Console
        public void arenaChecklist(CommandContext args, CommandSender player) throws CommandException {
            Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());
            player.sendMessage("§6Arena: " + (arena.isValid() ? "§a" : "§c") + arena.getName());
            sendLocationInfo(player, arena.getFirstSpawn(), 1);
            sendLocationInfo(player, arena.getSecondSpawn(), 2);

            arena.sendChecklist(player); //TODO click on checklist to get commands suggested
        }

        @Command(aliases = {"set"},
                desc = "Setzt Arenaoptionen.")
        @NestedCommand(SetCommands.class)
        @CommandPermissions({"minoduel.arena.set"})
        public void arenaSetOption() {

        }

        public static class SetCommands {
            private final MinoDuelPlugin plugin;

            public SetCommands(MinoDuelPlugin plugin) {
                this.plugin = plugin;
            }

            @Command(aliases = {"spawn", "sporn"}, //kek
                    desc = "Setzt den Spawn einer Arena zu deiner Position!",
                    usage = "<Arena> <1|2>", min = 1)
            public void arenaSetSpawn(CommandContext args, Player player) throws CommandException {
                Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());

                switch (args.getString(1, "ui")) { //No need to convert to int since there are only two cases & we can't pass the int anyways
                    case "1":
                        arena.setFirstSpawn(player.getLocation());
                        break;
                    case "2":
                        arena.setSecondSpawn(player.getLocation());
                        break;
                    default:
                        //@formatter:off
                        new FancyMessage("Unbekannte Spawnzahl! ")
                                    .color(RED)
                                .then("Wolltest du vielleicht: ")
                                    .color(YELLOW)
                                .send(player);
                        new FancyMessage("[Spawn 1 setzen] ")
                                    .color(DARK_BLUE)
                                    .tooltip("/mda set spawn " + arena.getName() + " 1")
                                    .command("/mda set spawn " + arena.getName() + " 1")
                                .then("[Spawn 2 setzen] ")
                                    .color(DARK_RED)
                                    .tooltip("/mda set spawn " + arena.getName() + " 2")
                                    .command("/mda set spawn " + arena.getName() + " 2")
                                .send(player);
                        return;
                    //@formatter:on
                }

                player.sendMessage(plugin.getPrefix() + "§aSpawn §2" + args.getString(1) + " §afür §2" + arena.getName() + " §agesetzt.");
            }

            @Command(aliases = {"icon"},
                    desc = "Setzt das Icon einer Arena zu dem Item in deiner Hand!",
                    usage = "<Arena>", min = 1)
            public void arenaSetIcon(CommandContext args, Player player) throws CommandException {
                Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());

                ItemStack newIcon = CmdValidate.validateStackNotEmpty(player.getItemInHand()); //CommandException if AIR or null

                arena.setIconStack(newIcon);

                player.sendMessage(MinoDuelPlugin.PREFIX + "§aDu hast das Icon der Arena §2" + arena.getName() + " §agesetzt!");
            }

            @Command(aliases = {"kit"},
                    desc = "Setzt das Kit einer Arena zu deinem aktuellen Inventar",
                    help = "Setzt das Kit einer Arena.\n" +
                            "-a: Setzt nur deine Rüstung\n" +
                            "-i: Setzt nur dein Inventar\n" +
                            "Keines: Setzt beides",
                    usage = "<Arena>",
                    flags = "ai", min = 1, max = 1)
            public void arenaSetKit(CommandContext args, Player player) throws CommandException {
                Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());

                if (args.hasFlag('a') || !args.hasFlag('i')) {
                    arena.setArmorKit(player.getInventory().getArmorContents());
                    player.sendMessage(MinoDuelPlugin.PREFIX + "§aRüstung für §2" + arena.getName() + "§a gesetzt!");
                }

                if (args.hasFlag('i') || !args.hasFlag('a')) {
                    arena.setInventoryKit(player.getInventory().getContents());
                    player.sendMessage(MinoDuelPlugin.PREFIX + "§aHauptkit für §2" + arena.getName() + "§a gesetzt!");
                }
            }

            @Command(aliases = {"reward"},
                    desc = "Setzt die Arenabelohnung!",
                    help = "Setzt die Belohnung einer Arena.\n" +
                            "-s: Quelle der Belohnung\n" +
                            "-r: Gibt jedem Gewinner nur einen zufälligen\n" +
                            "    Stack aus der Belohnung",
                    usage = "[-s *Hotbar*|Hand|Inv] <Arena>",
                    flags = "s:r", min = 1, max = 1)
            public void arenaSetReward(CommandContext args, Player player) throws CommandException {
                Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());

                List<ItemStack> stacks;

                switch (args.getFlag('s', "Hotbar").toLowerCase()) {
                    case "hand":
                        stacks = Lists.newArrayList(player.getItemInHand());
                        break;
                    case "inv":
                    case "i":
                    case "inventar":
                        stacks = Lists.newArrayList(player.getInventory().getContents());
                        break;
                    case "hotbar":
                    case "hb":
                        stacks = Lists.newArrayList(player.getInventory().getContents()).stream()
                                .limit(9) //9 Hotbar slots - IDs 0-8
                                .collect(Collectors.toList());
                        break;
                    default:
                        player.sendMessage("§cUnbekannte Quelle! Valide Quellen: -s [Hotbar|Hand|Inv]");
                        return;
                }

                stacks.removeIf(stack -> stack == null || stack.getType() == Material.AIR);

                CmdValidate.validateNotEmpty(stacks, "Die Quelle (" + args.getFlag('s', "Hotbar") + ") ist leer!");

                arena.setRewards(stacks);
                arena.setDoAllRewards(!args.hasFlag('r')); //When randomness is not asked for, we wanna give everything

                player.sendMessage("§aBelohnung für Arena §2" + arena.getName() + "§a gesetzt!");
            }

            @Command(aliases = {"enabled"},
                    desc = "Aktiviert oder deaktiviert eine Arena",
                    usage = "<Arena> <true|false>", min = 2)
            public void arenaSetEnabled(CommandContext args, Player player) throws CommandException {
                Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());

                arena.setEnabled(CmdValidate.getBooleanChecked(args.getString(1)));

                player.sendMessage("§aDie Arena §2" + arena.getName() + "§a wurde " +
                        (arena.isEnabled() ? "§2" : "§4de") +
                        "aktiviert§a!");
            }
        }

        private void sendLocationInfo(CommandSender player, Location location, int spawnId) {
            FancyMessage message = new FancyMessage("Spawn " + spawnId + ": ").color(GOLD);
            //@formatter:off
            if (location == null) {
                message.then("nicht gesetzt! [Hier setzen]")
                        .color(RED)
                        .tooltip("Hier klicken für", "/mda set spawn "+spawnId)
                        .command("/mda set spawn "+spawnId)
                    .send(player);
                return;
            }

            String tpCommand = LocationHelper.createTpCommand(location, player.getName());
            message.then(LocationHelper.prettyPrint(location))
                    .color(YELLOW)
                    .tooltip("Hier klicken zum Teleportieren:", tpCommand)
                    .command(tpCommand)
                .send(player);
            //@formatter:on
        }
    }
}