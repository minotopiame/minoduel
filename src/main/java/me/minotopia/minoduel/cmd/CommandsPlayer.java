package me.minotopia.minoduel.cmd;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.arena.Arena;
import me.minotopia.minoduel.queue.DualQueueItem;
import me.minotopia.minoduel.queue.QueueItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import li.l1t.common.chat.XyComponentBuilder;
import li.l1t.common.util.inventory.InventoryHelper;

import java.util.ArrayList;
import java.util.Collection;

import static net.md_5.bungee.api.ChatColor.DARK_GREEN;
import static net.md_5.bungee.api.ChatColor.DARK_RED;
import static net.md_5.bungee.api.ChatColor.GOLD;
import static net.md_5.bungee.api.ChatColor.YELLOW;

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

    @Command(aliases = {"1vs1", "mdu"}, desc = "MinoDuel - 1vs1 auf MinoTopia.")
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
        public void playerJoin(CommandContext args, Player player) throws CommandException {
            if (!plugin.getArenaManager().anyExist()) {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§cKeine Arenen vorhanden =(!");
                return;
            }

            if (plugin.getArenaManager().isInGame(player)) {
                player.sendMessage(MinoDuelPlugin.PREFIX + " §eDu bist bereits in einem Kampf!");
                return;
            }

            CmdValidate.validateInventoryEmpty(player);

            if (args.hasFlag('a')) {
                Arena arena = CmdValidate.getArenaOrNull(args.getFlag('a'), plugin.getArenaManager());

                plugin.getQueueManager().enqueue(player, arena);

                if (plugin.getQueueManager().isQueued(player)) {
                    player.sendMessage(plugin.getPrefix() + "Du bist nun in der Warteschlange" +
                            (arena == null ? "" : " für die Arena §e" + arena.getName() + "§6") +
                            "!");
                }

                return;
            }

            plugin.getArenaManager().getArenaMenu().open(player);
        }

        @Command(aliases = {"leave"},
                desc = "Gibt deinen aktuellen Kampf auf")
        public void playerLeave(CommandContext args, Player player) throws CommandException {
            Arena arenaToLeave = plugin.getArenaManager().getPlayerArena(player);

            if (arenaToLeave != null) {
                arenaToLeave.endGame(arenaToLeave.getOther(player));
            } else if (plugin.getQueueManager().remove(player) != null) {
                player.sendMessage(plugin.getPrefix() + "§aDu hast die Warteschlange verlassen.");
            } else {
                player.sendMessage(MinoDuelPlugin.PREFIX + "§eDu befindest dich in keinem Kampf!");
            }
        }

        @Command(aliases = {"arenas", "list"},
                desc = "Listet alle Arenen auf.",
                usage = "[Suchbegriff]")
        @CommandPermissions({"minoduel.user.arenas"})
        public void adminListArenas(CommandContext args, Player player) throws CommandException {
            Collection<Arena> arenas = new ArrayList<>(plugin.getArenaManager().all());
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
                player.spigot().sendMessage(
                        new XyComponentBuilder("Du bist nicht in der Warteschlange! ").color(YELLOW)
                                .append("[Beitreten]", GOLD)
                                .tooltip("/1vs1 join")
                                .suggest("/1vs1 join")
                                .create()
                );
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

            if (opponent.equals(player)) {
                throw new CommandException("Sorry, du kannst nicht gegen dich selbst kämpfen. Kauf dir Freunde ;D");
            }

            boolean opponentInGame = plugin.getArenaManager().isInGame(opponent);

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

            if (plugin.getMtcHook().isInOtherGame(opponent.getUniqueId())) {
                player.sendMessage(plugin.getPrefix() + opponent.getName() + " ist gerade in einem Spiel von " +
                        plugin.getMtcHook().getBlockingPlugin(opponent.getUniqueId()).getName() + ".");
                opponentInGame = true;
            }

            if (plugin.getRequestManager().hasPending(player, opponent)) { //If this is an answer to a request
                if (opponentInGame) {
                    player.sendMessage(plugin.getPrefix() + "§cDu kannst dieses Duell daher momentan nicht akzeptieren. " +
                            "Bitte versuche es später erneut.");
                    //TODO: Do we notify them when the opponent is done? TODO: Send players list of pending requests when they finish a fight
                } else {
                    QueueItem opponentItem = plugin.getQueueManager().getQueueItem(opponent);
                    if (opponentItem != null && opponentItem instanceof DualQueueItem) {
                        player.sendMessage(plugin.getPrefix() + "§4" + opponent.getName() +
                                "§c ist gerade in einem Duell mit §4" + ((DualQueueItem) opponentItem).getOther(opponent).getName() + "§c!");
                        return;
                    }

                    CmdValidate.validateInventoryEmpty(player);
                    if (!InventoryHelper.isInventoryEmpty(opponent)) {
                        player.sendMessage(plugin.getPrefix() + "§4" + opponent.getName() + "§c muss zuerst sein/ihr Inventar leeren!");
                        opponent.sendMessage(plugin.getPrefix() + "§cDu musst zuerst dein Inventar leeren, bevor §4" +
                                player.getName() + "§c dein Duell annehmen kann!");
                        return;
                    }

                    plugin.getQueueManager().enqueue(new DualQueueItem(plugin.getRequestManager().remove(player, opponent).get(), opponent, player));
                    player.sendMessage(plugin.getPrefix() + "§aDu hast die Duellanfrage von §2" + opponent.getName() + "§a angenommen!");
                    opponent.sendMessage(plugin.getPrefix() + "§2" + player.getName() + " hat deine Duellanfrage angenommen!");

                    if (plugin.getQueueManager().isQueued(player)) {
                        player.sendMessage(plugin.getPrefix() + "Ihr seid nun in der Warteschlange.");
                        opponent.sendMessage(plugin.getPrefix() + "Ihr seid nun in der Warteschlange.");
                    }
                }
            } else if (plugin.getRequestManager().hasPending(opponent, player)) {
                player.sendMessage(plugin.getPrefix() + "Du hast §e" + opponent.getName() +
                        "§6 bereits eine Anfrage geschickt!"); //TODO: which arena?
                player.spigot().sendMessage(
                        new XyComponentBuilder("Möchtest du diese zurückziehen? (hier klicken)").color(GOLD)
                                .tooltip("/1vs1 duel -c " + opponent.getName())
                                .command("/1vs1 duel -c " + opponent.getName())
                                .create()
                );
            } else {
                Arena arena = CmdValidate.getArenaOrNull(args.getString(1, null), plugin.getArenaManager());
                plugin.getRequestManager().request(opponent, player, arena);
                //@formatter:off
                opponent.sendMessage(plugin.getPrefix() + "§1§l" + player.getName() + "§9§l möchte sich mit dir" +
                        (arena == null ? "" : " in der Arena §1§l" + arena.getName() + "§9§l") +
                        " duellieren!");
                opponent.spigot().sendMessage(
                        plugin.getPrefixBuilder()
                        .append("[Annehmen] <-- ", DARK_GREEN)
                        .tooltip("/1vs1 duel " + player.getName())
                            .command("/1vs1 duel " + player.getName())
                        .append("klick (i)", GOLD) //Let's hope that users actually understand that lol
                        .tooltip("Wähle eine der Optionen durch Anklicken.",  "Du kannst die Anfrage auch",
                                "jederzeit später bearbeiten.", "Wenn du oder " + player.getName() + " offline gehen,",
                                "wird die Anfrage gelöscht.")
                        .append(" --> [Ablehnen]", DARK_RED)
                        .tooltip("/1vs1 duel -c " + player.getName())
                            .command("/1vs1 duel -c " + player.getName())
                        .create()
                );
                player.sendMessage(plugin.getPrefix() + "§e" + opponent.getName() + "§6 hat deine Anfrage erhalten. " +
                        "Bitte warte nun, bis er/sie diese annimmt.");
            }
        }
    }
}
