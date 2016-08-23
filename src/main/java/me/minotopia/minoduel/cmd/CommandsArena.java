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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import li.l1t.common.chat.XyComponentBuilder;
import li.l1t.common.util.LocationHelper;

import java.util.List;
import java.util.stream.Collectors;

import static net.md_5.bungee.api.ChatColor.DARK_BLUE;
import static net.md_5.bungee.api.ChatColor.DARK_RED;
import static net.md_5.bungee.api.ChatColor.GOLD;
import static net.md_5.bungee.api.ChatColor.RED;
import static net.md_5.bungee.api.ChatColor.YELLOW;

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

            if (plugin.getArenaManager().existsByName(arenaName)){
                player.sendMessage(MinoDuelPlugin.PREFIX + "§cDie Arena §4" + arenaName + " §cexistiert bereits!");
                return;
            }

            plugin.getArenaManager().createArena(arenaName);
            player.sendMessage(MinoDuelPlugin.PREFIX + "§aDu hast die Arena §2" + arenaName + " §aerfolgreich erstellt!");
        }

        @Command(aliases = {"remove"},
                desc = "Löscht eine Arena.",
                usage = "<Arena>",
                flags = "y", min = 1)
        @CommandPermissions({"minoduel.arena.remove"})
        public void arenaRemove(CommandContext args, Player player) throws CommandException {
            String arenaName = args.getString(0);

            if (!args.hasFlag('y')){
                player.spigot().sendMessage(
                        new XyComponentBuilder("Möchtest du die Arena ").color(RED)
                                .append(arenaName, DARK_RED)
                                .append(" wirklich entfernen? ", RED)
                                .append("[Ja (klick)]", GOLD)
                                .tooltip("Dies entfernt die Arena und alle Optionen permanent!")
                                .command("/mdarena remove -y " + arenaName)
                                .create()
                );
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
        public void arenaChecklist(CommandContext args, Player player) throws CommandException {
            Arena arena = CmdValidate.getArenaChecked(args.getString(0), plugin.getArenaManager());
            player.sendMessage("§6Arena: " + (arena.isValid() ? "§a" : "§c") + arena.getName());
            sendLocationInfo(player, arena.getFirstSpawn(), arena, 1);
            sendLocationInfo(player, arena.getSecondSpawn(), arena, 2);

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
                        player.spigot().sendMessage(
                                new XyComponentBuilder("Unbekannte Spawnzahl! ").color(RED)
                                        .append("Wolltest du vielleicht: ", YELLOW)
                                        .create()
                        );
                        player.spigot().sendMessage(
                                new XyComponentBuilder("[Spawn 1 setzen] ").color(DARK_BLUE)
                                        .tooltip("/mda set spawn " + arena.getName() + " 1")
                                        .command("/mda set spawn " + arena.getName() + " 1")
                                        .append("[Spawn 2 setzen] ", DARK_RED)
                                        .tooltip("/mda set spawn " + arena.getName() + " 2")
                                        .command("/mda set spawn " + arena.getName() + " 2")
                                        .create()
                        );
                        return;
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

                if (args.hasFlag('a') || !args.hasFlag('i')){
                    arena.setArmorKit(player.getInventory().getArmorContents());
                    player.sendMessage(MinoDuelPlugin.PREFIX + "§aRüstung für §2" + arena.getName() + "§a gesetzt!");
                }

                if (args.hasFlag('i') || !args.hasFlag('a')){
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

        private void sendLocationInfo(Player player, Location location, Arena arena, int spawnId) {
            XyComponentBuilder msg = new XyComponentBuilder("Spawn " + spawnId + ": ").color(GOLD);
            if (location == null){
                msg.append("nicht gesetzt! [Hier setzen]").color(RED)
                        .tooltip("Hier klicken für", String.format("/mda set spawn %s %d", arena.getName(), spawnId))
                        .command(String.format("/mda set spawn %s %d", arena.getName(), spawnId));
            } else {
                String tpCommand = LocationHelper.createTpCommand(location, player.getName());
                msg.append(LocationHelper.prettyPrint(location), YELLOW)
                        .tooltip("Hier klicken zum Teleportieren:", tpCommand)
                        .command(tpCommand);
            }

            player.spigot().sendMessage(msg.create());
        }
    }
}
