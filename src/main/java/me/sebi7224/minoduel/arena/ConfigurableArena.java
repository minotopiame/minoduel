package me.sebi7224.minoduel.arena;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import io.github.xxyy.common.checklist.Checklist;
import io.github.xxyy.common.checklist.renderer.CommandSenderRenderer;
import io.github.xxyy.common.util.XyValidate;
import io.github.xxyy.lib.intellij_annotations.NotNull;
import io.github.xxyy.lib.intellij_annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A configurable arena.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 6.8.14
 */
public abstract class ConfigurableArena implements Arena {
    public static final String CONFIG_PATH = "arenas";
    private static final String FIRST_SPAWN_PATH = "spawn1";
    private static final String SECOND_SPAWN_PATH = "spawn2";
    private static final String ICON_STACK_PATH = "icon";
    private static final String SPECIFIC_REWARD_PATH = "rewards";
    private static final String INVENTORY_KIT_PATH = "items";
    private static final String ARMOR_KIT_PATH = "armor";
    private static final String REWARD_ALL_PATH = "reward-all";
    private static final String ENABLED_PATH = "enabled";
    private static final CommandSenderRenderer CHECKLIST_RENDERER = new CommandSenderRenderer.Builder()
            .brackets(true).uncheckedEmpty(false).build();
    protected final String name;
    @Nullable
    protected ConfigurationSection configSection;
    protected Location firstSpawn;
    protected Location secondSpawn;
    protected ItemStack iconStack;
    protected List<ItemStack> specificRewards;

    private final ArenaManager arenaManager;
    private ItemStack[] inventoryKit;
    private ItemStack[] armorKit;
    private boolean doAllRewards = true;
    private boolean enabled = true;

    private Checklist validityChecklist = new Checklist()
            .append("Arena existiert", () -> configSection != null)
            .append("Spawn 1 gesetzt", () -> firstSpawn != null)
            .append("Spawn 2 gesetzt", () -> secondSpawn != null)
            .append("Kit gesetzt", () -> inventoryKit != null && armorKit != null)
            .append("Icon gesetzt", () -> iconStack != null)
            .append("Arena aktiviert", () -> enabled);

    public ConfigurableArena(@NotNull ConfigurationSection storageBackend, ArenaManager arenaManager) {
        this.configSection = storageBackend;
        this.arenaManager = arenaManager;
        this.name = storageBackend.getName();
    }

    @Override
    public void endGame(MinoDuelArena.PlayerInfo winner) {
        endGame(winner, true);
    }

    /**
     * Ends this game, gives the winner their reward (if applicable)
     *
     * @param winner               The winner of the game or NULL if no winner could be determined.
     * @param sendUndecidedMessage whether to send the "no winner could be determined" message if {@code winner} is NULL
     */
    public abstract void endGame(MinoDuelArena.PlayerInfo winner, boolean sendUndecidedMessage);

    @Override
    public void remove() {
        XyValidate.validateState(configSection != null, "Can't remove already removed arena!");

        if (isOccupied()) {
            getPlayers().forEach(plr -> plr.getPlayer().sendMessage("§cDie Arena, in der du warst, wurde entfernt. Bitte entschuldige die Unannehmlichkeiten."));
            endGame(null);
        }

        configSection.getParent().set(configSection.getName(), null);
        arenaManager.arenaCache.remove(getName());
        configSection = null;
    }

    @Override
    public boolean isValid() {
        return validityChecklist.isDone(); //Overhead is probably not that bad
    }

    @Override
    public void sendChecklist(CommandSender sender) {
        if (isOccupied()) {
            sender.sendMessage("§7Die Arena ist momentan besetzt: " + getPlayerString());
        }
        if (configSection == null) {
            sender.sendMessage("§cDiese Arena wurde gelöscht!");
            return;
        }

        CHECKLIST_RENDERER.renderFor(sender, validityChecklist);
    }

