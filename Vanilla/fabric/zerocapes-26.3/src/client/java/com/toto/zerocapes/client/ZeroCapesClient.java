package com.toto.zerocapes.client;

import com.toto.zerocapes.client.cape.CapeRegistry;
import com.toto.zerocapes.client.config.ZeroCapesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeroCapesClient implements ClientModInitializer {
    public static final String MOD_ID = "zerocapes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ZeroCapes] Initializing ZeroCapes v1.0.0 by toto...");

        CapeRegistry.init();
        ZeroCapesConfig.getInstance();

        LOGGER.info("[ZeroCapes] ZeroCapes initialized successfully!");
    }

    public static boolean isLocalPlayer(AbstractClientPlayer player) {
        if (player == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (player == mc.player || player.getUUID().equals(mc.player.getUUID()))) {
            return true;
        }
        if (mc.getUser() != null && player.getUUID().equals(mc.getUser().getProfileId())) {
            return true;
        }
        return false;
    }
}
