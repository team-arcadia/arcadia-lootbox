package com.arcadia.lootbox.manager;

import com.arcadia.lootbox.config.LootboxConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks lootbox opening history per player. Thread-safe and bounded.
 *
 * @author vyrriox
 */
public final class HistoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");
    private static final Map<UUID, Deque<HistoryEntry>> HISTORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Long>> CLICK_TIMESTAMPS = new ConcurrentHashMap<>();

    private HistoryManager() {}

    public record HistoryEntry(String lootboxId, String lootboxName, List<String> itemsReceived, long timestamp) {
        public String formattedTime() { return Instant.ofEpochMilli(timestamp).toString(); }
    }

    public static void record(UUID uuid, String lootboxId, String lootboxName, List<String> items) {
        int max = LootboxConfig.HISTORY_MAX_ENTRIES.get();
        if (max <= 0) return;
        // Defensive copy — the caller's list is reused/mutated after recording.
        List<String> itemsCopy = items == null ? List.of() : new ArrayList<>(items);
        Deque<HistoryEntry> entries = HISTORY.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (entries) {
            entries.addFirst(new HistoryEntry(lootboxId, lootboxName, itemsCopy, System.currentTimeMillis()));
            while (entries.size() > max) entries.removeLast();
        }
    }

    public static List<HistoryEntry> getHistory(UUID uuid) {
        Deque<HistoryEntry> entries = HISTORY.get(uuid);
        if (entries == null) return List.of();
        synchronized (entries) { return List.copyOf(entries); }
    }

    public static void clearHistory(UUID uuid) { HISTORY.remove(uuid); }

    /** Drops the per-player click-timestamp window. Call on logout — the 60s window
     *  is meaningless across a disconnect and would otherwise leak per player. */
    public static void clearAutoclicker(UUID uuid) { CLICK_TIMESTAMPS.remove(uuid); }

    public static void clearAll() { HISTORY.clear(); CLICK_TIMESTAMPS.clear(); }

    public static int getTotalOpenings(UUID uuid) {
        Deque<HistoryEntry> entries = HISTORY.get(uuid);
        if (entries == null) return 0;
        synchronized (entries) { return entries.size(); }
    }

    public static boolean checkAutoclicker(UUID uuid) {
        if (!LootboxConfig.ANTI_AUTOCLICKER.get()) return false;
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = CLICK_TIMESTAMPS.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            timestamps.addLast(now);
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - 60_000) timestamps.removeFirst();
            if (timestamps.size() > LootboxConfig.ANTI_AUTOCLICKER_THRESHOLD.get()) {
                LOGGER.warn("[ArcadiaLootbox] Anti-autoclicker: {} — {} opens/min", uuid, timestamps.size());
                return true;
            }
        }
        return false;
    }
}
