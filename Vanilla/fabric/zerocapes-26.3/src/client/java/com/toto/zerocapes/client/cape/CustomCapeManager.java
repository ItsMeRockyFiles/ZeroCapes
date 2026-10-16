package com.toto.zerocapes.client.cape;

import com.mojang.blaze3d.platform.NativeImage;
import com.toto.zerocapes.client.ZeroCapesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the user-uploaded custom cape texture.
 *
 * Workflow:
 *   1. openFilePicker()  – opens a native OS file dialog (TinyFileDialogs) and returns
 *      the path chosen by the user, or null if cancelled.
 *   2. loadFromFile(Path) – reads the PNG, converts it to the Minecraft cape texture
 *      layout (64×32 RGBA), registers a DynamicTexture and returns its Identifier.
 *   3. The caller stores the path in config; the Identifier is returned to the mixin
 *      through ZeroCapesConfig.getCurrentCapeTexture().
 */
public final class CustomCapeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ZeroCapes/CustomCape");

    /** The identifier used to register the dynamic texture with Minecraft's TextureManager. */
    public static final Identifier CUSTOM_CAPE_TEXTURE_ID =
            Identifier.fromNamespaceAndPath("zerocapes", "dynamic/custom_cape");

    /** Standard Minecraft cape texture dimensions. */
    private static final int CAPE_TEX_W = 64;
    private static final int CAPE_TEX_H = 32;

    /** Region of the cape texture that shows on the back (outer) face – 10×16 at (1,1). */
    private static final int BACK_X = 1, BACK_Y = 1, BACK_W = 10, BACK_H = 16;
    /** Region for the front (inner) face – 10×16 at (12,1). */
    private static final int FRONT_X = 12, FRONT_Y = 1, FRONT_W = 10, FRONT_H = 16;

    /** Currently registered DynamicTexture, kept so we can close it on reload. */
    private static DynamicTexture currentTexture = null;

    /** Whether a valid custom cape is currently loaded. */
    private static boolean loaded = false;

    private CustomCapeManager() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Opens a native OS "Open File" dialog filtered to PNG files.
     *
     * Must be called from the main game thread.
     *
     * @return the selected file path, or {@code null} if the user cancelled.
     */
    public static String openFilePicker() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Build a PointerBuffer with our filter pattern "*.png"
            var filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();

            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "Open Custom Cape PNG",   // title
                    "",                        // default path
                    filters,                   // filter patterns
                    "PNG Images (*.png)",      // filter description
                    false                      // allow multi-select
            );
            return result; // null if cancelled
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to open file dialog", e);
            return null;
        }
    }

    /**
     * Loads a PNG from {@code filePath}, converts it to the 64×32 cape layout
     * and registers it as a DynamicTexture.
     *
     * @param filePath absolute path to the source PNG
     * @return the texture Identifier on success, or {@code null} on failure
     */
    public static Identifier loadFromFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            LOGGER.warn("[ZeroCapes] Custom cape file not found: {}", filePath);
            return null;
        }

        try (InputStream is = Files.newInputStream(path)) {
            NativeImage source = NativeImage.read(is);
            NativeImage capeImage = convertToCapeLayout(source);
            source.close();

            return registerTexture(capeImage);
        } catch (IOException e) {
            LOGGER.error("[ZeroCapes] Failed to load custom cape from {}", filePath, e);
            return null;
        }
    }

    /**
     * Returns whether a custom cape is currently loaded and active.
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Unloads the current custom cape (releases GPU texture) and deregisters it.
     */
    public static void unload() {
        if (currentTexture != null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().release(CUSTOM_CAPE_TEXTURE_ID);
            } catch (Exception ignored) {}
            try { currentTexture.close(); } catch (Exception ignored) {}
            currentTexture = null;
        }
        loaded = false;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Converts an arbitrary-size RGBA source image into the Minecraft 64×32 cape
     * texture layout.
     *
     * If the source is already exactly 64×32 it is used as-is (allowing advanced
     * users to provide a properly-formatted cape). Otherwise the image is scaled
     * to fit the visible back-face region (10×16) and mirrored onto the front-face.
     */
    private static NativeImage convertToCapeLayout(NativeImage src) {
        int sw = src.getWidth();
        int sh = src.getHeight();

        // If the source is already 64x32, use directly (advanced user)
        if (sw == CAPE_TEX_W && sh == CAPE_TEX_H) {
            NativeImage copy = new NativeImage(NativeImage.Format.RGBA, CAPE_TEX_W, CAPE_TEX_H, false);
            copy.copyFrom(src);
            return copy;
        }

        // Create a blank 64×32 canvas
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, CAPE_TEX_W, CAPE_TEX_H, true);
        // clear to transparent
        out.fillRect(0, 0, CAPE_TEX_W, CAPE_TEX_H, 0x00000000);

        // Scale source → back face region (10×16)
        blitScaled(src, out, BACK_X, BACK_Y, BACK_W, BACK_H);

        // Mirror source → front face region (10×16)
        blitScaledMirroredH(src, out, FRONT_X, FRONT_Y, FRONT_W, FRONT_H);

        // Also paint the edge-strips so the 3-D cape sides aren't transparent:
        // Top edge (1×BACK_W): copy average top row of source
        fillEdgeFromSource(src, out, BACK_X, 0, BACK_W, 1, false);
        // Bottom edge: at (11, 0): copy average bottom row of source
        fillEdgeFromSource(src, out, BACK_X + BACK_W + 1, 0, BACK_W, 1, true);
        // Left side strip (1×BACK_H) at (0, BACK_Y)
        fillSideStripFromSource(src, out, 0, BACK_Y, 1, BACK_H);
        // Right side strip (1×BACK_H) at (BACK_X+BACK_W, BACK_Y)
        fillSideStripFromSource(src, out, BACK_X + BACK_W, BACK_Y, 1, BACK_H);

        return out;
    }

    /** Blit a scaled copy of {@code src} into region (dx, dy, dw, dh) of {@code dst}. */
    private static void blitScaled(NativeImage src, NativeImage dst,
                                   int dx, int dy, int dw, int dh) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                int sx = (int) Math.floor((double) x / dw * sw);
                int sy = (int) Math.floor((double) y / dh * sh);
                sx = Math.min(sx, sw - 1);
                sy = Math.min(sy, sh - 1);
                dst.setPixel(dx + x, dy + y, toABGR(src.getPixel(sx, sy)));
            }
        }
    }

    /** Blit a horizontally-mirrored scaled copy into region (dx, dy, dw, dh) of {@code dst}. */
    private static void blitScaledMirroredH(NativeImage src, NativeImage dst,
                                            int dx, int dy, int dw, int dh) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                // mirror: map x → (dw-1-x)
                int sx = (int) Math.floor((double) (dw - 1 - x) / dw * sw);
                int sy = (int) Math.floor((double) y / dh * sh);
                sx = Math.min(sx, sw - 1);
                sy = Math.min(sy, sh - 1);
                dst.setPixel(dx + x, dy + y, toABGR(src.getPixel(sx, sy)));
            }
        }
    }

    /**
     * Fills a 1-pixel-tall edge strip in {@code dst} by sampling the top (or bottom)
     * row of {@code src}, scaled to the strip width.
     */
    private static void fillEdgeFromSource(NativeImage src, NativeImage dst,
                                           int dx, int dy, int dw, int dh,
                                           boolean fromBottom) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        int sy = fromBottom ? (sh - 1) : 0;
        for (int x = 0; x < dw; x++) {
            int sx = (int) Math.floor((double) x / dw * sw);
            sx = Math.min(sx, sw - 1);
            dst.setPixel(dx + x, dy, toABGR(src.getPixel(sx, sy)));
        }
    }

    /**
     * Fills a 1-pixel-wide side strip in {@code dst} by sampling the left column
     * of {@code src}, scaled to the strip height.
     */
    private static void fillSideStripFromSource(NativeImage src, NativeImage dst,
                                                int dx, int dy, int dw, int dh) {
        int sh = src.getHeight();
        int sx = 0;
        for (int y = 0; y < dh; y++) {
            int sy = (int) Math.floor((double) y / dh * sh);
            sy = Math.min(sy, sh - 1);
            dst.setPixel(dx, dy + y, toABGR(src.getPixel(sx, sy)));
        }
    }

    /**
     * NativeImage.getPixel() returns ABGR; NativeImage.setPixel() also expects ABGR.
     * The conversion is therefore a no-op – we just pass through. This helper exists
     * as an explicit reminder and to be swapped if the MC API changes.
     */
    private static int toABGR(int abgr) {
        return abgr;
    }

    /**
     * Registers (or replaces) the custom cape as a DynamicTexture with Minecraft's
     * TextureManager and returns the Identifier used to reference it.
     */
    private static Identifier registerTexture(NativeImage image) {
        Minecraft mc = Minecraft.getInstance();

        // Release the old texture first
        unload();

        DynamicTexture tex = new DynamicTexture(
                () -> "zerocapes:custom_cape",
                image
        );
        mc.getTextureManager().register(CUSTOM_CAPE_TEXTURE_ID, tex);
        currentTexture = tex;
        loaded = true;

        LOGGER.info("[ZeroCapes] Custom cape texture registered ({}x{}).",
                image.getWidth(), image.getHeight());
        return CUSTOM_CAPE_TEXTURE_ID;
    }
}
