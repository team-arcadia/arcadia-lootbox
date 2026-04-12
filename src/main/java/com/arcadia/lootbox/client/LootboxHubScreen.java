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
 * key category (auto-detected from keyItem name). Each category is a row
 * with a copper header label.
 *
 * @author vyrriox
 */
public class LootboxHubScreen extends Screen {

    private static final int CARD_W = 100, CARD_H = 72, CARD_GAP = 10;
    private static final int CATEGORY_HEADER_H = 18;
    private static final int ROW_GAP = 8;

    // Category display config
    private static final Map<String, CategoryInfo> CATEGORY_INFO = new LinkedHashMap<>();
    static {
        CATEGORY_INFO.put("shop",     new CategoryInfo("Shop",     "Boutique",    0xFFAA00, "\uD83D\uDED2"));
        CATEGORY_INFO.put("vote",     new CategoryInfo("Vote",     "Vote",        0x55FF55, "\uD83D\uDDF3"));
        CATEGORY_INFO.put("dungeon",  new CategoryInfo("Dungeon",  "Donjon",      0x5555FF, "\u2694"));
        CATEGORY_INFO.put("lootable", new CategoryInfo("Lootable", "Trouvable",   0xAAAAAA, "\uD83D\uDD11"));
        CATEGORY_INFO.put("event",    new CategoryInfo("Event",    "Evenement",   0xFF55FF, "\u2728"));
        CATEGORY_INFO.put("boss",     new CategoryInfo("Boss",     "Boss",        0xFF5555, "\uD83D\uDC80"));
        CATEGORY_INFO.put("other",    new CategoryInfo("Other",    "Autre",       0xBBBBBB, "\u2B50"));
    }

    record CategoryInfo(String nameEN, String nameFR, int color, String emoji) {}

    private Map<String, List<S2CSyncLootboxList.LootboxEntry>> categorized;
    private int scrollOffset = 0;
    private int totalContentHeight = 0;

    // Clickable card hitboxes (rebuilt every frame)
    private record CardHitbox(int x, int y, int w, int h, String lootboxId) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
    private final List<CardHitbox> cardHitboxes = new ArrayList<>();

    public LootboxHubScreen() { super(Component.literal("Arcadia Lootbox Hub")); }

    public static void open() { Minecraft.getInstance().setScreen(new LootboxHubScreen()); }

