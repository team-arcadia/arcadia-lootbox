package com.arcadia.lootbox.client.screen;

import com.arcadia.lib.client.ArcadiaTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.jetbrains.annotations.NotNull;

/**
 * Themed lootbox preview screen with ArcadiaTheme steampunk rendering.
 * The "Draw!" button (slot 49) glows with copper accent on hover.
 *
 * @author vyrriox
 */
public class LootboxPreviewScreen extends ThemedContainerScreen {

    private static final int DRAW_BUTTON_SLOT = 49;

    public LootboxPreviewScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        // Title bar with copper decorations
        ArcadiaTheme.drawTitleBar(g, this.title, this.imageWidth / 2, 4, this.imageWidth - 16);

        // Draw button glow effect when hovered (slot 49 = bottom row, center)
        // Slot 49 position: col=4 (x=8+4*18=80), row in bottom bar (y=197)
        int slotX = 8 + 4 * 18; // slot 49 = column 4 of bottom row
        int slotY = 17 + 5 * 18; // row 5 (0-indexed) of 6-row chest

        int relMouseX = mouseX - this.leftPos;
        int relMouseY = mouseY - this.topPos;

        boolean hovered = relMouseX >= slotX && relMouseX < slotX + 18
                && relMouseY >= slotY && relMouseY < slotY + 18;

        if (hovered) {
            // Copper glow around the draw button
            ArcadiaTheme.drawGlow(g, slotX - 2, slotY - 2, 22, 22, ArcadiaTheme.AMBER);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Dark overlay
        g.fill(0, 0, this.width, this.height, ArcadiaTheme.OVERLAY_BG);

        // Steampunk container background
        ArcadiaTheme.drawContainerBg(g, this.leftPos, this.topPos, this.imageWidth, 6);
    }
}
