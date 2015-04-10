package me.minotopia.minoduel;

import me.minotopia.minoduel.arena.Arena;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import io.github.xxyy.common.misc.NullableOptional;
import io.github.xxyy.lib.guava17.collect.Multimap;
import io.github.xxyy.lib.guava17.collect.MultimapBuilder;
import io.github.xxyy.lib.intellij_annotations.NotNull;
import io.github.xxyy.lib.intellij_annotations.Nullable;

import java.util.UUID;

/**
 * Manages pending duel requests.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 7.8.14
 */
public class DuelRequestManager {
    private final Multimap<UUID, DuelRequest> pendingDuelRequests = MultimapBuilder.hashKeys().hashSetValues().build(); //target -> requesters

    /**
     * Checks if {@code requester} has requested a duel with {@code target}.
     *
     * @param target    the target to search for
     * @param requester the requester to search for
     * @return whether there is a request matching the parameters
     */
    public boolean hasPending(@NotNull Player target, @NotNull Player requester) {
        return pendingDuelRequests.get(target.getUniqueId()).stream()
                .anyMatch(rq -> rq.from.equals(requester.getUniqueId()));
    }

    /**
     * Removes a duel request.
     *
     * @param target    the target of the request
     * @param requester the player who initially requested the duel
     * @return an Optional containing the preferred arena if such request was found
     */
    public NullableOptional<Arena> remove(@NotNull Player target, @NotNull Player requester) {
        DuelRequest duelRequest = pendingDuelRequests.get(target.getUniqueId()).stream()
                .filter(rq -> rq.from.equals(requester.getUniqueId()))
                .findFirst().orElse(null);

        if (duelRequest != null) {
            pendingDuelRequests.get(target.getUniqueId()).removeIf(rq -> rq.from.equals(requester.getUniqueId()));
            return NullableOptional.of(duelRequest.preferredArena);
        }

        return NullableOptional.of();
    }

    /**
     * Requests a duel between the given players.
     *
     * @param target         the target of the request
     * @param requester      the player who requested the duel
     * @param preferredArena the preferred arena or null if none
     */
    public void request(@NotNull Player target, @NotNull Player requester, @Nullable Arena preferredArena) {
        Validate.isTrue(!target.equals(requester), "Cannot request a duel with self. Get friends :P");
        Validate.isTrue(!hasPending(requester, target), "This request was already issued the other way round!");
        Validate.isTrue(!hasPending(target, requester), "This request was already issued!");
        pendingDuelRequests.put(target.getUniqueId(), new DuelRequest(requester, preferredArena));
    }

    /**
     * Removes all pending duel requests which target or were issued by a player.
     *
     * @param player the player to target
     */
    public void removeAll(@NotNull Player player) {
        pendingDuelRequests.removeAll(player.getUniqueId());
        pendingDuelRequests.values().removeIf(rq -> rq.from.equals(player.getUniqueId()));
    }

    private static class DuelRequest {
        final UUID from;
        final Arena preferredArena;

        private DuelRequest(Player requester, Arena preferredArena) {
            this.from = requester.getUniqueId();
            this.preferredArena = preferredArena;
        }
    }
}
