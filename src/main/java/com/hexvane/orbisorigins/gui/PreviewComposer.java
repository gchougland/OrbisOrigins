package com.hexvane.orbisorigins.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

/**
 * Builds one model and one texture from a species body and its default attachments (eyes, fur
 * tufts), so a single preview item can show the whole creature.
 *
 * <p>Attachments join a body the way the client joins them: each top-level node of the attachment
 * names a node of the body, and its children are hung under that node. The attachment textures are
 * stacked under the body texture, and the attachment faces' texture offsets move down with them.
 * The results are registered as common assets made in memory: the model under
 * OrbisOrigins/Previews/, the texture under Items/OrbisOrigins/Previews/ (see ATLAS_FOLDERS).
 */
final class PreviewComposer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PACK = "Hexvane:OrbisOrigins";
    private static final String DIR = "OrbisOrigins/Previews/";
    /**
     * Item textures must live under one of these folders: the client packs only those into the
     * entity atlas, and any other item texture draws garbled ("Cannot use ... as texture").
     */
    private static final String[] ATLAS_FOLDERS = {"Characters/", "NPC/", "NPCs/", "Items/", "Consumable/", "Resources/", "VFX/", "Trailer/"};
    private static final String TEXTURE_DIR = "Items/OrbisOrigins/Previews/";
    private static final Gson GSON = new Gson();

    private PreviewComposer() {}

    /**
     * @param parts model, texture, then attachment model/texture pairs
     * @return the merged model and texture paths, or null when nothing could be merged
     */
    @Nullable
    static String[] compose(@Nonnull String id, @Nonnull String[] parts) {
        try {
            JsonObject body = json(parts[0]);
            BufferedImage bodyTex = parts[1] == null ? null : image(parts[1]);
            if (body == null || bodyTex == null || !body.has("nodes")) return null;

            Map<String, JsonObject> byName = new HashMap<>();
            int[] maxId = {0};
            for (JsonElement n : body.getAsJsonArray("nodes")) index(n.getAsJsonObject(), byName, maxId);

            BufferedImage[] sheets = new BufferedImage[parts.length / 2];
            sheets[0] = bodyTex;
            int y = bodyTex.getHeight();
            int width = bodyTex.getWidth();
            boolean added = false;
            for (int i = 2; i + 1 < parts.length; i += 2) {
                JsonObject att = json(parts[i]);
                BufferedImage tex = image(parts[i + 1]);
                if (att == null || tex == null || !att.has("nodes")) continue;
                boolean used = false;
                for (JsonElement top : att.getAsJsonArray("nodes")) {
                    JsonObject t = top.getAsJsonObject();
                    JsonObject target = t.has("name") ? byName.get(t.get("name").getAsString()) : null;
                    if (target == null || !t.has("children")) continue;
                    JsonArray into = target.has("children") ? target.getAsJsonArray("children") : new JsonArray();
                    for (JsonElement c : t.getAsJsonArray("children")) {
                        JsonObject copy = c.getAsJsonObject().deepCopy();
                        shift(copy, y, maxId);
                        into.add(copy);
                        used = true;
                    }
                    target.add("children", into);
                }
                if (used) {
                    sheets[i / 2] = tex;
                    y += tex.getHeight();
                    width = Math.max(width, tex.getWidth());
                    added = true;
                }
            }
            if (!added) return null;

            // Texture sizes in multiples of 32, as the client expects.
            BufferedImage sheet = new BufferedImage(roundUp(width), roundUp(y), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            int at = 0;
            for (BufferedImage s : sheets) {
                if (s == null) continue;
                g.drawImage(s, 0, at, null);
                at += s.getHeight();
            }
            g.dispose();

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(sheet, "png", png);
            String model = DIR + id + ".blockymodel";
            String texture = TEXTURE_DIR + id + ".png";
            register(model, GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
            register(texture, png.toByteArray());
            return new String[] {model, texture};
        } catch (IOException | RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not build the preview of %s", parts[0]);
            return null;
        }
    }

    /** True when the client will pack this texture for items. */
    static boolean inAtlasFolder(@Nullable String texture) {
        if (texture == null) return false;
        for (String f : ATLAS_FOLDERS) {
            if (texture.startsWith(f)) return true;
        }
        return false;
    }

    /** A copy of a texture under an atlas folder, for item textures kept elsewhere (Cosmetics/). */
    @Nullable
    static String relocate(@Nonnull String id, @Nonnull String texture) {
        byte[] data = bytes(texture);
        if (data == null) return null;
        String path = TEXTURE_DIR + id + ".png";
        register(path, data);
        return path;
    }

    private static int roundUp(int v) {
        return Math.max(32, (v + 31) / 32 * 32);
    }

    private static void index(@Nonnull JsonObject node, @Nonnull Map<String, JsonObject> byName, @Nonnull int[] maxId) {
        if (node.has("name")) byName.putIfAbsent(node.get("name").getAsString(), node);
        maxId[0] = Math.max(maxId[0], idOf(node));
        if (node.has("children")) {
            for (JsonElement c : node.getAsJsonArray("children")) index(c.getAsJsonObject(), byName, maxId);
        }
    }

    private static int idOf(@Nonnull JsonObject node) {
        try {
            return node.has("id") ? Integer.parseInt(node.get("id").getAsString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Moves a copied node's texture offsets down by dy and gives it and its children fresh ids. */
    private static void shift(@Nonnull JsonObject node, int dy, @Nonnull int[] maxId) {
        node.addProperty("id", String.valueOf(++maxId[0]));
        if (node.has("shape") && node.get("shape").isJsonObject()) {
            JsonObject shape = node.getAsJsonObject("shape");
            if (shape.has("textureLayout") && shape.get("textureLayout").isJsonObject()) {
                for (Map.Entry<String, JsonElement> face : shape.getAsJsonObject("textureLayout").entrySet()) {
                    if (!face.getValue().isJsonObject()) continue;
                    JsonObject f = face.getValue().getAsJsonObject();
                    if (f.has("offset") && f.get("offset").isJsonObject()) {
                        JsonObject o = f.getAsJsonObject("offset");
                        // The client reads texture offsets as whole numbers: a "160.0" fails the
                        // whole model, and with it the client's item icon atlas.
                        long oy = o.has("y") ? Math.round(o.get("y").getAsDouble()) : 0L;
                        o.addProperty("y", oy + dy);
                    }
                }
            }
        }
        if (node.has("children")) {
            for (JsonElement c : node.getAsJsonArray("children")) shift(c.getAsJsonObject(), dy, maxId);
        }
    }

    @Nullable
    private static byte[] bytes(@Nonnull String path) {
        CommonAsset asset = CommonAssetRegistry.getByName(path);
        return asset == null ? null : asset.getBlob().join();
    }

    @Nullable
    private static JsonObject json(@Nonnull String path) {
        byte[] b = bytes(path);
        if (b == null) return null;
        String text = new String(b, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Nullable
    private static BufferedImage image(@Nonnull String path) throws IOException {
        byte[] b = bytes(path);
        return b == null ? null : ImageIO.read(new ByteArrayInputStream(b));
    }

    private static void register(@Nonnull String name, @Nonnull byte[] data) {
        CommonAssetModule.get().addCommonAsset(PACK, new MemoryAsset(name, data), false);
    }

    /** A common asset whose bytes live in memory. */
    private static final class MemoryAsset extends CommonAsset {
        private final byte[] data;

        MemoryAsset(@Nonnull String name, @Nonnull byte[] data) {
            super(name, data);
            this.data = data;
        }

        @Nonnull
        @Override
        protected CompletableFuture<byte[]> getBlob0() {
            return CompletableFuture.completedFuture(data);
        }

        @Nonnull
        protected MemorySegment readBlobPrefix(int length) {
            return MemorySegment.ofArray(Arrays.copyOf(data, Math.min(length, data.length)));
        }
    }
}