    @Override
    protected void init() {
        super.init();
        var lootboxes = LootboxClientData.getLootboxList();
        categorized = new LinkedHashMap<>();

        // Sort into categories based on key item name
        for (var entry : lootboxes) {
            String cat = extractCategory(entry.keyItem());
            categorized.computeIfAbsent(cat, k -> new ArrayList<>()).add(entry);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, ArcadiaTheme.OVERLAY_BG);
        int cx = width / 2;
        boolean fr = isFrench();

        // Title
        ArcadiaTheme.drawTitleBar(g, Component.literal(fr ? "Hub Lootbox" : "Lootbox Hub"), cx, 14, width - 80);
        g.drawCenteredString(font, Component.literal(fr ? "Parcourir les lootbox disponibles" : "Browse available lootboxes"),
                cx, 30, ArcadiaTheme.TEXT_SECONDARY);
        ArcadiaTheme.drawSeparator(g, 40, 42, width - 80, ArcadiaTheme.withAlpha(ArcadiaTheme.COPPER, 0x44));

        if (categorized == null || categorized.isEmpty()) {
            ArcadiaTheme.drawCenteredText(g, Component.literal(fr ? "Aucune lootbox disponible" : "No lootboxes available"),
                    cx, height / 2, ArcadiaTheme.TEXT_DIM);
            super.render(g, mouseX, mouseY, pt);
            return;
        }

        // Rebuild hitboxes each frame
        cardHitboxes.clear();

        // Enable scissor for scrollable area
        int contentTop = 48;
        int contentBottom = height - 48;
        g.enableScissor(0, contentTop, width, contentBottom);

        int curY = contentTop - scrollOffset;

        for (var catEntry : categorized.entrySet()) {
            String catKey = catEntry.getKey();
            List<S2CSyncLootboxList.LootboxEntry> entries = catEntry.getValue();
            CategoryInfo info = CATEGORY_INFO.getOrDefault(catKey, CATEGORY_INFO.get("other"));

            // Category header
            String headerText = info.emoji + " " + (fr ? info.nameFR : info.nameEN) + " (" + entries.size() + ")";
            int headerColor = info.color | 0xFF000000;

            if (curY + CATEGORY_HEADER_H > contentTop - 20 && curY < contentBottom + 20) {
                // Header background bar
                g.fill(30, curY, width - 30, curY + CATEGORY_HEADER_H, ArcadiaTheme.withAlpha(info.color, 0x18));
                g.fill(30, curY + CATEGORY_HEADER_H - 1, width - 30, curY + CATEGORY_HEADER_H, ArcadiaTheme.withAlpha(info.color, 0x33));
                g.drawString(font, Component.literal(headerText), 40, curY + 5, headerColor, false);
            }
            curY += CATEGORY_HEADER_H + 4;

            // Cards for this category
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

        // Shop button
        int btnW = 220, btnH = 22, btnX = cx - btnW / 2, btnY = height - 38;
        boolean btnHov = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        ArcadiaTheme.drawPanel(g, btnX, btnY, btnW, btnH, btnHov, ArcadiaTheme.PATINA);
        g.drawCenteredString(font, Component.literal(fr ? "\uD83D\uDED2 Acheter des Cles" : "\uD83D\uDED2 Buy Lootbox Keys"),
                cx, btnY + 7, btnHov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.TEXT_SECONDARY);

        g.drawCenteredString(font, Component.literal(fr ? "ESC pour fermer" : "ESC to close"),
                cx, height - 12, ArcadiaTheme.TEXT_DIM);

        super.render(g, mouseX, mouseY, pt);
    }

    private void drawCard(GuiGraphics g, S2CSyncLootboxList.LootboxEntry e, int x, int y,
                          boolean hov, int catColor, boolean fr) {
        int accent = rarityColor(e.rarity());
        if (hov) ArcadiaTheme.drawGlow(g, x, y, CARD_W, CARD_H, accent);
        ArcadiaTheme.drawPanel(g, x, y, CARD_W, CARD_H, hov, accent);
        int cx = x + CARD_W / 2;

        // Rarity emoji
        g.drawCenteredString(font, Component.literal(rarityEmoji(e.rarity())), cx, y + 6, accent | 0xFF000000);
        // Name
        g.drawCenteredString(font, Component.literal(e.displayName()), cx, y + 20,
                hov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.darken(ArcadiaTheme.TEXT_PRIMARY, 20));
        // Rarity
        g.drawCenteredString(font, Component.literal(fr ? frRarity(e.rarity()) : cap(e.rarity())), cx, y + 33, accent | 0xFF000000);
        // Type
        String typeLabel = e.type().equalsIgnoreCase("guaranteed")
                ? (fr ? "Garanti" : "Guaranteed") : (fr ? "Pondere" : "Weighted");
        g.drawCenteredString(font, Component.literal(typeLabel), cx, y + 46, ArcadiaTheme.TEXT_SECONDARY);
        // Drops count
        g.drawCenteredString(font, Component.literal(e.lootCount() + " drops"), cx, y + 58, ArcadiaTheme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            // Shop button
            int btnX = width / 2 - 110, btnY = height - 38;
            if (mx >= btnX && mx < btnX + 220 && my >= btnY && my < btnY + 22) {
                String url = LootboxClientData.getShopUrl();
                if (!url.isEmpty()) {
                    handleComponentClicked(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
                }
                return true;
            }

            // Card click — open preview via C2S packet
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

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int maxScroll = Math.max(0, totalContentHeight - (height - 100));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(sy * 25)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float pt) {}

    // --- Category extraction from key item ---

    private static String extractCategory(String keyItem) {
        if (keyItem == null || keyItem.isEmpty()) return "other";
        // Remove namespace: "arcadialootbox:dungeon_key_rare" -> "dungeon_key_rare"
        String name = keyItem.contains(":") ? keyItem.substring(keyItem.indexOf(':') + 1) : keyItem;
        // Extract category: "dungeon_key_rare" -> "dungeon"
        if (name.contains("_key_")) {
            return name.substring(0, name.indexOf("_key_"));
        }
        // Fallback: try common patterns
        for (String cat : CATEGORY_INFO.keySet()) {
            if (name.contains(cat)) return cat;
        }
        return "other";
    }

    // --- Helpers ---

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
        if (r == null) return "\u2B50";
        return switch (r.toLowerCase()) {
            case "common" -> "\u26AA"; case "uncommon" -> "\uD83D\uDFE2"; case "rare" -> "\uD83D\uDD35";
            case "epic" -> "\uD83D\uDFE3"; case "legendary" -> "\uD83D\uDFE0"; case "mythic" -> "\u2728"; default -> "\u2B50";
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
