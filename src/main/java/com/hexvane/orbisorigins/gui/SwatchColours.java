package com.hexvane.orbisorigins.gui;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

/**
 * The colour a texture reads as, for colour swatches: read from the texture itself (any pack's),
 * so swatches work for every species without a hand-made colour table.
 */
public final class SwatchColours {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FALLBACK = "#7a8696";
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private SwatchColours() {}

    /** "#rrggbb" for a texture path relative to Common/. */
    @Nonnull
    public static String of(@Nonnull String texturePath) {
        return CACHE.computeIfAbsent(texturePath, SwatchColours::compute);
    }

    @Nonnull
    private static String compute(@Nonnull String path) {
        try {
            CommonAsset asset = CommonAssetRegistry.getByName(path);
            if (asset == null) return FALLBACK;
            byte[] bytes = asset.getBlob().join();
            if (bytes == null) return FALLBACK;
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return FALLBACK;
            int w = img.getWidth(), h = img.getHeight();
            int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
            int n = 0;
            long[] keyed = new long[argb.length];
            for (int p : argb) {
                if ((p >>> 24) < 200) continue;
                int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
                long luma = (r * 299L + g * 587L + b * 114L);
                keyed[n++] = (luma << 24) | (p & 0xffffffL);
            }
            if (n == 0) return FALLBACK;
            // Average the middle half by brightness: skips outlines and highlights.
            Arrays.sort(keyed, 0, n);
            int from = n / 4, to = Math.max(from + 1, (3 * n) / 4);
            long r = 0, g = 0, b = 0;
            for (int i = from; i < to; i++) {
                int p = (int) (keyed[i] & 0xffffff);
                r += (p >> 16) & 0xff;
                g += (p >> 8) & 0xff;
                b += p & 0xff;
            }
            int c = to - from;
            return String.format("#%02x%02x%02x", r / c, g / c, b / c);
        } catch (Exception e) {
            LOGGER.at(Level.FINE).withCause(e).log("No swatch colour for %s", path);
            return FALLBACK;
        }
    }
}
