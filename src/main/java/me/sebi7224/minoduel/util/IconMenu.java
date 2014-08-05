package me.sebi7224.minoduel.util;

import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import org.bukkit.Bukkit;
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
 * @since 1.0
 */
public class IconMenu implements Listener {

    private final String name;
    private final int size;
    private OptionClickEventHandler handler;
    private MinoDuelPlugin plugin;

    private Arena[] optionArenas;

    public IconMenu(String name, int size, OptionClickEventHandler handler, MinoDuelPlugin plugin) {
        this.name = name;

        this.size = size;
        this.handler = handler;
        this.plugin = plugin;
        this.optionArenas = new Arena[size];
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public IconMenu setArena(int position, Arena arena) {
        optionArenas[position] = arena;
        return this;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, name);
        for (int i = 0; i < optionArenas.length; i++) {
            if (optionArenas[i] == null) {
                inventory.setItem(i, Arenas.getAnyArenaIcon());
            } else {
                inventory.setItem(i, getIcon(optionArenas[i]));
            }
        }
        player.openInventory(inventory);
    }

    public void destroy() {
        HandlerList.unregisterAll(this);
        handler = null;
        plugin = null;
        optionArenas = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getTitle().equals(name)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < size/* && optionArenas[slot] != null*/) {
                OptionClickEvent e = new OptionClickEvent((Player) event.getWhoClicked(), slot, optionArenas[slot]);
                handler.onOptionClick(e);
                if (e.willClose()) {
                    InventoryHelper.closeInventoryLater(event.getWhoClicked(), this.plugin);
                }
            }
        }
    }

    public int getSize() {
        return size;
    }

    private ItemStack getIcon(Arena arena) {
        return new ItemStackFactory(arena.getIconStack())
                .lore(arena.getPlayerString())
                .produce();
    }

    public interface OptionClickEventHandler {
        public void onOptionClick(OptionClickEvent event);
    }

    public class OptionClickEvent {
        private final Player player;
        private final int position;
        private final Arena arena;
        private boolean close;

        public OptionClickEvent(Player player, int position, Arena arena) {
            this.player = player;
            this.position = position;
            this.arena = arena;
            this.close = true;
        }

        public Player getPlayer() {
            return player;
        }

        public int getPosition() {
            return position;
        }

        public Arena getArena() {
            return arena;
        }

        public boolean willClose() {
            return close;
        }

        public void setWillClose(boolean close) {
            this.close = close;
        }
    }

}
