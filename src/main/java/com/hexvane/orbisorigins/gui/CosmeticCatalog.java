package com.hexvane.orbisorigins.gui;

import com.hexvane.orbisorigins.species.AttachmentOption;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.species.SpeciesVariantData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What the species page offers for one species variant, shaped for the editor: categories (Body
 * first, then each attachment slot), each holding styles, each style holding its colour choices.
 *
 * <p>Options of a slot that share a model and are all named "Style - Colour" with the same style
 * become one style with colour swatches (a Trork's 86 hair options are 4 haircuts in 21 colours);
 * every other option is a style of its own with a single choice.
 */
public final class CosmeticCatalog {
    public static final String BODY = "__body";

    public record Choice(@Nullable String key, @Nonnull String label, @Nullable String texture, @Nullable String previewItem, int variantIndex) {}

    public record Style(@Nonnull String name, @Nonnull List<Choice> choices) {
        @Nullable
        public String previewItem() {
            return choices.isEmpty() ? null : choices.get(0).previewItem();
        }
    }

    public record Category(@Nonnull String key, @Nonnull String label, @Nonnull String iconDoc, boolean allowsNone, @Nonnull List<Style> styles) {
        public boolean isBody() {
            return BODY.equals(key);
        }

        public int choiceCount() {
            int n = 0;
            for (Style s : styles) n += s.choices().size();
            return n;
        }
    }

    private CosmeticCatalog() {}

    @Nonnull
    public static List<Category> build(@Nonnull SpeciesData species, int variantIndex, @Nonnull String bodyLabel) {
        List<Category> out = new ArrayList<>();
        if (species.usesPlayerModel()) return out;

        Category body = bodyCategory(species, bodyLabel);
        if (body != null) out.add(body);

        if (species.isVersion2()) {
            for (String slot : species.getAttachmentSlotNames(variantIndex)) {
                Map<String, AttachmentOption> options = species.getAttachmentOptions(variantIndex, slot);
                if (options.isEmpty()) continue;
                out.add(new Category(slot, pretty(slot), iconFor(slot), species.attachmentAllowsNone(variantIndex, slot), group(options)));
            }
        } else {
            String modelName = species.getModelName(variantIndex);
            Map<String, Map<String, AttachmentOption>> slots = SpeciesRegistry.getAvailableAttachments(species, variantIndex, modelName);
            for (Map.Entry<String, Map<String, AttachmentOption>> e : slots.entrySet()) {
                Map<String, AttachmentOption> options = new LinkedHashMap<>(e.getValue());
                boolean allowsNone = options.remove("null") != null || options.containsKey(null);
                options.remove(null);
                if (options.isEmpty()) continue;
                out.add(new Category(e.getKey(), pretty(e.getKey()), iconFor(e.getKey()), allowsNone, group(options)));
            }
        }
        return out;
    }

    @Nullable
    private static Category bodyCategory(@Nonnull SpeciesData species, @Nonnull String label) {
        int variants = species.getVariantCount();
        List<Style> styles = new ArrayList<>();
        int textures = 0;
        for (int vi = 0; vi < variants; vi++) {
            List<Choice> choices = new ArrayList<>();
            String name;
            if (species.isVersion2()) {
                SpeciesVariantData v = species.getVariantData(vi);
                if (v == null) continue;
                name = v.getVariantName() != null ? v.getVariantName() : pretty(v.getParentModel());
                List<String> labels = textureLabels(v.getTextures());
                for (int ti = 0; ti < v.getTextures().size(); ti++) {
                    String t = v.getTextures().get(ti);
                    String skin = v.getTextures().size() == 1 ? name : labels.get(ti);
                    choices.add(new Choice(t, skin, t, DisplayItems.forSpecies(species, vi, t), vi));
                }
                if (choices.isEmpty()) {
                    choices.add(new Choice(null, name, null, DisplayItems.forSpecies(species, vi, null), vi));
                }
            } else {
                name = pretty(species.getModelName(vi));
                choices.add(new Choice(null, name, null, DisplayItems.forSpecies(species, vi, null), vi));
            }
            textures += choices.size();
            styles.add(new Style(name, choices));
        }
        if (styles.size() <= 1 && textures <= 1) return null;
        return new Category(BODY, label, "BodyCharacteristic", false, styles);
    }

    @Nonnull
    private static List<Style> group(@Nonnull Map<String, AttachmentOption> options) {
        // model -> keys in order of first appearance
        Map<String, List<String>> byModel = new LinkedHashMap<>();
        for (Map.Entry<String, AttachmentOption> e : options.entrySet()) {
            if (e.getValue() == null) continue;
            byModel.computeIfAbsent(e.getValue().getModel(), k -> new ArrayList<>()).add(e.getKey());
        }
        List<Style> styles = new ArrayList<>();
        for (List<String> keys : byModel.values()) {
            String prefix = null;
            boolean shared = keys.size() > 1;
            for (String k : keys) {
                String[] split = split(nameOf(options.get(k), k));
                if (split == null || (prefix != null && !prefix.equals(split[0]))) {
                    shared = false;
                    break;
                }
                prefix = split[0];
            }
            if (shared) {
                List<Choice> choices = new ArrayList<>();
                for (String k : keys) {
                    AttachmentOption o = options.get(k);
                    String[] split = split(nameOf(o, k));
                    choices.add(new Choice(k, split[1], o.getTexture(), DisplayItems.forAttachment(o), 0));
                }
                styles.add(new Style(prefix, choices));
            } else {
                for (String k : keys) {
                    AttachmentOption o = options.get(k);
                    String name = nameOf(o, k);
                    styles.add(new Style(name, List.of(new Choice(k, name, o.getTexture(), DisplayItems.forAttachment(o), 0))));
                }
            }
        }
        return styles;
    }

    @Nonnull
    private static String nameOf(@Nonnull AttachmentOption o, @Nonnull String key) {
        String n = o.getDisplayName();
        return n != null && !n.isBlank() ? n : pretty(key);
    }

    @Nullable
    private static String[] split(@Nonnull String name) {
        int i = name.indexOf(" - ");
        if (i <= 0 || i + 3 >= name.length()) return null;
        return new String[] {name.substring(0, i).trim(), name.substring(i + 3).trim()};
    }

    @Nonnull
    static String textureLabel(@Nonnull String texturePath) {
        String file = texturePath.substring(texturePath.lastIndexOf('/') + 1);
        if (file.toLowerCase(Locale.ROOT).endsWith(".png")) file = file.substring(0, file.length() - 4);
        return pretty(file);
    }

    /**
     * Names for a set of skin textures: the words every file shares ("Model_" in Model_Foxy,
     * Model_Ginger) are dropped, as are filler words, leaving "Foxy" and "Ginger".
     */
    @Nonnull
    static List<String> textureLabels(@Nonnull List<String> texturePaths) {
        List<String[]> words = new ArrayList<>();
        for (String t : texturePaths) words.add(textureLabel(t).split(" "));
        int common = 0;
        if (words.size() > 1) {
            outer:
            while (true) {
                for (String[] w : words) {
                    if (w.length <= common + 1 || !w[common].equalsIgnoreCase(words.get(0)[common])) break outer;
                }
                common++;
            }
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            List<String> kept = new ArrayList<>();
            String[] w = words.get(i);
            for (int j = common; j < w.length; j++) {
                String s = w[j].toLowerCase(Locale.ROOT);
                if (w.length - common > 1 && (s.equals("model") || s.equals("texture") || s.equals("skin") || s.equals("greyscale"))) continue;
                kept.add(w[j]);
            }
            String label = String.join(" ", kept).trim();
            out.add(label.isEmpty() || label.equalsIgnoreCase("texture") ? "#" + (i + 1) : label);
        }
        return out;
    }

    @Nonnull
    static String pretty(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').trim();
        StringBuilder b = new StringBuilder(s.length());
        boolean up = true;
        for (char c : s.toCharArray()) {
            b.append(up ? Character.toUpperCase(c) : c);
            up = c == ' ';
        }
        return b.toString();
    }

    /** Which vanilla avatar-editor icon (a document in OrbisOrigins/Cat/) represents a slot. */
    @Nonnull
    static String iconFor(@Nonnull String slot) {
        String s = slot.toLowerCase(Locale.ROOT);
        if (s.contains("hair")) return "Haircut";
        if (s.contains("beard") || s.contains("mustache") || s.contains("moustache")) return "FacialHair";
        if (s.contains("eyebrow")) return "Eyebrows";
        if (s.contains("eyelash") || s.contains("eye")) return "Eyes";
        if (s.contains("glass") || s.contains("mask") || s.contains("pipe")) return "FaceAccessory";
        if (s.contains("mouth") || s.contains("teeth") || s.contains("tusk") || s.contains("jaw") || s.contains("fang")) return "Mouth";
        if (s.contains("ear")) return "Ears";
        if (s.contains("face")) return "Face";
        if (s.contains("head") || s.contains("hat") || s.contains("helm") || s.contains("horn")) return "HeadAccessory";
        if (s.contains("neck") || s.contains("jewel") || s.contains("necklace")) return "EarAccessory";
        if (s.contains("cape") || s.contains("cloak") || s.contains("back") || s.contains("wing")) return "Cape";
        if (s.contains("hand") || s.contains("glove") || s.contains("arm")) return "Gloves";
        if (s.contains("shoe") || s.contains("feet") || s.contains("foot") || s.contains("boot")) return "Shoes";
        if (s.contains("leg") || s.contains("pant")) return "Pants";
        if (s.contains("pelvis") || s.contains("belt") || s.contains("waist")) return "Overpants";
        if (s.contains("shoulder")) return "Torso";
        if (s.contains("chest") || s.contains("cloth") || s.contains("outfit") || s.contains("shirt") || s.contains("top")) return "Overtop";
        if (s.contains("tail") || s.contains("body")) return "BodyCharacteristic";
        return "Various";
    }
}
