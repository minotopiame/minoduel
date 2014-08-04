package me.sebi7224.onevsone.cmd;

import me.sebi7224.onevsone.MainClass;
import me.sebi7224.onevsone.WaitingQueueManager;
import me.sebi7224.onevsone.arena.Arena;
import me.sebi7224.onevsone.arena.Arenas;
import me.sebi7224.onevsone.util.IconMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.xxyy.common.util.CommandHelper;
import io.github.xxyy.common.util.inventory.InventoryHelper;

import java.util.Collection;

public class Command1vs1 implements CommandExecutor {

    private final MainClass plugin;
    private static IconMenu arenaMenu;

    public Command1vs1(MainClass plugin) {
        this.plugin = plugin;

        initArenaMenu();
    }

    public void initArenaMenu() {
        arenaMenu = new IconMenu("§8Wähle eine Arena!", //Title
                InventoryHelper.validateInventorySize(Arenas.all().size() + 1), //Round arena amount up to next valid inv size - Need +1 so that at least one "any arena" option is included
                event -> {
                    final Player playerClicked = event.getPlayer();
                    playerClicked.playSound(playerClicked.getLocation(), Sound.NOTE_PIANO, 1, 1);
                    Arena arena = event.getArena();
                    WaitingQueueManager.enqueue(playerClicked, arena);

                }, plugin);

        int i = 0;
        for (Arena arena : Arenas.all()) {
            if (i >= arenaMenu.getSize()) {
                return;
            }

            arenaMenu.setArena(i, arena);
            i++;
        }
    }

