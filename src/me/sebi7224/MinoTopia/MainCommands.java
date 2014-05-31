package me.sebi7224.MinoTopia;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainCommands implements CommandExecutor {

    public static HashMap<Player, Location> waitingPlayers = new HashMap<>();
    private static IconMenu arenaMenu;

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
            @SuppressWarnings({"deprecation", "rawtypes"})
            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                final Player player = event.getPlayer();
                player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
                String name = event.getName();
                name = name.replace("§6", "");
                System.out.println(name);
                System.out.println(getArenaPlayers(name).size());
                if (getArenaPlayers(name).size() == 2) {

                    player.sendMessage(MainClass.prefix + "§eDiese Arena ist bereits voll!");

                } else if (getArenaPlayers(name).size() == 1) {

                    for (final Map.Entry entry : MainClass.PlayersinFight.entrySet()) {
                        if (entry.getValue().equals(name)) {

                            final Player player1 = ((Player) entry.getKey());

                            MainClass.PlayersinFight.put(player, name);

                            waitingPlayers.put(player, player.getLocation());
                            waitingPlayers.put(player1, player1.getLocation());

                            player.sendMessage(MainClass.prefix + "§eIn 3 Sekunden wirst du in die Arena teleportiert...");
                            player1.sendMessage(MainClass.prefix + "§eEs wurde ein Spieler gefunden in 3 Sekunden wirst du teleportiert...");

                            final String finalName = name;
                            final Location locationPlayer = waitingPlayers.get(player);
                            final Location locationPlayer1 = waitingPlayers.get(player1);

                            new BukkitRunnable() {
                                public void run() {
                                    if (waitingPlayers.containsKey(player) && waitingPlayers.containsKey(player1)) {
                                        if (locationPlayer.getX() == player.getLocation().getX() && locationPlayer.getY() == player.getLocation().getY() && locationPlayer.getZ() == player.getLocation().getZ()) {
                                            if (locationPlayer1.getX() == player1.getLocation().getX() && locationPlayer1.getY() == player1.getLocation().getY() && locationPlayer1.getZ() == player1.getLocation().getZ()) {


                                                player.sendMessage(MainClass.prefix + "§eDu hast die Arena §4" + finalName + " §ebetreten!");
                                                player.sendMessage(MainClass.prefix + "§eMögen die Spiele beginnen!");

                                                player1.sendMessage(MainClass.prefix + "§eDer Spieler §6" + player.getName() + "§e hat die Arena betreten!");
                                                player1.sendMessage(MainClass.prefix + "§eMögen die Spiele beginnen!");

                                                for (Player player2 : MainCommands.getArenaPlayers(finalName)) {

                                                    MainClass.PlayerslastLocation.put(player2, player2.getLocation());
                                                    MainClass.PlayerssavedEXP.put(player2, player2.getExp());

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
                                                player.teleport(ArenaManager.getLocation("arenas." + finalName + ".Spawn1"));
                                                player1.teleport(ArenaManager.getLocation("arenas." + finalName + ".Spawn2"));
                                            } else {
                                                MainClass.PlayersinFight.remove(player);
                                                MainClass.PlayersinFight.remove(player1);
                                                waitingPlayers.remove(player);
                                                waitingPlayers.remove(player1);
                                                player1.sendMessage("§cTeleportierung abgebrochen du hast dich bewegt!");
                                                player.sendMessage("§cTeleportierung abgebrochen §e" + player1.getName() + " §chat sich bewegt!");
                                            }
                                        } else {
                                            MainClass.PlayersinFight.remove(player);
                                            MainClass.PlayersinFight.remove(player1);
                                            waitingPlayers.remove(player);
                                            waitingPlayers.remove(player1);
                                            player.sendMessage("§cTeleportierung abgebrochen du hast dich bewegt!");
                                            player1.sendMessage("§cTeleportierung abgebrochen §e" + player.getName() + " §chat sich bewegt!");
                                        }
                                    }
                                }
                            }.runTaskLaterAsynchronously(MainClass.plugin, 60L);

                        }
                    }
                } else {
                    MainClass.PlayersinFight.put(player, name);
                    player.sendMessage(MainClass.prefix + "§eDu hast die Arena §4" + name + " §ebetreten!!ASDSDSXCNJSBN!");
                    player.sendMessage(MainClass.prefix + "§eDu wirst teleportiert sobald ein zweiter Spieler beigetreten ist!");
                }
            }
        }, MainClass.plugin);
    }

    private static void openMenuAt(Player player) {
        int slot = 0;
        for (String arenas : ArenaManager.getReadyArenas()) {
            if (slot > 53) {
                return;
            }
            String lore = "§7Klicke um in die Arena§6 " + arenas + " §7zu Joinen!";
            if (getArenaPlayers(arenas).size() == 1) {
                lore = lore + "§c" + getArenaPlayers(arenas).get(0).getName() + " §avs. " + "§a<Unbekannt>";
            }
            if (getArenaPlayers(arenas).size() == 2) {
                lore = lore + "§c" + getArenaPlayers(arenas).get(0).getName() + " §avs. " + "§c" + getArenaPlayers(arenas).get(1).getName();
            }
            arenaMenu.setOption(slot, new ItemStack(ArenaManager.getArenaItem(arenas)), "§6" + arenas, lore);
            slot++;
        }
        arenaMenu.open(player);
    }

    public static ArrayList<Player> getArenaPlayers(String arena) {
        ArrayList<Player> ArenaPlayers = new ArrayList<>();
        for (Map.Entry e : MainClass.PlayersinFight.entrySet()) {
            if (e.getValue().equals(arena)) {
                ArenaPlayers.add((Player) e.getKey());
            }
        }
        return ArenaPlayers;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 0) {
            result.add("select"); //!Arrays.asList(new String[]{...});
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
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (args.length > 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(MainClass.prefix + "§cZu viele Argumente!");
                    sender.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 [help]");
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
                || args[0].equalsIgnoreCase("leave"))) { //!Use a switch statement (RECOMMENDED!!!!!) or listOfValidCommands.contains(args[0].toLowerCase());
            sender.sendMessage(MainClass.prefix + "§cUnbekanntes Argument!");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("[1vs1] Du musst ein Spieler sein um diesen Befehl auszufuehren!");
            return true;
        }
        Player player = (Player) sender; //!The following logic is not at all good practice and should be replace by a switch and common methods
        if (sender.hasPermission("1vs1.admin")) {  //!Unnecessary permission check - start with checking what command the player wants
            if (args[0].equalsIgnoreCase("setspawn")) { //!Use a switch statement
                if (args.length > 3) {
                    player.sendMessage(MainClass.prefix + "§cZu viele Argumente!");
                    player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 " + args[0] + " <1|2> <Name>");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(MainClass.prefix + "§cZu wenig Argumente!");
                    player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 " + args[0] + " <1|2> <Name>");
                    return true;
                }

                if (!(ArenaManager.Arenaexists(args[2]))) {
                    player.sendMessage(MainClass.prefix + "§cDie Arena §4" + args[2] + " §cexistiert nicht!");
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("seticon")) {
                if (args.length > 2) { //!Unnecessary execution stop #ux !Spaghetti code
                    player.sendMessage(MainClass.prefix + "§cZu viele Argumente!");
                    player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 " + args[0] + " <Name>");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(MainClass.prefix + "§cZu wenig Argumente!");
                    player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 " + args[0] + " <Name>");
                    return true;
                }

                if (!(args[0].equalsIgnoreCase("create"))) {
                    if (!(ArenaManager.Arenaexists(args[1]))) {
                        player.sendMessage(MainClass.prefix + "§cDie Arena §4" + args[1] + " §cexistiert nicht!");
                        return true;
                    }
                }
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (ArenaManager.Arenaexists(args[1])) {
                    player.sendMessage(MainClass.prefix + "§cDie Arena §4" + args[1] + " §cexistiert bereits!");
                    return true;
                }
                ArenaManager.createArena(args[1]);
                player.sendMessage(MainClass.prefix + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich erstellt!");
                return true;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                if (ArenaManager.Arenaexists(args[1])) {
                    player.sendMessage(MainClass.prefix + "§cDie Arena §4" + args[1] + " §cexistiert bereits!");
                    return true;
                }
                ArenaManager.removeArena(args[1]);
                player.sendMessage(MainClass.prefix + "§7Du hast die Arena §6" + args[1] + " §7erfolgreich entfernt!");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                if (args.length > 1) { //!Unnecessary execution stop !Spaghetti code
                    player.sendMessage(MainClass.prefix + "§cZu viele Argumente!");
                    player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 list");
                    return true;
                }

                if (ArenaManager.getArenas() == null) {
                    player.sendMessage(MainClass.prefix + "§cEs existiert keine Arena!");
                    return true;
                }

                player.sendMessage("§b========> §6MinoTopia §c| §61vs1 §b<========");
                String arena_status;
                int id = 1;
                for (String arenas : ArenaManager.getArenas()) {
                    if (ArenaManager.isArenaReady(arenas)) {
                        arena_status = "§a(bereit)";
                    } else {
                        arena_status = "§c(nicht bereit)";
                        if (ArenaManager.getArenaItem(arenas) == null) {
                            arena_status = arena_status + " §4-> Icon fehlt!";
                        }
                        if (ArenaManager.getLocation("arenas." + arenas + ".Spawn1") == null) {
                            arena_status += " §4-> Spawnpunkt 1 fehlt!";
                        }
                        if (ArenaManager.getLocation("arenas." + arenas + ".Spawn2") == null) {
                            arena_status += " §4-> Spawnpunkt 2 fehlt!";
                        }
                    }
                    player.sendMessage("§7" + id + ". " + arenas + " " + arena_status);
                    id++;
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("leave")) {
                if (args.length > 1) {
                    player.sendMessage(MainClass.prefix + "§cZu viele Argumente!");
                    player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 leave");
                    return true;
                }
                if (MainClass.PlayersinFight.get(player) != null) {
                    if (getArenaPlayers(MainClass.PlayersinFight.get(player)).size() == 1) {
                        player.sendMessage(MainClass.prefix + "§cDu hast die Arena verlassen!");
                        MainClass.PlayersinFight.remove(player);
                        return true;
                    } else {
                        player.sendMessage(MainClass.prefix + "§cEs hat bereits ein Kampf begonnen!");
                        return true;
                    }
                } else {
                    player.sendMessage(MainClass.prefix + "§eDu befindest dich in keinem 1vs1!");
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("setspawn")) {
                if (args[1].equalsIgnoreCase("1")) {
                    ArenaManager.saveLocation("arenas." + args[2] + ".Spawn1", player.getLocation());
                    player.sendMessage(MainClass.prefix + "§7Du hast den §61. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                    return true;
                } else if (args[1].equalsIgnoreCase("2")) {
                    ArenaManager.saveLocation("arenas." + args[2] + ".Spawn2", player.getLocation());
                    player.sendMessage(MainClass.prefix + "§7Du hast den §62. Spawn §7der Arena §6" + args[2] + " §7erfolgreich gesetzt!");
                    return true;
                } else {
                    player.sendMessage(MainClass.prefix + "§cDu kannst nur den Spawn 1 und 2 setzen!");
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("seticon")) {
                if (player.getItemInHand().getType() == Material.AIR) {
                    player.sendMessage(MainClass.prefix + "§cDu hast kein §4Item §cin deiner §4Hand§c!");
                    return true;
                }
                ArenaManager.setArenaItem(args[1], player.getItemInHand().getType());
                player.sendMessage(MainClass.prefix + "§7Du hast das §6Icon§7 der Arena §6" + args[1] + " §7gesetzt!");
                return true;
            }
        } else {
            player.sendMessage("§4Du hast keinen Zugriff auf diesen Befehl.");
            return true;
        }
        if (args[0].equalsIgnoreCase("select")) {
            if (ArenaManager.getArenas() != null) {
                for (String arenas : ArenaManager.getArenas()) {
                    if (ArenaManager.getArenaItem(arenas) != null && ArenaManager.getLocation("arenas." + arenas + ".Spawn1") != null && ArenaManager.getLocation("arenas." + arenas + ".Spawn2") != null) {
                        ArenaManager.setArenaReady(arenas, true);
                    } else {
                        ArenaManager.setArenaReady(arenas, false);
                    }
                }
            }
            if (args.length > 1) {
                player.sendMessage(MainClass.prefix + "§cZu viele Argumente!");
                player.sendMessage(MainClass.prefix + "§cBenutzung: /1vs1 select");
                return true;
            }

            if (ArenaManager.getArenas() == null) {
                player.sendMessage(MainClass.prefix + "§cEs existiert keine Arena!");
                return true;
            }

            if (!MainClass.PlayersinFight.containsKey(player)) {
                if (isEmpty(player) && player.getInventory().getHelmet() == null && player.getInventory().getChestplate() == null && player.getInventory().getLeggings() == null && player.getInventory().getBoots() == null) {
                    openMenuAt(player);
                    return true;
                } else {
                    player.sendMessage(MainClass.prefix + "§eDu musst erst dein Inventar leeren!");
                    return true;
                }
            } else {
                player.sendMessage(MainClass.prefix + " §eDu bist bereits in einem Kampf!");
                return true;
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
