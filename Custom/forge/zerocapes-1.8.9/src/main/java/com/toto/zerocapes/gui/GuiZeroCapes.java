package com.toto.zerocapes.gui;

import com.toto.zerocapes.cape.CapeDefinition;
import com.toto.zerocapes.cape.CapeRegistry;
import com.toto.zerocapes.config.ZeroCapesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiZeroCapes extends GuiScreen {
    private final GuiScreen parentScreen;

    private GuiTextField searchField;
    private GuiButton equipButton;
    private GuiButton selectNoneButton;
    private GuiButton doneButton;

    private CapeDefinition selectedCape;
    private List<CapeDefinition> filteredCapes = new ArrayList<CapeDefinition>();

    // Scrolling list dimensions
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private final int itemHeight = 36;
    private float scrollOffset = 0.0F;
    private boolean isDraggingScrollbar = false;

    public GuiZeroCapes(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        this.listLeft = 15;
        this.listTop = 64;
        this.listWidth = Math.min(240, this.width / 2 - 10);
        this.listHeight = this.height - this.listTop - 45;

        // Search field
        this.searchField = new GuiTextField(0, this.fontRendererObj, this.listLeft, 40, this.listWidth, 18);
        this.searchField.setMaxStringLength(40);
        this.searchField.setFocused(false);

        // Buttons at bottom
        int btnY = this.height - 30;
        int btnW = 100;
        int btnSpacing = 8;
        int totalBtnW = btnW * 3 + btnSpacing * 2;
        int startBtnX = (this.width - totalBtnW) / 2;

        this.equipButton = new GuiButton(1, startBtnX, btnY, btnW, 20, "Equip Cape");
        this.selectNoneButton = new GuiButton(2, startBtnX + btnW + btnSpacing, btnY, btnW, 20, "Reset to None");
        this.doneButton = new GuiButton(3, startBtnX + (btnW + btnSpacing) * 2, btnY, btnW, 20, "Done");

        this.buttonList.add(this.equipButton);
        this.buttonList.add(this.selectNoneButton);
        this.buttonList.add(this.doneButton);

        updateFilteredList();

        // Initial selection: currently equipped cape
        String equippedId = ZeroCapesConfig.getInstance().getSelectedCapeId();
        this.selectedCape = CapeRegistry.getById(equippedId);

        updateButtonStates();
    }

    private void updateFilteredList() {
        this.filteredCapes.clear();
        String query = this.searchField != null ? this.searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        for (CapeDefinition def : CapeRegistry.getAllCapes()) {
            if (query.isEmpty() || def.getName().toLowerCase(Locale.ROOT).contains(query)
                || def.getCategory().toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredCapes.add(def);
            }
        }
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, this.filteredCapes.size() * this.itemHeight - this.listHeight);
        if (this.scrollOffset > maxScroll) {
            this.scrollOffset = maxScroll;
        }
        if (this.scrollOffset < 0) {
            this.scrollOffset = 0;
        }
    }

    private void updateButtonStates() {
        String equippedId = ZeroCapesConfig.getInstance().getSelectedCapeId();
        if (this.selectedCape != null) {
            boolean isAlreadyEquipped = this.selectedCape.getId().equalsIgnoreCase(equippedId);
            this.equipButton.enabled = !isAlreadyEquipped;
            this.equipButton.displayString = isAlreadyEquipped ? "Equipped" : "Equip Cape";
        } else {
            this.equipButton.enabled = false;
        }

        this.selectNoneButton.enabled = !"none".equalsIgnoreCase(equippedId);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1 && this.selectedCape != null) {
            ZeroCapesConfig.getInstance().setSelectedCapeId(this.selectedCape.getId());
            updateButtonStates();
        } else if (button.id == 2) {
            ZeroCapesConfig.getInstance().setSelectedCapeId("none");
            this.selectedCape = CapeRegistry.getDefaultCape();
            updateButtonStates();
        } else if (button.id == 3) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int maxScroll = Math.max(0, this.filteredCapes.size() * this.itemHeight - this.listHeight);
            if (dWheel > 0) {
                this.scrollOffset -= 24;
            } else {
                this.scrollOffset += 24;
            }
            clampScroll();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.searchField != null) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton == 0) {
            // Check click inside cape list
            if (mouseX >= this.listLeft && mouseX <= this.listLeft + this.listWidth
                && mouseY >= this.listTop && mouseY <= this.listTop + this.listHeight) {
                
                int relativeY = mouseY - this.listTop + (int) this.scrollOffset;
                int clickedIndex = relativeY / this.itemHeight;
                if (clickedIndex >= 0 && clickedIndex < this.filteredCapes.size()) {
                    this.selectedCape = this.filteredCapes.get(clickedIndex);
                    updateButtonStates();
                }
            }

            // Scrollbar click
            int scrollbarX = this.listLeft + this.listWidth - 6;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + 6
                && mouseY >= this.listTop && mouseY <= this.listTop + this.listHeight) {
                this.isDraggingScrollbar = true;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            this.isDraggingScrollbar = false;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (this.isDraggingScrollbar && clickedMouseButton == 0) {
            int maxScroll = Math.max(0, this.filteredCapes.size() * this.itemHeight - this.listHeight);
            if (maxScroll > 0) {
                float ratio = (float) (mouseY - this.listTop) / (float) this.listHeight;
                this.scrollOffset = ratio * maxScroll;
                clampScroll();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField != null && this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateFilteredList();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.searchField != null) {
            this.searchField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark translucent background
        this.drawDefaultBackground();

        // Header Title
        drawCenteredString(this.fontRendererObj, "ZeroCapes - Official Capes", this.width / 2, 12, 0xFFFFFFFF);
        drawCenteredString(this.fontRendererObj, "\u00a77Choose from all official Minecraft Java capes or keep your vanilla cape.", this.width / 2, 25, 0xFFAAAAAA);

        // Search Field
        if (this.searchField != null) {
            this.searchField.drawTextBox();
            if (this.searchField.getText().isEmpty() && !this.searchField.isFocused()) {
                this.fontRendererObj.drawString("\u00a78Search capes...", this.listLeft + 4, 45, 0xFF777777);
            }
        }

        // Draw Cape List
        renderCapeList(mouseX, mouseY);

        // Draw Preview Panel
        renderPreviewPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawOutline(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y + 1, x + 1, y + h - 1, color);
        drawRect(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void renderCapeList(int mouseX, int mouseY) {
        // Background for list
        drawRect(this.listLeft, this.listTop, this.listLeft + this.listWidth, this.listTop + this.listHeight, 0xB0000000);
        drawOutline(this.listLeft, this.listTop, this.listWidth, this.listHeight, 0xFF444444);

        int maxScroll = Math.max(0, this.filteredCapes.size() * this.itemHeight - this.listHeight);
        int startIndex = (int) (this.scrollOffset / this.itemHeight);
        int visibleCount = (this.listHeight / this.itemHeight) + 2;
        int endIndex = Math.min(this.filteredCapes.size(), startIndex + visibleCount);

        String equippedId = ZeroCapesConfig.getInstance().getSelectedCapeId();

        for (int i = startIndex; i < endIndex; i++) {
            CapeDefinition def = this.filteredCapes.get(i);
            int itemY = this.listTop + (i * this.itemHeight) - (int) this.scrollOffset;

            if (itemY + this.itemHeight < this.listTop || itemY > this.listTop + this.listHeight) {
                continue;
            }

            boolean isSelected = (this.selectedCape != null && def.getId().equalsIgnoreCase(this.selectedCape.getId()));
            boolean isEquipped = def.getId().equalsIgnoreCase(equippedId);

            // Item box background
            if (isSelected) {
                drawRect(this.listLeft + 2, itemY, this.listLeft + this.listWidth - 8, itemY + this.itemHeight - 2, 0x40FFFFFF);
                drawOutline(this.listLeft + 2, itemY, this.listWidth - 10, this.itemHeight - 2, 0xFFFFFFFF);
            } else if (mouseX >= this.listLeft && mouseX <= this.listLeft + this.listWidth - 8 && mouseY >= itemY && mouseY <= itemY + this.itemHeight - 2) {
                drawRect(this.listLeft + 2, itemY, this.listLeft + this.listWidth - 8, itemY + this.itemHeight - 2, 0x20FFFFFF);
            }

            // Cape Icon
            int iconX = this.listLeft + 6;
            int iconY = itemY + 4;
            int iconW = 16;
            int iconH = 26;

            drawRect(iconX - 1, iconY - 1, iconX + iconW + 1, iconY + iconH + 1, 0xFF333333);

            if (def.isDefault() || def.getTexture() == null) {
                // Vanilla 'X' icon
                drawCenteredString(this.fontRendererObj, "\u00a77\u2715", iconX + iconW / 2, iconY + iconH / 2 - 4, 0xFF888888);
            } else {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.mc.getTextureManager().bindTexture(def.getTexture());
                // UV for back of cape: u=1..11 (10px), v=1..17 (16px) out of 64x32
                Gui.drawScaledCustomSizeModalRect(iconX, iconY, 1.0F, 1.0F, 10, 16, iconW, iconH, 64.0F, 32.0F);
            }

            // Title & Category
            int textX = iconX + iconW + 6;
            int nameColor = isSelected ? 0xFF55FF55 : 0xFFFFFFFF;
            String displayName = def.getName();
            if (this.fontRendererObj.getStringWidth(displayName) > this.listWidth - 65) {
                displayName = this.fontRendererObj.trimStringToWidth(displayName, this.listWidth - 75) + "...";
            }
            this.fontRendererObj.drawString(displayName, textX, itemY + 6, nameColor);
            this.fontRendererObj.drawString("\u00a78[" + def.getCategory() + "]", textX, itemY + 18, 0xFF888888);

            // EQ badge if equipped
            if (isEquipped) {
                int eqX = this.listLeft + this.listWidth - 34;
                int eqY = itemY + 8;
                drawRect(eqX, eqY, eqX + 22, eqY + 14, 0xFF005500);
                drawOutline(eqX, eqY, 22, 14, 0xFF00FF00);
                drawCenteredString(this.fontRendererObj, "\u00a7aEQ", eqX + 11, eqY + 3, 0xFF55FF55);
            }
        }

        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarX = this.listLeft + this.listWidth - 6;
            int barHeight = Math.max(16, (int) ((float) this.listHeight * ((float) this.listHeight / (float) (this.filteredCapes.size() * this.itemHeight))));
            int barY = this.listTop + (int) ((this.scrollOffset / (float) maxScroll) * (this.listHeight - barHeight));

            drawRect(scrollbarX, this.listTop, scrollbarX + 5, this.listTop + this.listHeight, 0xFF222222);
            drawRect(scrollbarX, barY, scrollbarX + 5, barY + barHeight, 0xFF888888);
        }
    }

    private int getCategoryColor(String cat) {
        if ("Minecon".equalsIgnoreCase(cat)) return 0xFFFFD700;
        if ("Staff".equalsIgnoreCase(cat)) return 0xFFFF5555;
        if ("Ownership".equalsIgnoreCase(cat)) return 0xFF55FFFF;
        if ("Events".equalsIgnoreCase(cat)) return 0xFFAAAAFF;
        if ("Special".equalsIgnoreCase(cat)) return 0xFFFF55FF;
        return 0xFFAAAAAA;
    }

    private void renderPreviewPanel(int mouseX, int mouseY) {
        int previewX = this.listLeft + this.listWidth + 15;
        int previewY = 64;
        int previewWidth = this.width - previewX - 15;
        int previewHeight = this.height - previewY - 45;

        if (previewWidth < 120 || previewHeight < 100) return;

        // Background Box
        drawRect(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xB0000000);
        drawOutline(previewX, previewY, previewWidth, previewHeight, 0xFF555555);

        if (this.selectedCape == null) {
            drawCenteredString(this.fontRendererObj, "Select a cape from the list to preview", previewX + previewWidth / 2, previewY + previewHeight / 2 - 4, 0xFF888888);
            return;
        }

        int textY = previewY + 12;
        this.fontRendererObj.drawString("\u00a7l" + this.selectedCape.getName(), previewX + 16, textY, 0xFFFFFFFF);
        textY += 14;

        int catColor = getCategoryColor(this.selectedCape.getCategory());
        this.fontRendererObj.drawString("\u00a77Category: \u00a7r" + this.selectedCape.getCategory(), previewX + 16, textY, catColor);
        textY += 14;

        String currentEquipped = ZeroCapesConfig.getInstance().getSelectedCapeId();
        boolean isEquipped = this.selectedCape.getId().equalsIgnoreCase(currentEquipped);
        if (isEquipped) {
            this.fontRendererObj.drawString("\u00a7a\u2714 Currently Equipped", previewX + 16, textY, 0xFF55FF55);
        } else {
            this.fontRendererObj.drawString("\u00a78Status: Not equipped (Click 'Equip Cape' below)", previewX + 16, textY, 0xFF888888);
        }
        textY += 16;

        this.fontRendererObj.drawString("\u00a77" + this.selectedCape.getDescription(), previewX + 16, textY, 0xFFBBBBBB);
        textY += 24;

        int capeBoxHeight = (previewY + previewHeight) - textY - 12;
        if (capeBoxHeight > 50) {
            int centerX = previewX + previewWidth / 2;
            int centerY = textY + capeBoxHeight / 2;

            if (this.selectedCape.isDefault() || this.selectedCape.getTexture() == null) {
                drawCenteredString(this.fontRendererObj, "\u00a7e[ Vanilla / Normal Cape ]", centerX, centerY - 10, 0xFFFFFF55);
                drawCenteredString(this.fontRendererObj, "\u00a77Displays your Minecraft account cape", centerX, centerY + 4, 0xFF888888);
            } else {
                ResourceLocation tex = this.selectedCape.getTexture();
                float scale = Math.min(5.0F, (float) capeBoxHeight / 20.0F);
                int scaledW = (int) (10 * scale);
                int scaledH = (int) (16 * scale);

                // Back side of cape (Outer emblem)
                int backX = centerX - scaledW - 14;
                int backY = centerY - scaledH / 2;

                drawRect(backX - 2, backY - 2, backX + scaledW + 2, backY + scaledH + 2, 0x80222222);
                drawOutline(backX - 2, backY - 2, scaledW + 4, scaledH + 4, 0xFF666666);

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.mc.getTextureManager().bindTexture(tex);
                // Back UV: u=1, v=1, size 10x16
                Gui.drawScaledCustomSizeModalRect(backX, backY, 1.0F, 1.0F, 10, 16, scaledW, scaledH, 64.0F, 32.0F);

                drawCenteredString(this.fontRendererObj, "\u00a77Back (Outer)", backX + scaledW / 2, backY + scaledH + 5, 0xFFAAAAAA);

                // Front side of cape (Inner lining)
                int frontX = centerX + 14;
                int frontY = centerY - scaledH / 2;

                drawRect(frontX - 2, frontY - 2, frontX + scaledW + 2, frontY + scaledH + 2, 0x80222222);
                drawOutline(frontX - 2, frontY - 2, scaledW + 4, scaledH + 4, 0xFF666666);

                // Front UV: u=12, v=1, size 10x16
                Gui.drawScaledCustomSizeModalRect(frontX, frontY, 12.0F, 1.0F, 10, 16, scaledW, scaledH, 64.0F, 32.0F);

                drawCenteredString(this.fontRendererObj, "\u00a77Front (Inner)", frontX + scaledW / 2, frontY + scaledH + 5, 0xFFAAAAAA);
            }
        }
    }
}