    private void openMenuFor(Player player) {
        int slot = 0;
        for (Arena arena : Arenas.all()) {
            if (slot > 53) {
                return;
            }
            arenaMenu.setArena(slot, arena);
            slot++;
        }
        arenaMenu.open(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
            sender.sendMessage("§6/1vs1 join §c- §bÖffnet das Arenamenü");
            sender.sendMessage("§6/1vs1 leave §c- §bVerlässt ein Spiel, wenn du alleine in einer Arena bist");
            if (sender.hasPermission("1vs1.admin")) {
                sender.sendMessage("§6/1vs1 create <arena> §c- §bErstellt eine neue Arena");
                sender.sendMessage("§6/1vs1 remove <arena> §c- §bLöscht eine Arena");
                sender.sendMessage("§6/1vs1 setspawn <1|2> <arena> §c- §bSetzt ein Spawn");
                sender.sendMessage("§6/1vs1 seticon <arena> §c- §bSetzt das Icon in deiner Hand als ArenaIcon");
                sender.sendMessage("§6/1vs1 list §c- §bZeigt die Arenen");
                sender.sendMessage("§6/1vs1 setitems <arena> §c- §bSetzt deine Items in deimen Inventar als Kit der Arena");
                sender.sendMessage("§6/1vs1 setreward <arena|global> §c- §bSetzt den globalen Reward oder deine Items in deinem Inventar als Reward der Arena");
            }
            sender.sendMessage("§b=================================");
            return true;
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("[1vs1] Du musst ein Spieler sein, um diesen Befehl auszufuehren!");
                return true;
            }
            Player player = (Player) sender;
            switch (args[0].toLowerCase()) {
                case "create":
                    if (!CommandHelper.checkPermAndMsg(player, "1vs1.create", commandLabel)) {
                        return true;
                    }

                    if (args.length < 2) {
                        player.sendMessage("§cZu wenige Argumente übergeben! /1vs1 help");
                        return true;
                    }

                    if (Arenas.existsByName(args[1])) {
                        player.sendMessage(MainClass.getPrefix() + "§cDie Arena §4" + args[1] + " §cexistiert bereits!");
                        return true;
                    }

                    Arenas.createArena(args[1], plugin.getConfig());
                    player.sendMessage(MainClass.getPrefix() + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich erstellt!");
                    return true;
                case "remove":
                    if (!CommandHelper.checkPermAndMsg(player, "1vs1.remove", commandLabel)) {
                        return true;
                    }

                    if (args.length < 2) {
                        player.sendMessage("§cVerwendung: /1vs1 remove [Arenaname]");
                        return true;
                    }

                    Arena arenaToRemove = Arenas.byName(args[1]);

                    if (arenaToRemove == null) {
                        player.sendMessage(MainClass.getPrefix() + "§cDie Arena §4" + args[1] + " §cexistiert nicht!");
                        return true;
                    }

                    arenaToRemove.remove();
                    player.sendMessage(MainClass.getPrefix() + "§7Die Arena §6" + args[1] + " §7wurde erfolgreich entfernt!");
                    return true;
                case "list":
                    if (!CommandHelper.checkPermAndMsg(player, "1vs1.list", commandLabel)) {
                        return true;
                    }

                    Collection<Arena> arenasToList = Arenas.all(); //Strange naming because of switch scope thing

                    if (arenasToList.isEmpty()) {
                        player.sendMessage(MainClass.getPrefix() + "§cEs sind keine Arenen vorhanden!");
                        return true;
                    }

                    player.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
                    int i = 1;
                    for (Arena arena : arenasToList) {
                        player.sendMessage("§7" + i + ". " + arena.getName() + " " + arena.getValidityString());
                        i++;
                    }
                    return true;
                case "leave":
                    Arena arenaToLeave = Arenas.getPlayerArena(player);

                    if (arenaToLeave != null) {
                        arenaToLeave.endGame(arenaToLeave.getOther(player));
                    } else {
                        player.sendMessage(MainClass.getPrefix() + "§eDu befindest dich in keinem Kampf!");
                        return true;
                    }

//                case "setspawn": //TODO separate cmd
//                    if (player.hasPermission("1vs1.setspawn")) {
//                        if (args.length < 3) {
//                            player.sendMessage("§cÜberprüfe bitte deine Argumente!");
//                            return true;
//                        }
//                        if (args[1].equalsIgnoreCase("1")) {
//                            Arenas.saveLocation("arenas." + args[2] + ".Spawn1", player.getLocation());
//                            player.sendMessage(MainClass.getPrefix() + "§7Du hast den §61. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
//                            return true;
//                        } else if (args[1].equalsIgnoreCase("2")) {
//                            Arenas.saveLocation("arenas." + args[2] + ".Spawn2", player.getLocation());
//                            player.sendMessage(MainClass.getPrefix() + "§7Du hast den §62. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
//                            return true;
//                        } else {
//                            player.sendMessage(MainClass.getPrefix() + "§cDu kannst nur den Spawn 1 und 2 setzen!");
//                            return true;
//                        }
//
//                    } else {
//                        player.sendMessage("noperm");
//                        return true;
//                    }
//                case "seticon": //TODO separate cmd, "any arena" icon, Adapt to Arena obj
//                    if (player.hasPermission("1vs1.seticon")) {
//                        if (args.length < 2) {
//                            player.sendMessage("§cDu hast keinen Arenanamen angegeben!");
//                            return true;
//                        }
//                        if (player.getItemInHand().getType() == Material.AIR || player.getItemInHand() == null) {
//                            player.sendMessage(MainClass.getPrefix() + "§cDu hast kein §4Item §cin deiner §4Hand§c!");
//                            return true;
//                        }
//                        Arenas.setArenaIconItem(args[1], player.getItemInHand().getType());
//                        player.sendMessage(MainClass.getPrefix() + "§7Du hast das §6Icon§7 der Arena §6" + args[1] + " §7gesetzt!");
//                        return true;
//                    } else {
//                        player.sendMessage("noperm");
//                        return true;
//                    }
//                case "setitems": //TODO separate cmd, "setkit", adapt to Arena obj
//                    if (player.hasPermission("1vs1.setitems")) {
//                        if (args.length < 2) {
//                            player.sendMessage("§cDu hast keinen Arenanamen angegeben!");
//                            return true;
//                        }
//                        if (player.getInventory().getContents().length == 0) {
//                            player.sendMessage(MainClass.getPrefix() + "§cDu hast keine §4Items §cin deinem §4Inventar§c!");
//                            return true;
//                        }
//                        for (int i = 0; player.getInventory().getContents().length < i; i++) {
//                            if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
//                                MainClass.instance().getConfig().set("arenas." + args[1] + ".items", player.getInventory().getContents()[i]);
//                            }
//                        }
//                        for (int i = 0; player.getInventory().getArmorContents().length < i; i++) {
//                            if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
//                                MainClass.instance().getConfig().set("arenas." + args[1] + ".armor", player.getInventory().getArmorContents()[i]);
//                            }
//                        }
//                        MainClass.instance().saveConfig();
//                        player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich die Items in der Arena §6" + args[1] + " §7gesetzt!");
//                        return true;
//                    } else {
//                        player.sendMessage("noperm");
//                        return true;
//                    }
//                case "setreward": //TODO: spaarate cmd, adapt to Arena obj
//                    if (player.hasPermission("1vs1.setreward")) {
//                        if (args.length < 2) {
//                            player.sendMessage("§cBitte überprüfe deine Argumente!");
//                            return true;
//                        }
//                        if (player.getInventory().getContents().length == 0) {
//                            player.sendMessage(MainClass.getPrefix() + "§cDu hast keine §4Items §cin deinem §4Inventar§c!");
//                            return true;
//                        }
//                        if (args[1].equalsIgnoreCase("global")) {
//                            for (int i = 0; player.getInventory().getContents().length < i; i++) {
//                                if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
//                                    MainClass.instance().getConfig().set("globalRewards", player.getInventory().getContents()[i]);
//                                }
//                            }
//                            MainClass.instance().saveConfig();
//                            player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich den globalen Reward gesetzt!");
//                            return true;
//                        } else {
//                            if (Arenas.exists(args[1])) {
//                                for (int i = 0; player.getInventory().getContents().length < i; i++) {
//                                    if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
//                                        MainClass.instance().getConfig().set("arenas" + args[1] + ".rewards", player.getInventory().getContents()[i]);
//                                    }
//                                }
//                                MainClass.instance().saveConfig();
//                                player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich den Reward in Arena §e" + args[1] + "§7gesetzt!");
//                                return true;
//                            } else {
//                                player.sendMessage(MainClass.getPrefix() + "§7Die Arena §e" + args[1] + " §7existiert nicht!");
//                                return true;
//                            }
//                        }
//                    }
                case "join":
                    if (!CommandHelper.checkPermAndMsg(player, "1vs1.join", commandLabel)) {
                        return true;
                    }

                    if (!Arenas.anyExist()) {
                        player.sendMessage(MainClass.getPrefix() + "§cKeine Arenen vorhanden =(!");
                        return true;
                    }

                    if (Arenas.isInGame(player)) {
                        player.sendMessage(MainClass.getPrefix() + " §eDu bist bereits in einem Kampf!");
                        return true;
                    }

                    if (InventoryHelper.isInventoryEmpty(player)) {
                        openMenuFor(player);
                        return true;
                    } else {
                        player.sendMessage(MainClass.getPrefix() + "§eDu musst zuerst dein Inventar leeren!");
                        return true;
                    }
                default:
                    sender.sendMessage("§cUnbekannte Aktion. /1vs1 help");
            }
        }
        return true;
    }
}
