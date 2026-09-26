package io.josemmo.bukkit.plugin.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player limit of placed images
 */
public class PlacementLimit {
    public static final int UNLIMITED = -1;
    public static final String BYPASS_PERMISSION = "yamipa.limit.bypass";
    public static final String PERMISSION_PREFIX = "yamipa.limit.";
    public static final String META_KEY = "yamipa-images-per-player";
    private final Object lock = new Object();
    private final Map<UUID, Integer> counts = new HashMap<>();

    /**
     * Get image limit for a player
     * <p>
     * {@code yamipa.limit.bypass} means no limit. Otherwise the highest {@code yamipa.limit.<n>}
     * permission is used, then {@code yamipa-images-per-player}, then {@code global}.
     * A negative number means no limit.
     * @param  global       Default from config
     * @param  bypass       Has {@code yamipa.limit.bypass}
     * @param  metaRaw      Meta value or NULL
     * @param  grantedNodes Granted permission nodes
     * @return              Image limit, or {@link #UNLIMITED}
     */
    public static int resolve(
        int global,
        boolean bypass,
        @Nullable String metaRaw,
        @NotNull Iterable<String> grantedNodes
    ) {
        if (bypass) {
            return UNLIMITED;
        }

        Integer fromNodes = null;
        for (String node : grantedNodes) {
            Integer value = parsePermissionNode(node);
            if (value == null) continue;
            fromNodes = (fromNodes == null) ? value : Math.max(fromNodes, value);
        }
        if (fromNodes != null) {
            return fromNodes;
        }

        Integer meta = parseMeta(metaRaw);
        if (meta != null) {
            return meta < 0 ? UNLIMITED : meta;
        }
        return global < 0 ? UNLIMITED : global;
    }

    /**
     * Parse meta value
     * @param  raw Raw value
     * @return     Parsed integer or NULL
     */
    public static @Nullable Integer parseMeta(@Nullable String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a {@code yamipa.limit.<n>} permission
     * @param  node Permission node
     * @return      Limit, or NULL if the node does not match
     */
    public static @Nullable Integer parsePermissionNode(@Nullable String node) {
        if (node == null || node.length() <= PERMISSION_PREFIX.length()) return null;
        if (!node.regionMatches(true, 0, PERMISSION_PREFIX, 0, PERMISSION_PREFIX.length())) return null;
        String suffix = node.substring(PERMISSION_PREFIX.length());
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) return null;
        }
        try {
            return Integer.valueOf(suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Try to count one more placed image
     * @param  playerId Player UUID
     * @param  limit    Limit, or {@link #UNLIMITED}
     * @return          FALSE if the player is already at the limit
     */
    public boolean tryAcquire(@NotNull UUID playerId, int limit) {
        synchronized (lock) {
            int current = count(playerId);
            if (limit >= 0 && current >= limit) return false;
            counts.put(playerId, current + 1);
            return true;
        }
    }

    /**
     * Count one less placed image
     * @param playerId Player UUID
     */
    public void release(@NotNull UUID playerId) {
        synchronized (lock) {
            int current = count(playerId);
            if (current <= 1) {
                counts.remove(playerId);
            } else {
                counts.put(playerId, current - 1);
            }
        }
    }

    /**
     * Set placed image count for a player
     * @param playerId Player UUID
     * @param count    Number of placed images
     */
    public void set(@NotNull UUID playerId, int count) {
        synchronized (lock) {
            if (count <= 0) {
                counts.remove(playerId);
            } else {
                counts.put(playerId, count);
            }
        }
    }

    /**
     * Get placed image count for a player
     * @param  playerId Player UUID
     * @return          Number of placed images
     */
    public int get(@NotNull UUID playerId) {
        synchronized (lock) {
            return count(playerId);
        }
    }

    /**
     * Get placed image count by player
     * @return Map of player UUID to count
     */
    public @NotNull Map<UUID, Integer> snapshot() {
        synchronized (lock) {
            return new HashMap<>(counts);
        }
    }

    /**
     * Clear placed image counts
     */
    public void clear() {
        synchronized (lock) {
            counts.clear();
        }
    }

    private int count(@NotNull UUID playerId) {
        Integer current = counts.get(playerId);
        return current == null ? 0 : current;
    }
}
