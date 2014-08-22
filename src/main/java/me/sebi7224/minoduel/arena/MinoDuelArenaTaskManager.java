package me.sebi7224.minoduel.arena;

import me.sebi7224.minoduel.MinoDuelPlugin;

import io.github.xxyy.common.util.XyValidate;
import io.github.xxyy.common.util.task.NonAsyncBukkitRunnable;

/**
 * Manages repeating tasks for {@link me.sebi7224.minoduel.arena.MinoDuelArena}, i.e. takes care of pre-game countdown and
 * in-game time messages as well as ending the game if time has run out.
 */
class MinoDuelArenaTaskManager {
    private final MinoDuelArena arena;
    private RunnablePreGame waitTask;
    private RunnableInGame gameTask;

    public MinoDuelArenaTaskManager(MinoDuelArena arena) {
        this.arena = arena;
        this.waitTask = new RunnablePreGame();
        this.gameTask = new RunnableInGame();
    }

    public void start() {
        waitTask.start();
    }

    public void stop() {
        waitTask.stop();
        gameTask.stop();
    }

    private class RunnablePreGame extends NonAsyncBukkitRunnable {
        public static final int MAX_SECONDS_UNTIL_START = 3;
        private int secondsUntilStart = -1;

        public void start() {
            tryCancel();
            secondsUntilStart = MAX_SECONDS_UNTIL_START;

            runTaskTimer(arena.getArenaManager().getPlugin(), 1L, 20L);
        }

        @Override
        public void run() {
            XyValidate.validateState(secondsUntilStart != -1, "Cannot run while stopped!");

            if (secondsUntilStart == 0) {
                arena.getPlayers().forEach(MinoDuelArena.PlayerInfo::notifyGameStart);
                stop();
                gameTask.start();
            } else {
                arena.getPlayers().forEach(pi -> pi.notifyWaitTick(secondsUntilStart));
            }
            secondsUntilStart--;
        }

        public void stop() {
            secondsUntilStart = -1;
            tryCancel();
        }
    }

    private class RunnableInGame extends NonAsyncBukkitRunnable { //This task so far only announces time left and the smallest interval is 5 seconds
        public static final int TICKS_IN_A_GAME = 60; //A game can last up to 5 minutes
        public static final long MINECRAFT_TICKS_PER_TICK = 20L * 5L; //We tick every 5 seconds
        private int ticksLeft = 0;

        public void start() {
            ticksLeft = TICKS_IN_A_GAME;
            arena.setState(ArenaState.RUNNING);
            gameTask.runTaskTimer(arena.getArenaManager().getPlugin(), MINECRAFT_TICKS_PER_TICK, MINECRAFT_TICKS_PER_TICK);
        }

        @Override
        public void run() {
            XyValidate.validateState(ticksLeft != -1, "Cannot run while stopped!");

            ticksLeft--; //Ticks, as in 1vs1 ticks, not to be confused with game ticks

            //Announce full minutes
            if (ticksLeft % 12 == 0) { //every minute
                sendTimeLeft(ticksLeft / 12, "Minute");
            } else if (ticksLeft == 6 || ticksLeft < 4) { //30, 15, 10 & 5 seconds before end
                sendTimeLeft(ticksLeft * 5, "Sekunde");
            } else if (ticksLeft == 0) {
                arena.getArenaManager().getPlugin().getLogger().info("Arena " + arena.getName() + " w/ " + arena.getPlayerString() + " timed out.");
                arena.endGame(null);
            }
        }

        public void stop() {
            this.ticksLeft = -1;
            tryCancel();
        }

        private void sendTimeLeft(int amount, String unit) {
            arena.getPlayers().stream()
                    .forEach(pi -> pi.getPlayer().sendMessage(
                            MinoDuelPlugin.PREFIX + "§7Das Spiel endet in §e" + amount + " §7" +
                                    unit + (amount == 1 ? "" : "n") + "!"));
        }
    }
}
