package com.toto.zerocapes.event;

import com.toto.zerocapes.gui.GuiZeroCapes;
import com.toto.zerocapes.layer.CustomLayerCape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ZeroCapesEventHandler {
    private static final Logger LOGGER = LogManager.getLogger("ZeroCapes");
    private static final int ZEROCAPES_BTN_ID = 8998;
    private boolean layersInjected = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!layersInjected && Minecraft.getMinecraft().getRenderManager() != null) {
            injectCustomCapeLayers();
            layersInjected = true;
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiCustomizeSkin) {
            int btnX = event.gui.width / 2 - 100;
            int btnY = event.gui.height / 6 + 138;
            event.buttonList.add(new GuiButton(ZEROCAPES_BTN_ID, btnX, btnY, 200, 20, "ZeroCapes..."));
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.gui instanceof GuiCustomizeSkin && event.button.id == ZEROCAPES_BTN_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiZeroCapes(event.gui));
        }
    }

    @SuppressWarnings("unchecked")
    private void injectCustomCapeLayers() {
        try {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            if (skinMap == null || skinMap.isEmpty()) return;

            Field layerListField = ReflectionHelper.findField(RendererLivingEntity.class, "layerRenderers", "field_177097_h");
            layerListField.setAccessible(true);

            for (RenderPlayer renderPlayer : skinMap.values()) {
                List<LayerRenderer<?>> layers = (List<LayerRenderer<?>>) layerListField.get(renderPlayer);
                LayerCape original = null;
                int originalIndex = -1;

                for (int i = 0; i < layers.size(); i++) {
                    if (layers.get(i) instanceof LayerCape && !(layers.get(i) instanceof CustomLayerCape)) {
                        original = (LayerCape) layers.get(i);
                        originalIndex = i;
                        break;
                    }
                }

                if (originalIndex != -1) {
                    layers.set(originalIndex, new CustomLayerCape(renderPlayer, original));
                } else {
                    renderPlayer.addLayer(new CustomLayerCape(renderPlayer, null));
                }
            }
            LOGGER.info("[ZeroCapes] Custom cape layer injected successfully into player renderers!");
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to inject custom cape layer!", e);
        }
    }
}
