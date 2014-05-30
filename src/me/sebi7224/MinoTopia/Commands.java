package me.sebi7224.MinoTopia;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commands implements CommandExecutor {

    public static HashMap<Player, Location> waiting_players = new HashMap<Player, Location>();
    private static IconMenu arena_menu;

    public static void registerArenaMenu() {
        int ready_arenas = ArenaManager.ready_arenas();
        int multiple = 0;
        do multiple++;
        while (9 * multiple < ready_arenas);
        int slots = 9 * multiple;
        if (slots > 54) {
            Bukkit.getConsoleSender().sendMessage("§4WARNUNG: Es existieren mehr als §454 §cMaps!");
            slots = 54;
        }
        if (slots < 9) slots = 9;

        arena_menu = new IconMenu("§8Wähle eine Arena!", slots, new IconMenu.OptionClickEventHandler() {
            @SuppressWarnings({"deprecation", "rawtypes"})
            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                final Player player = event.getPlayer();
                player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
                String name = event.getName();
                name = name.replace("§6", "");
                int players = getArenaPlayer(name).size();
                if (players == 2) {

                    player.sendMessage(Plugin.prefix + "§eDiese Arena ist bereits voll!");

                } else {
                    if (players == 1) {

                        for (final Map.Entry entry : Plugin.ingame.entrySet()) {
                            if (entry.getValue().equals(name)) {

                                final Player player1 = ((Player) entry.getKey());

                                Plugin.ingame.put(player, name);

                                waiting_players.put(player, player.getLocation());
                                waiting_players.put(player1, player1.getLocation());

                                player.sendMessage(Plugin.prefix + "§eIn 3 Sekunden wirst du in die Arena teleportiert...");
                                player1.sendMessage(Plugin.prefix + "§eEs wurde ein Spieler gefunden in 3 Sekunden wirst du teleportiert...");

                                final String finalName = name;
                                final Location location_player = waiting_players.get(player);
                                final Location location_player1 = waiting_players.get(player1);

                                new BukkitRunnable() {
                                    public void run() {
                                        if (waiting_players.containsKey(player) && waiting_players.containsKey(player1)) {
                                            if (location_player.getX() == player.getLocation().getX() && location_player.getY() == player.getLocation().getY() && location_player.getZ() == player.getLocation().getZ()) {
                                                if (location_player1.getX() == player1.getLocation().getX() && location_player1.getY() == player1.getLocation().getY() && location_player1.getZ() == player1.getLocation().getZ()) {

                                                    player.teleport(ArenaManager.getLocation("arenas." + finalName + ".Spawn1"));
                                                    player1.teleport(ArenaManager.getLocation("arenas." + finalName + ".Spawn2"));
                                                    player.sendMessage(Plugin.prefix + "§eDu hast die Arena §4" + finalName + " §ebetreten!");
                                                    player.sendMessage(Plugin.prefix + "§eMögen die Spiele beginnen!");

                                                    player1.sendMessage(Plugin.prefix + "§eDer Spieler §6" + player.getName() + "§e hat die Arena betreten!");
                                                    player1.sendMessage(Plugin.prefix + "§eMögen die Spiele beginnen!");

                                                    for (Player player2 : Commands.getArenaPlayer(finalName)) {

                                                        Plugin.lastlocation.put(player2, player2.getLocation());
                                                        Plugin.savedxp.put(player2, player2.getExp());

                                                        player2.setFireTicks(0);
                                                        player2.setHealth(20);
                                                        player2.getActivePotionEffects().clear();
                                                        player2.setFoodLevel(20);
                                                        player2.getInventory().clear();

                                                        player2.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                                                        player2.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                                                        player2.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                                                        player2.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                                                        player2.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
                                                        player2.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                                                        player2.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 10));

                                                        player2.setGameMode(GameMode.SURVIVAL);
                                                        player2.updateInventory();
                                                        player2.closeInventory();
                                                    }
                                                } else {
                                                    Plugin.ingame.remove(player);
                                                    Plugin.ingame.remove(player1);
                                                    waiting_players.remove(player);
                                                    waiting_players.remove(player1);
                                                    player1.sendMessage("§cTeleportierung abgebrochen du hast dich bewegt!");
                                                    player.sendMessage("§cTeleportierung abgebrochen §e" + player1.getName() + " §chat sich bewegt!");
                                                }
                                            } else {
                                                Plugin.ingame.remove(player);
                                                Plugin.ingame.remove(player1);
                                                waiting_players.remove(player);
                                                waiting_players.remove(player1);
                                                player.sendMessage("§cTeleportierung abgebrochen du hast dich bewegt!");
                                                player1.sendMessage("§cTeleportierung abgebrochen §e" + player.getName() + " §chat sich bewegt!");
                                            }
                                        }
                                    }
                                }.runTaskLaterAsynchronously(Plugin.plugin, 60L);

                            }
                        }
                    } else {
                        Plugin.ingame.put(player, name);
                        player.sendMessage(Plugin.prefix + "§eDu hast die Arena §4" + name + " §ebetreten!");
                        player.sendMessage(Plugin.prefix + "§eDu wirst teleportiert sobald ein zweiter Spieler beigetreten ist!");
                    }
                }
            }
        }, Plugin.plugin);
    }

    private static void openMenuAt(Player player) {
        int slot = 0;
        for (String arenas : ArenaManager.getReadyArenas()) {
            if (slot > 53) return;
            String lore = "§7Klicke um in die Arena§6 " + arenas + " §7zu Joinen!";
            if (getArenaPlayer(arenas).size() == 1) {
                lore = lore + "§c" + getArenaPlayer(arenas).get(0).getName() + " §avs. " + "§a<Unbekannt>";
            }
            if (getArenaPlayer(arenas).size() == 2) {
                lore = lore + "§c" + getArenaPlayer(arenas).get(0).getName() + " §avs. " + "§c" + getArenaPlayer(arenas).get(1).getName();
            }
            arena_menu.setOption(slot, new ItemStack(ArenaManager.getItem(arenas)), "§6" + arenas, lore);
            slot++;
        }
        arena_menu.open(player);
    }

    @SuppressWarnings("rawtypes")
    public static ArrayList<Player> getArenaPlayer(String arena) {
        ArrayList<Player> player_in_arena = new ArrayList<Player>();
        for (Map.Entry e : Plugin.ingame.entrySet()) {
            if (e.getValue().equals(arena)) {
                player_in_arena.add((Player) e.getKey());
            }
        }
        return player_in_arena;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<String>();
        if (command.getName().equalsIgnoreCase("1vs")) {
            if (args.length == 0) {
                result.add("select");
                result.add("?");
                result.add("help");
                result.add("create");
                result.add("remove");
                result.add("setspawn");
                result.add("seticon");
                result.add("list");
                result.add("leave");
            }

            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("seticon")) {
                for (String arenas : ArenaManager.getArenas()) {
                    result.add(arenas);
                }
            }
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("1vs1")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                if (args.length > 1) {
                    if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                        sender.sendMessage(Plugin.prefix + "§cZu viele Argumente!");
                        sender.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 [help;?]");
                        return true;
                    }
                }
                sender.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
                sender.sendMessage("§6/1vs1 select §c- §bÖffnet das Arenamenü");
                sender.sendMessage("§6/1vs1 leave §c- §bVerlässt ein Spiel, wenn du alleine in einer Arena bist");
                if (sender.hasPermission("1vs1.admin")) {
                    sender.sendMessage("§6/1vs1 create <arena> §c- §bErstellt eine neue Arena");
                    sender.sendMessage("§6/1vs1 remove <arena> §c- §bLöscht eine Arena");
                    sender.sendMessage("§6/1vs1 setspawn <1|2> <arena> §c- §bSetzt ein Spawn");
                    sender.sendMessage("§6/1vs1 seticon <arena> §c- §bSetzt das Icon in deiner Hand als ArenaIcon");
                    sender.sendMessage("§6/1vs1 list §c- §bZeigt die Arenen");
                }
                sender.sendMessage("§b=================================");
                return true;
            }
            if (!(args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")
                    || args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("create")
                    || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("remove")
                    || args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("seticon")
                    || args[0].equalsIgnoreCase("leave"))) {
                sender.sendMessage(Plugin.prefix + "§cUnbekanntes Argument!");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("[1vs1] Du musst ein Spieler sein um diesen Befehl auszufuehren!");
                return true;
            }
            Player player = (Player) sender;
            if (sender.hasPermission("1vs1.admin")) {
                if (args[0].equalsIgnoreCase("setspawn")) {
                    if (args.length > 3) {
                        player.sendMessage(Plugin.prefix + "§cZu viele Argumente!");
                        player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 " + args[0] + " <1|2> <Name>");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(Plugin.prefix + "§cZu wenig Argumente!");
                        player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 " + args[0] + " <1|2> <Name>");
                        return true;
                    }

                    if (!(ArenaManager.Arena_exists(args[2]))) {
                        player.sendMessage(Plugin.prefix + "§cDie Arena §4" + args[2] + " §cexistiert nicht!");
                        return true;
                    }
                }

                if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("seticon")) {
                    if (args.length > 2) {
                        player.sendMessage(Plugin.prefix + "§cZu viele Argumente!");
                        player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 " + args[0] + " <Name>");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(Plugin.prefix + "§cZu wenig Argumente!");
                        player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 " + args[0] + " <Name>");
                        return true;
                    }

                    if (!(args[0].equalsIgnoreCase("create"))) {
                        if (!(ArenaManager.Arena_exists(args[1]))) {
                            player.sendMessage(Plugin.prefix + "§cDie Arena §4" + args[1] + " §cexistiert nicht!");
                            return true;
                        }
                    }
                }

                if (args[0].equalsIgnoreCase("create")) {
                    if (ArenaManager.Arena_exists(args[1])) {
                        player.sendMessage(Plugin.prefix + "§cDie Arena §4" + args[1] + " §cexistiert bereits!");
                        return true;
                    }
                    ArenaManager.createArena(args[1]);
                    player.sendMessage(Plugin.prefix + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich erstellt!");
                    return true;
                }

                if (args[0].equalsIgnoreCase("remove")) {
                    ArenaManager.removeArena(args[1]);
                    player.sendMessage(Plugin.prefix + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich entfernt!");
                    return true;
                }

                if (args[0].equalsIgnoreCase("list")) {
                    if (args.length > 1) {
                        player.sendMessage(Plugin.prefix + "§cZu viele Argumente!");
                        player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 list");
                        return true;
                    }

                    if (ArenaManager.getArenas() == null) {
                        player.sendMessage(Plugin.prefix + "§cEs existiert keine Arena!");
                        return true;
                    }

                    player.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
                    String arena_status;
                    int id = 1;
                    for (String arenas : ArenaManager.getArenas()) {
                        if (ArenaManager.isArenaReady(arenas)) arena_status = "§a(bereit)";
                        else {
                            arena_status = "§c(nicht bereit)";
                            if (ArenaManager.getItem(arenas) == null) arena_status = arena_status + " §4-> Icon fehlt!";
                            if (ArenaManager.getLocation("arenas." + arenas + ".Spawn1") == null)
                                arena_status = arena_status + " §4-> Spawnpunkt 1 fehlt!";
                            if (ArenaManager.getLocation("arenas." + arenas + ".Spawn2") == null)
                                arena_status = arena_status + " §4-> Spawnpunkt 2 fehlt!";
                        }
                        player.sendMessage("§7" + id + ". " + arenas + " " + arena_status);
                        id++;
                    }
                    return true;
                }

                if (args[0].equalsIgnoreCase("leave")) {
                    if (args.length > 1) {
                        player.sendMessage(Plugin.prefix + "§cZu viele Argumente!");
                        player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 leave");
                        return true;
                    }
                    if (Plugin.ingame.get(player) != null) {
                        if (getArenaPlayer(Plugin.ingame.get(player)).size() == 1) {
                            player.sendMessage(Plugin.prefix + "§cDu hast die Arena verlassen!");
                            Plugin.ingame.remove(player);
                            return true;
                        } else {
                            player.sendMessage(Plugin.prefix + "§cEs hat bereits ein Kampf begonnen!");
                            return true;
                        }
                    } else {
                        player.sendMessage(Plugin.prefix + "§eDu befindest dich in keinem 1vs1!");
                        return true;
                    }
                }

                if (args[0].equalsIgnoreCase("setspawn")) {
                    if (args[1].equalsIgnoreCase("1")) {
                        ArenaManager.saveLocation("arenas." + args[2] + ".Spawn1", player.getLocation());
                        player.sendMessage(Plugin.prefix + "§7Du hast den §61. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                        return true;
                    } else if (args[1].equalsIgnoreCase("2")) {
                        ArenaManager.saveLocation("arenas." + args[2] + ".Spawn2", player.getLocation());
                        player.sendMessage(Plugin.prefix + "§7Du hast den §62. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                        return true;
                    } else {
                        player.sendMessage(Plugin.prefix + "§cDu kannst nur den Spawn 1 und 2 setzen!");
                        return true;
                    }
                }

                if (args[0].equalsIgnoreCase("seticon")) {
                    if (player.getItemInHand().getType() == Material.AIR) {
                        player.sendMessage(Plugin.prefix + "§cDu hast kein §4Item §cin deiner §4Hand§c!");
                        return true;
                    }
                    ArenaManager.setItem(args[1], player.getItemInHand().getType());
                    player.sendMessage(Plugin.prefix + "§7Du hast das §6Icon§7 der Arena §6" + args[1] + " §7gesetzt!");
                    return true;
                }
            } else {
                player.sendMessage("§4Du hast keinen Zugriff auf diesen Befehl.");
                return true;
            }
            if (args[0].equalsIgnoreCase("select")) {
                if (ArenaManager.getArenas() != null) {
                    for (String arenas : ArenaManager.getArenas()) {
                        if (ArenaManager.getItem(arenas) != null && ArenaManager.getLocation("arenas." + arenas + ".Spawn1") != null && ArenaManager.getLocation("arenas." + arenas + ".Spawn2") != null) {
                            ArenaManager.setArenaReady(arenas, true);
                        } else {
                            ArenaManager.setArenaReady(arenas, false);
                        }
                    }
                }
                if (args.length > 1) {
                    player.sendMessage(Plugin.prefix + "§cZu viele Argumente!");
                    player.sendMessage(Plugin.prefix + "§cBenutzung: /1vs1 select");
                    return true;
                }

                if (ArenaManager.getArenas() == null) {
                    player.sendMessage(Plugin.prefix + "§cEs existiert keine Arena!");
                    return true;
                }

                if (!Plugin.ingame.containsKey(player)) {
                    if (isEmpty(player) && player.getInventory().getHelmet() == null && player.getInventory().getChestplate() == null && player.getInventory().getLeggings() == null && player.getInventory().getBoots() == null) {
                        openMenuAt(player);
                        return true;
                    } else {
                        player.sendMessage(Plugin.prefix + "§eDu musst erst dein Inventar leeren!");
                        return true;
                    }
                } else {
                    player.sendMessage(Plugin.prefix + " §eDu bist bereits in einem Kampf!");
                    return true;
                }
            }


        }
        return true;
    }

    boolean isEmpty(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null)
                return false;
        }
        return true;
    }
}
