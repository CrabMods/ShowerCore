package mod.crabmod.showercore.entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Each shower releases only its own claim when a player leaves or it stops. */
final class ActiveShowerPlayers<S> {
    private final Map<UUID, Set<S>> sources = new ConcurrentHashMap<>();

    void add(UUID player, S shower) {
        sources.compute(player, (id, owners) -> {
            if (owners == null) owners = new HashSet<>();
            owners.add(shower);
            return owners;
        });
    }

    void remove(UUID player, S shower) {
        sources.computeIfPresent(player, (id, owners) -> {
            owners.remove(shower);
            return owners.isEmpty() ? null : owners;
        });
    }

    void remove(UUID player) {
        sources.remove(player);
    }

    boolean contains(UUID player) {
        return sources.containsKey(player);
    }
}
