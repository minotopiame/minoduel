package me.minotopia.minoduel;

import me.minotopia.minoduel.arena.Arena;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import li.l1t.common.misc.NullableOptional;
import li.l1t.common.test.util.MockHelper;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DuelRequestManagerTest {
    private static final Player PLAYER_1 = MockHelper.mockPlayer(UUID.randomUUID(), "plr1");
    private static final Player PLAYER_2 = MockHelper.mockPlayer(UUID.randomUUID(), "plr2");
    private static final Player PLAYER_3 = MockHelper.mockPlayer(UUID.randomUUID(), "plr3");
    private static final Player PLAYER_4 = MockHelper.mockPlayer(UUID.randomUUID(), "plr4");

    private DuelRequestManager duelRequestManager;

    @Before
    public void setUp() throws Exception {
        duelRequestManager = new DuelRequestManager();
    }

    @Test
    public void testHasPending() throws Exception {
        duelRequestManager.request(PLAYER_1, PLAYER_2, null);

        assertFalse("hasPending recognizes requests the wrong way!", duelRequestManager.hasPending(PLAYER_2, PLAYER_1));
        assertFalse("hasPending recognizes anything!", duelRequestManager.hasPending(PLAYER_3, PLAYER_4));
        assertFalse("hasPending recognizes anything if a player has any request!", duelRequestManager.hasPending(PLAYER_1, PLAYER_4));
        assertTrue("hasPending doesn't recognize requests or request doesn't work!", duelRequestManager.hasPending(PLAYER_1, PLAYER_2));
    }

    @Test
    public void testRemove_NotPresent() throws Exception {
        assertFalse("remove returns present Optional for not-requested duel!", duelRequestManager.remove(PLAYER_2, PLAYER_4).isPresent());
    }

    @Test
    public void testRemove_NonNullArena() throws Exception {
        Arena testArena = mock(Arena.class);
        duelRequestManager.request(PLAYER_1, PLAYER_2, testArena);
        NullableOptional<Arena> return1 = duelRequestManager.remove(PLAYER_1, PLAYER_2);
        assertTrue("remove returns an Optional which is not present for removed value!", return1.isPresent());
        assertEquals("remove returns wrong arena object!", testArena, return1.get());
        assertFalse("remove doesn't actually remove", duelRequestManager.hasPending(PLAYER_1, PLAYER_2));
    }

    @Test
    public void testRemove_NullArena() throws Exception {
        duelRequestManager.request(PLAYER_1, PLAYER_2, null);
        NullableOptional<Arena> return1 = duelRequestManager.remove(PLAYER_1, PLAYER_2);
        assertTrue("remove returns an Optional which is not present for removed value!", return1.isPresent());
        assertNull("remove returns non-null Arena when null was set!", return1.get());
        assertFalse("remove doesn't actually remove", duelRequestManager.hasPending(PLAYER_1, PLAYER_2));
    }

    @Test
    public void testRemoveAll() throws Exception {
        duelRequestManager.request(PLAYER_1, PLAYER_4, null);
        duelRequestManager.request(PLAYER_3, PLAYER_1, null);
        duelRequestManager.request(PLAYER_3, PLAYER_2, null);
        duelRequestManager.request(PLAYER_4, PLAYER_3, null);

        duelRequestManager.removeAll(PLAYER_3);

        assertFalse("removeAll doesn't remove all (1)", duelRequestManager.hasPending(PLAYER_3, PLAYER_1));
        assertFalse("removeAll doesn't remove all (2)", duelRequestManager.hasPending(PLAYER_3, PLAYER_2));
        assertFalse("removeAll doesn't remove all (3)", duelRequestManager.hasPending(PLAYER_4, PLAYER_3));
        assertTrue("removeAll removes *everything*", duelRequestManager.hasPending(PLAYER_1, PLAYER_4));
    }
}
