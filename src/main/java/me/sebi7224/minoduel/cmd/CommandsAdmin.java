package me.sebi7224.minoduel.cmd;

import me.sebi7224.minoduel.MinoDuelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command for administering 1vs1.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 4.8.14 // 1.0
 */
public class CommandsAdmin implements CommandExecutor {
    private final MinoDuelPlugin plugin;

    public CommandsAdmin(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        return false;
    }
}
