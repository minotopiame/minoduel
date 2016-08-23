package me.minotopia.minoduel;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.NestedCommand;
import li.l1t.common.chat.XyComponentBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
    private ListMultimap<String, BaseComponent[]> messageCache = MultimapBuilder.hashKeys().arrayListValues().build();

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
        List<BaseComponent[]> messages;

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

        messages.stream().forEach(msg -> {
            if(sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(msg);
            } else {
                sender.sendMessage(BaseComponent.toLegacyText(msg));
            }
        });
        sender.sendMessage("§6Tipp: §eBewege deine Maus über die Befehle! :)");

        return true;
    }

    private BaseComponent[] buildHelpLine(String commandKey, Method method) {
        com.sk89q.minecraft.util.commands.Command cmd = method.getAnnotation(com.sk89q.minecraft.util.commands.Command.class);
        String usage = getUsage(commandKey, cmd);

        XyComponentBuilder message = new XyComponentBuilder(usage)
                .color(ChatColor.GOLD)
                .tooltip("Hier klicken, um folgenden Befehl zu kopieren:\n" + usage)
                .suggest(usage);

        String help = cmd.help().isEmpty() ? cmd.desc() : cmd.help();

        if (!help.isEmpty()) { //Append help if we have some
            String[] helpLines = help.split("\n");
            String helpMessage = helpLines[0];
            if(helpLines.length > 1) {
                helpMessage += " [Mehr...]";
            }
            message.append(helpMessage, ChatColor.YELLOW, ComponentBuilder.FormatRetention.NONE);
            if (helpLines.length > 1) { //If we have multiple lines, make them available as tooltip
                message.tooltip(help);
            }
        }

        return message.create();
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
