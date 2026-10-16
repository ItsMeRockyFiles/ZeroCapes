package com.toto.zerocapes.client.gui;

import com.toto.zerocapes.client.cape.CapeDefinition;
import com.toto.zerocapes.client.cape.CapeRegistry;
import com.toto.zerocapes.client.config.ZeroCapesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class ZeroCapesScreen extends Screen {
    private final Screen parentScreen;
    private CapeListWidget listWidget;
    private EditBox searchBox;
    private Button equipButton;
    private Button selectNoneButton;
    private CapeDefinition selectedCape;
    private String currentCategoryFilter = "All";
    private String currentSearchQuery = "";

    public ZeroCapesScreen(@Nullable Screen parentScreen) {
        super(Component.translatable("zerocapes.screen.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = Math.min(240, this.width / 2 - 10);
        int listLeft = 20;
        int listTop = 64;
        int listHeight = this.height - listTop - 46;

        // Initialize cape selection to current config
        this.selectedCape = ZeroCapesConfig.getInstance().getCurrentCapeDefinition();

        // Search Box
        this.searchBox = new EditBox(
                this.font,
                listLeft,
                38,
                listWidth,
                20,
                Component.translatable("zerocapes.screen.search")
        );
        this.searchBox.setHint(Component.translatable("zerocapes.screen.search"));
        this.searchBox.setResponder(query -> {
            this.currentSearchQuery = query;
            refreshCapeList();
        });
        this.addRenderableWidget(this.searchBox);

        // Cape List
        this.listWidget = new CapeListWidget(
                this.minecraft,
                listWidth,
                listHeight,
                listTop,
                36
        );
        this.listWidget.updateSizeAndPosition(listWidth, listHeight, listLeft, listTop);
        this.addRenderableWidget(this.listWidget);

        // Bottom Action Buttons
        int buttonWidth = 100;
        int buttonY = this.height - 32;

        this.equipButton = this.addRenderableWidget(
                Button.builder(Component.translatable("zerocapes.screen.equip"), button -> {
                            if (this.selectedCape != null) {
                                equipSelectedCape(this.selectedCape);
                            }
                        })
                        .bounds(this.width / 2 - 220, buttonY, buttonWidth, 20)
                        .build()
        );

        this.selectNoneButton = this.addRenderableWidget(
                Button.builder(Component.translatable("zerocapes.screen.none"), button -> {
                            CapeDefinition def = CapeRegistry.getDefaultCape();
                            this.selectedCape = def;
                            equipSelectedCape(def);
                            if (this.listWidget != null) {
                                for (CapeListWidget.CapeEntry entry : this.listWidget.children()) {
                                    if (entry.getCape().isDefault()) {
                                        this.listWidget.setSelected(entry);
                                        break;
                                    }
                                }
                            }
                        })
                        .bounds(this.width / 2 + 10, buttonY, buttonWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                        .bounds(this.width / 2 + 115, buttonY, buttonWidth, 20)
                        .build()
        );

        refreshCapeList();
        updateButtons();
    }

    private void refreshCapeList() {
        if (this.listWidget == null) return;
        this.listWidget.clearEntries();

        String query = this.currentSearchQuery.toLowerCase(Locale.ROOT).trim();
        List<CapeDefinition> all = CapeRegistry.getAllCapes();

        for (CapeDefinition cape : all) {
            // Category filter check
            if (!"All".equalsIgnoreCase(this.currentCategoryFilter)) {
                if (!cape.category().equalsIgnoreCase(this.currentCategoryFilter)) {
                    continue;
                }
            }

            // Search query check
            if (!query.isEmpty()) {
                boolean matchName = cape.name().toLowerCase(Locale.ROOT).contains(query);
                boolean matchCategory = cape.category().toLowerCase(Locale.ROOT).contains(query);
                boolean matchDesc = cape.description().toLowerCase(Locale.ROOT).contains(query);
                if (!matchName && !matchCategory && !matchDesc) {
                    continue;
                }
            }

            CapeListWidget.CapeEntry entry = this.listWidget.addCape(cape);
            if (this.selectedCape != null && cape.id().equalsIgnoreCase(this.selectedCape.id())) {
                this.listWidget.setSelected(entry);
            }
        }

        int listWidth = Math.min(240, this.width / 2 - 10);
        int listLeft = 20;
        int listTop = 64;
        int listHeight = this.height - listTop - 46;
        this.listWidget.updateSizeAndPosition(listWidth, listHeight, listLeft, listTop);
    }

    public void selectCape(CapeDefinition cape) {
        this.selectedCape = cape;
        updateButtons();
    }

    public void equipSelectedCape(CapeDefinition cape) {
        ZeroCapesConfig.getInstance().setSelectedCapeId(cape.id());
        updateButtons();
    }

    private void updateButtons() {
        String equippedId = ZeroCapesConfig.getInstance().getSelectedCapeId();
        if (this.selectedCape != null) {
            boolean isAlreadyEquipped = this.selectedCape.id().equalsIgnoreCase(equippedId);
            this.equipButton.active = !isAlreadyEquipped;
            if (isAlreadyEquipped) {
                this.equipButton.setMessage(Component.translatable("zerocapes.screen.equipped"));
            } else {
                this.equipButton.setMessage(Component.translatable("zerocapes.screen.equip"));
            }
        } else {
            this.equipButton.active = false;
        }

        this.selectNoneButton.active = !"none".equalsIgnoreCase(equippedId);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // Header Titles (with full alpha 0xFF)
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        guiGraphics.centeredText(
                this.font,
                Component.literal("§7Choose from all official Minecraft Java capes or keep your vanilla cape."),
                this.width / 2,
                25,
                0xFFAAAAAA
        );

        // Right Preview Area
        renderPreviewPanel(guiGraphics, mouseX, mouseY);
    }

    private void drawOutline(GuiGraphicsExtractor gui, int x, int y, int w, int h, int color) {
        gui.fill(x, y, x + w, y + 1, color);
        gui.fill(x, y + h - 1, x + w, y + h, color);
        gui.fill(x, y + 1, x + 1, y + h - 1, color);
        gui.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void renderPreviewPanel(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int listWidth = Math.min(240, this.width / 2 - 10);
        int previewX = 35 + listWidth;
        int previewY = 64;
        int previewWidth = this.width - previewX - 25;
        int previewHeight = this.height - 110;

        if (previewWidth < 120 || previewHeight < 100) return;

        // Background Box
        guiGraphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xB0000000);
        drawOutline(guiGraphics, previewX, previewY, previewWidth, previewHeight, 0xFF555555);

        if (this.selectedCape == null) {
            guiGraphics.centeredText(
                    this.font,
                    Component.literal("Select a cape from the list to preview"),
                    previewX + previewWidth / 2,
                    previewY + previewHeight / 2 - 4,
                    0xFF888888
            );
            return;
        }

        // Cape Name Title
        int textY = previewY + 12;
        guiGraphics.text(
                this.font,
                Component.literal("§l" + this.selectedCape.name()),
                previewX + 16,
                textY,
                0xFFFFFFFF
        );
        textY += 14;

        // Category Badge
        int catColor = getCategoryColor(this.selectedCape.category());
        guiGraphics.text(
                this.font,
                Component.literal("§7Category: §r" + this.selectedCape.category()),
                previewX + 16,
                textY,
                catColor
        );
        textY += 14;

        // Equipped Status
        String currentEquipped = ZeroCapesConfig.getInstance().getSelectedCapeId();
        boolean isEquipped = this.selectedCape.id().equalsIgnoreCase(currentEquipped);
        if (isEquipped) {
            guiGraphics.text(
                    this.font,
                    Component.literal("§a✔ Currently Equipped"),
                    previewX + 16,
                    textY,
                    0xFF55FF55
            );
        } else {
            guiGraphics.text(
                    this.font,
                    Component.literal("§8Status: Not equipped (Click 'Equip Cape' below)"),
                    previewX + 16,
                    textY,
                    0xFF888888
            );
        }
        textY += 16;

        // Description
        guiGraphics.text(
                this.font,
                Component.literal("§7" + this.selectedCape.description()),
                previewX + 16,
                textY,
                0xFFBBBBBB
        );
        textY += 24;

        // Visual Cape Rendering
        int capeRenderBoxY = textY;
        int capeBoxHeight = (previewY + previewHeight) - capeRenderBoxY - 12;

        if (capeBoxHeight > 50) {
            int centerX = previewX + previewWidth / 2;
            int centerY = capeRenderBoxY + capeBoxHeight / 2;

            if (this.selectedCape.isDefault() || this.selectedCape.texture() == null) {
                // Vanilla cape placeholder graphic
                guiGraphics.centeredText(
                        this.font,
                        Component.literal("§e[ Vanilla / Normal Cape ]"),
                        centerX,
                        centerY - 10,
                        0xFFFFFF55
                );
                guiGraphics.centeredText(
                        this.font,
                        Component.literal("§7Displays your Minecraft account cape"),
                        centerX,
                        centerY + 4,
                        0xFF888888
                );
            } else {
                Identifier tex = this.selectedCape.texture();
                float scale = Math.min(5.0f, (float) capeBoxHeight / 20.0f);
                int scaledW = (int) (10 * scale);
                int scaledH = (int) (16 * scale);

                // Back side of cape (Outer emblem)
                int backX = centerX - scaledW - 14;
                int backY = centerY - scaledH / 2;

                guiGraphics.fill(backX - 2, backY - 2, backX + scaledW + 2, backY + scaledH + 2, 0x80222222);
                drawOutline(guiGraphics, backX - 2, backY - 2, scaledW + 4, scaledH + 4, 0xFF666666);

                guiGraphics.blit(
                        tex,
                        backX,
                        backY,
                        backX + scaledW,
                        backY + scaledH,
                        1.0f / 64.0f,
                        11.0f / 64.0f,
                        1.0f / 32.0f,
                        17.0f / 32.0f
                );

                guiGraphics.centeredText(
                        this.font,
                        Component.literal("§7Back (Outer)"),
                        backX + scaledW / 2,
                        backY + scaledH + 5,
                        0xFFAAAAAA
                );

                // Front side of cape (Inner lining)
                int frontX = centerX + 14;
                int frontY = centerY - scaledH / 2;

                guiGraphics.fill(frontX - 2, frontY - 2, frontX + scaledW + 2, frontY + scaledH + 2, 0x80222222);
                drawOutline(guiGraphics, frontX - 2, frontY - 2, scaledW + 4, scaledH + 4, 0xFF666666);

                guiGraphics.blit(
                        tex,
                        frontX,
                        frontY,
                        frontX + scaledW,
                        frontY + scaledH,
                        12.0f / 64.0f,
                        22.0f / 64.0f,
                        1.0f / 32.0f,
                        17.0f / 32.0f
                );

                guiGraphics.centeredText(
                        this.font,
                        Component.literal("§7Front (Inner)"),
                        frontX + scaledW / 2,
                        frontY + scaledH + 5,
                        0xFFAAAAAA
                );
            }
        }
    }

    public static int getCategoryColor(String category) {
        return switch (category) {
            case "Ownership" -> 0xFFFFAA00;
            case "Staff" -> 0xFFFF5555;
            case "Physical Event", "Virtual Event", "Events" -> 0xFF55FFFF;
            case "Personal" -> 0xFFFF55FF;
            case "Volunteer", "Competition" -> 0xFF55FF55;
            default -> 0xFFAAAAAA;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.listWidget != null && this.listWidget.isMouseOver(event.x(), event.y())) {
            if (this.searchBox != null) {
                this.searchBox.setFocused(false);
            }
            if (this.listWidget.mouseClicked(event, doubleClick)) {
                this.setFocused(this.listWidget);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        }
    }

    // Cape List Widget
    public class CapeListWidget extends ObjectSelectionList<CapeListWidget.CapeEntry> {
        public CapeListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        public CapeEntry addCape(CapeDefinition cape) {
            CapeEntry entry = new CapeEntry(cape);
            this.addEntry(entry);
            return entry;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!this.isMouseOver(event.x(), event.y())) {
                return false;
            }

            // Scrollbar dragging check
            if (this.updateScrolling(event)) {
                return true;
            }

            boolean isLeftClick = event.button() == 0 || event.button() == 1
                    || this.isValidClickButton(event.buttonInfo());

            if (isLeftClick) {
                // Find entry under cursor (check precise position or screen Y range)
                CapeEntry target = this.getEntryAtPosition(event.x(), event.y());
                if (target == null) {
                    for (CapeEntry entry : this.children()) {
                        if (event.y() >= entry.getY() && event.y() < entry.getY() + entry.getHeight()) {
                            target = entry;
                            break;
                        }
                    }
                }

                if (target != null) {
                    this.setSelected(target);
                    ZeroCapesScreen.this.selectCape(target.getCape());
                    if (doubleClick) {
                        ZeroCapesScreen.this.equipSelectedCape(target.getCape());
                    }
                    return true;
                }
            }

            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        public class CapeEntry extends ObjectSelectionList.Entry<CapeEntry> {
            private final CapeDefinition cape;

            public CapeEntry(CapeDefinition cape) {
                this.cape = cape;
            }

            public CapeDefinition getCape() {
                return this.cape;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.cape.name());
            }

            @Override
            public void extractContent(
                    GuiGraphicsExtractor guiGraphics,
                    int mouseX,
                    int mouseY,
                    boolean hovering,
                    float partialTick
            ) {
                int left = this.getX();
                int top = this.getY();
                int width = this.getWidth();
                int height = this.getHeight();

                boolean isSelected = (ZeroCapesScreen.this.selectedCape != null
                        && ZeroCapesScreen.this.selectedCape.id().equalsIgnoreCase(this.cape.id()));
                boolean isEquipped = this.cape.id().equalsIgnoreCase(ZeroCapesConfig.getInstance().getSelectedCapeId());

                // Item highlight / border
                if (isSelected) {
                    guiGraphics.fill(left, top, left + width, top + height, 0x40FFFFFF);
                    drawOutline(guiGraphics, left, top, width, height, 0xFFFFFFFF);
                } else if (hovering) {
                    guiGraphics.fill(left, top, left + width, top + height, 0x20FFFFFF);
                }

                // Cape Icon / Mini Preview
                int iconX = left + 6;
                int iconY = top + 5;
                int iconW = 16;
                int iconH = 26;

                guiGraphics.fill(iconX - 1, iconY - 1, iconX + iconW + 1, iconY + iconH + 1, 0xFF222222);

                if (this.cape.isDefault() || this.cape.texture() == null) {
                    guiGraphics.centeredText(
                            ZeroCapesScreen.this.font,
                            Component.literal("§7✕"),
                            iconX + iconW / 2,
                            iconY + iconH / 2 - 4,
                            0xFF888888
                    );
                } else {
                    Identifier tex = this.cape.texture();
                    guiGraphics.blit(
                            tex,
                            iconX,
                            iconY,
                            iconX + iconW,
                            iconY + iconH,
                            1.0f / 64.0f,
                            11.0f / 64.0f,
                            1.0f / 32.0f,
                            17.0f / 32.0f
                    );
                }

                // Title & Category
                int textX = iconX + iconW + 8;
                String namePrefix = isEquipped ? "§a" : (isSelected ? "§f" : "§7");
                guiGraphics.text(
                        ZeroCapesScreen.this.font,
                        Component.literal(namePrefix + this.cape.name()),
                        textX,
                        top + 6,
                        0xFFFFFFFF
                );

                int catCol = getCategoryColor(this.cape.category());
                guiGraphics.text(
                        ZeroCapesScreen.this.font,
                        Component.literal("§8[" + this.cape.category() + "]"),
                        textX,
                        top + 18,
                        catCol
                );

                // EQUIPPED Badge
                if (isEquipped) {
                    int badgeW = 32;
                    int badgeX = left + width - badgeW - 6;
                    guiGraphics.fill(badgeX, top + 9, badgeX + badgeW, top + 21, 0xDD115511);
                    drawOutline(guiGraphics, badgeX, top + 9, badgeW, 12, 0xFF44FF44);
                    guiGraphics.centeredText(
                            ZeroCapesScreen.this.font,
                            Component.literal("§aEQ"),
                            badgeX + badgeW / 2,
                            top + 11,
                            0xFF55FF55
                    );
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                boolean isLeftClick = event.button() == 0 || event.button() == 1
                        || CapeListWidget.this.isValidClickButton(event.buttonInfo());
                if (isLeftClick) {
                    ZeroCapesScreen.this.selectCape(this.cape);
                    ZeroCapesScreen.this.listWidget.setSelected(this);

                    if (doubleClick) {
                        ZeroCapesScreen.this.equipSelectedCape(this.cape);
                    }
                    return true;
                }
                return false;
            }
        }
    }
}