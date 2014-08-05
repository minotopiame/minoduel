package me.sebi7224.minoduel.cmd;

import com.sk89q.minecraft.util.commands.CommandException;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * Validation methods for commands
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 5.8.14
 */
public final class CmdValidate {

    private CmdValidate() {

    }

    public static ItemStack validateStackNotEmpty(ItemStack stack) throws CommandException {
        if(stack == null || stack.getType() == Material.AIR) {
            throw new CommandException("§cDu hast nichts in der Hand!");
        }

        return stack;
    }

    public static <T extends Collection<?>> T validateNotEmpty(T t, String message) throws CommandException {
        if(t.isEmpty()) {
            throw new CommandException(message);
        }

        return t;
    }

    public static Arena getArenaChecked(String arenaName) throws CommandException {
        Arena arena = Arenas.byName(arenaName);

        if (arena == null) {
            throw new CommandException(MinoDuelPlugin.getPrefix() + "§cDie Arena §4" + arenaName + " §cexistiert nicht!");
        }

        return arena;
    }
}
