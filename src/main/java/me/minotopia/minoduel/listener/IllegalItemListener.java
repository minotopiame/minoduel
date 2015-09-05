package me.minotopia.minoduel.listener;

import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.arena.Arena;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Checks player inventories, drops and pickups for MinoDuel kit items and removes any,
 * if found. This might be necessary due to Minecraft bugs allowing users to extract items
 * out of the game. While, from a factual perspective, this is not really going to be
 * necessary, admins and some other developers have insisted this functionality be
 * implemented. By clearing all inventories and closing them on teleport away,
 * there shouldn't be any way that this will ever be of any use. I will continue
 * to highlight that I do not endorse any usage of this listener.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 05/09/15
 */
public class IllegalItemListener implements Listener {
    private final MinoDuelPlugin plugin;

    public IllegalItemListener(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent evt) {
        if (!plugin.getArenaManager().isInGame(evt.getPlayer()) &&
                isKitItem(evt.getItemDrop().getItemStack())) {
            evt.getPlayer().sendMessage(plugin.getPrefix() +
                    "Du darfst außerhalb von 1vs1 keine 1vs1-Items besitzen!");
            evt.getItemDrop().remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent evt) {
        if (!plugin.getArenaManager().isInGame(evt.getPlayer()) &&
                isKitItem(evt.getItem().getItemStack())) {
            evt.getPlayer().sendMessage(plugin.getPrefix() +
                    "Du darfst außerhalb von 1vs1 keine 1vs1-Items besitzen!");
            evt.getItem().remove();
            evt.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInvClose(InventoryCloseEvent evt) {
        if(!plugin.getArenaManager().isInGame((Player) evt.getPlayer())) {
            ItemStack[] contents = evt.getPlayer().getInventory().getContents();
            boolean modified = false;
            for(int i = 0; i < contents.length; i++) {
                if(isKitItem(contents[i])) {
                    contents[i] = new ItemStack(Material.AIR);
                    modified = true;
                }
            }
            if(modified) {
                evt.getPlayer().getInventory().setContents(contents);
                evt.getPlayer().sendMessage(plugin.getPrefix() +
                        "Du darfst außerhalb von 1vs1 keine 1vs1-Items besitzen!");
            }
        }
    }

    private boolean isKitItem(ItemStack stack) {
        if(stack == null || !stack.hasItemMeta()) {
            return false;
        }
        if (stack.getItemMeta().hasLore()) {
            for(String loreLine : stack.getItemMeta().getLore()) {
                if(Arena.KIT_LORE_MARKER.equals(loreLine)) {
                    return true;
                }
            }
        }
        return false;
    }
}
