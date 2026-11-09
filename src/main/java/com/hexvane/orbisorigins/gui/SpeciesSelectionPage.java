package com.hexvane.orbisorigins.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.OrbisOriginsPlugin;
import com.hexvane.orbisorigins.ability.AbilityApiBridge;
import com.hexvane.orbisorigins.species.SpeciesAbilityConfig;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hexvane.orbisorigins.util.SpeciesCommandUtil;
import com.hexvane.orbisorigins.util.SpeciesStatUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The species selection page ("Avatar Studio"): species rail, category icons and a grid of live 3D
 * cosmetic previews with colour swatches on the left, the in-world preview entity in the open
 * middle, and a character sheet (attributes, resistances, abilities, starter kit) on the right.
 *
 * <p>Live previews are ItemPreviewComponents showing {@link DisplayItems}. The client allows at most
 * 32 at once: the style grid is paged ({@value #STYLES_PER_PAGE} at a time), the sheet has one
 * portrait, and the scrolling species rail draws at most {@value #SPECIES_PREVIEWS} species in 3D
 * (any further species, from extra packs, get a name tile instead).
 */
public class SpeciesSelectionPage extends InteractiveCustomUIPage<SpeciesSelectionPage.SpeciesEventData> {
    private static final String PAGE = "OrbisOrigins/SpeciesPage.ui";
    private static final int SPECIES_PREVIEWS = 18;
    private static final int STYLES_PER_PAGE = 12;
    /** Sideways shift of the in-world preview, as a share of its distance from the player. */
    private static final double PREVIEW_SIDE_SHIFT = 0.21;
    private static final int BAR_WIDTH = 200;
    private static final String LANG = "server.customUI.orbisOrigins.";
    /** Species ids of the rail's tiles, in order. */
    private final List<String> railIds = new java.util.ArrayList<>();

    private final World world;
    private final PlayerRef playerRef;
    @Nullable
    private String selectedSpeciesId;
    private final Map<String, Integer> variantIndices = new HashMap<>(); // species ID -> variant index
    private final Map<String, Map<String, String>> attachmentSelections = new HashMap<>(); // species ID -> slot -> option key ("null" = none)
    private final Map<String, String> textureSelections = new HashMap<>(); // species ID -> body texture (v2 only)
    private final Map<String, String> categorySelections = new HashMap<>(); // species ID -> category key
    private boolean initialised;
    private int stylePage;
    private String searchQuery = "";
    @Nullable
    private Ref<EntityStore> modelPreview;
    private Vector3d previewPosition;
    private Rotation3f previewRotation;
    /** Yaw offset in radians for rotating the preview model so the player can see all sides. */
    private float previewRotationYawOffset;
    private volatile boolean dismissed;

    public SpeciesSelectionPage(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpeciesEventData.CODEC);
        this.world = world;
        this.playerRef = playerRef;
        List<SpeciesData> allSpecies = SpeciesRegistry.getAllSpecies();
        if (!allSpecies.isEmpty()) {
            this.selectedSpeciesId = allSpecies.get(0).getId();
        }
    }

    // ------------------------------------------------------------------ building

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        if (!initialised) {
            initialised = true;
            startFromCurrentSelection(ref, store);
        }
        DisplayItems.syncAll();

        cmd.append(PAGE);
        renderAll(cmd, evt);

        bind(evt, "#ConfirmButton", EventData.of("Action", "Confirm"));
        bind(evt, "#CancelButton", EventData.of("Action", "Cancel"));
        bind(evt, "#RotateLeft", EventData.of("Action", "RotateLeft"));
        bind(evt, "#RotateRight", EventData.of("Action", "RotateRight"));
        bind(evt, "#Randomize", EventData.of("Action", "Randomize"));
        bind(evt, "#StylePrev", EventData.of("Action", "StylePage").append("Index", "-1"));
        bind(evt, "#StyleNext", EventData.of("Action", "StylePage").append("Index", "1"));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Search",
                EventData.of("Action", "Search").append("@Query", "#Search.Value"), false);

        schedulePreview();
    }

    /** Opens on the species and outfit the player already has, when there is one. */
    private void startFromCurrentSelection(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            PlayerSpeciesData.SpeciesSelection current = PlayerSpeciesData.getSpeciesSelection(ref, store, world);
            if (current == null || current.getSpeciesId() == null) return;
            SpeciesData species = SpeciesRegistry.getSpecies(current.getSpeciesId());
            if (species == null || !species.isEnabled()) return;
            selectedSpeciesId = species.getId();
            variantIndices.put(species.getId(), Math.max(0, Math.min(current.getVariantIndex(), Math.max(0, species.getVariantCount() - 1))));
            attachmentSelections.put(species.getId(), new HashMap<>(current.getAttachmentSelections()));
            if (current.getTextureSelection() != null) textureSelections.put(species.getId(), current.getTextureSelection());

        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).fine("No stored selection: " + e);
        }
    }

    private static void bind(@Nonnull UIEventBuilder evt, @Nonnull String selector, @Nonnull EventData data) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, data, false);
    }

    private void renderAll(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        renderSpeciesRail(cmd, evt);
        renderEditor(cmd, evt);
        renderSheet(cmd);
    }

    private void update(boolean species, boolean editor, boolean sheet, boolean preview) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        if (species) renderRailRings(cmd);
        if (editor) renderEditor(cmd, evt);
        if (sheet) renderSheet(cmd);
        if (preview) schedulePreview();
        sendUpdate(cmd, evt, false);
    }

    private void renderSpeciesRail(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        List<SpeciesData> all = SpeciesRegistry.getAllSpecies();
        cmd.clear("#SpeciesRail");
        railIds.clear();
        for (int i = 0; i < all.size(); i++) {
            railIds.add(all.get(i).getId());
            SpeciesData s = all.get(i);
            String tile = "#SpeciesRail[" + i + "]";
            cmd.append("#SpeciesRail", "OrbisOrigins/SpeciesTile.ui");
            String item = i < SPECIES_PREVIEWS ? DisplayItems.forSpecies(s, 0, null) : null;
            if (item != null) {
                cmd.set(tile + " #TilePreview.ItemId", item);
            } else {
                cmd.set(tile + " #TilePreview.Visible", false);
                cmd.set(tile + " #TileLabel.Text", s.getDisplayName());
            }
            cmd.set(tile + ".TooltipText", s.getDisplayName());
            cmd.set(tile + " #TileRing.Visible", s.getId().equals(selectedSpeciesId));
            bind(evt, tile, EventData.of("Action", "SelectSpecies").append("SpeciesId", s.getId()));
        }
    }

    /** Moves the selection ring without rebuilding the rail, so its scroll position stays put. */
    private void renderRailRings(@Nonnull UICommandBuilder cmd) {
        // The tiles sent at build time; a reload while the page is open must not address missing ones.
        for (int i = 0; i < railIds.size(); i++) {
            cmd.set("#SpeciesRail[" + i + "] #TileRing.Visible", railIds.get(i).equals(selectedSpeciesId));
        }
    }

    private void renderEditor(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        SpeciesData species = currentSpecies();
        cmd.clear("#CategoryRail");
        cmd.clear("#StyleGrid");
        cmd.clear("#ColourGrid");
        cmd.set("#ColourSection.Visible", false);
        cmd.set("#StylePager.Visible", false);
        cmd.set("#CrumbSpecies.Text", species != null ? species.getDisplayName() : "");
        if (species == null) {
            cmd.set("#EmptyNote.Visible", false);
            cmd.set("#CrumbCategory.Text", "");
            cmd.set("#CrumbCount.Text", "");
            cmd.set("#StyleName.Text", "");
            cmd.set("#ColourName.Text", "");
            return;
        }

        List<CosmeticCatalog.Category> categories = catalog(species);
        cmd.set("#EmptyNote.Visible", categories.isEmpty());
        cmd.set("#StyleGrid.Visible", !categories.isEmpty());
        cmd.set("#Search.Visible", !categories.isEmpty());
        if (categories.isEmpty()) {
            cmd.set("#CrumbCategory.Text", "");
            cmd.set("#CrumbCount.Text", "");
            cmd.set("#StyleName.Text", "");
            cmd.set("#ColourName.Text", "");
            return;
        }
        CosmeticCatalog.Category category = currentCategory(species, categories);

        for (int i = 0; i < categories.size(); i++) {
            CosmeticCatalog.Category c = categories.get(i);
            String tile = "#CategoryRail[" + i + "]";
            boolean on = c.key().equals(category.key());
            cmd.append("#CategoryRail", "OrbisOrigins/Cat/" + c.iconDoc() + ".ui");
            cmd.set(tile + ".TooltipText", c.label());
            cmd.set(tile + " #CatRing.Visible", on);
            cmd.set(tile + " #CatOn.Visible", on);
            cmd.set(tile + " #CatOff.Visible", !on);
            bind(evt, tile, EventData.of("Action", "Category").append("Key", c.key()));
        }

        cmd.set("#CrumbCategory.Text", category.label());
        int colours = 0;
        for (CosmeticCatalog.Style st : category.styles()) colours = Math.max(colours, st.choices().size());
        cmd.set("#CrumbCount.TextSpans", colours > 1
                ? Message.translation(LANG + "stylesColours").param("styles", category.styles().size()).param("colours", colours)
                : Message.translation(LANG + "styles").param("styles", category.styles().size()));

        // Filtered styles; the "none" tile leads the list when the slot may be empty.
        List<Integer> shown = new ArrayList<>();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < category.styles().size(); i++) {
            CosmeticCatalog.Style s = category.styles().get(i);
            if (q.isEmpty() || matches(s, q)) shown.add(i);
        }
        boolean none = category.allowsNone() && q.isEmpty();
        int total = shown.size() + (none ? 1 : 0);
        int pages = Math.max(1, (total + STYLES_PER_PAGE - 1) / STYLES_PER_PAGE);
        stylePage = Math.max(0, Math.min(stylePage, pages - 1));
        int selectedStyle = selectedStyleIndex(species, category);

        int from = stylePage * STYLES_PER_PAGE;
        int to = Math.min(total, from + STYLES_PER_PAGE);
        for (int slot = from; slot < to; slot++) {
            String tile = "#StyleGrid[" + (slot - from) + "]";
            if (none && slot == 0) {
                cmd.append("#StyleGrid", "OrbisOrigins/NoneTile.ui");
                cmd.set(tile + " #TileRing.Visible", selectedStyle < 0);
                bind(evt, tile, EventData.of("Action", "Style").append("Index", "-1"));
                continue;
            }
            int styleIndex = shown.get(slot - (none ? 1 : 0));
            CosmeticCatalog.Style s = category.styles().get(styleIndex);
            cmd.append("#StyleGrid", "OrbisOrigins/StyleTile.ui");
            String item = previewFor(species, category, s);
            if (item != null) {
                cmd.set(tile + " #TilePreview.ItemId", item);
            } else {
                cmd.set(tile + " #TilePreview.Visible", false);
                cmd.set(tile + " #TileLabel.Text", s.name());
            }
            cmd.set(tile + ".TooltipText", s.name());
            cmd.set(tile + " #TileRing.Visible", styleIndex == selectedStyle);
            bind(evt, tile, EventData.of("Action", "Style").append("Index", Integer.toString(styleIndex)));
        }
        if (pages > 1) {
            cmd.set("#StylePager.Visible", true);
            cmd.set("#StylePage.TextSpans", Message.translation(LANG + "page").param("page", stylePage + 1).param("pages", pages));
        }

        // Selected style's name and colours.
        if (selectedStyle < 0) {
            cmd.set("#StyleName.TextSpans", Message.translation(LANG + "none"));
            cmd.set("#ColourName.Text", "");
            return;
        }
        CosmeticCatalog.Style style = category.styles().get(selectedStyle);
        int selectedChoice = selectedChoiceIndex(species, category, style);
        cmd.set("#StyleName.Text", style.name());
        cmd.set("#ColourName.Text", style.choices().size() > 1 ? style.choices().get(selectedChoice).label() : "");
        if (style.choices().size() > 1) {
            cmd.set("#ColourSection.Visible", true);
            for (int i = 0; i < style.choices().size(); i++) {
                CosmeticCatalog.Choice choice = style.choices().get(i);
                String sw = "#ColourGrid[" + i + "]";
                cmd.append("#ColourGrid", "OrbisOrigins/Swatch.ui");
                PatchStyle fill = new PatchStyle();
                fill.setColor(Value.of(choice.texture() != null ? SwatchColours.of(choice.texture()) : "#7a8696"));
                cmd.setObject(sw + " #SwFill.Background", fill);
                cmd.set(sw + " #SwRing.Visible", i == selectedChoice);
                cmd.set(sw + ".TooltipText", choice.label());
                bind(evt, sw, EventData.of("Action", "Colour").append("Index", Integer.toString(i)));
            }
        }
    }

    private void renderSheet(@Nonnull UICommandBuilder cmd) {
        SpeciesData species = currentSpecies();
        cmd.clear("#ResistList");
        cmd.clear("#AbilityList");
        cmd.clear("#KitList");
        if (species == null) return;
        int variant = variantIndices.getOrDefault(species.getId(), 0);

        String portrait = DisplayItems.forSpecies(species, variant, textureSelections.get(species.getId()));
        cmd.set("#SheetPreview.Visible", portrait != null);
        if (portrait != null) cmd.set("#SheetPreview.ItemId", portrait);
        cmd.set("#SheetName.Text", species.getDisplayName());
        cmd.set("#SheetSub.Text", variantName(species, variant));
        cmd.set("#SheetDesc.Text", species.getDescription() != null ? species.getDescription() : "");
        cmd.set("#ConfirmButton.TextSpans", Message.translation(LANG + "become").param("species", species.getDisplayName()));

        stat(cmd, "#HealthRow", species.getHealthModifier());
        stat(cmd, "#StaminaRow", species.getStaminaModifier());
        stat(cmd, "#ManaRow", species.getManaModifier());

        int resist = 0;
        for (Map.Entry<String, Float> e : species.getDamageResistances().entrySet()) {
            float m = e.getValue() == null ? 1f : e.getValue();
            if (m == 1f) continue;
            String chip = "#ResistList[" + resist++ + "]";
            cmd.append("#ResistList", "OrbisOrigins/ResistChip.ui");
            cmd.set(chip + " #ChipIcon.AssetPath", damageIcon(e.getKey()));
            String type = CosmeticCatalog.pretty(e.getKey());
            if (m <= 0f) {
                cmd.set(chip + " #ChipText.TextSpans", Message.translation(LANG + "immune").param("type", type));
            } else {
                int pct = Math.round((m - 1f) * 100f);
                cmd.set(chip + " #ChipText.Text", type + " " + (pct > 0 ? "+" : "") + pct + "%");
            }
            cmd.set(chip + " #ChipText.Style.TextColor", m > 1f ? "#e36b5b" : "#7fd36b");
        }
        cmd.set("#ResistNone.Visible", resist == 0);

        int abilities = 0;
        for (SpeciesAbilityConfig a : species.getAbilities()) {
            if (a == null || a.getName() == null || a.getName().isBlank()) continue;
            String card = "#AbilityList[" + abilities++ + "]";
            cmd.append("#AbilityList", "OrbisOrigins/AbilityCard.ui");
            cmd.set(card + " #AbIcon.AssetPath", abilityIcon(a.getId()));
            cmd.set(card + " #AbName.Text", a.getName());
            cmd.set(card + " #AbDesc.Text", a.getDescription() != null ? a.getDescription() : "");
            cmd.set(card + " #AbTag.Text", conditionLabel(a.getCondition()));
        }
        cmd.set("#AbilityNone.Visible", abilities == 0);

        int kit = 0;
        for (String itemId : species.getStarterItems()) {
            if (itemId == null || Item.getAssetMap().getAsset(itemId) == null) continue; // unknown ids crash the client's tooltip
            String slot = "#KitList[" + kit++ + "]";
            cmd.append("#KitList", "OrbisOrigins/KitItem.ui");
            cmd.set(slot + " #KitIcon.ItemId", itemId);
            cmd.set(slot + ".TooltipText", CosmeticCatalog.pretty(itemId));
        }
        cmd.set("#KitNone.Visible", kit == 0);
    }

    /** A stat bar: the tick in the middle is the unmodified value; +-50 fills or empties the bar. */
    private static void stat(@Nonnull UICommandBuilder cmd, @Nonnull String row, int modifier) {
        int fill = Math.max(2, Math.min(BAR_WIDTH, BAR_WIDTH / 2 + modifier * (BAR_WIDTH / 2) / 50));
        Anchor a = new Anchor();
        a.setLeft(Value.of(1));
        a.setTop(Value.of(1));
        a.setBottom(Value.of(1));
        a.setWidth(Value.of(fill));
        cmd.setObject(row + " #StatFill.Anchor", a);
        cmd.set(row + " #StatValue.Text", modifier > 0 ? "+" + modifier : Integer.toString(modifier));
        cmd.set(row + " #StatValue.Style.TextColor", modifier > 0 ? "#7fd36b" : modifier < 0 ? "#e36b5b" : "#96a9be");
    }

    /** A short tag for when an ability applies; plain words for the conditions the mod knows. */
    @Nonnull
    private static String conditionLabel(@Nullable String condition) {
        if (condition == null || condition.isBlank()) return "";
        return switch (condition.toLowerCase(Locale.ROOT)) {
            case "health_below" -> "Low health";
            case "in_sunlight" -> "In sunlight";
            case "in_water" -> "In water";
            case "in_zone" -> "In its homeland";
            case "at_night" -> "At night";
            default -> CosmeticCatalog.pretty(condition);
        };
    }

    @Nonnull
    private static String damageIcon(@Nonnull String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if (t.contains("fire") || t.contains("lava") || t.contains("burn")) return "UI/StatusEffects/Burn.png";
        if (t.contains("poison")) return "UI/StatusEffects/Poison.png";
        if (t.contains("nature")) return "Icons/ItemCategories/Natural-Vegetal.png";
        if (t.contains("water") || t.contains("drown") || t.contains("cold") || t.contains("frost") || t.contains("ice")) return "Icons/ItemCategories/Natural-Fluid.png";
        if (t.contains("magic") || t.contains("void") || t.contains("lightning") || t.contains("thunder")) return "Icons/ItemCategories/Items-Ingredients.png";
        return "Icons/ItemCategories/Items-Shields.png";
    }

    @Nonnull
    private static String abilityIcon(@Nullable String id) {
        String a = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (a.contains("health")) return "UI/StatusEffects/HealthRegen.png";
        if (a.contains("stamina")) return "UI/StatusEffects/Stamina/Large.png";
        if (a.contains("poison")) return "Icons/Abilities/Rune_PoisonImbue.png";
        if (a.contains("fire")) return "Icons/Abilities/Rune_Fireball.png";
        if (a.contains("speed") || a.contains("move") || a.contains("jump")) return "Icons/Abilities/Rune_WindStrike.png";
        if (a.contains("strength") || a.contains("damage") || a.contains("rage")) return "Icons/Abilities/Rune_Enrage.png";
        if (a.contains("resist") || a.contains("armor") || a.contains("defense")) return "Icons/Abilities/Rune_GroundSlam.png";
        return "Icons/Abilities/Rune_ChargedShot.png";
    }

    // ------------------------------------------------------------------ state helpers

    @Nullable
    private SpeciesData currentSpecies() {
        return selectedSpeciesId == null ? null : SpeciesRegistry.getSpecies(selectedSpeciesId);
    }

    @Nonnull
    private List<CosmeticCatalog.Category> catalog(@Nonnull SpeciesData species) {
        DisplayItems.ensure(species);
        int variant = variantIndices.getOrDefault(species.getId(), 0);
        String bodyLabel = "Body";
        List<CosmeticCatalog.Category> categories = CosmeticCatalog.build(species, variant, bodyLabel);
        // Slots never touched start as they did on the old page: empty when allowed, else the first option.
        Map<String, String> sel = attachmentSelections.computeIfAbsent(species.getId(), k -> new HashMap<>());
        for (CosmeticCatalog.Category c : categories) {
            if (c.isBody() || sel.containsKey(c.key())) continue;
            if (c.allowsNone()) {
                sel.put(c.key(), "null");
            } else if (!c.styles().isEmpty() && !c.styles().get(0).choices().isEmpty()) {
                sel.put(c.key(), c.styles().get(0).choices().get(0).key());
            }
        }
        return categories;
    }

    @Nonnull
    private CosmeticCatalog.Category currentCategory(@Nonnull SpeciesData species, @Nonnull List<CosmeticCatalog.Category> categories) {
        String key = categorySelections.get(species.getId());
        for (CosmeticCatalog.Category c : categories) {
            if (c.key().equals(key)) return c;
        }
        CosmeticCatalog.Category first = categories.get(0);
        categorySelections.put(species.getId(), first.key());
        return first;
    }

    /** Index of the selected style in the category, or -1 when the slot is empty. */
    private int selectedStyleIndex(@Nonnull SpeciesData species, @Nonnull CosmeticCatalog.Category category) {
        if (category.isBody()) {
            return Math.max(0, Math.min(variantIndices.getOrDefault(species.getId(), 0), category.styles().size() - 1));
        }
        String key = attachmentSelections.getOrDefault(species.getId(), Map.of()).get(category.key());
        if (key == null || "null".equals(key)) return category.allowsNone() ? -1 : 0;
        for (int i = 0; i < category.styles().size(); i++) {
            for (CosmeticCatalog.Choice c : category.styles().get(i).choices()) {
                if (key.equals(c.key())) return i;
            }
        }
        return category.allowsNone() ? -1 : 0;
    }

    private int selectedChoiceIndex(@Nonnull SpeciesData species, @Nonnull CosmeticCatalog.Category category, @Nonnull CosmeticCatalog.Style style) {
        String key = category.isBody()
                ? textureSelections.get(species.getId())
                : attachmentSelections.getOrDefault(species.getId(), Map.of()).get(category.key());
        for (int i = 0; i < style.choices().size(); i++) {
            String k = style.choices().get(i).key();
            if (k != null && k.equals(key)) return i;
        }
        return 0;
    }

    /** The preview for a style tile: the colour in use when the style is worn, else its first colour. */
    @Nullable
    private String previewFor(@Nonnull SpeciesData species, @Nonnull CosmeticCatalog.Category category, @Nonnull CosmeticCatalog.Style style) {
        if (style.choices().size() > 1) {
            String item = style.choices().get(selectedChoiceIndex(species, category, style)).previewItem();
            if (item != null) return item;
        }
        return style.previewItem();
    }

    private static boolean matches(@Nonnull CosmeticCatalog.Style s, @Nonnull String q) {
        if (s.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (CosmeticCatalog.Choice c : s.choices()) {
            if (c.label().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    @Nonnull
    private static String variantName(@Nonnull SpeciesData species, int variant) {
        if (species.isVersion2()) {
            com.hexvane.orbisorigins.species.SpeciesVariantData v = species.getVariantData(variant);
            if (v != null && v.getVariantName() != null && !v.getVariantName().equals(species.getDisplayName())) return v.getVariantName();
            return "";
        }
        return species.getVariantCount() > 1 ? CosmeticCatalog.pretty(species.getModelName(variant)) : "";
    }

    // ------------------------------------------------------------------ events

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull SpeciesEventData data
    ) {
        if (data.action == null) return;
        switch (data.action) {
            case "SelectSpecies" -> {
                if (data.speciesId != null && SpeciesRegistry.getSpecies(data.speciesId) != null) {
                    selectedSpeciesId = data.speciesId;
                    variantIndices.putIfAbsent(data.speciesId, 0);
                    stylePage = 0;
                    searchQuery = "";
                    update(true, true, true, true);
                    clearSearchField();
                }
            }
            case "Category" -> {
                SpeciesData species = currentSpecies();
                if (species != null && data.key != null) {
                    categorySelections.put(species.getId(), data.key);
                    stylePage = 0;
                    searchQuery = "";
                    update(false, true, false, false);
                    clearSearchField();
                }
            }
            case "StylePage" -> {
                stylePage += parseIndex(data.index, 0);
                update(false, true, false, false);
            }
            case "Search" -> {
                searchQuery = data.query != null ? data.query : "";
                stylePage = 0;
                update(false, true, false, false);
            }
            case "Style" -> selectStyle(parseIndex(data.index, Integer.MIN_VALUE));
            case "Colour" -> selectColour(parseIndex(data.index, -1));
            case "Randomize" -> randomize();
            case "Confirm" -> confirmSelection(ref, store);
            case "Cancel" -> this.close();
            case "RotateLeft" -> rotatePreview(ref, store, 1);
            case "RotateRight" -> rotatePreview(ref, store, -1);
            default -> {
            }
        }
    }

    private void clearSearchField() {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#Search.Value", "");
        sendUpdate(cmd, false);
    }

    private static int parseIndex(@Nullable String s, int fallback) {
        if (s == null) return fallback;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void selectStyle(int styleIndex) {
        SpeciesData species = currentSpecies();
        if (species == null || styleIndex == Integer.MIN_VALUE) return;
        List<CosmeticCatalog.Category> categories = catalog(species);
        if (categories.isEmpty()) return;
        CosmeticCatalog.Category category = currentCategory(species, categories);
        if (styleIndex < 0) {
            if (!category.allowsNone()) return;
            attachmentSelections.computeIfAbsent(species.getId(), k -> new HashMap<>()).put(category.key(), "null");
            update(false, true, false, true);
            return;
        }
        if (styleIndex >= category.styles().size()) return;
        CosmeticCatalog.Style style = category.styles().get(styleIndex);
        if (category.isBody()) {
            int variant = style.choices().isEmpty() ? styleIndex : style.choices().get(0).variantIndex();
            boolean changed = variant != variantIndices.getOrDefault(species.getId(), 0);
            variantIndices.put(species.getId(), variant);
            textureSelections.put(species.getId(), style.choices().isEmpty() ? null : style.choices().get(0).key());
            if (changed) attachmentSelections.put(species.getId(), new HashMap<>()); // slots differ per variant
            update(false, true, true, true);
            return;
        }
        // Keep the colour in use when switching to another style that offers it.
        Map<String, String> sel = attachmentSelections.computeIfAbsent(species.getId(), k -> new HashMap<>());
        String colour = currentColourLabel(species, category);
        CosmeticCatalog.Choice pick = style.choices().get(0);
        if (colour != null) {
            for (CosmeticCatalog.Choice c : style.choices()) {
                if (c.label().equalsIgnoreCase(colour)) pick = c;
            }
        }
        sel.put(category.key(), pick.key());
        update(false, true, false, true);
    }

    @Nullable
    private String currentColourLabel(@Nonnull SpeciesData species, @Nonnull CosmeticCatalog.Category category) {
        int s = selectedStyleIndex(species, category);
        if (s < 0) return null;
        CosmeticCatalog.Style style = category.styles().get(s);
        if (style.choices().size() < 2) return null;
        return style.choices().get(selectedChoiceIndex(species, category, style)).label();
    }

    private void selectColour(int choiceIndex) {
        SpeciesData species = currentSpecies();
        if (species == null || choiceIndex < 0) return;
        List<CosmeticCatalog.Category> categories = catalog(species);
        if (categories.isEmpty()) return;
        CosmeticCatalog.Category category = currentCategory(species, categories);
        int s = selectedStyleIndex(species, category);
        if (s < 0) return;
        CosmeticCatalog.Style style = category.styles().get(s);
        if (choiceIndex >= style.choices().size()) return;
        CosmeticCatalog.Choice choice = style.choices().get(choiceIndex);
        if (category.isBody()) {
            textureSelections.put(species.getId(), choice.key());
            update(false, true, true, true);
        } else {
            attachmentSelections.computeIfAbsent(species.getId(), k -> new HashMap<>()).put(category.key(), choice.key());
            update(false, true, false, true);
        }
    }

    private void randomize() {
        SpeciesData species = currentSpecies();
        if (species == null) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<CosmeticCatalog.Category> categories = catalog(species);
        for (CosmeticCatalog.Category c : categories) {
            if (c.isBody() || c.styles().isEmpty()) continue;
            Map<String, String> sel = attachmentSelections.computeIfAbsent(species.getId(), k -> new HashMap<>());
            if (c.allowsNone() && rnd.nextInt(4) == 0) {
                sel.put(c.key(), "null");
                continue;
            }
            CosmeticCatalog.Style s = c.styles().get(rnd.nextInt(c.styles().size()));
            sel.put(c.key(), s.choices().get(rnd.nextInt(s.choices().size())).key());
        }
        // Body colour, within the current variant.
        for (CosmeticCatalog.Category c : categories) {
            if (!c.isBody()) continue;
            int v = variantIndices.getOrDefault(species.getId(), 0);
            if (v < c.styles().size()) {
                List<CosmeticCatalog.Choice> choices = c.styles().get(v).choices();
                if (!choices.isEmpty()) textureSelections.put(species.getId(), choices.get(rnd.nextInt(choices.size())).key());
            }
        }
        update(false, true, true, true);
    }

    private void schedulePreview() {
        if (selectedSpeciesId == null) return;
        world.execute(() -> {
            Ref<EntityStore> playerRef = this.playerRef.getReference();
            if (playerRef != null && playerRef.isValid()) {
                createPreviewEntity(playerRef, playerRef.getStore());
            }
        });
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.dismissed = true;
        if (this.modelPreview != null && this.modelPreview.isValid()) {
            Ref<EntityStore> toRemove = this.modelPreview;
            this.modelPreview = null;
            Store<EntityStore> storeRef = store;
            world.execute(() -> {
                if (toRemove.isValid()) {
                    storeRef.removeEntity(toRemove, RemoveReason.REMOVE);
                }
            });
        }
    }

    // ------------------------------------------------------------------ preview entity (unchanged)

    private void createPreviewEntity(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (dismissed) {
            return;
        }
        if (selectedSpeciesId == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("createPreviewEntity: selectedSpeciesId is null");
            return;
        }
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("createPreviewEntity: species not found: " + selectedSpeciesId);
            return;
        }
        
        // Skip preview for orbian (no model)
        if (species.usesPlayerModel()) {
            if (modelPreview != null && modelPreview.isValid()) {
                store.removeEntity(modelPreview, RemoveReason.REMOVE);
                modelPreview = null;
            }
            return;
        }
        
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        Map<String, String> attachmentSelectionsForSpecies = attachmentSelections.getOrDefault(selectedSpeciesId, new HashMap<>());
        String textureSelectionForSpecies = textureSelections.get(selectedSpeciesId);
        
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName());
        com.hypixel.hytale.server.core.asset.type.model.config.Model model;
        if (species.isVersion2()) {
            model = ModelUtil.createModelForV2(species, variantIndex, textureSelectionForSpecies, attachmentSelectionsForSpecies);
            if (model == null) {
                logger.warning("createPreviewEntity: Failed to create v2 model");
                return;
            }
        } else {
            String modelName = species.getModelName(variantIndex);
            com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset modelAsset = 
                com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.getAssetMap().getAsset(modelName);
            if (modelAsset == null) {
                logger.warning("createPreviewEntity: Model asset not found: " + modelName);
                return;
            }
            com.hypixel.hytale.server.core.asset.type.model.config.Model baseModel = ModelUtil.getModel(modelName);
            if (baseModel == null) {
                logger.warning("createPreviewEntity: Failed to get model: " + modelName);
                return;
            }
            Map<String, String> attachmentMap = new HashMap<>();
            if (baseModel.getRandomAttachmentIds() != null) {
                attachmentMap.putAll(baseModel.getRandomAttachmentIds());
            }
            for (Map.Entry<String, String> entry : attachmentSelectionsForSpecies.entrySet()) {
                String attachmentType = entry.getKey();
                String selectedOption = entry.getValue();
                if (selectedOption != null && !selectedOption.isEmpty() && !"null".equals(selectedOption)) {
                    attachmentMap.put(attachmentType, selectedOption);
                } else {
                    attachmentMap.remove(attachmentType);
                }
            }
            float scale = species.getModelScale(variantIndex);
            model = com.hypixel.hytale.server.core.asset.type.model.config.Model.createScaledModel(
                modelAsset, scale, attachmentMap);
        }
        
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            logger.warning("createPreviewEntity: TransformComponent is null");
            return;
        }
        
        HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
        if (headRotationComponent == null) {
            logger.warning("createPreviewEntity: HeadRotation is null");
            return;
        }
        
        Vector3d playerPosition = transformComponent.getPosition();
        Rotation3f headRotation = headRotationComponent.getRotation();
        
        // Spawn entity 4 blocks in front of player
        Vector3d direction = Transform.getDirection(headRotation.pitch(), headRotation.yaw());
        Vector3d previewPosition = TargetUtil.getTargetLocation(ref, 8.0, store);
        if (previewPosition == null) {
            previewPosition = new Vector3d(playerPosition).fma(4.0, direction);
        }

        // Stand it right of the view centre, in the open gap between the editor and the sheet:
        // that gap's middle sits about 16% of the half-screen right of centre, which in the
        // client is about 0.21 x the distance to the spot, sideways (0.4 overshot in game).
        double fx = direction.x, fz = direction.z;
        double flat = Math.sqrt(fx * fx + fz * fz);
        if (flat > 1.0e-4) {
            fx /= flat;
            fz /= flat;
            double dist = Math.hypot(previewPosition.x - playerPosition.x, previewPosition.z - playerPosition.z);
            double side = PREVIEW_SIDE_SHIFT * dist;
            // Right of the view = forward x up = (-fz, 0, fx).
            previewPosition = new Vector3d(previewPosition.x - fz * side, previewPosition.y + 2.0, previewPosition.z + fx * side);
        }

        // Find ground level for the preview entity
        Vector3d targetGround = TargetUtil.getTargetLocation(
                store.getExternalData().getWorld(),
                blockId -> blockId != 0,
                previewPosition.x, previewPosition.y, previewPosition.z,
                0.0, -1.0, 0.0, 8.0
        );
        if (targetGround != null) {
            previewPosition = targetGround;
        } else {
            // Fallback: spawn at player's feet level
            previewPosition.y = playerPosition.y;
        }
        
        // Make entity face player, then apply rotation offset so player can spin the preview
        Vector3d relativePos = new Vector3d(playerPosition).sub(previewPosition);
        relativePos.y = 0.0;
        Rotation3f previewRot = Rotation3f.lookAt(relativePos);
        previewRot = new Rotation3f(previewRot.pitch(), previewRot.yaw() + previewRotationYawOffset, previewRot.roll());
        
        Vector3d spawnPosition = previewPosition;
        
        if (modelPreview == null || !modelPreview.isValid()) {
            // Create new preview entity
            Holder<EntityStore> holder = store.getRegistry().newHolder();
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(spawnPosition, previewRot));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(previewRot));
            
            this.modelPreview = store.addEntity(holder, AddReason.SPAWN);
            this.previewPosition = spawnPosition;
            this.previewRotation = previewRot;
        } else {
            // Update existing preview model and position
            store.putComponent(modelPreview, ModelComponent.getComponentType(), new ModelComponent(model));
            store.putComponent(modelPreview, TransformComponent.getComponentType(), new TransformComponent(spawnPosition, previewRot));
            store.putComponent(modelPreview, HeadRotation.getComponentType(), new HeadRotation(previewRot));
            this.previewPosition = spawnPosition;
            this.previewRotation = previewRot;
        }
    }

    private static final float ROTATE_PREVIEW_STEP_RADIANS = (float) (Math.PI / 12); // 15 degrees per click

    private void rotatePreview(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int direction) {
        if (dismissed) {
            return;
        }
        previewRotationYawOffset += direction * ROTATE_PREVIEW_STEP_RADIANS;
        if (previewRotation != null) {
            float newYaw = previewRotation.yaw() + direction * ROTATE_PREVIEW_STEP_RADIANS;
            this.previewRotation = new Rotation3f(previewRotation.pitch(), newYaw, previewRotation.roll());
            Ref<EntityStore> previewRef = this.modelPreview;
            Vector3d pos = this.previewPosition;
            Rotation3f rot = this.previewRotation;
            Store<EntityStore> storeRef = store;
            world.execute(() -> {
                if (dismissed || previewRef == null || !previewRef.isValid()) {
                    return;
                }
                storeRef.putComponent(previewRef, TransformComponent.getComponentType(), new TransformComponent(pos, rot));
                storeRef.putComponent(previewRef, HeadRotation.getComponentType(), new HeadRotation(rot));
            });
        }
    }

    private void confirmSelection(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (selectedSpeciesId == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("confirmSelection: selectedSpeciesId is null");
            return;
        }
        
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("confirmSelection: Player component is null");
            return;
        }
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("confirmSelection: Species not found: " + selectedSpeciesId);
            return;
        }
        
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        Map<String, String> attachmentSelectionsForSpecies = attachmentSelections.getOrDefault(selectedSpeciesId, new HashMap<>());
        String textureSelectionForSpecies = textureSelections.get(selectedSpeciesId);

        String previousSpeciesId = PlayerSpeciesData.getSelectedSpeciesId(ref, store, world);
        boolean sameSpecies = previousSpeciesId != null && previousSpeciesId.equals(selectedSpeciesId);
        if (!sameSpecies && previousSpeciesId != null && !previousSpeciesId.isEmpty()) {
            SpeciesData previousSpecies = SpeciesRegistry.getSpecies(previousSpeciesId);
            if (previousSpecies != null) {
                SpeciesCommandUtil.runSpeciesCommands(playerRef, previousSpecies.getDeselectCommands());
            }
        }

        // Picking a species shows it, even if the player had hidden their old one.
        PlayerSpeciesData.setSpeciesModelHidden(ref, store, false);

        // Apply model (if not orbian)
        if (!species.usesPlayerModel()) {
            if (species.isVersion2()) {
                ModelUtil.applyModelToPlayerV2(ref, store, species, variantIndex, textureSelectionForSpecies, attachmentSelectionsForSpecies);
            } else {
                String modelName = species.getModelName(variantIndex);
                float eyeHeightModifier = species.getEyeHeightModifier(modelName);
                float hitboxHeightModifier = species.getHitboxHeightModifier(modelName);
                ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier, attachmentSelectionsForSpecies);
            }
        } else {
            // Reset to player skin for orbian
            ModelUtil.resetToPlayerSkin(ref, store);
        }
        
        // Apply species stats
        SpeciesStatUtil.applySpeciesStats(ref, store, species);
        
        // Give starter items
        var combinedInventory = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);
        for (String itemId : species.getStarterItems()) {
            combinedInventory.addItemStack(new ItemStack(itemId, 1));
        }
        
        // Store choice with attachment selections and texture selection
        PlayerSpeciesData.setSpeciesSelection(ref, store, world, selectedSpeciesId, variantIndex, attachmentSelectionsForSpecies, textureSelectionForSpecies);

        // Apply AbilityAPI-backed abilities if integration is available
        OrbisOriginsPlugin plugin = OrbisOriginsPlugin.getInstance();
        boolean abilityApiPresent = plugin != null && plugin.isAbilityApiPresent();
        java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName())
                .info("confirmSelection: species=" + species.getId()
                        + ", abilities=" + species.getAbilities().size()
                        + ", abilityApiPresent=" + abilityApiPresent);
        if (abilityApiPresent) {
            // Clear abilities from previous species selection, if any.
            if (previousSpeciesId != null && !previousSpeciesId.isEmpty()) {
                SpeciesData previousSpecies = SpeciesRegistry.getSpecies(previousSpeciesId);
                if (previousSpecies != null) {
                    AbilityApiBridge.clearSpeciesAbilities(playerRef, ref, store, world, previousSpecies);
                }
            }
            // Apply new species abilities (via AbilityAPI plugin classloader), unless the player switched them off.
            if (!PlayerSpeciesData.getAbilitiesDisabled(ref, store)) {
                AbilityApiBridge.applySpeciesAbilities(playerRef, ref, store, world, species);
            }
        }

        if (!sameSpecies) {
            SpeciesCommandUtil.runSpeciesCommands(playerRef, species.getSelectCommands());
        }

        // Consume item from inventory
        consumeSelectorItem(ref, store);
        
        // Clean up preview - defer to next tick to avoid race with ChunkSavingSystems during serialization
        if (modelPreview != null && modelPreview.isValid()) {
            Ref<EntityStore> toRemove = modelPreview;
            modelPreview = null;
            Store<EntityStore> storeRef = store;
            world.execute(() -> {
                if (toRemove.isValid()) {
                    storeRef.removeEntity(toRemove, RemoveReason.REMOVE);
                }
            });
        }
        
        // Close GUI
        this.close();
    }

    private void consumeSelectorItem(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_FIRST);
        short capacity = inventory.getCapacity();
        for (short i = 0; i < capacity; i++) {
            var itemStack = inventory.getItemStack(i);
            if (itemStack != null && "OrbisOrigins_Species_Selector".equals(itemStack.getItemId())) {
                inventory.removeItemStackFromSlot(i);
                return;
            }
        }
    }

    public static class SpeciesEventData {
        public static final BuilderCodec<SpeciesEventData> CODEC = BuilderCodec.builder(
                SpeciesEventData.class, SpeciesEventData::new
        )
        .append(new KeyedCodec<>("Action", Codec.STRING), (data, s) -> data.action = s, data -> data.action)
        .add()
        .append(new KeyedCodec<>("SpeciesId", Codec.STRING), (data, s) -> data.speciesId = s, data -> data.speciesId)
        .add()
        .append(new KeyedCodec<>("Key", Codec.STRING), (data, s) -> data.key = s, data -> data.key)
        .add()
        .append(new KeyedCodec<>("Index", Codec.STRING), (data, s) -> data.index = s, data -> data.index)
        .add()
        .append(new KeyedCodec<>("@Query", Codec.STRING), (data, s) -> data.query = s, data -> data.query)
        .add()
        .build();

        private String action;
        private String speciesId;
        private String key;
        private String index;
        private String query;
    }
}
