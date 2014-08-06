package me.sebi7224.minoduel.cmd;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.queue.DuelWaitingQueue;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import mkremins.fanciful.FancyMessage;
import org.bukkit.entity.Player;

import io.github.xxyy.common.util.inventory.InventoryHelper;

import java.util.Collection;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.YELLOW;

/**
 * Handles MinoDuel player commands.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 5.8.14
 */
public class CommandsPlayer {
    @Command(aliases = {"1vs1", "mdu"}, desc = "Spielerbefehle von MinoDuel")
    @NestedCommand(SubCommands.class)
    public static void mduMain() {
        //body is ignored
    }

    public class SubCommands {
        private final MinoDuelPlugin plugin;

        public SubCommands(MinoDuelPlugin plugin) {
            this.plugin = plugin;
        }

        @Command(aliases = {"join", "new"},
                desc = "Lässt dich eine Arena auswählen, um ein 1vs1 zu beginnen! (Verwende -a, um das Menü zu unterdrücken)",
                usage = "<-a [Arena|'egal']>",
                flags = "a:")
        @CommandPermissions({"minoduel.user.join"})
        public void playerJoin(CommandContext args, Player player) throws CommandException {
            if (!Arenas.anyExist()) {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§cKeine Arenen vorhanden =(!");
                return;
            }

            if (Arenas.isInGame(player)) {
                player.sendMessage(MinoDuelPlugin.getPrefix() + " §eDu bist bereits in einem Kampf!");
                return;
            }

            if (args.hasFlag('a')) {
                String arenaName = args.getFlag('a');
                Arena arena = null; //Allow for "any arena"
                if (!arenaName.equalsIgnoreCase("egal") && !arenaName.equalsIgnoreCase("null")) {
                    arena = Arenas.byName(arenaName);
                    if (arena == null) {
                        throw new CommandException("Unbekannte Arena! Probiere /1vs1 arenas!");
                    }
                }

                DuelWaitingQueue.enqueue(player, arena);
                return;
            }

            if (InventoryHelper.isInventoryEmpty(player)) {
                plugin.getArenaMenu().open(player);
            } else {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§eDu musst zuerst dein Inventar leeren!");
            }
        }

        @Command(aliases = {"leave"},
                desc = "Gib deinen aktuellen Kampf auf!")
        public void playerLeave(CommandContext args, Player player) throws CommandException {
            Arena arenaToLeave = Arenas.getPlayerArena(player);

            if (arenaToLeave != null) {
                arenaToLeave.endGame(arenaToLeave.getOther(player));
            } else {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§eDu befindest dich in keinem Kampf!");
            }
        }

        @Command(aliases = {"arenas"},
                desc = "Listet alle Arenen auf.",
                usage = "[Suchbegriff]")
        @CommandPermissions({"minoduel.user.arenas"})
        public void adminListArenas(CommandContext args, Player player) throws CommandException {
            Collection<Arena> arenas = Arenas.all();
            if(args.argsLength() >= 1) { //If we have a filter
                String search = args.getString(0).toLowerCase(); //Get that
                arenas.removeIf(arena -> !arena.getName().toLowerCase().contains(search)); //And remove non-matching arenas
            }

            arenas.removeIf(arena -> !arena.isValid());

            if (arenas.isEmpty()) {
                player.sendMessage(MinoDuelPlugin.getPrefix() + "§cKeine Arena entspricht deinem Suchkriterium!"); return;
            }

            player.sendMessage("§6========> §eMinoTopia §6| §e1vs1 §6<========");
            for (Arena arena : arenas) {
                player.sendMessage("§6" + arena.getName() + " §7-> §e" + arena.getPlayerString());
            }
        }

        @Command(aliases = {"position"},
                desc = "Zeigt deine Position in der Warteschlange.",
                usage = "")
        @CommandPermissions({"minoduel.user.arenas"})
        public void playerPosition(CommandContext args, Player player) {
            if(!DuelWaitingQueue.notifyPosition(player)) {
                //@formatter:off
                new FancyMessage("Du bist nicht in der Warteschlange! ")
                         .color(YELLOW)
                        .then("[Beitreten]")
                            .color(GOLD)
                            .tooltip("/1vs1 join")
                            .suggest("/1vs1 join")
                        .send(player);
                //@formatter:on
            }
        }

        public void playerDuel(CommandContext args, Player player) {
            //TODO: actual duel, as in duel <player>
        }

        //TODO: sk89q must have some nice answer to help topics too probably. Find that!
    }
}
