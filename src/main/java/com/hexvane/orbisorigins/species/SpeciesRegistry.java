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
                List.of("Kweebec_Rootling", "Kweebec_Sapling", "Kweebec_Seedling", "Kweebec_Sapling_Orange", "Kweebec_Sapling_Pink", "Kweebec_Sproutling", "Kweebec_Razorleaf"),
                "A small, agile forest-dwelling creature. Higher stamina and nature resistance. Starts with a kweebec helm.",
                -5,  // Slightly lower health (offset by resistance and starter item)
                20,  // Higher stamina
                List.of("Armor_Kweebec_Head"),
                kweebecResistances
        ));

        // Trork - Higher health, lower stamina, physical resistance
        Map<String, Float> trorkResistances = new HashMap<>();
        trorkResistances.put("Physical", 0.75f); // 25% physical damage reduction
        registerSpecies(new SpeciesData(
                "trork",
                "Trork",
                "Trork_Warrior",
                List.of("Trork_Warrior", "Trork_Shaman", "Trork_Sentry", "Trork_Mauler", "Trork_Hunter", "Trork_Guard", "Trork_Doctor_Witch", "Trork_Chieftain", "Trork_Brawler"),
                "A large, brutish warrior. Much higher health and physical resistance. Starts with a trork helm.",
                40,  // Much higher health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
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
                "Feran_Civilian",
                List.of("Feran_Cub", "Feran_Civilian", "Feran_Burrower", "Feran_Sharptooth", "Feran_Longtooth", "Feran_Windwalker"),
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
                "Skeleton_Fighter",
                List.of("Skeleton", "Skeleton_Fighter", "Skeleton_Soldier", "Skeleton_Knight", "Skeleton_Scout", "Skeleton_Archer", "Skeleton_Ranger", "Skeleton_Mage", "Skeleton_Archmage"),
                "An undead creature. Immune to poison and balanced stamina. Vulnerable to fire.",
                -5,  // Slightly lower health (offset by poison immunity)
                5,   // Slightly higher stamina
                List.of(),
                skeletonResistances
        ));

        // Klops - Balanced stats, mining/trading focus
        Map<String, Float> klopsResistances = new HashMap<>();
        klopsResistances.put("Physical", 0.9f); // 10% physical damage reduction
        registerSpecies(new SpeciesData(
                "klops",
                "Klops",
                "Klops_Merchant",
                List.of("Klops_Merchant", "Klops_Miner", "Klops_Gentleman"),
                "A small, industrious creature known for mining and trading. Slightly higher stamina and minor physical resistance.",
                5,   // Slightly higher health
                10,  // Higher stamina
                List.of(),
                klopsResistances
        ));

        // Goblin - Lower health, higher stamina, scavenger focus
        Map<String, Float> goblinResistances = new HashMap<>();
        goblinResistances.put("Poison", 0.75f); // 25% poison damage reduction
        registerSpecies(new SpeciesData(
                "goblin",
                "Goblin",
                "Goblin_Scrapper",
                List.of("Goblin_Scrapper", "Goblin_Scavenger", "Goblin_Ogre", "Goblin_Miner"),
                "A small, crafty creature known for scavenging. Higher stamina and poison resistance.",
                -5,  // Slightly lower health (offset by stamina and resistance)
                15,  // Much higher stamina
                List.of(),
                goblinResistances
        ));

        // Outlander - Balanced stats, mysterious wanderer
        Map<String, Float> outlanderResistances = new HashMap<>();
        outlanderResistances.put("Magic", 0.85f); // 15% magic damage reduction
        registerSpecies(new SpeciesData(
                "outlander",
                "Outlander",
                "Outlander",
                List.of("Outlander", "Outlander_Ranged", "Outlander_Cultist"),
                "A mysterious wanderer from distant lands. Balanced stats with minor magic resistance.",
                10,  // Higher health
                10,  // Higher stamina
                List.of(),
                outlanderResistances
        ));

        // Zombie - Higher health, lower stamina, undead
        Map<String, Float> zombieResistances = new HashMap<>();
        zombieResistances.put("Poison", 0.0f); // Immune to poison
        zombieResistances.put("Physical", 0.9f); // 10% physical damage reduction
        zombieResistances.put("Fire", 1.3f); // 30% more fire damage (weakness)
        registerSpecies(new SpeciesData(
                "zombie",
                "Zombie",
                "Zombie",
                List.of("Zombie", "Zombie_Burnt", "Zombie_Frost", "Zombie_Sand"),
                "A reanimated corpse. Much higher health, immune to poison, and physical resistance. Vulnerable to fire.",
                35,  // Much higher health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                zombieResistances
        ));

        // Golem Crystal Earth - High health, earth resistance
        Map<String, Float> golemEarthResistances = new HashMap<>();
        golemEarthResistances.put("Physical", 0.5f); // 50% physical damage reduction
        golemEarthResistances.put("Nature", 0.75f); // 25% nature damage reduction
        golemEarthResistances.put("Fire", 1.2f); // 20% more fire damage (weakness)
        registerSpecies(new SpeciesData(
                "golem_earth",
                "Earth Golem",
                "Golem_Crystal_Earth",
                List.of("Golem_Crystal_Earth"),
                "A construct of earth and crystal. Very high health and strong physical resistance. Weak to fire.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemEarthResistances
        ));

        // Golem Crystal Frost - High health, cold resistance
        Map<String, Float> golemFrostResistances = new HashMap<>();
        golemFrostResistances.put("Cold", 0.0f); // Immune to cold
        golemFrostResistances.put("Physical", 0.6f); // 40% physical damage reduction
        golemFrostResistances.put("Fire", 1.3f); // 30% more fire damage (weakness)
        registerSpecies(new SpeciesData(
                "golem_frost",
                "Frost Golem",
                "Golem_Crystal_Frost",
                List.of("Golem_Crystal_Frost"),
                "A construct of ice and crystal. Very high health, immune to cold, and physical resistance. Weak to fire.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemFrostResistances
        ));

        // Golem Crystal Flame - High health, fire resistance
        Map<String, Float> golemFlameResistances = new HashMap<>();
        golemFlameResistances.put("Fire", 0.0f); // Immune to fire
        golemFlameResistances.put("Lava", 0.0f); // Immune to lava
        golemFlameResistances.put("Physical", 0.6f); // 40% physical damage reduction
        golemFlameResistances.put("Cold", 1.5f); // 50% more cold damage (weakness)
        registerSpecies(new SpeciesData(
                "golem_flame",
                "Flame Golem",
                "Golem_Crystal_Flame",
                List.of("Golem_Crystal_Flame"),
                "A construct of fire and crystal. Very high health, immune to fire and lava, and physical resistance. Weak to cold.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemFlameResistances
        ));

        // Golem Crystal Thunder - High health, lightning resistance
        Map<String, Float> golemThunderResistances = new HashMap<>();
        golemThunderResistances.put("Lightning", 0.0f); // Immune to lightning
        golemThunderResistances.put("Physical", 0.6f); // 40% physical damage reduction
        golemThunderResistances.put("Magic", 0.8f); // 20% magic damage reduction
        registerSpecies(new SpeciesData(
                "golem_thunder",
                "Thunder Golem",
                "Golem_Crystal_Thunder",
                List.of("Golem_Crystal_Thunder"),
                "A construct of lightning and crystal. Very high health, immune to lightning, and strong resistances.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemThunderResistances
        ));

        // Golem Crystal Sand - High health, sand/physical resistance
        Map<String, Float> golemSandResistances = new HashMap<>();
        golemSandResistances.put("Physical", 0.5f); // 50% physical damage reduction
        golemSandResistances.put("Nature", 0.8f); // 20% nature damage reduction
        golemSandResistances.put("Water", 1.3f); // 30% more water damage (weakness)
        registerSpecies(new SpeciesData(
                "golem_sand",
                "Sand Golem",
                "Golem_Crystal_Sand",
                List.of("Golem_Crystal_Sand"),
                "A construct of sand and crystal. Very high health and strong physical resistance. Weak to water.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemSandResistances
        ));

        // Golem Firesteel - High health, fire resistance
        Map<String, Float> golemFiresteelResistances = new HashMap<>();
        golemFiresteelResistances.put("Fire", 0.5f); // 50% fire damage reduction
        golemFiresteelResistances.put("Lava", 0.5f); // 50% lava damage reduction
        golemFiresteelResistances.put("Physical", 0.7f); // 30% physical damage reduction
        golemFiresteelResistances.put("Cold", 1.4f); // 40% more cold damage (weakness)
        registerSpecies(new SpeciesData(
                "golem_firesteel",
                "Firesteel Golem",
                "Golem_Firesteel",
                List.of("Golem_Firesteel"),
                "A construct of firesteel. Very high health, fire resistance, and physical resistance. Weak to cold.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemFiresteelResistances
        ));

        // Golem Guardian Void - High health, void resistance
        Map<String, Float> golemVoidResistances = new HashMap<>();
        golemVoidResistances.put("Void", 0.0f); // Immune to void
        golemVoidResistances.put("Magic", 0.5f); // 50% magic damage reduction
        golemVoidResistances.put("Physical", 0.6f); // 40% physical damage reduction
        registerSpecies(new SpeciesData(
                "golem_void",
                "Void Golem",
                "Golem_Guardian_Void",
                List.of("Golem_Guardian_Void"),
                "A construct of void energy. Very high health, immune to void, and strong magic resistance.",
                50,  // Very high health
                -5,  // Lower stamina (base 10, so 5 total - minimum allowed)
                List.of(),
                golemVoidResistances
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
