package me.sebi7224.minoduel.arena;

import me.sebi7224.minoduel.MinoDuelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.xxyy.common.util.inventory.InventoryHelper;
import io.github.xxyy.common.util.inventory.ItemStackFactory;

/**
 * A simple icon menu framework, adapted to MinoDuel.
 *
 * @since 1.0
 */
public class ArenaMenu implements Listener {

    private final String name;
    private Arena[] optionArenas;
    private int size;

    private final MinoDuelPlugin plugin;
    private final ArenaManager arenaManager;

    public ArenaMenu(ArenaManager arenaManager, MinoDuelPlugin plugin) {
        this.arenaManager = arenaManager;
        this.name = "§8Wähle eine Arena!";
        this.plugin = plugin;

        refresh();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void refresh() {
        this.optionArenas = new Arena[InventoryHelper.validateInventorySize(arenaManager.all().size() + 1)];
        this.size = optionArenas.length;

        int i = 0;
        for (Arena arena : arenaManager.all()) {
            if (arena.isValid()) {
                setArena(i, arena);
            }
            i++;
        }
    }

    public ArenaMenu setArena(int position, Arena arena) {
        optionArenas[position] = arena;
        return this;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, name);
        for (int i = 0; i < optionArenas.length; i++) {
            if (optionArenas[i] == null) {
                inventory.setItem(i, arenaManager.getAnyArenaIcon());
            } else {
                inventory.setItem(i, getIcon(optionArenas[i]));
            }
        }
        player.openInventory(inventory);
    }

    public void destroy() {
        HandlerList.unregisterAll(this);
        optionArenas = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().equals(name)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < size) {
                Player player = (Player) event.getWhoClicked();
                Arena arena = optionArenas[slot];

                player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
                plugin.getQueueManager().enqueue(player, arena); //This takes care of teleportation etc if a match is found

                if (plugin.getQueueManager().isQueued(player)) {
                    player.sendMessage(plugin.getPrefix() + "Du bist nun in der Warteschlange" +
                            (arena == null ? "" : " für die Arena §e" + arena.getName() + "§6") +
                            "!");
                }

                InventoryHelper.closeInventoryLater(event.getWhoClicked(), this.plugin);
            }
        }
    }

    public int getSize() {
        return size;
    }

    private ItemStack getIcon(Arena arena) {
        return new ItemStackFactory(arena.getIconStack())
                .lore(arena.getPlayerString())
                .defaultDisplayName("§6" + arena.getName())
                .produce();
    }
}
