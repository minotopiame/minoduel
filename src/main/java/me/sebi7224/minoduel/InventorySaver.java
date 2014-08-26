package me.sebi7224.minoduel;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import io.github.xxyy.common.util.CommandHelper;
import io.github.xxyy.common.util.inventory.InventoryHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Helps persisting inventories if players are too retarded to keep them empty while waiting to be teleported.
 * Note: This might later be used to save inventories at any time, but is currently not tested well enough.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 25.8.14
 */
public class InventorySaver {
    private final Logger logger;
    private final Map<UUID, List<ItemStack>> cache = new HashMap<>();
    private final File file;
    private final YamlConfiguration storage;


    public InventorySaver(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), "inventories.persist.yml");

        if (!file.exists()) {
            try {
                Files.createParentDirs(file);
                java.nio.file.Files.createFile(file.toPath());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            //noinspection ConstantConditions
            Arrays.asList(file.getParentFile().listFiles()).stream()
                    .filter(fl -> fl.getName().endsWith(".lck"))
                    .forEach(File::delete);
        }

        this.storage = YamlConfiguration.loadConfiguration(file);

        this.logger = Logger.getLogger(getClass().getName());
        logger.setUseParentHandlers(false);
        try {
            logger.addHandler(new FileHandler(plugin.getDataFolder().getAbsolutePath() + "/inventories.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the inventory of given player to disk & detodated wam.
     *
     * @param plr the player whose inventory to save
     * @return whether the inventory was saved
     */
    public boolean saveInventory(Player plr) {
        List<ItemStack> inventory = getSavedStacks(plr);
        inventory.addAll(Arrays.asList(InventoryHelper.cloneAll(plr.getInventory().getContents())));
        inventory.addAll(Arrays.asList(InventoryHelper.cloneAll(plr.getInventory().getArmorContents())));
        inventory.removeIf(Objects::isNull);

        return saveInventory(plr, inventory);
    }

    private boolean saveInventory(Player plr, List<ItemStack> inventory) {
        if (inventory.isEmpty()) {
            return true;
        }

        storage.set(plr.getUniqueId().toString(), inventory);
        logger.info("Saving inventory for " + plr.getUniqueId() + ": " + CommandHelper.CSCollection(inventory));
        if (trySaveStorage()) return false;

        cache.put(plr.getUniqueId(), inventory);
        return true;
    }

    private boolean trySaveStorage() {
        try {
            storage.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * Attempts to load the saved inventory of given player, if any. If the saved inventory does not fit, parts which didn't
     * will be saved back. This will send a message to the player informing them if they got anything back.
     * @param plr the player whose inventory to load
     * @return whether any items were returned to the player
     * @see #loadInventory(org.bukkit.entity.Player)
     */
    public boolean loadInventoryWithMessage(Player plr) {
        if (loadInventory(plr)) {
            plr.sendMessage(MinoDuelPlugin.PREFIX + "Du hast dein Inventar von deinem vorherigen 1vs1 zurückbekommen!");
            return true;
        }
        return false;
    }

    /**
     * Attempts to load the saved inventory of given player, if any. If the saved inventory does not fit, parts which didn't
     * will be saved back.
     *
     * @param plr the player whose inventory to load
     * @return whether any items were returned to the player
     */
    public boolean loadInventory(Player plr) {
        if (plr == null) {
            return false;
        }

        List<ItemStack> inventory = getSavedStacks(plr);

        if (inventory.isEmpty()) {
            return false;
        }

        logger.info("Loaded inventory for " + plr.getName() + ": " + inventory + " @ size: " + CommandHelper.safeSize(inventory));

        cache.remove(plr.getUniqueId());
        storage.set(plr.getUniqueId().toString(), null);
        trySaveStorage();

        return returnInventory(plr, inventory);
    }

    private List<ItemStack> getSavedStacks(Player plr) {
        if (cache.containsKey(plr.getUniqueId())) {
            return cache.get(plr.getUniqueId());
        }

        if (storage.isList(plr.getUniqueId().toString())) {
            @SuppressWarnings("unchecked") List<ItemStack> inventory = (List<ItemStack>) storage.getList(plr.getUniqueId().toString());
            if (inventory == null || inventory.isEmpty()) {
                return Lists.newArrayList();
            }
            return inventory;
        }

        return Lists.newArrayList();
    }

    /**
     * Returns a list of items to given player, storing any rejected items back to storage.
     *
     * @param plr       the receiver of the items
     * @param inventory a list of the items to return
     * @return whether any items were returned to the player
     */
    private boolean returnInventory(Player plr, List<ItemStack> inventory) {
        Collection<ItemStack> rejected = plr.getInventory().addItem(
                InventoryHelper.cloneAll(inventory).toArray(new ItemStack[inventory.size()])
        ).values();

        if (!rejected.isEmpty()) {
            saveInventory(plr, InventoryHelper.cloneAll(rejected));
            plr.sendMessage("§4" + rejected.size() + "§c Items passten nicht in dein Inventar. Bitte leere dein Inventar und rejoine dann.");
        }

        return rejected.size() < inventory.size();
    }

    /**
     * Checks if a given player has an inventory saved.
     *
     * @param plr the player whose inventory to check
     * @return whether any items are saved for given player
     */
    public boolean hasInventory(Player plr) {
        return cache.containsKey(plr.getUniqueId()) ||
                storage.isList(plr.getUniqueId().toString());
    }

    public void onReload() {
        Arrays.asList(logger.getHandlers()).forEach(Handler::close);
        trySaveStorage();
    }
}
