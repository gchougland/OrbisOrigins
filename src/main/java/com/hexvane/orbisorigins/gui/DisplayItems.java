package com.hexvane.orbisorigins.gui;

import com.hexvane.orbisorigins.species.AttachmentOption;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.species.SpeciesVariantData;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Hidden "display items" that let the species page draw cosmetics and species as live 3D previews
 * (ItemPreviewComponent shows an item's model). One item per model and texture, created in code
 * from the species definitions, so species loaded from any pack or JSON file get them too.
 *
 * <p>The items are marked as variants and belong to no creative category, so the creative library
 * leaves them out unless a player turns on showing variants. Nothing gives them to players.
 */
public final class DisplayItems {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PREFIX = "OrbisOrigins_Preview_";
    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();
    /** Species definitions already handled; a reload makes new objects, so they are handled again. */
    private static final Set<SpeciesData> DONE = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private DisplayItems() {}

    /** The display item id for a model and texture (both paths relative to Common/). */
    @Nonnull
    public static String idFor(@Nonnull String model, @Nullable String texture) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha.digest((model + "|" + (texture == null ? "" : texture)).getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The display item for a cosmetic, or null when it cannot be shown. */
    @Nullable
    public static String forAttachment(@Nonnull AttachmentOption option) {
        String id = idFor(option.getModel(), option.getTexture());
        return REGISTERED.contains(id) ? id : null;
    }

    /** The display item for a species variant's body, or null when it cannot be shown. */
    @Nullable
    public static String forSpecies(@Nonnull SpeciesData species, int variantIndex, @Nullable String texture) {
        String[] body = bodyOf(species, variantIndex, texture);
        if (body == null) return null;
        String id = idFor(body[0], body[1]);
        return REGISTERED.contains(id) ? id : null;
    }

    /** Model and texture of a species variant's body, or null for species drawn with the player's own skin. */
    @Nullable
    static String[] bodyOf(@Nonnull SpeciesData species, int variantIndex, @Nullable String texture) {
        if (species.usesPlayerModel()) return null;
        if (species.isVersion2()) {
            SpeciesVariantData v = species.getVariantData(variantIndex);
            if (v == null) return null;
            String tex = texture;
            if (tex == null && !v.getTextures().isEmpty()) tex = v.getTextures().get(0);
            if (tex == null) {
                ModelAsset parent = ModelAsset.getAssetMap().getAsset(v.getParentModel());
                if (parent != null) tex = parent.getTexture();
            }
            return new String[] {v.getModel(), tex};
        }
        String modelName = species.getModelName(variantIndex);
        if (modelName == null) return null;
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(modelName);
        if (asset == null || asset.getModel() == null) return null;
        return new String[] {asset.getModel(), asset.getTexture()};
    }

    /**
     * A body plus the species' default attachments (eyes, fur tufts) as extra model/texture pairs:
     * the preview item has one model, so they are merged into it (see {@link PreviewComposer}).
     */
    @Nullable
    private static String[] withParts(@Nullable String[] body, @Nonnull SpeciesData species, int variantIndex) {
        if (body == null) return null;
        List<String> out = new ArrayList<>(List.of(body[0], body[1] == null ? "" : body[1]));
        if (species.isVersion2()) {
            SpeciesVariantData v = species.getVariantData(variantIndex);
            if (v != null) {
                for (SpeciesVariantData.DefaultAttachmentDef a : v.getDefaultAttachments()) {
                    out.add(a.getModel());
                    out.add(a.getTexture());
                }
            }
        } else {
            ModelAsset asset = ModelAsset.getAssetMap().getAsset(species.getModelName(variantIndex));
            if (asset != null && asset.getDefaultAttachments() != null) {
                for (com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment a : asset.getDefaultAttachments()) {
                    if (a.getModel() != null && a.getTexture() != null && a.getGradientSet() == null) {
                        out.add(a.getModel());
                        out.add(a.getTexture());
                    }
                }
            }
        }
        if (body[1] == null) out.set(1, null);
        return out.toArray(new String[0]);
    }

    /** Registers the display items of every loaded species. Safe to call again after a reload. */
    public static void syncAll() {
        for (SpeciesData species : SpeciesRegistry.getAllSpecies()) {
            ensure(species);
        }
    }

    /** Registers whatever display items this species still lacks. */
    public static void ensure(@Nonnull SpeciesData species) {
        synchronized (DONE) {
            if (DONE.contains(species)) return;
        }
        Map<String, String[]> wanted = new java.util.LinkedHashMap<>();
        boolean unresolved = false; // a body whose model asset has not loaded yet
        int variants = Math.max(1, species.getVariantCount());
        for (int vi = 0; vi < variants; vi++) {
            if (species.isVersion2()) {
                SpeciesVariantData v = species.getVariantData(vi);
                if (v != null) {
                    List<String> textures = v.getTextures().isEmpty() ? java.util.Collections.singletonList(null) : v.getTextures();
                    for (String t : textures) unresolved |= !wantBody(wanted, withParts(bodyOf(species, vi, t), species, vi));
                }
            } else if (!species.usesPlayerModel()) {
                unresolved |= !wantBody(wanted, withParts(bodyOf(species, vi, null), species, vi));
            }
            String modelName = species.isVersion2() ? null : species.getModelName(vi);
            Map<String, Map<String, AttachmentOption>> slots = SpeciesRegistry.getAvailableAttachments(species, vi, modelName);
            for (Map<String, AttachmentOption> options : slots.values()) {
                for (AttachmentOption o : options.values()) {
                    if (o != null && o.getModel() != null && !o.getModel().isEmpty()) {
                        want(wanted, new String[] {o.getModel(), o.getTexture()});
                    }
                }
            }
        }
        List<Item> missing = new ArrayList<>();
        for (Map.Entry<String, String[]> e : wanted.entrySet()) {
            if (REGISTERED.contains(e.getKey())) continue;
            if (Item.getAssetMap().getAsset(e.getKey()) == null) {
                String[] v = e.getValue();
                String[] merged = v.length > 2 ? PreviewComposer.compose(e.getKey(), v) : null;
                if (merged != null) {
                    missing.add(new DisplayItem(e.getKey(), merged[0], merged[1]));
                } else {
                    String tex = v[1];
                    if (tex != null && !PreviewComposer.inAtlasFolder(tex)) {
                        String copy = PreviewComposer.relocate(e.getKey(), tex);
                        if (copy != null) tex = copy;
                    }
                    missing.add(new DisplayItem(e.getKey(), v[0], tex));
                }
            }
        }
        if (!missing.isEmpty()) {
            try {
                Item.getAssetStore().loadAssets(DefaultAssetMap.DEFAULT_PACK_KEY, missing);
                long unframed = missing.stream().filter(i -> i.getIconProperties() == null).count();
                LOGGER.atInfo().log("Registered %d preview item(s) for species %s (%d unframed)", missing.size(), species.getId(), unframed);
            } catch (RuntimeException ex) {
                LOGGER.atWarning().withCause(ex).log("Could not register preview items for species %s", species.getId());
                return;
            }
        }
        boolean complete = true;
        for (String id : wanted.keySet()) {
            if (Item.getAssetMap().getAsset(id) != null) {
                REGISTERED.add(id);
            } else {
                complete = false;
            }
        }
        // Model assets can load after the species: only stop looking once every body resolved.
        if (complete && !unresolved) {
            synchronized (DONE) {
                DONE.add(species);
            }
        }
    }

    /** Adds a body preview; false when its model or texture cannot be resolved yet. */
    private static boolean wantBody(@Nonnull Map<String, String[]> wanted, @Nullable String[] body) {
        if (body == null || body[0] == null || body[1] == null) return false;
        want(wanted, body);
        return true;
    }

    private static void want(@Nonnull Map<String, String[]> wanted, @Nullable String[] modelAndTexture) {
        if (modelAndTexture == null || modelAndTexture[0] == null) return;
        wanted.putIfAbsent(idFor(modelAndTexture[0], modelAndTexture[1]), modelAndTexture);
    }

    // ---------- Framing ----------
    // The client's ItemPreviewComponent draws an item with its IconProperties and never fits it to
    // the tile itself: screen = (Ry*Rx*Rz * v + T) * Scale * ItemScale * item.Scale / 32, in
    // fractions of the tile's width, centred, orthographic, cropped at the tile (read from the
    // 0.7.5 client). So each display item carries properties that centre its own model.

    /** Viewing angle in degrees: a little from above and turned a third of the way to its left. */
    private static final float PITCH = 10f;
    private static final float YAW = 30f;
    /** Share of the tile the model's larger side fills (by measure; the client drew 0.86 about 10% larger). Tiles are square and use ItemScale 1. */
    private static final float FILL = 0.74f;

    @Nullable
    static AssetIconProperties framing(@Nonnull String model) {
        float[] box = projectedBounds(model);
        if (box == null) return null;
        float w = box[2] - box[0];
        float h = box[3] - box[1];
        float size = Math.max(w, h);
        if (size <= 0.01f) return null;
        float scale = FILL * 32f / size;
        Vector2f t = new Vector2f(-(box[0] + box[2]) / 2f, -(box[1] + box[3]) / 2f);
        return new AssetIconProperties(scale, t, new Vector3f(PITCH, YAW, 0f));
    }

    /** minX, minY, maxX, maxY of a blockymodel's corners as the preview camera sees them. */
    @Nullable
    private static float[] projectedBounds(@Nonnull String model) {
        try {
            CommonAsset asset = CommonAssetRegistry.getByName(model);
            if (asset == null) return null;
            byte[] bytes = asset.getBlob().join();
            if (bytes == null) return null;
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            Quaternionf view = new Quaternionf()
                    .rotateY((float) Math.toRadians(YAW))
                    .rotateX((float) Math.toRadians(PITCH));
            float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            JsonArray nodes = root.has("nodes") ? root.getAsJsonArray("nodes") : null;
            if (nodes == null) return null;
            for (JsonElement n : nodes) walk(n.getAsJsonObject(), new Vector3f(), new Quaternionf(), view, box);
            return box[0] <= box[2] ? box : null;
        } catch (RuntimeException e) {
            LOGGER.atFine().withCause(e).log("Could not measure model %s", model);
            return null;
        }
    }

    private static void walk(@Nonnull JsonObject node, @Nonnull Vector3f parentPos, @Nonnull Quaternionf parentRot,
                             @Nonnull Quaternionf view, @Nonnull float[] box) {
        Vector3f local = vec(node.getAsJsonObject("position"), 0f);
        Vector3f pos = parentRot.transform(local, new Vector3f()).add(parentPos);
        Quaternionf rot = new Quaternionf(parentRot).mul(quat(node.getAsJsonObject("orientation")));
        JsonObject shape = node.has("shape") && node.get("shape").isJsonObject() ? node.getAsJsonObject("shape") : null;
        if (shape != null && shape.has("type")) {
            String type = shape.get("type").getAsString();
            if (type.equals("box") || type.equals("quad")) {
                JsonObject settings = shape.has("settings") ? shape.getAsJsonObject("settings") : null;
                Vector3f size = vec(settings != null ? settings.getAsJsonObject("size") : null, 0f);
                Vector3f stretch = vec(shape.getAsJsonObject("stretch"), 1f);
                Vector3f offset = vec(shape.getAsJsonObject("offset"), 0f);
                size.mul(stretch).mul(0.5f);
                Vector3f p = new Vector3f();
                for (int i = 0; i < 8; i++) {
                    p.set(offset.x + ((i & 1) == 0 ? -size.x : size.x),
                          offset.y + ((i & 2) == 0 ? -size.y : size.y),
                          offset.z + ((i & 4) == 0 ? -size.z : size.z));
                    rot.transform(p).add(pos);
                    view.transform(p);
                    box[0] = Math.min(box[0], p.x);
                    box[1] = Math.min(box[1], p.y);
                    box[2] = Math.max(box[2], p.x);
                    box[3] = Math.max(box[3], p.y);
                }
            }
        }
        if (node.has("children") && node.get("children").isJsonArray()) {
            for (JsonElement c : node.getAsJsonArray("children")) {
                if (c.isJsonObject()) walk(c.getAsJsonObject(), pos, rot, view, box);
            }
        }
    }

    @Nonnull
    private static Vector3f vec(@Nullable JsonObject o, float fallback) {
        if (o == null) return new Vector3f(fallback);
        return new Vector3f(num(o, "x", fallback), num(o, "y", fallback), num(o, "z", fallback));
    }

    @Nonnull
    private static Quaternionf quat(@Nullable JsonObject o) {
        if (o == null) return new Quaternionf();
        return new Quaternionf(num(o, "x", 0f), num(o, "y", 0f), num(o, "z", 0f), num(o, "w", 1f));
    }

    private static float num(@Nonnull JsonObject o, @Nonnull String key, float fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsFloat() : fallback;
    }

    /** A copy of the engine's own fallback item with another model: complete enough for the client. */
    private static final class DisplayItem extends Item {
        DisplayItem(@Nonnull String id, @Nonnull String model, @Nullable String texture) {
            super(Item.UNKNOWN);
            this.id = id;
            this.model = model;
            this.texture = texture != null ? texture : Item.UNKNOWN_TEXTURE;
            this.variant = true;
            this.categories = null;
            this.maxStack = 1;
            this.scale = 1f;
            this.iconProperties = framing(model);
        }
    }
}
