package me.sebi7224.minoduel;

import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.NestedCommand;
import mkremins.fanciful.FancyMessage;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import io.github.xxyy.lib.guava17.collect.ListMultimap;
import io.github.xxyy.lib.guava17.collect.MultimapBuilder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helps parsing help for commands.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 15.8.14
 */
public class CommandHelpHelper {
    private final CommandsManager<CommandSender> commandsManager;
    private ListMultimap<String, FancyMessage> messageCache = MultimapBuilder.hashKeys().arrayListValues().build();

    public CommandHelpHelper(CommandsManager<CommandSender> commandsManager) {
        this.commandsManager = commandsManager;
    }

    public void sendNestedHelp(CommandSender sender, Command command, String[] args) {
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = command.getName();
        sendNestedHelp(null, sender, newArgs, 0);
    }

    private boolean sendNestedHelp(Method parent, CommandSender sender, String[] args, int level) {
        String commandName = args[level];

        Map<String, Method> map = commandsManager.getMethods().get(parent);
        Method method = map.get(commandName.toLowerCase());

        if (method == null) {
            return false;
        }

        if (level == 0) {
            com.sk89q.minecraft.util.commands.Command cmd = method.getAnnotation(com.sk89q.minecraft.util.commands.Command.class);
            String help = cmd.help().isEmpty() ? cmd.desc() : cmd.help();

            for (String line : help.split("\n")) {
                sender.sendMessage("§e" + line);
            }
        }

        if (method.isAnnotationPresent(NestedCommand.class) && (args.length - level - 1) > 0) { //Whether we proceed to the next level
            if (sendNestedHelp(method, sender, args, level + 1)) {
                return true; //true -> Help was sent
            } else {
                args = Arrays.copyOf(args, level + 1);
            }
        }

        Map<String, Method> subCommands = commandsManager.getMethods().get(method);
        List<FancyMessage> messages;

        String commandKey = StringUtils.join(args, ' ').toLowerCase();
        if (messageCache.containsKey(commandKey)) {
            messages = messageCache.get(commandKey);
        } else {
            Set<String> encounteredMethods = new HashSet<>(subCommands.size());

            messages = subCommands.values().stream()
                    .filter(meth -> encounteredMethods.add(meth.getName())) //Returns false if name is already present - Prevents aliases showing multiple times
                    .map(meth -> buildHelpLine(commandKey, meth)) //lelelelelel we're breaking bad now
                    .collect(Collectors.toList());
            messageCache.putAll(commandKey, messages);
        }

        messages.stream().forEach(msg -> msg.send(sender));
        sender.sendMessage("§6Tipp: §eBewege deine Maus über die Befehle! :)");

        return true;
    }

    private FancyMessage buildHelpLine(String commandKey, Method method) {
        com.sk89q.minecraft.util.commands.Command cmd = method.getAnnotation(com.sk89q.minecraft.util.commands.Command.class);
        String usage = getUsage(commandKey, cmd);

        FancyMessage message = new FancyMessage(usage)
                .color(ChatColor.GOLD)
                .tooltip("Hier klicken, um folgenden Befehl zu kopieren:", usage)
                .suggest(usage);

        String help = cmd.help().isEmpty() ? cmd.desc() : cmd.help();

        if (!help.isEmpty()) { //Append help if we have some
            String[] helpLines = help.split("\n");
            message.then(helpLines[0] + (helpLines.length > 1 ? " [Mehr...]" : ""))
                    .color(ChatColor.YELLOW);

            if (helpLines.length > 1) { //If we have multiple lines, make them available as tooltip
                message.tooltip(helpLines);
            }
        }

        return message;
    }

    private String getUsage(String commandKey, com.sk89q.minecraft.util.commands.Command cmd) {
        StringBuilder commandUsage = new StringBuilder("/")
                .append(commandKey)
                .append(' ')
                .append(cmd.aliases()[0])
                .append(' ');

        if (cmd.flags().length() > 0) {
            String flagString = cmd.flags().replaceAll(".:", "");
            if (flagString.length() > 0) {
                commandUsage.append("[-")
                        .append(flagString)
                        .append("] ");
            }
        }

        return commandUsage
                .append(cmd.usage())
                .append(' ')
                .toString();
    }


}
