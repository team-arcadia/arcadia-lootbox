package com.arcadia.lootbox.util;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.lib.player.CooldownManager;
import com.arcadia.lib.scheduler.SchedulerService;
import com.arcadia.lib.text.MessageHelper;
import com.arcadia.lib.text.TextFormatter;
import com.arcadia.lootbox.config.LootboxConfig;
import com.arcadia.lootbox.data.LootboxDefinition;
import com.arcadia.lootbox.manager.HistoryManager;
import com.arcadia.lootbox.manager.LootboxManager;
import com.arcadia.lootbox.manager.UsageTracker;
import com.arcadia.lootbox.menu.PreviewMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Core lootbox logic — opening, previewing, giving, broadcasting.
 * Supports "weighted" (multi-drop) and "guaranteed" (single-drop + guaranteed item) modes.
 *
 * @author vyrriox
 */
public final class LootHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");
    private static final String COOLDOWN_PREFIX = "lootbox.open.";

    private LootHelper() {}

    // --- NBT ---

    public static String getLootboxIdFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("LootboxID")) return tag.getString("LootboxID");
        }
        return null;
    }

    // --- Give lootbox item ---

    private static final Random SHARED_RANDOM = new Random();

    public static void giveLootboxItem(ServerPlayer player, String id) {
        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return;
        boolean fr = LanguageHelper.isFrench(player);

        DyeColor dye = resolveDyeColor(def.color());
        ItemStack stack = new ItemStack(ShulkerBoxBlock.getBlockByColor(dye));

        CompoundTag beTag = new CompoundTag();
        beTag.putString("LootboxID", id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(beTag));
        String name = (fr && !def.displayNameFR().isEmpty()) ? def.displayNameFR() : def.displayName();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                def.rarityColor() + "Lootbox: §f" + name));

        List<Component> lore = new ArrayList<>();
        String rarityName = fr ? LanguageHelper.getFR("rarity." + (def.rarity() != null ? def.rarity().toLowerCase() : "common"))
                : def.rarityDisplayName();
        lore.add(Component.translatable("arcadialootbox.lore.rarity",
                def.rarityColor() + rarityName));
        lore.add(Component.translatable("arcadialootbox.lore.key", def.keyItem()));
        lore.add(Component.translatable("arcadialootbox.lore.type",
                Component.translatable(def.isGuaranteedType()
                        ? "arcadialootbox.type.guaranteed" : "arcadialootbox.type.weighted")));
        if (def.maxUses() > 0) lore.add(Component.translatable("arcadialootbox.lore.max_uses", def.maxUses()));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    // --- Give key item ---

    public static void giveKeyItem(ServerPlayer player, String keyId, int amount) {
        ResourceLocation keyRes = ResourceLocation.tryParse(keyId);
        if (keyRes == null) return;
        var item = BuiltInRegistries.ITEM.get(keyRes);
        if (item == Items.AIR) return;

        ItemStack stack = new ItemStack(item, amount);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    // --- Preview GUI ---

    // Title prefix used by the client interceptor to detect lootbox preview menus
    public static final String PREVIEW_TITLE_MARKER = "\u00A7r\u00A76\u2699 ";

    public static void openPreviewGui(ServerPlayer player, String id, BlockPos pos) {
        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return;
        String lang = player.clientInformation() != null ? player.clientInformation().language() : "en_us";
        boolean fr = lang.startsWith("fr");
        String rarityName = LanguageHelper.getRarityName(player, def.rarity());
        // Use FR display name if available
        String name = (fr && !def.displayNameFR().isEmpty()) ? def.displayNameFR() : def.displayName();
        // Prefix with marker so the client interceptor can detect it reliably
        Component title = Component.literal(PREVIEW_TITLE_MARKER + def.rarityColor() + "§l" + name +
                " §r§7(" + def.rarityColor() + rarityName + "§7)");
        player.openMenu(new SimpleMenuProvider((winId, inv, p) ->
                new PreviewMenu(winId, inv, id, pos, def, lang), title));
    }

    // --- Lootbox opening attempt ---

    /**
     * Counts how many copies of a lootbox's key the player currently holds.
     */
    public static int countKeysInInventory(ServerPlayer player, String id) {
        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return 0;
        ResourceLocation keyRes = ResourceLocation.tryParse(def.keyItem());
        if (keyRes == null) return 0;
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (!slot.isEmpty() && BuiltInRegistries.ITEM.getKey(slot.getItem()).equals(keyRes)) {
                total += slot.getCount();
            }
        }
        // Offhand is included in inv.getContainerSize() on 1.21
        return total;
    }

    /**
     * Hard cap on how many lootboxes can be opened in one bulk action.
     * Prevents lag spikes and griefing via inventory spam.
     */
    public static final int BULK_OPEN_LIMIT = 10;

    /**
     * Opens up to {@code requested} lootboxes in a row, consuming one key per success.
     * Skips per-lootbox cooldown (only the first opening sets it) but enforces autoclicker
     * and usage caps. Returns the number of successful openings.
     */
    public static int handleBulkLootboxAttempt(Level level, BlockPos pos, ServerPlayer player, String id, int requested) {
        if (id == null || id.isEmpty() || requested <= 0) return 0;
        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return 0;

        if (!def.permission().isEmpty() && !PermissionHelper.hasPermission(player, def.permission())) {
            player.sendSystemMessage(ArcadiaMessages.error(LanguageHelper.get(player, "lootbox.no.permission")));
            return 0;
        }

        if (HistoryManager.checkAutoclicker(player.getUUID())) {
            player.sendSystemMessage(ArcadiaMessages.error(LanguageHelper.get(player, "lootbox.autoclicker")));
            return 0;
        }

        ResourceLocation keyRes = ResourceLocation.tryParse(def.keyItem());
        if (keyRes == null) return 0;

        int cap = Math.min(requested, BULK_OPEN_LIMIT);
        int opened = 0;
        for (int i = 0; i < cap; i++) {
            ItemStack keyStack = findKeyInInventory(player, keyRes);
            if (keyStack == null) break;

            // Usage cap (re-check each iteration so block destruction halts the loop)
            boolean hasBlock = level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock;
            if (hasBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null && !UsageTracker.hasUsesRemaining(be, def.maxUses())) {
                    player.sendSystemMessage(ArcadiaMessages.warning(LanguageHelper.get(player, "lootbox.no.uses")));
                    break;
                }
            }

            openLootboxLogic((ServerLevel) level, pos, player, def, keyStack, id);
            opened++;
        }

        if (opened > 0) {
            int cdTicks = def.cooldownTicks() > 0 ? def.cooldownTicks() : LootboxConfig.DEFAULT_COOLDOWN_TICKS.get();
            CooldownManager.set(player.getUUID(), COOLDOWN_PREFIX + id, cdTicks * 50L);
        }
        return opened;
    }

    public static boolean handleLootboxAttempt(Level level, BlockPos pos, ServerPlayer player, String id) {
        if (id == null || id.isEmpty()) return false;
        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return false;

        // Permission check (soft LuckPerms — works without LP)
        if (!def.permission().isEmpty()) {
            if (!PermissionHelper.hasPermission(player, def.permission())) {
                player.sendSystemMessage(ArcadiaMessages.error(LanguageHelper.get(player, "lootbox.no.permission")));
                return true;
            }
        }

        // Sneak check (skip if opened from hub — no block at position)
        boolean fromBlock = level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
        if (fromBlock && (def.requireSneakToOpen() || LootboxConfig.REQUIRE_SNEAK_DEFAULT.get())) {
            if (!player.isShiftKeyDown()) {
                player.sendSystemMessage(ArcadiaMessages.info(LanguageHelper.get(player, "lootbox.sneak.required")));
                return true;
            }
        }

        // Key check — search entire inventory (main hand first, then inventory)
        ResourceLocation keyRes = ResourceLocation.tryParse(def.keyItem());
        if (keyRes == null) return false;

        ItemStack keyStack = findKeyInInventory(player, keyRes);
        if (keyStack == null) {
            boolean fr = LanguageHelper.isFrench(player);
            player.sendSystemMessage(ArcadiaMessages.error(LanguageHelper.get(player, "lootbox.no.key")));
            return true;
        }

        // Cooldown check
        int cooldownTicks = def.cooldownTicks() > 0 ? def.cooldownTicks() : LootboxConfig.DEFAULT_COOLDOWN_TICKS.get();
        String cooldownKey = COOLDOWN_PREFIX + id;
        if (!CooldownManager.isReady(player.getUUID(), cooldownKey)) {
            String remaining = CooldownManager.getRemainingFormatted(player.getUUID(), cooldownKey);
            player.sendSystemMessage(ArcadiaMessages.warning(LanguageHelper.get(player, "lootbox.cooldown", "time", remaining)));
            return true;
        }

        // Anti-autoclicker
        if (HistoryManager.checkAutoclicker(player.getUUID())) {
            player.sendSystemMessage(ArcadiaMessages.error(LanguageHelper.get(player, "lootbox.autoclicker")));
            return true;
        }

        // Usage check (only if there's a block at the position)
        boolean hasBlock = level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
        if (hasBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null && !UsageTracker.hasUsesRemaining(be, def.maxUses())) {
                player.sendSystemMessage(ArcadiaMessages.warning(LanguageHelper.get(player, "lootbox.no.uses")));
                return true;
            }
        }

        CooldownManager.set(player.getUUID(), cooldownKey, cooldownTicks * 50L);
        openLootboxLogic((ServerLevel) level, pos, player, def, keyStack, id);
        return true;
    }

    // --- Core opening logic ---

    private static void openLootboxLogic(ServerLevel level, BlockPos pos, ServerPlayer player,
                                         LootboxDefinition def, ItemStack keyStack, String lootboxId) {
        // Check if there's an actual lootbox block at this position (may be null if opened from hub)
        boolean hasBlock = level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock;

        var random = level.random;
        playSound(level, pos, def.openSound());

        List<String> receivedItems = new ArrayList<>();

        if (def.isGuaranteedType()) {
            // --- GUARANTEED MODE: pick ONE item + guaranteed item ---
            handleGuaranteedDrop(level, pos, player, def, random, receivedItems, lootboxId);
        } else {
            // --- WEIGHTED MODE: each item rolls independently ---
            handleWeightedDrop(level, pos, player, def, random, receivedItems, lootboxId);
        }

        // Command rewards
        for (LootboxDefinition.CommandReward cmd : def.commandRewards()) {
            if (random.nextDouble() <= cmd.chance()) {
                // Sanitize player name to prevent command injection
                String safeName = player.getGameProfile().getName().replaceAll("[^A-Za-z0-9_]", "");
                String command = cmd.command().replace("{player}", safeName);
                var src = cmd.asConsole() ? level.getServer().createCommandSourceStack() : player.createCommandSourceStack();
                level.getServer().getCommands().performPrefixedCommand(src, command);
            }
        }

        // XP
        if (def.experienceReward() > 0) player.giveExperiencePoints((int) def.experienceReward());

        // Particles
        spawnParticles(level, pos, def, random);

        // Title animation (use FR variants if player is French)
        if (LootboxConfig.ANIMATIONS_ENABLED.get() && LootboxConfig.TITLE_ON_OPEN.get()) {
            boolean fr = LanguageHelper.isFrench(player);
            String titleText = fr && !def.openTitleFR().isEmpty() ? def.openTitleFR()
                    : !def.openTitle().isEmpty() ? def.openTitle()
                    : def.rarityColor() + "§l" + (fr && !def.displayNameFR().isEmpty() ? def.displayNameFR() : def.displayName());
            String subtitleText = fr && !def.openSubtitleFR().isEmpty() ? def.openSubtitleFR()
                    : !def.openSubtitle().isEmpty() ? def.openSubtitle()
                    : "§7" + LanguageHelper.get(player, "lootbox.items.received", "count", String.valueOf(receivedItems.size()));
            MessageHelper.sendTitle(player, Component.literal(titleText), Component.literal(subtitleText),
                    LootboxConfig.TITLE_FADE_IN.get(), LootboxConfig.TITLE_STAY.get(), LootboxConfig.TITLE_FADE_OUT.get());
        }

        // Action bar message (use FR variant if available)
        boolean fr = LanguageHelper.isFrench(player);
        String msg = fr && !def.openMessageFR().isEmpty() ? def.openMessageFR() : def.openMessage();
        if (msg == null || msg.isEmpty()) msg = "§a" + LanguageHelper.get(player, "lootbox.opened");
        player.displayClientMessage(Component.literal(msg), true);

        // Consume key
        if (!player.isCreative()) keyStack.shrink(1);

        // Track usage (only if there's an actual lootbox block)
        if (hasBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                int usageCount = UsageTracker.incrementUsage(be);
                if (def.destroyOnOpen() || (def.maxUses() > 0 && usageCount >= def.maxUses())) {
                    SchedulerService.delayed(5, () -> {
                        if (level.isLoaded(pos)) {
                            level.destroyBlock(pos, false);
                            UsageTracker.removeFromCache(pos);
                        }
                    });
                }
            }
        }

        // Record history
        HistoryManager.record(player.getUUID(), lootboxId, def.displayName(), receivedItems);

        // Log
        if (def.logOpening() || LootboxConfig.LOG_ALL_OPENINGS.get()) {
            LOGGER.info("[ArcadiaLootbox] {} opened '{}' ({}) at {} — {}",
                    player.getName().getString(), lootboxId, def.type(), pos.toShortString(), receivedItems);
        }
    }

    // --- WEIGHTED DROP: each item rolls with its own chance ---

    private static void handleWeightedDrop(ServerLevel level, BlockPos pos, ServerPlayer player,
                                            LootboxDefinition def, net.minecraft.util.RandomSource random,
                                            List<String> receivedItems, String lootboxId) {
        Map<net.minecraft.world.item.Item, Integer> drops = new LinkedHashMap<>();
        int maxDrops = LootboxConfig.MAX_DROPS_PER_OPEN.get();
        int dropCount = 0;

        for (LootboxDefinition.LootEntry entry : def.lootTable()) {
            if (dropCount >= maxDrops) break;
            if (random.nextDouble() <= entry.chance()) {
                ResourceLocation itemRes = ResourceLocation.tryParse(entry.item());
                if (itemRes == null) continue;
                var item = BuiltInRegistries.ITEM.get(itemRes);
                if (item == Items.AIR) continue;

                int min = Math.max(0, entry.minCount());
                int max = Math.max(min, entry.maxCount());
                int count = min == max ? min : random.nextInt(max - min + 1) + min;
                if (count > 0) {
                    drops.merge(item, count, Integer::sum);
                    String name = entry.displayName() != null ? entry.displayName() : entry.item();
                    receivedItems.add(count + "x " + name);
                    dropCount++;
                    if (entry.broadcast() || (def.broadcastRare() && shouldBroadcast(entry.rarity()))) {
                        broadcastDrop(level, player, def, entry, count, lootboxId);
                    }
                }
            }
        }
        giveDrops(player, drops);
    }

    // --- GUARANTEED DROP: pick ONE from pool (weighted) + guaranteed item ---

    private static void handleGuaranteedDrop(ServerLevel level, BlockPos pos, ServerPlayer player,
                                              LootboxDefinition def, net.minecraft.util.RandomSource random,
                                              List<String> receivedItems, String lootboxId) {
        // 1. Give guaranteed item
        if (def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            ResourceLocation gRes = ResourceLocation.tryParse(def.guaranteedItem());
            if (gRes != null) {
                var gItem = BuiltInRegistries.ITEM.get(gRes);
                if (gItem != Items.AIR) {
                    int gMin = Math.max(1, def.guaranteedMinCount());
                    int gMax = Math.max(gMin, def.guaranteedMaxCount());
                    int gCount = gMin == gMax ? gMin : random.nextInt(gMax - gMin + 1) + gMin;
                    giveItem(player, gItem, gCount);
                    boolean fr = LanguageHelper.isFrench(player);
                    receivedItems.add(gCount + "x " + def.guaranteedItem() + (fr ? " (garanti)" : " (guaranteed)"));
                }
            }
        }

        // 2. Pick ONE item from pool using chance as weight
        if (!def.lootTable().isEmpty()) {
            double totalWeight = 0;
            for (LootboxDefinition.LootEntry entry : def.lootTable()) {
                totalWeight += Math.max(0, entry.chance());
            }

            if (totalWeight > 0) {
                double roll = random.nextDouble() * totalWeight;
                double cumulative = 0;
                for (LootboxDefinition.LootEntry entry : def.lootTable()) {
                    cumulative += Math.max(0, entry.chance());
                    if (roll < cumulative) {
                        ResourceLocation itemRes = ResourceLocation.tryParse(entry.item());
                        if (itemRes != null) {
                            var item = BuiltInRegistries.ITEM.get(itemRes);
                            if (item != Items.AIR) {
                                int min = Math.max(1, entry.minCount());
                                int max = Math.max(min, entry.maxCount());
                                int count = min == max ? min : random.nextInt(max - min + 1) + min;
                                if (count <= 0) break;
                                giveItem(player, item, count);
                                String name = entry.displayName() != null ? entry.displayName() : entry.item();
                                receivedItems.add(count + "x " + name);
                                if (entry.broadcast() || (def.broadcastRare() && shouldBroadcast(entry.rarity()))) {
                                    broadcastDrop(level, player, def, entry, count, lootboxId);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    // --- Item giving ---

    private static void giveDrops(ServerPlayer player, Map<net.minecraft.world.item.Item, Integer> drops) {
        for (var entry : drops.entrySet()) {
            giveItem(player, entry.getKey(), entry.getValue());
        }
    }

    private static void giveItem(ServerPlayer player, net.minecraft.world.item.Item item, int total) {
        int maxStack = item.getDefaultInstance().getMaxStackSize();
        while (total > 0) {
            int split = Math.min(total, maxStack);
            ItemStack stack = new ItemStack(item, split);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            total -= split;
        }
    }

    // --- Particles ---

    private static void spawnParticles(ServerLevel level, BlockPos pos, LootboxDefinition def,
                                       net.minecraft.util.RandomSource random) {
        if (!LootboxConfig.ANIMATIONS_ENABLED.get() || def.particles() == null) return;
        int limit = LootboxConfig.PARTICLE_LIMIT.get();
        var anim = def.animation();
        int count = Math.min(anim != null ? anim.particleCount() : 15, limit);
        float speed = anim != null ? anim.particleSpeed() : 0.1f;
        double radius = anim != null ? anim.particleRadius() : 0.5;

        for (String pStr : def.particles()) {
            ResourceLocation pRes = ResourceLocation.tryParse(pStr);
            if (pRes == null) continue;
            var type = BuiltInRegistries.PARTICLE_TYPE.get(pRes);
            if (type instanceof net.minecraft.core.particles.SimpleParticleType simple) {
                level.sendParticles(simple, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        count, radius, radius, radius, speed);
            }
        }
    }

    // --- Sound ---

    private static void playSound(ServerLevel level, BlockPos pos, String soundId) {
        SoundEvent sound = SoundEvents.SHULKER_BOX_OPEN;
        if (soundId != null && !soundId.isEmpty()) {
            ResourceLocation res = ResourceLocation.tryParse(soundId);
            if (res != null) {
                SoundEvent s = BuiltInRegistries.SOUND_EVENT.get(res);
                if (s != null) sound = s;
            }
        }
        level.playSound(null, pos, sound, SoundSource.BLOCKS,
                (float) LootboxConfig.SOUND_VOLUME.get().doubleValue(),
                (float) LootboxConfig.SOUND_PITCH.get().doubleValue());
    }

    // --- Broadcast ---

    private static boolean shouldBroadcast(String rarity) {
        if (!LootboxConfig.BROADCAST_ENABLED.get()) return false;
        return LootboxConfig.rarityToLevel(rarity) >= LootboxConfig.BROADCAST_MIN_RARITY.get();
    }

    private static void broadcastDrop(ServerLevel level, ServerPlayer player, LootboxDefinition def,
                                       LootboxDefinition.LootEntry entry, int count, String lootboxId) {
        String name = entry.displayName() != null ? entry.displayName() : shortItem(entry.item());
        String rarity = entry.rarity() != null ? entry.rarity() : "common";
        String rarityColor = def.rarityColor();

        // Build broadcast message directly (config format has placeholder issues with §)
        String msg = "§6\u2699 §d\u2726 §e" + player.getName().getString() +
                " §7a trouvé " + rarityColor + capitalize(rarity) +
                " §7: §f" + count + "x §e" + name +
                " §7dans §e" + def.displayName() + "§7! §d\u2726";

        Component component = Component.literal(msg);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) p.sendSystemMessage(component);
    }

    private static String shortItem(String fullId) {
        if (fullId == null) return "???";
        int colon = fullId.indexOf(':');
        return colon >= 0 ? fullId.substring(colon + 1).replace('_', ' ') : fullId;
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? "Common" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // --- Key search ---

    /**
     * Searches the player's entire inventory for the required key.
     * Checks main hand first, then offhand, then all inventory slots.
     * Returns the ItemStack reference (for shrinking later), or null if not found.
     */
    private static ItemStack findKeyInInventory(ServerPlayer player, ResourceLocation keyRes) {
        // Main hand first
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && BuiltInRegistries.ITEM.getKey(mainHand.getItem()).equals(keyRes)) {
            return mainHand;
        }
        // Offhand
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && BuiltInRegistries.ITEM.getKey(offHand.getItem()).equals(keyRes)) {
            return offHand;
        }
        // Full inventory scan
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && BuiltInRegistries.ITEM.getKey(slot.getItem()).equals(keyRes)) {
                return slot;
            }
        }
        return null;
    }

    // --- Utility ---

    public static DyeColor resolveDyeColor(String colorName) {
        if (colorName == null || "random".equalsIgnoreCase(colorName)) {
            return DyeColor.values()[SHARED_RANDOM.nextInt(DyeColor.values().length)];
        }
        for (DyeColor d : DyeColor.values()) {
            if (d.getName().equalsIgnoreCase(colorName)) return d;
        }
        return DyeColor.WHITE;
    }
}
