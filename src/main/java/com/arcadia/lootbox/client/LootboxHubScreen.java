package com.arcadia.lootbox.client;

import com.arcadia.lib.client.ArcadiaTheme;
import com.arcadia.lootbox.network.S2CSyncLootboxList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * Client-side Lootbox Hub screen with ArcadiaTheme styling.
 * Shows all available lootboxes with rarity, type, and shop link.
 *
 * @author vyrriox
 */
public class LootboxHubScreen extends Screen {

    private static final int CARD_W = 120, CARD_H = 85, CARD_GAP = 12, CARDS_PER_ROW = 4;
    private List<S2CSyncLootboxList.LootboxEntry> lootboxes;
    private int hoveredIndex = -1;
    private int scrollOffset = 0;

    public LootboxHubScreen() { super(Component.literal("Arcadia Lootbox Hub")); }

    public static void open() { Minecraft.getInstance().setScreen(new LootboxHubScreen()); }

    @Override protected void init() { super.init(); lootboxes = LootboxClientData.getLootboxList(); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, ArcadiaTheme.OVERLAY_BG);
        int cx = width / 2;

        ArcadiaTheme.drawTitleBar(g, Component.literal("Lootbox Hub"), cx, 20, width - 80);
        g.drawCenteredString(font, Component.literal("Browse available lootboxes"), cx, 36, ArcadiaTheme.TEXT_SECONDARY);
        ArcadiaTheme.drawSeparator(g, 40, 48, width - 80, ArcadiaTheme.withAlpha(ArcadiaTheme.COPPER, 0x44));

        if (lootboxes == null || lootboxes.isEmpty()) {
            ArcadiaTheme.drawCenteredText(g, Component.literal("No lootboxes available"), cx, height / 2, ArcadiaTheme.TEXT_DIM);
            super.render(g, mouseX, mouseY, pt); return;
        }

        int totalW = Math.min(lootboxes.size(), CARDS_PER_ROW) * (CARD_W + CARD_GAP) - CARD_GAP;
        int startX = (width - totalW) / 2;
        int startY = 58;
        hoveredIndex = -1;

        for (int i = 0; i < lootboxes.size(); i++) {
            int row = i / CARDS_PER_ROW, col = i % CARDS_PER_ROW;
            int cardX = startX + col * (CARD_W + CARD_GAP);
            int cardY = startY + row * (CARD_H + CARD_GAP) - scrollOffset;
            if (cardY + CARD_H < 50 || cardY > height - 30) continue;

            var entry = lootboxes.get(i);
            boolean hovered = mouseX >= cardX && mouseX < cardX + CARD_W && mouseY >= cardY && mouseY < cardY + CARD_H;
            if (hovered) hoveredIndex = i;
            drawCard(g, entry, cardX, cardY, hovered);
        }

        // Shop button
        int btnW = 200, btnH = 24, btnX = cx - btnW / 2, btnY = height - 40;
        boolean btnHov = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        ArcadiaTheme.drawPanel(g, btnX, btnY, btnW, btnH, btnHov, ArcadiaTheme.PATINA);
        g.drawCenteredString(font, Component.literal("Buy Lootbox Keys"), cx, btnY + 8,
                btnHov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.TEXT_SECONDARY);

        g.drawCenteredString(font, Component.literal("ESC to close"), cx, height - 14, ArcadiaTheme.TEXT_DIM);
        super.render(g, mouseX, mouseY, pt);
    }

    private void drawCard(GuiGraphics g, S2CSyncLootboxList.LootboxEntry e, int x, int y, boolean hov) {
        int accent = rarityColor(e.rarity());
        if (hov) ArcadiaTheme.drawGlow(g, x, y, CARD_W, CARD_H, accent);
        ArcadiaTheme.drawPanel(g, x, y, CARD_W, CARD_H, hov, accent);
        int cx = x + CARD_W / 2;

        g.drawCenteredString(font, Component.literal(rarityEmoji(e.rarity())), cx, y + 8, accent | 0xFF000000);
        g.drawCenteredString(font, Component.literal(e.displayName()), cx, y + 24, hov ? ArcadiaTheme.TEXT_PRIMARY : ArcadiaTheme.darken(ArcadiaTheme.TEXT_PRIMARY, 20));
        g.drawCenteredString(font, Component.literal(cap(e.rarity())), cx, y + 38, accent | 0xFF000000);
        g.drawCenteredString(font, Component.literal("Type: " + cap(e.type())), cx, y + 52, ArcadiaTheme.TEXT_SECONDARY);
        g.drawCenteredString(font, Component.literal(e.lootCount() + " drops"), cx, y + 66, ArcadiaTheme.TEXT_DIM);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            int btnX = width / 2 - 100, btnY = height - 40;
            if (mx >= btnX && mx < btnX + 200 && my >= btnY && my < btnY + 24) {
                handleComponentClicked(Style.EMPTY.withClickEvent(
                        new ClickEvent(ClickEvent.Action.OPEN_URL, "https://store.yourserver.com/lootbox")));
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int rows = (lootboxes.size() + CARDS_PER_ROW - 1) / CARDS_PER_ROW;
        int maxScroll = Math.max(0, rows * (CARD_H + CARD_GAP) - (height - 120));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(sy * 20)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderBlurredBackground(float pt) {}

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

    private static String cap(String s) { return s == null || s.isEmpty() ? "Common" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase(); }
}
