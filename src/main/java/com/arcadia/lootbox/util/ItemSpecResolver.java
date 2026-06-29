package com.arcadia.lootbox.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a loot item specification into a single-count template {@link ItemStack}.
 *
 * <p>Accepts the vanilla command item syntax, so lootbox configs can declare items
 * carrying data components / NBT exactly like the {@code /give} command:
 * <pre>
 *   "minecraft:diamond"
 *   "minecraft:enchanted_book[stored_enchantments={levels:{\"minecraft:sharpness\":5}}]"
 *   "minecraft:potion[potion_contents={potion:\"minecraft:strong_healing\"}]"
 *   "minecraft:diamond_sword[enchantments={levels:{\"minecraft:sharpness\":5}},custom_name='\"Excalibur\"']"
 * </pre>
 *
 * <p>Fully backward compatible: a bare {@code namespace:id} resolves through the fast
 * path with no parsing overhead. Malformed specs degrade gracefully to the base item.
 *
 * @author vyrriox
 */
public final class ItemSpecResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");

    private ItemSpecResolver() {}

    /**
     * Parses {@code spec} into a single-count template stack.
     *
     * @param spec       the item id, optionally followed by {@code [component=value,...]}
     * @param registries registry access used to resolve components (may be {@code null})
     * @return a template stack with count 1, or {@code null} if the id is unknown / spec invalid
     */
    public static ItemStack resolve(String spec, HolderLookup.Provider registries) {
        if (spec == null || spec.isBlank()) return null;
        String trimmed = spec.trim();

        // Fast path: bare id with no components — avoids the parser entirely.
        if (trimmed.indexOf('[') < 0 && trimmed.indexOf('{') < 0) {
            return resolvePlain(trimmed);
        }

        // Component syntax requires registry access; without it, grant the base item
        // (stripped of NBT) rather than losing the reward entirely.
        if (registries == null) {
            return resolvePlain(stripComponents(trimmed));
        }

        try {
            ItemParser parser = new ItemParser(registries);
            ItemParser.ItemResult result = parser.parse(new StringReader(trimmed));
            ItemStack stack = new ItemStack(result.item());
            stack.applyComponents(result.components());
            return stack.isEmpty() ? null : stack;
        } catch (CommandSyntaxException e) {
            LOGGER.warn("[ArcadiaLootbox] Invalid item spec '{}': {}", trimmed, e.getMessage());
            return resolvePlain(stripComponents(trimmed));
        }
    }

    private static ItemStack resolvePlain(String id) {
        ResourceLocation res = ResourceLocation.tryParse(id);
        if (res == null) return null;
        var item = BuiltInRegistries.ITEM.get(res);
        if (item == Items.AIR) return null;
        return new ItemStack(item);
    }

    /** Returns the bare item id, dropping any {@code [...]} component block. */
    private static String stripComponents(String spec) {
        int bracket = spec.indexOf('[');
        int brace = spec.indexOf('{');
        int cut = bracket < 0 ? brace : (brace < 0 ? bracket : Math.min(bracket, brace));
        return cut < 0 ? spec : spec.substring(0, cut).trim();
    }

    /** Human-friendly short name for display, stripping namespace and component block. */
    public static String shortName(String spec) {
        if (spec == null) return "???";
        String base = stripComponents(spec);
        int colon = base.indexOf(':');
        return (colon >= 0 ? base.substring(colon + 1) : base).replace('_', ' ');
    }
}
