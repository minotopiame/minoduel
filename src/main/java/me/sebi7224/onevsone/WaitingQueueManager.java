package me.sebi7224.onevsone;

import com.google.common.collect.ImmutableList;
import io.github.xxyy.common.collections.Pair;
import me.sebi7224.onevsone.arena.Arena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class which takes care of maintaining a waiting queue of players with their arena preferences and automatically
 * sending players with matching interests into a corresponding arena, if free.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 22.7.14
 */
public final class WaitingQueueManager {

    private static LinkedList<Pair<Player, Arena>> queue = new LinkedList<>(); //Using impl type here because LinkedList is not only a Queue but also a List and we need that...sorry standards

    private WaitingQueueManager() {

    }

    /**
     * Adds a Player to the waiting queue. If the Player was already in the queue, their Arena preference is updated.
     *
     * @param plr   Player to add, may not be NULL
     * @param arena Arena the player prefers or NULL if no preference was stated.
     */
    public static void enqueue(@NotNull Player plr, @Nullable Arena arena) {
        remove(plr);
        queue.add(new Pair<>(plr, arena));
        findMatches();
    }

    /**
     * Checks whether a Player is queued in the waiting queue manager
     *
     * @param plr Player to check for
     * @return whether given player is queued
     */
    public static boolean isQueued(Player plr) {
        return queue.stream()
                .filter(pair -> pair.getLeft().equals(plr))
                .findAny().isPresent();
    }

    /**
     * Removes a Player from the queue, for example if they quit.
     * If given Player is not in the queue, no action is performed.
     *
     * @param plr Player to remove
     */
    public static void remove(@NotNull Player plr) {
        ImmutableList.copyOf(queue).stream()
                .filter(pair -> pair.getLeft().equals(plr))
                .forEach(queue::remove);
    }

    /**
     * Finds matches in the current queue. A match are two players who have either the same arena choice or at least one
     * of them doesn't care about what arena they play in.
     * This is automatically called when a new player is added to the queue.
     */
    public static void findMatches() {
        Map<Arena, Player> arenaChoices = new HashMap<>();

        if (queue.size() < 2) {
            return;
        }

        ImmutableList.copyOf(queue).stream().forEach(pair -> { //Need to copy to avoid concurrent modifications
            Player match = arenaChoices.remove(pair.getRight()); //Get a player who chose the same arena

            if(match == null) {
                match = arenaChoices.get(null); //Get player w/o arena preference
            }

            if(match != null) { //If we found someone, start a game
                tryPop(pair.getRight(), pair.getLeft(), match); //That method gets a random arena if NULL is passed
            } else {
                arenaChoices.put(pair.getRight(), pair.getLeft()); //If we found no match, queue this player to be matched
            }
        });
    }

    //TODO: Method for informing players about their position in the queue

    //TODO: Method to visualize arena queues
    //Should return Map<Arena, List<Player>>
    //Visualisation: (egal) => Player1, Player2
    //(arena1) => Player4, Player5, Player7
    //arena names should be green if this queue would allow for joining a game immediately

    //Returns whether a game has been started with given arguments
    private static boolean tryPop(Arena arena, Player plr1, Player plr2) {
        if(arena == null) {
            arena = Arena.any();
        }

        if(arena.isOccupied()) {
            return false;
        }

        arena.startGame(plr1, plr2);
        remove(plr2);
        remove(plr2);

        return true;
    }
}
