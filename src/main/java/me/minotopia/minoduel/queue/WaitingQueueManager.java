package me.minotopia.minoduel.queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.arena.Arena;
import me.minotopia.minoduel.arena.ArenaManager;
import me.minotopia.minoduel.arena.MinoDuelArena;
import org.bukkit.entity.Player;

import io.github.xxyy.lib.guava17.collect.ImmutableListMultimap;
import io.github.xxyy.lib.guava17.collect.ListMultimap;
import io.github.xxyy.lib.guava17.collect.Multimap;
import io.github.xxyy.lib.guava17.collect.MultimapBuilder;
import io.github.xxyy.lib.intellij_annotations.NotNull;
import io.github.xxyy.lib.intellij_annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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
 * @since 22.7.14 // 1.0
 */
public class WaitingQueueManager {
    public static final String POSITION_NOTIFICATION_FORMAT = "Du bist §e%d.§6 in der Warteschlange der Arena §e%s§6!";

    private List<QueueItem> queue = new ArrayList<>();
    private final ArenaManager arenaManager;

    public WaitingQueueManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    /**
     * Adds a Player to the waiting queue. If the Player was already in the queue, their Arena preference is updated.
     *
     * @param plr   Player to add, may not be NULL
     * @param arena Arena the player prefers or NULL if no preference was stated.
     */
    public void enqueue(@NotNull Player plr, @Nullable Arena arena) {
        remove(plr);
        queue.add(new PlayerQueueItem(plr, arena));
        findMatches();
    }

    /**
     * Adds an item to the end of the waiting queue. If the item is already present, it will first be removed and then
     * re-added to the end of the queue.
     *
     * @param item the item to add to the queue
     */
    public void enqueue(@NotNull QueueItem item) {
        item.getPlayers().forEach(this::remove);
        queue.add(item);
        findMatches();
    }

    /**
     * Checks whether a Player is queued in the waiting queue manager
     *
     * @param plr Player to check for
     * @return whether given player is queued
     */
    public boolean isQueued(@NotNull Player plr) {
        return getQueueItem(plr) != null;
    }

    /**
     * Gets the queue item which holds given player.
     *
     * @param plr the player to find
     * @return the queue item representing that player or NULL if none
     */
    @Nullable
    public QueueItem getQueueItem(@NotNull Player plr) {
        return queue.stream()
                .filter(item -> item.has(plr))
                .findAny().orElse(null);
    }

    /**
     * Removes a Player from the queue.
     * If given Player is not in the queue, no action is performed.
     *
     * @param plr the player to remove
     * @return the {@link QueueItem} which held the player or NULL if the player wasn't in the queue.
     */
    public QueueItem remove(@NotNull Player plr) {
        QueueItem previous = queue.stream()
                .filter(item -> item.has(plr))
                .findFirst().orElse(null);

        if (previous != null) {
            queue.remove(previous);
            previous.onRemoval(plr);
        }

        return previous;
    }

    /**
     * Removes an item from the queue.
     *
     * @param item the item to remove from the queue
     * @param deep if true, any item containing any of item's players will be removed
     * @return the first removed item or NULL if the item wasn't in the queue
     */
    public QueueItem remove(@Nullable QueueItem item, boolean deep) {
        if (item == null) {
            return null;
        }

        if (!queue.remove(item)) {
            if (deep) {
                for (QueueItem deepItem : queue) {
                    if (deepItem.hasAny(item.getPlayers())) {
                        remove(deepItem, false);
                        return deepItem;
                    }
                }
            }

            return null;
        } else {
            item.onRemoval(null);
        }

        return item;
    }

    /**
     * Removes an item from the queue. Shortcut for {@link #remove(QueueItem, boolean)} with deep=false
     *
     * @param item the item to remove from the queue
     * @return the removed item or NULL if the item wasn't in the queue
     */
    public QueueItem remove(@Nullable QueueItem item) {
        return remove(item, false);
    }

    /**
     * Finds matches in the current queue. A match are two players who have either the same arena choice or at least one
     * of them doesn't care about what arena they play in.
     * This is automatically called when a new player is added to the queue.
     */
    public void findMatches() { //I don't even want to know how bad the performance of this is
        ListMultimap<Arena, QueueItem> arenaChoices = MultimapBuilder.hashKeys().arrayListValues().build();

        if (queue.size() < 2 && (queue.isEmpty() || queue.get(0).size() != Arena.SIZE)) {
            return;
        }

        ImmutableList.copyOf(queue).stream().forEach(item -> { //Need to copy to avoid concurrent modification
            if (item.size() == Arena.SIZE) { //If we can fill this arena immediately, do it!
                if ((item.getPreferredArena() == null || !arenaChoices.containsKey(item.getPreferredArena()))) { //If someone else is queued for that arena, let them go first since they came first actually
                    if (tryPop(item).isEmpty()) {
                        arenaChoices.put(item.getPreferredArena(), item);
                    }
                }

                return; //No need for all that emotional match-finding
            }

            QueueItem match = getFitting(arenaChoices.get(item.getPreferredArena()), item); //Get an item preferring the same arena

            if (match == null) { //If none, get an item that doesn't care
                match = getFitting(arenaChoices.get(null), item); //Get player w/o arena preference
            }

            if (match == null && item.getPreferredArena() == null) { //No match, but this item doesn't care
                match = getFitting(queue, item); //Let's just get any item from the queue
            }

            if (match != null) { //If we found someone, start a game
                tryPop(item, match)
                        .forEach(removed -> arenaChoices.remove(removed.getPreferredArena(), removed));
            } else {
                arenaChoices.put(item.getPreferredArena(), item); //If we found no match, queue this player to be matched
            }
        });

        //TODO: notify players of position
    }

