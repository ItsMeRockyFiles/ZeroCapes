package com.toto.zerocapes.client.cape;

import com.mojang.blaze3d.platform.NativeImage;
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

public final class CustomCapeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ZeroCapes/CustomCape");

    public static final Identifier CUSTOM_CAPE_TEXTURE_ID =
            Identifier.fromNamespaceAndPath("zerocapes", "dynamic/custom_cape");

    private static final int CAPE_TEX_W = 64;
    private static final int CAPE_TEX_H = 32;

    private static final int BACK_X = 1, BACK_Y = 1, BACK_W = 10, BACK_H = 16;
    private static final int FRONT_X = 12, FRONT_Y = 1, FRONT_W = 10, FRONT_H = 16;

    private static DynamicTexture currentTexture = null;
    private static boolean loaded = false;

    private CustomCapeManager() {}

    public static String openFilePicker() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();

            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "Open Custom Cape PNG",
                    "",
                    filters,
                    "PNG Images (*.png)",
                    false
            );
            return result;
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to open file dialog", e);
            return null;
        }
    }

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

    public static boolean isLoaded() {
        return loaded;
    }

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


    private static NativeImage convertToCapeLayout(NativeImage src) {
        int sw = src.getWidth();
        int sh = src.getHeight();

        // If the source is already 64x32, use directly (advanced user)
        if (sw == CAPE_TEX_W && sh == CAPE_TEX_H) {
            NativeImage copy = new NativeImage(NativeImage.Format.RGBA, CAPE_TEX_W, CAPE_TEX_H, false);
            copy.copyFrom(src);
            return copy;
        }

        NativeImage out = new NativeImage(NativeImage.Format.RGBA, CAPE_TEX_W, CAPE_TEX_H, true);
        out.fillRect(0, 0, CAPE_TEX_W, CAPE_TEX_H, 0x00000000);

        // Scale source → back face region (10×16) with box filtering
        blitScaled(src, out, BACK_X, BACK_Y, BACK_W, BACK_H);

        // Mirror source → front face region (10×16) with box filtering
        blitScaledMirroredH(src, out, FRONT_X, FRONT_Y, FRONT_W, FRONT_H);

        // Edge strips so the 3-D sides aren't transparent
        fillEdgeFromSource(src, out, BACK_X, 0, BACK_W, 1, false);
        fillEdgeFromSource(src, out, BACK_X + BACK_W + 1, 0, BACK_W, 1, true);
        fillSideStripFromSource(src, out, 0, BACK_Y, 1, BACK_H);
        fillSideStripFromSource(src, out, BACK_X + BACK_W, BACK_Y, 1, BACK_H);

        return out;
    }

    private static void blitScaled(NativeImage src, NativeImage dst,
                                   int dx, int dy, int dw, int dh) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        for (int y = 0; y < dh; y++) {
            int sy0 = y * sh / dh;
            int sy1 = Math.max(sy0 + 1, (y + 1) * sh / dh);
            for (int x = 0; x < dw; x++) {
                int sx0 = x * sw / dw;
                int sx1 = Math.max(sx0 + 1, (x + 1) * sw / dw);

                long aSum = 0, bSum = 0, gSum = 0, rSum = 0;
                int count = 0;
                for (int sy = sy0; sy < sy1 && sy < sh; sy++) {
                    for (int sx = sx0; sx < sx1 && sx < sw; sx++) {
                        int p = src.getPixel(sx, sy);
                        aSum += (p >> 24) & 0xFF;
                        bSum += (p >> 16) & 0xFF;
                        gSum += (p >> 8) & 0xFF;
                        rSum += p & 0xFF;
                        count++;
                    }
                }
                if (count == 0) count = 1;
                int a = (int) (aSum / count);
                int b = (int) (bSum / count);
                int g = (int) (gSum / count);
                int r = (int) (rSum / count);
                dst.setPixel(dx + x, dy + y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }

    private static void blitScaledMirroredH(NativeImage src, NativeImage dst,
                                            int dx, int dy, int dw, int dh) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        for (int y = 0; y < dh; y++) {
            int sy0 = y * sh / dh;
            int sy1 = Math.max(sy0 + 1, (y + 1) * sh / dh);
            for (int x = 0; x < dw; x++) {
                int mx = dw - 1 - x;
                int sx0 = mx * sw / dw;
                int sx1 = Math.max(sx0 + 1, (mx + 1) * sw / dw);

                long aSum = 0, bSum = 0, gSum = 0, rSum = 0;
                int count = 0;
                for (int sy = sy0; sy < sy1 && sy < sh; sy++) {
                    for (int sx = sx0; sx < sx1 && sx < sw; sx++) {
                        int p = src.getPixel(sx, sy);
                        aSum += (p >> 24) & 0xFF;
                        bSum += (p >> 16) & 0xFF;
                        gSum += (p >> 8) & 0xFF;
                        rSum += p & 0xFF;
                        count++;
                    }
                }
                if (count == 0) count = 1;
                int a = (int) (aSum / count);
                int b = (int) (bSum / count);
                int g = (int) (gSum / count);
                int r = (int) (rSum / count);
                dst.setPixel(dx + x, dy + y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }

    private static void fillEdgeFromSource(NativeImage src, NativeImage dst,
                                           int dx, int dy, int dw, int dh,
                                           boolean fromBottom) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        int sy = fromBottom ? (sh - 1) : 0;
        for (int x = 0; x < dw; x++) {
            int sx = (int) Math.floor((double) x / dw * sw);
            sx = Math.min(sx, sw - 1);
            dst.setPixel(dx + x, dy, src.getPixel(sx, sy));
        }
    }

    private static void fillSideStripFromSource(NativeImage src, NativeImage dst,
                                                int dx, int dy, int dw, int dh) {
        int sh = src.getHeight();
        int sx = 0;
        for (int y = 0; y < dh; y++) {
            int sy = (int) Math.floor((double) y / dh * sh);
            sy = Math.min(sy, sh - 1);
            dst.setPixel(dx, dy + y, src.getPixel(sx, sy));
        }
    }

    private static Identifier registerTexture(NativeImage image) {
        Minecraft mc = Minecraft.getInstance();

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