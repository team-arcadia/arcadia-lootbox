package com.arcadia.lootbox.client;

import com.arcadia.lib.client.ArcadiaTheme;
import com.arcadia.lootbox.network.S2CSyncLootboxList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.*;

/**
 * Client-side Lootbox Hub with ArcadiaTheme — lootboxes organized by
 * key category. Categories start collapsed and expand on click.
 * Drastically reduces visual noise vs. the all-flat layout.
 *
 * @author vyrriox
 */
public class LootboxHubScreen extends Screen {

    private static final int CARD_W = 100, CARD_H = 72, CARD_GAP = 10;
    private static final int CATEGORY_HEADER_H = 26;
    private static final int ROW_GAP = 10;

    // Category display config (insertion order = render order)
    private static final Map<String, CategoryInfo> CATEGORY_INFO = new LinkedHashMap<>();
    static {
        CATEGORY_INFO.put("shop",     new CategoryInfo("Shop",     "Boutique",    0xFFAA00, "🛒"));
        CATEGORY_INFO.put("vote",     new CategoryInfo("Vote",     "Vote",        0x55FF55, "🗳"));
        CATEGORY_INFO.put("dungeon",  new CategoryInfo("Dungeon",  "Donjon",      0x5555FF, "⚔"));
        CATEGORY_INFO.put("lootable", new CategoryInfo("Lootable", "Trouvable",   0xAAAAAA, "🔑"));
        CATEGORY_INFO.put("event",    new CategoryInfo("Event",    "Evenement",   0xFF55FF, "✨"));
        CATEGORY_INFO.put("boss",     new CategoryInfo("Boss",     "Boss",        0xFF5555, "💀"));
        CATEGORY_INFO.put("other",    new CategoryInfo("Other",    "Autre",       0xBBBBBB, "⭐"));
    }

    record CategoryInfo(String nameEN, String nameFR, int color, String emoji) {}

    private Map<String, List<S2CSyncLootboxList.LootboxEntry>> categorized;

    /** Persists across re-init (resize) so the user keeps their open/closed state. */
    private static final Set<String> EXPANDED = new HashSet<>();

    private int scrollOffset = 0;
    private int totalContentHeight = 0;

    // Hitboxes rebuilt every frame
    private record CardHitbox(int x, int y, int w, int h, String lootboxId) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
    private record HeaderHitbox(int x, int y, int w, int h, String categoryKey) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
    private final List<CardHitbox> cardHitboxes = new ArrayList<>();
    private final List<HeaderHitbox> headerHitboxes = new ArrayList<>();
    private boolean expandAllHover = false;
    private boolean collapseAllHover = false;

    public LootboxHubScreen() { super(Component.translatable("arcadialootbox.gui.hub_title")); }

    public static void open() { Minecraft.getInstance().setScreen(new LootboxHubScreen()); }

    @Override
    protected void init() {
        super.init();
        var lootboxes = LootboxClientData.getLootboxList();
        categorized = new LinkedHashMap<>();

        for (var entry : lootboxes) {
            String cat = extractCategory(entry.keyItem());
            categorized.computeIfAbsent(cat, k -> new ArrayList<>()).add(entry);
        }
        for (var list : categorized.values()) {
            list.sort(Comparator.comparingInt(S2CSyncLootboxList.LootboxEntry::sortOrder));
        }

        // Re-order map by CATEGORY_INFO order (drop unknown buckets to "other")
        Map<String, List<S2CSyncLootboxList.LootboxEntry>> ordered = new LinkedHashMap<>();
        for (String key : CATEGORY_INFO.keySet()) {
            if (categorized.containsKey(key) && !categorized.get(key).isEmpty()) {
                ordered.put(key, categorized.get(key));
            }
        }
        categorized = ordered;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, ArcadiaTheme.OVERLAY_BG);
        int cx = width / 2;
        boolean fr = isFrench();

