package mod.crabmod.showercore.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class ShowerHeadContainerTrackingRegressionTest {
    @Test void lastShowerLeavingClearsThePlayer() {
        var players = new ActiveShowerPlayers<Object>();
        var player = UUID.randomUUID(); var shower = new Object();
        assertFalse(players.contains(player)); players.add(player, shower);
        assertTrue(players.contains(player)); players.remove(player, shower);
        assertFalse(players.contains(player));
    }

    @Test void repeatedTicksDoNotRequireRepeatedRemoval() {
        var players = new ActiveShowerPlayers<Object>();
        var player = UUID.randomUUID(); var shower = new Object();
        for (int i = 0; i < 200; i++) players.add(player, shower);
        players.remove(player, shower); assertFalse(players.contains(player));
    }

    @Test void stoppingOneShowerPreservesAnotherActiveShower() {
        var players = new ActiveShowerPlayers<Object>();
        var player = UUID.randomUUID(); var first = new Object(); var second = new Object();
        players.add(player, first); players.add(player, second);
        players.remove(player, first); assertTrue(players.contains(player));
        players.remove(player, second); assertFalse(players.contains(player));
    }

    @Test void unknownOrRepeatedRemovalDoesNotClearAnotherShower() {
        var players = new ActiveShowerPlayers<Object>();
        var player = UUID.randomUUID(); var first = new Object(); var second = new Object();
        players.add(player, first); players.remove(player, second); players.remove(player, second);
        assertTrue(players.contains(player));
    }

    @Test void logoutClearsAllClaimsAndAllowsARealReentry() {
        var players = new ActiveShowerPlayers<Object>();
        var player = UUID.randomUUID(); var first = new Object(); var second = new Object();
        players.add(player, first); players.add(player, second); players.remove(player);
        assertFalse(players.contains(player)); players.add(player, second);
        players.remove(player, first); assertTrue(players.contains(player));
    }

    @Test void playersAreIndependent() {
        var players = new ActiveShowerPlayers<Object>();
        var first = UUID.randomUUID(); var second = UUID.randomUUID(); var shower = new Object();
        players.add(first, shower); players.add(second, shower); players.remove(first);
        assertFalse(players.contains(first)); assertTrue(players.contains(second));
    }

    @Test void concurrentClaimsSurviveAnotherOwnerFinishing() throws Exception {
        var players = new ActiveShowerPlayers<Object>();
        var player = UUID.randomUUID(); var retained = new Object();
        players.add(player, retained);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 2; i++) jobs.add(pool.submit(() -> {
                try { start.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                var owner = new Object();
                for (int n = 0; n < 1000; n++) { players.add(player, owner); players.remove(player, owner); }
            }));
            start.countDown(); for (var job : jobs) job.get();
            assertTrue(players.contains(player)); players.remove(player, retained);
            assertFalse(players.contains(player));
        } finally { pool.shutdownNow(); }
    }
}
