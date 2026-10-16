package com.toto.zerocapes;

import com.toto.zerocapes.cape.CapeRegistry;
import com.toto.zerocapes.config.ZeroCapesConfig;
import com.toto.zerocapes.event.ZeroCapesEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ZeroCapes.MODID, name = ZeroCapes.NAME, version = ZeroCapes.VERSION, clientSideOnly = true)
public class ZeroCapes {
    public static final String MODID = "zerocapes";
    public static final String NAME = "ZeroCapes";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger("ZeroCapes");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[ZeroCapes] Pre-initializing ZeroCapes 1.8.9 by toto...");
        CapeRegistry.init();
        ZeroCapesConfig.getInstance();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("[ZeroCapes] Registering event bus listeners...");
        MinecraftForge.EVENT_BUS.register(new ZeroCapesEventHandler());
        LOGGER.info("[ZeroCapes] ZeroCapes 1.8.9 initialized successfully!");
    }
}
