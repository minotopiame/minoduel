package me.minotopia.minoduel.arena;

import com.google.common.collect.ImmutableList;
import li.l1t.common.checklist.Checklist;
import li.l1t.common.checklist.renderer.CommandSenderRenderer;
import li.l1t.common.util.XyValidate;
import li.l1t.common.util.inventory.InventoryHelper;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    protected ArenaState state = ArenaState.INVALID;
    private ItemStack[] inventoryKit;
    private ItemStack[] armorKit;
    private boolean doAllRewards = true;
    private boolean enabled = true;

    private Checklist validityChecklist = new Checklist()
            .append("Arena existiert", () -> configSection != null)
            .append("Spawn 1 gesetzt", () -> firstSpawn != null)
            .append("Spawn 2 gesetzt", () -> secondSpawn != null)
            .append("Kit gesetzt (Inventar)", () -> inventoryKit != null)
            .append("Kit gesetzt (Rüstung)", () -> armorKit != null)
            .append("Icon gesetzt", () -> iconStack != null)
            .append("Arena aktiviert", () -> enabled);
    private boolean lastValidity = false;

    public ConfigurableArena(@Nonnull ConfigurationSection storageBackend, ArenaManager arenaManager) {
        this.configSection = storageBackend;
        this.arenaManager = arenaManager;
        this.name = storageBackend.getName();
        lastValidity = isValid();
    }

    @Override
    public void endGame(@Nullable ArenaPlayerInfo winner) {
        endGame(winner, true);
    }

    @Override
    public void remove() {
        XyValidate.validateState(configSection != null, "Can't remove already removed arena!");

        if (isOccupied()){
            getPlayers().forEach(plr -> plr.getPlayer().sendMessage("§cDie Arena, in der du warst, wurde entfernt. Bitte entschuldige die Unannehmlichkeiten."));
            endGame(null, false);
        }

        configSection.getParent().set(configSection.getName(), null);
        arenaManager.arenaCache.remove(getName());
        configSection = null;
        saveConfig();
    }

    @Override
    public boolean isValid() {
        boolean valid = validityChecklist.isDone(); //Overhead is probably not that bad

        if (valid != lastValidity){
            lastValidity = valid;
            arenaManager.registerValidityChange(this);
            if (valid && state == ArenaState.INVALID){
                state = ArenaState.READY;
            } else if (!valid){
                state = ArenaState.INVALID;
            }
        }

        return valid;
    }

    @Override
    public void sendChecklist(CommandSender sender) {
        if (isOccupied()){
            sender.sendMessage("§7Die Arena ist momentan besetzt: " + getPlayerString());
        }
        if (configSection == null){
            sender.sendMessage("§cDiese Arena wurde gelöscht!");
            return;
        }

        CHECKLIST_RENDERER.renderFor(sender, validityChecklist);
    }

    @Override
    public List<ItemStack> getRewards() {
        if (specificRewards == null || specificRewards.isEmpty()){
            return arenaManager.getDefaultRewards();
        }

        return doAllRewards ? specificRewards : ImmutableList.of(specificRewards.get(RandomUtils.nextInt(specificRewards.size())));
    }

    @Override
    public void setRewards(List<ItemStack> specificRewards) {
        validateHasConfig();

        specificRewards.removeIf(stack -> stack == null || stack.getType() == Material.AIR);
        specificRewards = InventoryHelper.cloneAll(specificRewards);
        this.configSection.set(SPECIFIC_REWARD_PATH, specificRewards);
        this.specificRewards = specificRewards;
        saveConfig();
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
        saveConfig();
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
        saveConfig();
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
        saveConfig();
    }

    @Override
    public void setEnabled(boolean enabled) {
        validateHasConfig();

        configSection.set(ENABLED_PATH, enabled);
        this.enabled = enabled;
        saveConfig();
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

        inventoryKit = InventoryHelper.cloneAll(inventoryKit);

        configSection.set(INVENTORY_KIT_PATH, inventoryKit);
        this.inventoryKit = inventoryKit;
        saveConfig();
    }

    @Override
    public ItemStack[] getArmorKit() {
        return armorKit;
    }

    @Override
    public void setArmorKit(ItemStack[] armorKit) {
        validateHasConfig();

        armorKit = InventoryHelper.cloneAll(armorKit);

        configSection.set(ARMOR_KIT_PATH, armorKit);
        this.armorKit = armorKit;
        saveConfig();
    }

    protected ConfigurationSection getConfigSection() {
        return configSection;
    }

    @Override
    public void setDoAllRewards(boolean doAllRewards) {
        validateHasConfig();

        this.configSection.set(REWARD_ALL_PATH, doAllRewards);
        this.doAllRewards = doAllRewards;
        saveConfig();
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

        if (configSection.contains(INVENTORY_KIT_PATH)){
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(INVENTORY_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[InventoryType.PLAYER.getDefaultSize()]);
        }

        if (configSection.contains(ARMOR_KIT_PATH)){
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(ARMOR_KIT_PATH);
            this.armorKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[4]);
        }
    }

    private void saveConfig() {
        arenaManager.saveArena(this);
    }

    private void validateHasConfig() {
        XyValidate.validateState(configSection != null, "The arena %s has been removed!", getName());
    }

    public ArenaState getState() {
        return state;
    }
}
