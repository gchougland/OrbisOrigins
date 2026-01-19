package com.hexvane.orbisorigins.species;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Registry for all playable species.
 */
public class SpeciesRegistry {
    private static final Map<String, SpeciesData> SPECIES_MAP = new HashMap<>();
    private static final List<SpeciesData> SPECIES_LIST = new ArrayList<>();

    public static void initialize() {
        SPECIES_MAP.clear();
        SPECIES_LIST.clear();

        // Kweebec - Lower health, higher stamina, starts with kweebec helm, nature resistance
        Map<String, Float> kweebecResistances = new HashMap<>();
        kweebecResistances.put("Nature", 0.5f); // 50% nature damage reduction
        registerSpecies(new SpeciesData(
                "kweebec",
                "Kweebec",
                "Kweebec_Rootling",
                List.of("Kweebec_Rootling", "Kweebec_Sapling", "Kweebec_Seedling"),
                "A small, agile forest-dwelling creature. Lower health but higher stamina. Resistant to nature damage.",
                -10, // Lower health
                20,  // Higher stamina
                List.of("Armor_Kweebec_Head"),
                kweebecResistances
        ));

        // Trork - Higher health, lower stamina, physical resistance
        // Base stamina is 10, so -8 gives 2 stamina total (still usable but very limited)
        Map<String, Float> trorkResistances = new HashMap<>();
        trorkResistances.put("Physical", 0.75f); // 25% physical damage reduction
        registerSpecies(new SpeciesData(
                "trork",
                "Trork",
                "Trork",
                List.of("Trork"),
                "A large, brutish warrior. Higher health but lower stamina. Resistant to physical damage.",
                30,  // Higher health
                -8,  // Lower stamina (base 10, so 2 total)
                List.of("Armor_Trork_Head"),
                trorkResistances
        ));

        // Feran - Balanced stats, fire resistance
        Map<String, Float> feranResistances = new HashMap<>();
        feranResistances.put("Fire", 0.5f); // 50% fire damage reduction
        feranResistances.put("Lava", 0.5f); // 50% lava damage reduction
        registerSpecies(new SpeciesData(
                "feran",
                "Feran",
                "Feran",
                List.of("Feran"),
                "A balanced feline species. Moderate health and stamina. Resistant to fire and lava.",
                10,  // Slightly higher health
                10,  // Slightly higher stamina
                List.of(),
                feranResistances
        ));

        // Skeleton - Lower health, balanced stamina, poison resistance, fire weakness
        Map<String, Float> skeletonResistances = new HashMap<>();
        skeletonResistances.put("Poison", 0.0f); // Immune to poison
        skeletonResistances.put("Fire", 1.5f); // 50% more fire damage (weakness)
        registerSpecies(new SpeciesData(
                "skeleton",
                "Skeleton",
                "Skeleton",
                List.of("Skeleton"),
                "An undead creature. Lower health but balanced stamina. Immune to poison, but vulnerable to fire.",
                -15, // Lower health
                0,   // Balanced stamina
                List.of(),
                skeletonResistances
        ));

        // Human - Default stats, no starter items, no resistances
        registerSpecies(new SpeciesData(
                "human",
                "Human",
                "", // No model - uses player skin
                List.of(), // No variants
                "A standard human. No special bonuses or penalties.",
                0,   // No health modifier
                0,   // No stamina modifier
                List.of(), // No starter items
                new HashMap<>() // No resistances
        ));
    }

    private static void registerSpecies(@Nonnull SpeciesData species) {
        SPECIES_MAP.put(species.getId(), species);
        SPECIES_LIST.add(species);
    }

    @Nullable
    public static SpeciesData getSpecies(@Nonnull String id) {
        return SPECIES_MAP.get(id);
    }

    @Nonnull
    public static List<SpeciesData> getAllSpecies() {
        return new ArrayList<>(SPECIES_LIST);
    }

    @Nullable
    public static SpeciesData getDefaultSpecies() {
        return getSpecies("human");
    }
}
