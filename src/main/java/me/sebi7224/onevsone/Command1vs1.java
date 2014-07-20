package me.sebi7224.onevsone;

import io.github.xxyy.common.util.CommandHelper;
import me.sebi7224.onevsone.arena.ArenaManager;
import me.sebi7224.onevsone.util.IconMenu;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Command1vs1 implements CommandExecutor {

    public static HashMap<Player, Location> waitingPlayers = new HashMap<>();
    public static HashMap<String, Integer> runningTasks = new HashMap<>();
    private static IconMenu arenaMenu;
    private static int countdown;
    private String noperm = "§4Du hast keine Erlaubnis für diesen Befehl!";

    public static void registerArenaMenu() {
        int ready_arenas = ArenaManager.getReadyArenasSize();
        int multiple = 0;
        do multiple++;
        while (9 * multiple < ready_arenas);
        int slots = 9 * multiple;
        if (slots < 9) {
            slots = 9;
        }

        arenaMenu = new IconMenu("§8Wähle eine Arena!", slots, new IconMenu.OptionClickEventHandler() {
            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                final Player player = event.getPlayer();
                player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
                String name = event.getName();
                name = name.replace("§6", "");
                if (getArenaPlayers(name).size() == 2) {

                    player.sendMessage(MainClass.getPrefix() + "§eDiese Arena ist bereits voll!"); //TODO queue
                    //TODO Message: Die gewählte Arena ist bereits voll! Du bist nun in der Warteschlange. (Position: %)
                    // [Andere Arena wählen] [Die Arena ist mir egal] [Abbrechen]

                } else if (getArenaPlayers(name).size() == 1) {

                    for (final Map.Entry<Player, String> entry : MainClass.getPlayersinFight().entrySet()) {
                        if (entry.getValue().equals(name)) {
                            final Player player1 = entry.getKey();

                            MainClass.getPlayersinFight().put(player, name);

                            waitingPlayers.put(player, player.getLocation()); //plsno
                            waitingPlayers.put(player1, player1.getLocation());

                            player.sendMessage(MainClass.getPrefix() + "§eIn 3 Sekunden wirst du in die Arena teleportiert...");
                            player1.sendMessage(MainClass.getPrefix() + "§eEs wurde ein Spieler gefunden! In 3 Sekunden wirst du teleportiert...");

                            final String finalName = name;
                            final Location locationPlayer = waitingPlayers.get(player);
                            final Location locationPlayer1 = waitingPlayers.get(player1);
                            new BukkitRunnable() {
                                public void run() {
                                    if (waitingPlayers.containsKey(player) && waitingPlayers.containsKey(player1)) {
                                        if (locationPlayer.getX() == player.getLocation().getX() && locationPlayer.getY() == player.getLocation().getY() && locationPlayer.getZ() == player.getLocation().getZ()) {
                                            if (locationPlayer1.getX() == player1.getLocation().getX() && locationPlayer1.getY() == player1.getLocation().getY() && locationPlayer1.getZ() == player1.getLocation().getZ()) {


                                                player.sendMessage(MainClass.getPrefix() + "§eDu hast die Arena §4" + finalName + " §ebetreten!");
                                                player.sendMessage(MainClass.getPrefix() + "§eMögen die Spiele beginnen!");

                                                player1.sendMessage(MainClass.getPrefix() + "§eDer Spieler §6" + player.getName() + "§e hat die Arena betreten!");
                                                player1.sendMessage(MainClass.getPrefix() + "§eMögen die Spiele beginnen!");

                                                for (Player player2 : Command1vs1.getArenaPlayers(finalName)) {

                                                    MainClass.getSavedLocations().put(player2, player2.getLocation());
                                                    MainClass.getSavedExperience().put(player2, player2.getExp());

                                                    player2.setFireTicks(0);
                                                    player2.setHealth(20);
                                                    player2.getActivePotionEffects().clear();
                                                    player2.setFoodLevel(20);
                                                    player2.getInventory().clear();
                                                    List listb = (List) MainClass.instance().getConfig().getList("arenas." + finalName + ".items");
                                                    ItemStack[] items = (ItemStack[]) listb.toArray(new ItemStack[listb.size()]);
                                                    List lista = (List) MainClass.instance().getConfig().getList("arenas." + finalName + ".armor");
                                                    ItemStack[] armor = (ItemStack[]) lista.toArray(new ItemStack[lista.size()]);
                                                    player2.getInventory().setArmorContents(armor);
                                                    player2.getInventory().setContents(items);
                                                    player2.setGameMode(GameMode.SURVIVAL);
                                                    player2.updateInventory();
                                                    player2.closeInventory();
                                                }
                                                player.teleport(ArenaManager.getLocation("arenas." + finalName + ".Spawn1"));
                                                player1.teleport(ArenaManager.getLocation("arenas." + finalName + ".Spawn2"));
                                                countdown = 300;

                                                new BukkitRunnable() {
                                                    public void run() {
                                                        countdown--;
                                                        if (!runningTasks.containsKey(finalName)) {
                                                            runningTasks.put(finalName, this.getTaskId());
                                                        }
                                                        if (countdown == 180 || countdown == 120 || countdown == 60) {
                                                            int minutes = countdown / 60;
                                                            player.sendMessage(MainClass.getPrefix() + "§7Noch §e" + minutes + " §7Minuten!");
                                                            player1.sendMessage(MainClass.getPrefix() + "§7Noch §e" + minutes + " §7Minuten!");
                                                        }
                                                        if (countdown == 30 || countdown == 10 || countdown == 5) {
                                                            player.sendMessage(MainClass.getPrefix() + "§7Noch §e" + countdown + " §7Sekunden!");
                                                            player1.sendMessage(MainClass.getPrefix() + "§7Noch §e" + countdown + " §7Sekunden!");
                                                        }
                                                        if (countdown == 0) {
                                                            MainClass.setGameResult(player, player1, finalName);
                                                            this.cancel();
                                                            player1.teleport(MainClass.getSavedLocations().get(player1));
                                                            MainClass.getSavedLocations().remove(player1);
                                                            runningTasks.remove(finalName);
                                                        }
                                                    }
                                                }.runTaskTimerAsynchronously(MainClass.instance(), 0, 20);
                                            } else {
                                                MainClass.getPlayersinFight().remove(player);
                                                MainClass.getPlayersinFight().remove(player1);
                                                waitingPlayers.remove(player);
                                                waitingPlayers.remove(player1);
                                                player1.sendMessage("§cTeleportierung abgebrochen du hast dich bewegt!");
                                                player.sendMessage("§cTeleportierung abgebrochen §e" + player1.getName() + " §chat sich bewegt!");
                                            }
                                        } else {
                                            MainClass.getPlayersinFight().remove(player);
                                            MainClass.getPlayersinFight().remove(player1);
                                            waitingPlayers.remove(player);
                                            waitingPlayers.remove(player1);
                                            player.sendMessage("§cTeleportierung abgebrochen du hast dich bewegt!");
                                            player1.sendMessage("§cTeleportierung abgebrochen §e" + player.getName() + " §chat sich bewegt!");
                                        }
                                    }
                                }
                            }.runTaskLaterAsynchronously(MainClass.instance(), 60L);

                        }
                    }
                } else {
                    MainClass.getPlayersinFight().put(player, name);
                    player.sendMessage(MainClass.getPrefix() + "§eDu hast die Arena §4" + name + " §ebetreten!");
                    player.sendMessage(MainClass.getPrefix() + "§eDu wirst teleportiert sobald ein zweiter Spieler beigetreten ist!");
                }
            }
        }, MainClass.instance());
    }

    private static void openMenuAt(Player player) {
        int slot = 0;
        for (String arenas : ArenaManager.getReadyArenas()) {
            if (slot > 53) {
                return;
            }
            String lore = "§7Klicke, um die Arena§6 " + arenas + " §7zu wählen!";
            if (getArenaPlayers(arenas).size() == 1) {
                lore = lore + "§c" + getArenaPlayers(arenas).get(0).getName() + " §avs. " + "§a<Unbekannt>";
            }
            if (getArenaPlayers(arenas).size() == 2) {
                lore = lore + "§c" + getArenaPlayers(arenas).get(0).getName() + " §avs. " + "§c" + getArenaPlayers(arenas).get(1).getName();
            }
            arenaMenu.setOption(slot, new ItemStack(ArenaManager.getArenaIconItem(arenas)), "§6" + arenas, lore);
            slot++;
        }
        arenaMenu.open(player);
    }

    public static List<Player> getArenaPlayers(String arena) {
        return MainClass.getPlayersinFight().entrySet().stream()
                .filter(e -> e.getValue().equals(arena))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
            sender.sendMessage("§6/1vs1 select §c- §bÖffnet das Arenamenü");
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
                sender.sendMessage("[1vs1] Du musst ein Spieler sein um diesen Befehl auszufuehren!");
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

                    if (ArenaManager.exists(args[1])) {
                        player.sendMessage(MainClass.getPrefix() + "§cDie Arena §4" + args[1] + " §cexistiert bereits!");
                        return true;
                    }

                    ArenaManager.createArena(args[1]);
                    player.sendMessage(MainClass.getPrefix() + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich erstellt!");
                    return true;
                case "remove":
                    if (player.hasPermission("1vs1.remove")) {
                        if (args.length < 2) {
                            player.sendMessage("§cDer Arenaname ist ungültig!");
                            return true;
                        }
                        if (!ArenaManager.exists(args[1])) {
                            player.sendMessage(MainClass.getPrefix() + "§cDie Arena §4" + args[1] + " §cexistiert nicht!");
                            return true;
                        }
                        ArenaManager.removeArena(args[1]);
                        player.sendMessage(MainClass.getPrefix() + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich entfernt!");
                        return true;
                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }
                case "list":
                    if (player.hasPermission("1vs1.list")) {
                        if (ArenaManager.getArenas() == null) {
                            player.sendMessage(MainClass.getPrefix() + "§cEs existiert keine Arena!");
                            return true;
                        }

                        player.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
                        String arenaStatus;
                        int arenasSize = 1;
                        for (String arenas : ArenaManager.getArenas()) {
                            if (ArenaManager.isArenaReady(arenas)) {
                                arenaStatus = "§a(bereit)";
                            } else {
                                arenaStatus = "§c(nicht bereit)";
                                if (ArenaManager.getArenaIconItem(arenas) == null) {
                                    arenaStatus = arenaStatus + " §4-> Icon fehlt!";
                                }
                                if (ArenaManager.getLocation("arenas." + arenas + ".Spawn1") == null) {
                                    arenaStatus += " §4-> Spawnpunkt 1 fehlt!";
                                }
                                if (ArenaManager.getLocation("arenas." + arenas + ".Spawn2") == null) {
                                    arenaStatus += " §4-> Spawnpunkt 2 fehlt!";
                                }
                            }
                            player.sendMessage("§7" + arenasSize + ". " + arenas + " " + arenaStatus);
                            arenasSize++;
                        }
                        return true;
                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }
                case "leave":
                    if (player.hasPermission("1vs1.leave")) {
                        if (MainClass.getPlayersinFight().get(player) != null) {
                            if (getArenaPlayers(MainClass.getPlayersinFight().get(player)).size() == 1) {
                                player.sendMessage(MainClass.getPrefix() + "§cDu hast die Arena verlassen!");
                                MainClass.getPlayersinFight().remove(player);
                                return true;
                            } else {
                                player.sendMessage(MainClass.getPrefix() + "§cEs hat bereits ein Kampf begonnen!");
                                return true;
                            }
                        } else {
                            player.sendMessage(MainClass.getPrefix() + "§eDu befindest dich in keinem 1vs1!");
                            return true;
                        }
                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }

                case "setspawn":
                    if (player.hasPermission("1vs1.setspawn")) {
                        if (args.length < 3) {
                            player.sendMessage("§cÜberprüfe bitte deine Argumente!");
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("1")) {
                            ArenaManager.saveLocation("arenas." + args[2] + ".Spawn1", player.getLocation());
                            player.sendMessage(MainClass.getPrefix() + "§7Du hast den §61. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                            return true;
                        } else if (args[1].equalsIgnoreCase("2")) {
                            ArenaManager.saveLocation("arenas." + args[2] + ".Spawn2", player.getLocation());
                            player.sendMessage(MainClass.getPrefix() + "§7Du hast den §62. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                            return true;
                        } else {
                            player.sendMessage(MainClass.getPrefix() + "§cDu kannst nur den Spawn 1 und 2 setzen!");
                            return true;
                        }

                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }
                case "seticon":
                    if (player.hasPermission("1vs1.seticon")) {
                        if (args.length < 2) {
                            player.sendMessage("§cDu hast keinen Arenanamen angegeben!");
                            return true;
                        }
                        if (player.getItemInHand().getType() == Material.AIR || player.getItemInHand() == null) {
                            player.sendMessage(MainClass.getPrefix() + "§cDu hast kein §4Item §cin deiner §4Hand§c!");
                            return true;
                        }
                        ArenaManager.setArenaIconItem(args[1], player.getItemInHand().getType());
                        player.sendMessage(MainClass.getPrefix() + "§7Du hast das §6Icon§7 der Arena §6" + args[1] + " §7gesetzt!");
                        return true;
                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }
                case "setitems":
                    if (player.hasPermission("1vs1.setitems")) {
                        if (args.length < 2) {
                            player.sendMessage("§cDu hast keinen Arenanamen angegeben!");
                            return true;
                        }
                        if (player.getInventory().getContents().length == 0) {
                            player.sendMessage(MainClass.getPrefix() + "§cDu hast keine §4Items §cin deinem §4Inventar§c!");
                            return true;
                        }
                        for (int i = 0; player.getInventory().getContents().length < i; i++) {
                            if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                                MainClass.instance().getConfig().set("arenas." + args[1] + ".items", player.getInventory().getContents()[i]);
                            }
                        }
                        for (int i = 0; player.getInventory().getArmorContents().length < i; i++) {
                            if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                                MainClass.instance().getConfig().set("arenas." + args[1] + ".armor", player.getInventory().getArmorContents()[i]);
                            }
                        }
                        MainClass.instance().saveConfig();
                        player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich die Items in der Arena §6" + args[1] + " §7gesetzt!");
                        return true;
                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }
                case "setreward":
                    if (player.hasPermission("1vs1.setreward")) {
                        if (args.length < 2) {
                            player.sendMessage("§cBitte überprüfe deine Argumente!");
                            return true;
                        }
                        if (player.getInventory().getContents().length == 0) {
                            player.sendMessage(MainClass.getPrefix() + "§cDu hast keine §4Items §cin deinem §4Inventar§c!");
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("global")) {
                            for (int i = 0; player.getInventory().getContents().length < i; i++) {
                                if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                                    MainClass.instance().getConfig().set("globalRewards", player.getInventory().getContents()[i]);
                                }
                            }
                            MainClass.instance().saveConfig();
                            player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich den globalen Reward gesetzt!");
                            return true;
                        } else {
                            if (ArenaManager.exists(args[1])) {
                                for (int i = 0; player.getInventory().getContents().length < i; i++) {
                                    if (player.getInventory().getContents()[i].getType() != Material.AIR || player.getInventory().getContents()[i] != null) {
                                        MainClass.instance().getConfig().set("arenas" + args[1] + ".rewards", player.getInventory().getContents()[i]);
                                    }
                                }
                                MainClass.instance().saveConfig();
                                player.sendMessage(MainClass.getPrefix() + "§7Du hast erfolgreich den Reward in Arena §e" + args[1] + "§7gesetzt!");
                                return true;
                            } else {
                                player.sendMessage(MainClass.getPrefix() + "§7Die Arena §e" + args[1] + " §7existiert nicht!");
                                return true;
                            }
                        }


                    }
                case "select":
                    if (player.hasPermission("1vs1.select")) {
                        if (ArenaManager.getArenas() != null) {
                            for (String arenas : ArenaManager.getArenas()) {
                                if (ArenaManager.getArenaIconItem(arenas) != null && ArenaManager.getLocation("arenas." + arenas + ".Spawn1") != null && ArenaManager.getLocation("arenas." + arenas + ".Spawn2") != null) {
                                    ArenaManager.setArenaReady(arenas, true);
                                } else {
                                    ArenaManager.setArenaReady(arenas, false);
                                }
                            }
                        }

                        if (ArenaManager.getArenas() == null) {
                            player.sendMessage(MainClass.getPrefix() + "§cKeine Arenen vorhanden =(!");
                            return true;
                        }

                        if (!MainClass.getPlayersinFight().containsKey(player)) {
                            if (isInventoryEmpty(player) && player.getInventory().getHelmet() == null && player.getInventory().getChestplate() == null && player.getInventory().getLeggings() == null && player.getInventory().getBoots() == null) {
                                openMenuAt(player);
                                return true;
                            } else {
                                player.sendMessage(MainClass.getPrefix() + "§eDu musst erst dein Inventar leeren!");
                                return true;
                            }
                        } else {
                            player.sendMessage(MainClass.getPrefix() + " §eDu bist bereits in einem Kampf!");
                            return true;
                        }
                    } else {
                        player.sendMessage(noperm);
                        return true;
                    }
                default:
                    sender.sendMessage("§cUnbekannte Aktion.");
            }
        }
        return true;
    }

    boolean isInventoryEmpty(Player player) {
        return Arrays.asList(player.getInventory().getContents()).stream()
                .filter(is -> is != null && !is.getType().equals(Material.AIR))
                .findAny().isPresent();
    }
}
