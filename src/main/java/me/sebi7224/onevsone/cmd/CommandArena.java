package me.sebi7224.onevsone.cmd;

import me.sebi7224.onevsone.MainClass;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 4.8.14
 */
public class CommandArena implements CommandExecutor {
    private final MainClass plugin;

    public CommandArena(MainClass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        return false;
    }
}
