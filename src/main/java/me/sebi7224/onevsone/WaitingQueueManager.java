package me.sebi7224.onevsone;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.github.xxyy.common.collections.Pair;
import me.sebi7224.onevsone.arena.Arena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class which takes care of maintaining a waiting queue of players with their arena preferences and automatically
 * sending players with matching interests into a corresponding arena, if free.
 * This class utilizes a FIFO (first-in-first-out) order.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 22.7.14
 */
public final class WaitingQueueManager {

    private static List<Pair<Player, Arena>> queue = new ArrayList<>();

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

    /**
     * Returns the waiting queue for a given arena.
     * @param arena The arena to find
     * @param strict Whether or not to include NULL choices ("don't care about which arena) into the result
     * @return Ordered waiting queue for {@code arena}
     */
    public static List<Player> queueFor(@NotNull Arena arena, boolean strict) {
        Predicate<Pair<Player, Arena>> predicate;
        if(strict) {
            predicate = pair -> arena.equals(pair.getRight()); //Match same arena
        } else {
            predicate = pair -> pair.getRight() == null || arena.equals(pair.getRight()); //Match same arena or "don't care"
        }

        return queue.stream()
                .filter(predicate)
                .map(Pair::getLeft) //Get player out of pairs
                .collect(Collectors.toList());
    }

    /**
     * Returns a view of the current queues (not actually Queue instances!) for all arenas.
     * All queues are in insertion order, i.e. the first person in any list will be the next to play.
     * Arenas which don't have a queue are not present in the return value.
     * @return a view of current arena queues
     */
    public static Multimap<Arena, Player> getArenaQueues() {
        Multimap<Arena, Player> result = MultimapBuilder.hashKeys().arrayListValues().build();

        queue.forEach(pair -> result.put(pair.getRight(), pair.getLeft()));

        return ImmutableListMultimap.copyOf(result);
    }

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