    @Override
    public List<ItemStack> getRewards() {
        if (specificRewards == null || specificRewards.isEmpty()) {
            return arenaManager.getDefaultRewards();
        }

        return doAllRewards ? specificRewards : ImmutableList.of(specificRewards.get(RandomUtils.nextInt(specificRewards.size())));
    }

    @Override
    public void setRewards(List<ItemStack> specificRewards) {
        validateHasConfig();

        specificRewards.removeIf(stack -> stack == null || stack.getType() == Material.AIR);
        this.configSection.set(SPECIFIC_REWARD_PATH, specificRewards);
        this.specificRewards = specificRewards;
    }

    public abstract boolean isOccupied();

    /**
     * @return A string nicely showing this arena's current players.
     */
    public abstract String getPlayerString();

    @Override
    public Location getFirstSpawn() {
        return firstSpawn;
    }

    @Override
    public void setFirstSpawn(Location firstSpawn) {
        validateHasConfig();

        arenaManager.saveLocation(configSection.createSection(FIRST_SPAWN_PATH), firstSpawn);
        this.firstSpawn = firstSpawn;
    }

    @Override
    public Location getSecondSpawn() {
        return secondSpawn;
    }

    @Override
    public void setSecondSpawn(Location secondSpawn) {
        validateHasConfig();

        arenaManager.saveLocation(configSection.createSection(SECOND_SPAWN_PATH), secondSpawn);
        this.secondSpawn = secondSpawn;
    }

    @Override
    public ItemStack getIconStack() {
        return iconStack.clone();
    }

    @Override
    public void setIconStack(ItemStack iconStack) {
        validateHasConfig();

        configSection.set(ICON_STACK_PATH, iconStack);
        this.iconStack = iconStack;
    }

    @Override
    public void setEnabled(boolean enabled) {
        validateHasConfig();

        configSection.set(ENABLED_PATH, enabled);
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ItemStack[] getInventoryKit() {
        return inventoryKit;
    }

    @Override
    public void setInventoryKit(ItemStack[] inventoryKit) {
        validateHasConfig();

        configSection.set(INVENTORY_KIT_PATH, inventoryKit);
        this.inventoryKit = inventoryKit;
    }

    @Override
    public ItemStack[] getArmorKit() {
        return armorKit;
    }

    @Override
    public void setArmorKit(ItemStack[] armorKit) {
        validateHasConfig();

        configSection.set(ARMOR_KIT_PATH, armorKit);
        this.armorKit = armorKit;
    }

    protected ConfigurationSection getConfigSection() {
        return configSection;
    }

    @Override
    public void setDoAllRewards(boolean doAllRewards) {
        validateHasConfig();

        this.configSection.set(REWARD_ALL_PATH, doAllRewards);
        this.doAllRewards = doAllRewards;
    }

    @Override
    public String getName() {
        return name;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    //protected utlity methods
    protected void updateFrom(ConfigurationSection section) {
        this.configSection = section;
        updateFromConfig();
    }

    //private utility methods
    @SuppressWarnings("unchecked")
    protected void updateFromConfig() {
        validateHasConfig();

        this.firstSpawn = arenaManager.getLocation(configSection.getConfigurationSection("spawn1"));
        this.secondSpawn = arenaManager.getLocation(configSection.getConfigurationSection("spawn2"));
        this.iconStack = configSection.getItemStack(ICON_STACK_PATH);
        this.specificRewards = (List<ItemStack>) configSection.getList(SPECIFIC_REWARD_PATH, new ArrayList<ItemStack>());
        this.doAllRewards = configSection.getBoolean(REWARD_ALL_PATH, doAllRewards);
        this.enabled = configSection.getBoolean(ENABLED_PATH, enabled);

        if (configSection.contains(INVENTORY_KIT_PATH)) {
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(INVENTORY_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[InventoryType.PLAYER.getDefaultSize()]);
        }

        if (configSection.contains(ARMOR_KIT_PATH)) {
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(ARMOR_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[4]);
        }
    }

    private void validateHasConfig() {
        XyValidate.validateState(configSection != null, "The arena %s has been removed!", getName());
    }
}
