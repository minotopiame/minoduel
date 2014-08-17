package me.sebi7224.minoduel.cmd;

import com.sk89q.minecraft.util.commands.CommandException;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.ArenaManager;
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
        if (stack == null || stack.getType() == Material.AIR) {
            throw new CommandException("§cDu hast nichts in der Hand!");
        }

        return stack;
    }

    public static <T extends Collection<?>> T validateNotEmpty(T t, String message) throws CommandException {
        if (t.isEmpty()) {
            throw new CommandException(message);
        }

        return t;
    }

    public static Arena getArenaChecked(String arenaName, ArenaManager arenaManager) throws CommandException {
        Arena arena = arenaManager.byName(arenaName);

        if (arena == null) {
            throw new CommandException(MinoDuelPlugin.PREFIX + "§cDie Arena §4" + arenaName + " §cexistiert nicht!");
        }

        return arena;
    }

    public static Arena getArenaOrNull(String arenaName, ArenaManager arenaManager) throws CommandException {
        arenaName = arenaName == null ? null : arenaName.toLowerCase(); //Save an if statement, save a kitten!
        if (arenaName == null || arenaName.contains("null") || arenaName.contains("egal")) {
            return null;
        }

        Arena arena = arenaManager.byName(arenaName);

        if (arena == null) {
            throw new CommandException(MinoDuelPlugin.PREFIX + "§cDie Arena §4" + arenaName + " §cexistiert nicht!");
        }

        return arena;
    }

    public static boolean getBooleanChecked(String str) throws CommandException {
        switch (str.toLowerCase()) {
            case "true":
            case "on":
            case "yes":
            case "ja":
                return true;
            case "false":
            case "off":
            case "no":
            case "nein":
                return false;
            default:
                throw new CommandException("Das ist kein bool'scher Wert: " + str + " Valide bool'sche Werte: true/ja und false/nein");
        }
    }
}
