package me.minotopia.minoduel.arena;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Holds data related to players when in an arena.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 21.8.14
 */
public interface ArenaPlayerInfo {
    /**
     * This gets the Arena object associated with this PlayerInfo.
     * This always returns NULL if {@link #isValid()} returns FALSE.
     *
     * @return the associated Arena or NULL if none.
     */
    Arena getArena();

    Location getSpawnLocation();

    /**
     * Returns the Player object backing this PlayerInfo.
     * This returns NULL if {@link #isValid()} returns FALSE.
     *
     * @return the associated Player object or NULL if none.
     */
    Player getPlayer();

    /**
     * Returns the initial name of the wrapped player. Still works if {@link #isValid()} returns FALSE.
     *
     * @return the name of the wrapped Player.
     */
    String getName();

    /**
     * @return the unique id nof the wrapped player
     */
    UUID getUniqueId();

    /**
     * @return whether this information is still valid, i.e. the player is still playing in that arena.
     */
    boolean isValid();
}
