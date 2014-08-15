package me.sebi7224.minoduel.cmd;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import me.sebi7224.minoduel.queue.DualQueueItem;
import mkremins.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.github.xxyy.common.util.inventory.InventoryHelper;

import java.util.Collection;

import static org.bukkit.ChatColor.DARK_GREEN;
import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.YELLOW;

/**
 * Handles MinoDuel player commands.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 5.8.14
 */
public class CommandsPlayer {
    @SuppressWarnings("UnusedParameters")
    public CommandsPlayer(MinoDuelPlugin plugin) {

    }

    @Command(aliases = {"1vs1", "mdu"}, desc = "Spielerbefehle von MinoDuel")
    @NestedCommand(SubCommands.class)
    public static void mduMain() {
        //body is ignored
    }

    public static class SubCommands {
        private final MinoDuelPlugin plugin;

        public SubCommands(MinoDuelPlugin plugin) {
            this.plugin = plugin;
        }

        @Command(aliases = {"join"},
                desc = "Betritt 1vs1",
                help = "Lässt dich eine Arena auswählen.\n" +
                        "-a: Arena direkt wählen, kein Menü",
                usage = "[-a (Arena)|egal]",
                flags = "a:", max = 0)
        @CommandPermissions({"minoduel.user.join"})
        public void playerJoin(CommandContext args, Player player) throws CommandException {
            if (!Arenas.anyExist()) {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§cKeine Arenen vorhanden =(!");
                return;
            }

            if (Arenas.isInGame(player)) {
                player.sendMessage(MinoDuelPlugin.PREFIX + " §eDu bist bereits in einem Kampf!");
                return;
            }

            if (args.hasFlag('a')) {
                Arena arena = CmdValidate.getArenaOrNull(args.getFlag('a'));

                plugin.getQueueManager().enqueue(player, arena);
                return;
            }

            if (InventoryHelper.isInventoryEmpty(player)) {
                plugin.getArenaMenu().open(player);
            } else {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§eDu musst zuerst dein Inventar leeren!");
            }
        }

        @Command(aliases = {"leave"},
                desc = "Gibt deinen aktuellen Kampf auf")
        public void playerLeave(CommandContext args, Player player) throws CommandException {
            Arena arenaToLeave = Arenas.getPlayerArena(player);

            if (arenaToLeave != null) {
                arenaToLeave.endGame(arenaToLeave.getOther(player));
            } else {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§eDu befindest dich in keinem Kampf!");
            }
        }

        @Command(aliases = {"arenas", "list"},
                desc = "Listet alle Arenen auf.",
                usage = "[Suchbegriff]")
        @CommandPermissions({"minoduel.user.arenas"})
        public void adminListArenas(CommandContext args, Player player) throws CommandException {
            Collection<Arena> arenas = Arenas.all();
            if (args.argsLength() >= 1) { //If we have a filter
                String search = args.getString(0).toLowerCase(); //Get that
                arenas.removeIf(arena -> !arena.getName().toLowerCase().contains(search)); //And remove non-matching arenas
            }

            arenas.removeIf(arena -> !arena.isValid());

            if (arenas.isEmpty()) {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§cKeine Arena entspricht deinem Suchkriterium!");
                return;
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
            if (!plugin.getQueueManager().notifyPosition(player)) {
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

        @Command(aliases = {"duel", "fite"},
                desc = "Beginnt ein 1vs1-Duell mit einem anderen Spieler! (-c bricht ein Duell ab)",
                usage = "<Spieler> [Arena]", min = 1,
                flags = "c")
        @CommandPermissions({"minoduel.user.duel"})
        public void playerDuel(CommandContext args, Player player) throws CommandException { //TODO: Something to show players their pending requests (w/ clickable JSON link)
            @SuppressWarnings("deprecation")
            Player opponent = Bukkit.getPlayerExact(args.getString(0));

            if (opponent == null) {
                player.sendMessage(plugin.getPrefix() + "§cSorry, der Spieler §4" + args.getString(0) + "§c is nicht online.");
                if (args.hasFlag('c')) {
                    player.sendMessage(plugin.getPrefix() + "§aAlle Anfragen wurde entfernt, als er/sie den Server verlassen hat.");
                }
                return;
            }

            boolean opponentInGame = Arenas.isInGame(opponent);

            if (args.hasFlag('c')) {
                if (plugin.getRequestManager().remove(opponent, player).isPresent()) {
                    player.sendMessage(plugin.getPrefix() + "§aDu hast die Duellanfrage an §2" + opponent.getName() + " §azurückgezogen.");
                    opponent.sendMessage(plugin.getPrefix() + "§4" + player.getName() + " §chat die Duellanfrage an dich zurückgezogen.");
                } else if (plugin.getRequestManager().remove(player, opponent).isPresent()) {
                    player.sendMessage(plugin.getPrefix() + "§aDu hast die Duellanfrage von §2" + opponent.getName() + " §aabgelehnt.");
                    opponent.sendMessage(plugin.getPrefix() + "§4" + player.getName() + " §chat deine Duellanfrage abgelehnt!");
                } else {
                    player.sendMessage(plugin.getPrefix() + "§cDu hast §4" + opponent.getName() + "§c keine Anfrage geschickt!");
                }
                return;
            }

            if (opponentInGame) {
                player.sendMessage(plugin.getPrefix() + opponent.getName() + " ist gerade im Kampf.");
            }

            if (plugin.getRequestManager().hasPending(player, opponent)) { //If this is an answer to a request
                if (opponentInGame) {
                    player.sendMessage(plugin.getPrefix() + "§cDu kannst dieses Duell daher momentan nicht akzeptieren. Bitte versuche es später erneut."); //TODO: Do we notify them when the opponent is done? TODO: Send players list of pending requests when they finish a fight
                } else {
                    plugin.getQueueManager().enqueue(new DualQueueItem(plugin.getRequestManager().remove(player, opponent).get(), opponent, player));
                }
            } else if (plugin.getRequestManager().hasPending(opponent, player)) {
                player.sendMessage(plugin.getPrefix() + "Du hast §e" + opponent.getName() + "§6 bereits eine Anfrage geschickt!"); //TODO: which arena?
                new FancyMessage("Möchtest du diese zurückziehen? (hier klicken)")
                        .color(GOLD)
                        .tooltip("/1vs1 duel -c " + opponent.getName())
                        .command("/1vs1 duel -c " + opponent.getName())
                        .send(player);
            } else {
                Arena arena = CmdValidate.getArenaOrNull(args.getString(1, null));
                plugin.getRequestManager().request(opponent, player, arena);
                //@formatter:off
                opponent.sendMessage(plugin.getPrefix() + "§1§l" + player.getName() + "§9§l möchte sich mit dir" +
                        (arena == null ? "" : " in der Arena §1§l" + arena.getName() + "§9§l") +
                        " duellieren!");
                plugin.getFancifulPrefix().then("[Annehmen] <-- ")
                            .color(DARK_GREEN)
                            .tooltip("/1vs1 duel " + player.getName())
                            .command("/1vs1 duel " + player.getName())
                        .then("klick (i)") //Let's hope that users actually understand that lol
                            .color(GOLD)
                            .tooltip("Wähle eine der Optionen.",
                                    "Du kannst die Anfrage auch jederzeit später bearbeiten,",
                                    "die Befehle dafür siehst du, wenn du deine Maus über eine Option bewegst.",
                                    "Wenn du oder "+player.getName()+" offline gehen, wird die ANfrag automatisch gelöscht.")
                        .then(" --> [Ablehnen]")
                            .color(DARK_RED)
                            .tooltip("/1vs1 duel -c " + player.getName())
                            .command("/1vs1 duel -c " + player.getName())
                        .send(opponent);
                //@formatter:on
                player.sendMessage(plugin.getPrefix() + "§e" + opponent.getName() + "§6 hat deine Anfrage erhalten. Bitte warte nun, bis er/sie diese annimmt.");
            }
        }
    }
}
