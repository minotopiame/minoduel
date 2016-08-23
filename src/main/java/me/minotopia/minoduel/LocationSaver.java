package me.minotopia.minoduel;

import com.google.common.io.Files;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import li.l1t.common.misc.XyLocation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helps persisting previous locations if players leave etc
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 25.8.14
 */
public class LocationSaver {
    private final Map<UUID, Location> cache = new HashMap<>();
    private final File file;
    private final YamlConfiguration storage;
    private final AtomicBoolean storageDirty = new AtomicBoolean(false);

    public LocationSaver(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), "locations.persist.yml");

        if (!file.exists()) {
            try {
                Files.createParentDirs(file);
                java.nio.file.Files.createFile(file.toPath());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        this.storage = YamlConfiguration.loadConfiguration(file);
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::trySaveStorage, 20L, 20L);
    }

    /**
     * Saves the location of given player to disk & detodated wam.
     *
     * @param plr the player whose location to save
     */
    public void saveLocation(Player plr) {
        XyLocation location = new XyLocation(plr.getLocation());
        cache.put(plr.getUniqueId(), location);
        storage.set(plr.getUniqueId().toString(), location);
        markStorageDirty();
    }

    private void markStorageDirty() {
        storageDirty.compareAndSet(false, true);
    }

    /**
     * Attempts to teleport given player to their previous location, if any.
     *
     * @param plr the target player
     * @return whether the player was teleported
     * @see #loadLocation(org.bukkit.entity.Player)
     */
    public boolean loadLocationWithMessage(Player plr) {
        if (loadLocation(plr)) {
            plr.sendMessage(MinoDuelPlugin.PREFIX + "Du wurdest an die Position vor deinem 1vs1 teleportiert!");
            return true;
        }
        return false;
    }

    private void trySaveStorage() {
        if(storageDirty.getAndSet(false)) {
            try {
                storage.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Attempts to load the saved inventory of given player, if any. If the saved inventory does not fit, parts which didn't
     * will be saved back.
     *
     * @param plr the player whose inventory to load
     * @return whether any items were returned to the player
     */
    public boolean loadLocation(Player plr) {
        if (plr == null || !plr.isOnline()) {
            return false;
        }

        if (cache.containsKey(plr.getUniqueId())) {
            plr.teleport(cache.remove(plr.getUniqueId()));
            storage.set(plr.getUniqueId().toString(), null);
            markStorageDirty();
            return true;
        }

        if (storage.contains(plr.getUniqueId().toString())) {
            Object input = storage.get(plr.getUniqueId().toString());
            if (input instanceof XyLocation) {
                plr.teleport((XyLocation) input);
            }
            storage.set(plr.getUniqueId().toString(), null);
            markStorageDirty();
            return true;
        }

        return false;
    }

    /**
     * Checks if a given player has an inventory saved.
     *
     * @param plr the player whose inventory to check
     * @return whether any items are saved for given player
     */
    public boolean hasLocation(Player plr) {
        return cache.containsKey(plr.getUniqueId()) ||
                storage.contains(plr.getUniqueId().toString());
    }

    public void onReload() {
        markStorageDirty();
        trySaveStorage();
    }
}