        ArcadiaTheme.drawTitleBar(g, Component.translatable("arcadialootbox.gui.hub_title"), cx, 14, width - 80);
        g.drawCenteredString(font, Component.translatable("arcadialootbox.gui.hub_subtitle"),
                cx, 30, ArcadiaTheme.TEXT_SECONDARY);
        ArcadiaTheme.drawSeparator(g, 40, 42, width - 80, ArcadiaTheme.withAlpha(ArcadiaTheme.COPPER, 0x44));

        // Expand/Collapse all controls (top-right of header bar)
        int ctrlY = 22;
        int expandX = width - 175, collapseX = width - 80;
        expandAllHover = mouseX >= expandX && mouseX < expandX + 90 && mouseY >= ctrlY && mouseY < ctrlY + 14;
        collapseAllHover = mouseX >= collapseX && mouseX < collapseX + 70 && mouseY >= ctrlY && mouseY < ctrlY + 14;
        drawSmallButton(g, expandX, ctrlY, 90, 14, fr ? "▼ Tout déployer" : "▼ Expand all", expandAllHover);
        drawSmallButton(g, collapseX, ctrlY, 70, 14, fr ? "▲ Replier" : "▲ Collapse", collapseAllHover);

        if (categorized == null || categorized.isEmpty()) {
            ArcadiaTheme.drawCenteredText(g, Component.translatable("arcadialootbox.gui.none_available"),
                    cx, height / 2, ArcadiaTheme.TEXT_DIM);
            super.render(g, mouseX, mouseY, pt);
            return;
        }

        cardHitboxes.clear();
        headerHitboxes.clear();

        int contentTop = 48;
        int contentBottom = height - 48;
        g.enableScissor(0, contentTop, width, contentBottom);

        int curY = contentTop - scrollOffset;

        for (var catEntry : categorized.entrySet()) {
            String catKey = catEntry.getKey();
            List<S2CSyncLootboxList.LootboxEntry> entries = catEntry.getValue();
            CategoryInfo info = CATEGORY_INFO.getOrDefault(catKey, CATEGORY_INFO.get("other"));
            boolean expanded = EXPANDED.contains(catKey);

            int headerX = 30, headerW = width - 60;
            boolean headerHov = mouseX >= headerX && mouseX < headerX + headerW
                    && mouseY >= curY && mouseY < curY + CATEGORY_HEADER_H
                    && mouseY >= contentTop && mouseY <= contentBottom;

            if (curY + CATEGORY_HEADER_H > contentTop - 20 && curY < contentBottom + 20) {
                drawCategoryHeader(g, headerX, curY, headerW, info, entries.size(), expanded, headerHov, fr);
            }
            headerHitboxes.add(new HeaderHitbox(headerX, curY, headerW, CATEGORY_HEADER_H, catKey));
            curY += CATEGORY_HEADER_H + 4;

            if (!expanded) {
                curY += ROW_GAP;
                continue;
            }

            int cardsPerRow = Math.max(1, (width - 80) / (CARD_W + CARD_GAP));
            int totalW = Math.min(entries.size(), cardsPerRow) * (CARD_W + CARD_GAP) - CARD_GAP;
            int startX = (width - totalW) / 2;

            for (int i = 0; i < entries.size(); i++) {
                int col = i % cardsPerRow;
                int row = i / cardsPerRow;
                int cardX = startX + col * (CARD_W + CARD_GAP);
                int cardY = curY + row * (CARD_H + CARD_GAP);

                if (cardY + CARD_H > contentTop - 10 && cardY < contentBottom + 10) {
                    var entry = entries.get(i);
                    boolean hovered = mouseX >= cardX && mouseX < cardX + CARD_W
                            && mouseY >= cardY && mouseY < cardY + CARD_H
                            && mouseY >= contentTop && mouseY <= contentBottom;
                    drawCard(g, entry, cardX, cardY, hovered, info.color, fr);
                    cardHitboxes.add(new CardHitbox(cardX, cardY, CARD_W, CARD_H, entry.id()));
                }
            }

            int rows = (entries.size() + cardsPerRow - 1) / cardsPerRow;
            curY += rows * (CARD_H + CARD_GAP) + ROW_GAP;
        }

