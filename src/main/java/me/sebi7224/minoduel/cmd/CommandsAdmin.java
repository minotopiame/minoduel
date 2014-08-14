package me.sebi7224.minoduel.cmd;

import com.google.common.collect.Lists;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import mkremins.fanciful.FancyMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.YELLOW;

/**
 * Command for administering 1vs1.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 4.8.14 // 1.0
 */
public class CommandsAdmin {
    @SuppressWarnings("UnusedParameters")
    public CommandsAdmin(MinoDuelPlugin plugin) {

    }

    @Command(aliases = {"mdm", "mdmod", "mdadmin"}, desc = "Administrationsbefehle für MinoDuel")
    @NestedCommand(SubCommands.class)
    public static void mdmMain() {

    }

    public static class SubCommands {
        private final MinoDuelPlugin plugin;

        public SubCommands(MinoDuelPlugin plugin) {
            this.plugin = plugin;
        }

        @Command(aliases = {"reward"},
                desc = "Setzt die Standardbelohnung! (Inv ist ohne Rüstung, Hotbar ist Standard)",
                usage = "<-s *Hotbar*|Hand|Inv>",
                flags = "s:")
        @CommandPermissions({"minoduel.admin.setreward"})
        public void adminSetDefaultReward(CommandContext args, Player player) throws CommandException {
            List<ItemStack> stacks;

            switch (args.getFlag('s', "Hotbar").toLowerCase()) {
                case "hand":
                    stacks = Lists.newArrayList(player.getItemInHand());
                    break;
                case "inv":
                case "i":
                case "inventar":
                    stacks = Lists.newArrayList(player.getInventory().getContents());
                    break;
                case "hotbar":
                case "hb":
                    stacks = Lists.newArrayList(player.getInventory().getContents()).stream()
                            .limit(9) //9 Hotbar slots - IDs 0-8
                            .collect(Collectors.toList());
                    break;
                default:
                    player.sendMessage("§cUnbekannte Quelle! Valide Quellen: -s [Hotbar|Hand|Inv]");
                    return;
            }

            stacks.removeIf(stack -> stack == null || stack.getType() == Material.AIR);

            CmdValidate.validateNotEmpty(stacks, "Die Quelle (" + args.getFlag('s', "Hotbar") + ") ist leer!");

            Arenas.setDefaultRewards(stacks);

            player.sendMessage("§aStandardbelohnung gesetzt!");
        }

        @Command(aliases = {"arenas"},
                desc = "Listet alle Arenen auf.",
                usage = "[Suchbegriff]")
        @CommandPermissions({"minoduel.user.arenas"})
        public void playerListArenas(CommandContext args, Player player) throws CommandException {
            Collection<Arena> arenas = Arenas.all();
            if (args.argsLength() >= 1) { //If we have a filter
                String search = args.getString(0).toLowerCase(); //Get that
                arenas.removeIf(arena -> !arena.getName().toLowerCase().contains(search)); //And remove non-matching arenas
            }

            if (arenas.isEmpty()) {
                player.sendMessage(plugin.getPrefix() + "§cKeine Arena entspricht deinem Suchkriterium!");
                return;
            }

            player.sendMessage("§6========> §eMinoTopia §6| §e1vs1 §6<========");
            for (Arena arena : arenas) {
                //@formatter:off
                new FancyMessage(arena.getName())
                            .color(GOLD)
                        .then(" -> ")
                            .color(YELLOW)
                        .then(arena.getValidityString())
                            .color(GRAY)
                            .command("/mda info " + arena.getName())
                            .tooltip("Hier klicken für mehr Info")
                        .send(player);
                //@formatter:on
            }
        }
    }
}