    /**
     * Notifies each queued player of their position in the respective queue.
     */
    public void notifyPositions() {
        Map<Arena, Integer> queueSizes = new HashMap<>(arenaManager.all().size());

        queue.stream().forEach(item -> {
            Arena arena = item.getPreferredArena();

            if (arena == null && !queueSizes.isEmpty()) { //Select best arena w/ shortest wait time
                arena = queueSizes.entrySet().stream() //From all queue sizes
                        .min((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue())).get() //Get the one with the shortest queue
                        .getKey(); //And retrieve its arena's name
            }

            Integer queueSize = queueSizes.getOrDefault(arena, 0); //Get arena's queue size
            queueSize += item.size();
            if (arena != null) { //We can't actually write anything back if we don't know which arena the player is queueing for :/
                queueSizes.put(arena, queueSize); //Increase and put
            }

            String arenaName = arena == null ? "(egal)" : arena.getName();

            item.sendMessage(QueueItem.QueueMessage.POSITION_NOTIFICATION, queueSize - item.size() + 1, arenaName);
        });
    }

    /**
     * Notifies a single player of their position in the queue, and for which arena they are queuing for.<br>
     * <b>If you want to notify every player in the queue, {@link #notifyPositions()} is far more efficient!</b>
     *
     * @param target the player to notify
     * @return true if a notification was sent.
     */
    public boolean notifyPosition(Player target) { //TODO: maybe we can un-spaghetti this some time
        Map<Arena, Integer> queueSizes = new HashMap<>(arenaManager.all().size());

        for (QueueItem item : queue) {
            Arena arena = item.getPreferredArena();

            if (arena == null && !queueSizes.isEmpty()) { //Select best arena w/ shortest wait time
                arena = queueSizes.entrySet().stream() //From all queue sizes
                        .min((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue())).get() //Get the one with the shortest queue
                        .getKey(); //And retrieve its arena's name
            }

            Integer queueSize = queueSizes.getOrDefault(arena, 0); //Get arena's queue size
            queueSize += item.size();
            if (arena != null) { //We can't actually write anything back if we don't know which arena the player is queueing for :/
                queueSizes.put(arena, queueSize); //Increase and put
            }

            if (item.has(target)) {
                target.sendMessage(MinoDuelPlugin.PREFIX + String.format(POSITION_NOTIFICATION_FORMAT, queueSize,
                        arena == null ? "(egal)" : arena.getName())); //Hmm maybe this could be generified
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the waiting queue for a given arena.
     *
     * @param arena  The arena to find
     * @param strict Whether or not to include NULL choices ("don't care about which arena) into the result
     * @return Ordered waiting queue for {@code arena}
     */
    public List<Player> queueFor(@NotNull Arena arena, boolean strict) {
        Predicate<QueueItem> predicate;
        if (strict) {
            predicate = item -> arena.equals(item.getPreferredArena()); //Match same arena
        } else {
            predicate = item -> item.getPreferredArena() == null || arena.equals(item.getPreferredArena()); //Match same arena or "don't care"
        }

        return queue.stream()
                .filter(predicate)
                .flatMap(item -> item.getPlayers().stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns a view of the current queues (not actually Queue instances!) for all arenas.
     * All queues are in insertion order, i.e. the first person in any list will be the next to play.
     * Arenas which don't have a queue are not present in the return value.
     *
     * @return a view of current arena queues
     */
    public ListMultimap<Arena, Player> getArenaQueues() {
        Multimap<Arena, Player> result = MultimapBuilder.hashKeys().arrayListValues().build();

        queue.forEach(item -> result.putAll(item.getPreferredArena(), item.getPlayers()));

        return ImmutableListMultimap.copyOf(result);
    }

    /**
     * Notifies the queue manager of a reload occurring. THis will cause all items to be notified and then forcefully
     * removed.
     */
    public void notifyReload() {
        queue.forEach(item -> item.sendMessage(QueueItem.QueueMessage.RELOAD_REMOVED));
        queue.clear();
    }

    //Returns whether a game has been started with given arguments
    private Collection<QueueItem> tryPop(QueueItem... items) { //additional validation because of weird NPE; https://bugs.nowak-at.net/view.php?id=496
        Preconditions.checkNotNull(items, "items");
        Preconditions.checkNotNull(arenaManager, "arenaManager");
        Arena arena = null;

        for (QueueItem item : items) {
            if (item != null && item.getPreferredArena() != null) {
                arena = item.getPreferredArena();
                break;
            }
        }

        if (arena == null) {
            arena = arenaManager.firstReady();
        }

        if (arena == null || !arena.isReady()) {
            return ImmutableList.of();
        }

        Collection<QueueItem> acceptedItems = arena.scheduleGame(items);
        acceptedItems.stream()
                .forEach(queue::remove); //Remove all items that the arena accepted

        return acceptedItems;
    }

    /**
     * Gets an item which can fit into an Arena with a given initial item from a provided collection
     *
     * @param availableItems a collection of available items to get matches from
     * @param ourItem        the item to match against
     * @return a matching item or NULL if none match
     */
    private QueueItem getFitting(Collection<QueueItem> availableItems, QueueItem ourItem) {
        return availableItems.stream() //Gets first item that
                .filter(other -> !MinoDuelArena.whichCanFit(ourItem, other).isEmpty()) //can fit with the parameter from a list
                .findFirst().orElse(null);
    }
}
