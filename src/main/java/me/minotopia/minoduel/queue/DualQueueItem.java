package me.minotopia.minoduel.queue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.arena.Arena;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a dual-player queue item.
 * It's also a player duel queue item. (Do you get it!!! lolololol)
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 6.8.14
 */
public class DualQueueItem extends AbstractQueueItem {
    public static int SIZE = 2;
    private final List<Player> players;

    public DualQueueItem(@Nullable Arena preferredArena, @Nonnull Player... players) {
        this(preferredArena, Lists.newArrayList(players));
    }

    public DualQueueItem(@Nullable Arena preferredArena, @Nonnull List<Player> players) {
        super(SIZE, preferredArena);
        Validate.isTrue(players.size() == SIZE, "Size of players must be 2! (Given: %s)", players.size());
        this.players = ImmutableList.copyOf(players);
    }

    @Override
    public void sendMessage(@Nonnull QueueMessage type, @Nonnull Object... args) {
        String result = MinoDuelPlugin.PREFIX;

        switch (type) {
            case POSITION_NOTIFICATION:
                result += String.format("Ihr seid §e%d.§6 in der Warteschlange der Arena §e%s§6!", args);
                break;
            case REMOVAL_NOTIFICATION:
                Validate.isTrue(args[0] == null || args[0] instanceof Player, "Illegal argument for REMOVAL_NOTIFICATION: ", args[0]);
                Player cause = (Player) args[0];

                if (cause == null) {
                    getPlayers().forEach(plr -> plr.sendMessage(
                            MinoDuelPlugin.PREFIX + String.format("Dein Duell mit §e%s§6 wurde abgebrochen!", getOther(plr).getName())
                    ));
                } else {
                    cause.sendMessage(result + String.format("Du hast das Duell mit §e%s§6 abgebrochen!", getOther(cause).getName()));
                    getOther(cause).sendMessage(result + String.format("%s §chat das Duell mit dir abgebrochen! (Was für eine nette Person lol)", cause.getName()));
                }
                return;
            case RELOAD_REMOVED:
                result += "Ihr habt die Warteschlange wegen eines Serverreloads verlassen. Wir bitten, die Unannehmlichkeiten zu entschuldigen.";
                break; //TODO: This should be persisted over reloads http://bugs.nowak-at.net/view.php?id=185
            default:
                throw new AssertionError("Unknown message type for " + getClass() + ": "+type.name());
        }

        String finalResult = result; //Workaround: Variable used in lambda must be effectively final
        getPlayers().forEach(plr -> plr.sendMessage(finalResult));
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }

    public Player getOther(@Nonnull Player plr) {
        if (!has(plr)) {
            return null;
        }

        return plr.equals(players.get(0)) ? players.get(1) : players.get(0);
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DualQueueItem that = (DualQueueItem) o;

        if (!players.equals(that.players)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + players.hashCode();
        return result;
    }
}
