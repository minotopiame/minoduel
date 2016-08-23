package me.minotopia.minoduel;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import li.l1t.common.misc.NullableOptional;
import me.minotopia.minoduel.arena.Arena;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    public boolean hasPending(@Nonnull Player target, @Nonnull Player requester) {
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
    public NullableOptional<Arena> remove(@Nonnull Player target, @Nonnull Player requester) {
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
    public void request(@Nonnull Player target, @Nonnull Player requester, @Nullable Arena preferredArena) {
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
    public void removeAll(@Nonnull Player player) {
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