        totalContentHeight = curY + scrollOffset - contentTop;
        g.disableScissor();

        if (Minecraft.getInstance().isSingleplayer()) {
            g.drawCenteredString(font, Component.translatable("arcadialootbox.gui.shop_warning"),
                    cx, contentBottom + 2, 0xFFFF5555);
        }

        int btnW = 220, btnH = 22, btnX = cx - btnW / 2, btnY = height - 38;
        boolean btnHov = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        ArcadiaTheme.drawPanel(g, btnX, btnY, btnW, btnH, btnHov, ArcadiaTheme.PATINA);
        g.drawCenteredString(font, Component.translatable("arcadialootbox.gui.buy_keys"),
                cx, btnY + 7, btnHov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.TEXT_SECONDARY);

        g.drawCenteredString(font, Component.translatable("arcadialootbox.gui.esc_close"),
                cx, height - 12, ArcadiaTheme.TEXT_DIM);

        super.render(g, mouseX, mouseY, pt);
    }

    private void drawCategoryHeader(GuiGraphics g, int x, int y, int w, CategoryInfo info,
                                    int count, boolean expanded, boolean hovered, boolean fr) {
        int color = info.color | 0xFF000000;
        int bgAlpha = hovered ? 0x55 : (expanded ? 0x33 : 0x22);
        int borderAlpha = hovered ? 0xCC : (expanded ? 0x88 : 0x55);

        g.fill(x, y, x + w, y + CATEGORY_HEADER_H, ArcadiaTheme.withAlpha(info.color, bgAlpha));
        g.fill(x, y + CATEGORY_HEADER_H - 1, x + w, y + CATEGORY_HEADER_H, ArcadiaTheme.withAlpha(info.color, borderAlpha));
        g.fill(x, y, x + 3, y + CATEGORY_HEADER_H, ArcadiaTheme.withAlpha(info.color, 0xCC));

        String chevron = expanded ? "▼" : "▶";
        g.drawString(font, Component.literal(chevron), x + 10, y + 9, color, false);

        String title = info.emoji + "  " + (fr ? info.nameFR : info.nameEN);
        g.drawString(font, Component.literal(title), x + 28, y + 9, color, false);

        String sub = (fr ? "  ·  " : "  ·  ") + count + (fr ? " lootbox" : " lootbox");
        int titleWidth = font.width(title);
        g.drawString(font, Component.literal(sub), x + 28 + titleWidth, y + 9, ArcadiaTheme.TEXT_DIM, false);

        String hint = expanded
                ? (fr ? "Clique pour replier" : "Click to collapse")
                : (fr ? "Clique pour déployer" : "Click to expand");
        int hintWidth = font.width(hint);
        g.drawString(font, Component.literal(hint), x + w - hintWidth - 10, y + 9,
                hovered ? color : ArcadiaTheme.TEXT_DIM, false);
    }

    private void drawSmallButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean hov) {
        g.fill(x, y, x + w, y + h, ArcadiaTheme.withAlpha(ArcadiaTheme.COPPER, hov ? 0x55 : 0x22));
        g.fill(x, y + h - 1, x + w, y + h, ArcadiaTheme.withAlpha(ArcadiaTheme.COPPER, hov ? 0xCC : 0x66));
        g.drawCenteredString(font, Component.literal(label), x + w / 2, y + 3,
                hov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.TEXT_SECONDARY);
    }

    private void drawCard(GuiGraphics g, S2CSyncLootboxList.LootboxEntry e, int x, int y,
                          boolean hov, int catColor, boolean fr) {
        int accent = rarityColor(e.rarity());
        if (hov) ArcadiaTheme.drawGlow(g, x, y, CARD_W, CARD_H, accent);
        ArcadiaTheme.drawPanel(g, x, y, CARD_W, CARD_H, hov, accent);
        int cx = x + CARD_W / 2;

        g.drawCenteredString(font, Component.literal(rarityEmoji(e.rarity())), cx, y + 6, accent | 0xFF000000);
        g.drawCenteredString(font, Component.literal(e.displayName()), cx, y + 20,
                hov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.darken(ArcadiaTheme.TEXT_PRIMARY, 20));
        g.drawCenteredString(font, Component.literal(fr ? frRarity(e.rarity()) : cap(e.rarity())), cx, y + 33, accent | 0xFF000000);
        Component typeLabel = Component.translatable(e.type().equalsIgnoreCase("guaranteed")
                ? "arcadialootbox.type.guaranteed" : "arcadialootbox.type.weighted");
        g.drawCenteredString(font, typeLabel, cx, y + 46, ArcadiaTheme.TEXT_SECONDARY);
        g.drawCenteredString(font, Component.translatable("arcadialootbox.gui.drops", e.lootCount()), cx, y + 58, ArcadiaTheme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (expandAllHover) {
                EXPANDED.addAll(categorized.keySet());
                playClickSound();
                return true;
            }
            if (collapseAllHover) {
                EXPANDED.clear();
                scrollOffset = 0;
                playClickSound();
                return true;
            }

            int btnX = width / 2 - 110, btnY = height - 38;
            if (mx >= btnX && mx < btnX + 220 && my >= btnY && my < btnY + 22) {
                String url = LootboxClientData.getShopUrl();
                if (!url.isEmpty()) {
                    handleComponentClicked(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
                }
                return true;
            }

            for (HeaderHitbox h : headerHitboxes) {
                if (h.contains(mx, my)) {
                    if (EXPANDED.contains(h.categoryKey)) EXPANDED.remove(h.categoryKey);
                    else EXPANDED.add(h.categoryKey);
                    playClickSound();
                    return true;
                }
            }

            for (CardHitbox hitbox : cardHitboxes) {
                if (hitbox.contains(mx, my)) {
                    this.onClose();
                    com.arcadia.lootbox.network.LootboxNet.sendPreviewRequest(hitbox.lootboxId);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void playClickSound() {
        var mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int maxScroll = Math.max(0, totalContentHeight - (height - 100));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(sy * 25)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float pt) {}

    private static String extractCategory(String keyItem) {
        if (keyItem == null || keyItem.isEmpty()) return "other";
        String name = keyItem.contains(":") ? keyItem.substring(keyItem.indexOf(':') + 1) : keyItem;
        if (name.contains("_key_")) {
            return name.substring(0, name.indexOf("_key_"));
        }
        for (String cat : CATEGORY_INFO.keySet()) {
            if (name.contains(cat)) return cat;
        }
        return "other";
    }

    private static boolean isFrench() {
        var mc = Minecraft.getInstance();
        if (mc.options == null) return false;
        String lang = mc.options.languageCode;
        return lang != null && lang.startsWith("fr");
    }

    private static int rarityColor(String r) {
        if (r == null) return 0xAAAAAA;
        return switch (r.toLowerCase()) {
            case "uncommon" -> 0x55FF55; case "rare" -> 0x5555FF; case "epic" -> 0xAA00AA;
            case "legendary" -> 0xFFAA00; case "mythic" -> 0xFF55FF; default -> 0xAAAAAA;
        };
    }

    private static String rarityEmoji(String r) {
        if (r == null) return "⭐";
        return switch (r.toLowerCase()) {
            case "common" -> "⚪"; case "uncommon" -> "🟢"; case "rare" -> "🔵";
            case "epic" -> "🟣"; case "legendary" -> "🟠"; case "mythic" -> "✨"; default -> "⭐";
        };
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "Common" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static String frRarity(String r) {
        if (r == null) return "Commune";
        return switch (r.toLowerCase()) {
            case "uncommon" -> "Peu commune"; case "rare" -> "Rare"; case "epic" -> "Epique";
            case "legendary" -> "Legendaire"; case "mythic" -> "Mythique"; default -> "Commune";
        };
    }
}
