package com.hexvane.orbisorigins;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.util.Config;
import com.hexvane.orbisorigins.config.OrbisOriginsConfig;
import com.hexvane.orbisorigins.gui.OrbisOriginsPageSupplier;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.systems.FirstJoinSystem;
import com.hexvane.orbisorigins.systems.SpeciesDamageResistanceSystem;
import com.hexvane.orbisorigins.systems.SpeciesModelSystem;
import com.hexvane.orbisorigins.systems.SpeciesModelMaintenanceSystem;
import com.hexvane.orbisorigins.systems.SpeciesSleepingRaiseSystem;
import com.hexvane.orbisorigins.commands.OriginsCommand;
import com.hypixel.hytale.component.query.Query;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nonnull;

public class OrbisOriginsPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Recipe ID for the species selector item (generated from item JSON). */
    private static final String SPECIES_SELECTOR_RECIPE_ID = "OrbisOrigins_Species_Selector_Recipe_Generated_0";

    @Nonnull
    private final Config<OrbisOriginsConfig> config = this.withConfig("config", OrbisOriginsConfig.CODEC);

    public OrbisOriginsPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        // Initialize persistent data storage
        com.hexvane.orbisorigins.data.PlayerDataStorage.initialize(this.getDataDirectory());
        
        // Initialize species registry (loads from JSON files)
        SpeciesRegistry.initialize(this.getDataDirectory());
        
        // Register the custom UI page supplier
        OpenCustomUIInteraction.PAGE_CODEC.register(
                "OrbisOriginsSpeciesSelection",
                OrbisOriginsPageSupplier.class,
                OrbisOriginsPageSupplier.CODEC
        );
        LOGGER.atInfo().log("Registered Orbis Origins custom UI page supplier");

        OrbisOriginsConfig cfg = config.get();

        // Write default config.json if missing so server admins can find and edit it
        Path configPath = this.getDataDirectory().resolve("config.json");
        if (!Files.exists(configPath)) {
            config.save().join();
            LOGGER.atInfo().log("Created default config at %s", configPath);
        }

        // Register first join system only when config allows giving selector on first join
        if (cfg.isGiveSpeciesSelectorOnFirstJoin()) {
            FirstJoinSystem firstJoinSystem = new FirstJoinSystem();
            Query<?> firstJoinQuery = firstJoinSystem.getQuery();
            LOGGER.atInfo().log("FirstJoinSystem query: %s (null: %s)", firstJoinQuery, firstJoinQuery == null);
            this.getEntityStoreRegistry().registerSystem(firstJoinSystem);
            LOGGER.atInfo().log("Registered Orbis Origins first join system");
        } else {
            LOGGER.atInfo().log("First join species selector disabled by config");
        }

        // If crafting the species selector is disabled, remove the recipe (now or when it loads)
        if (!cfg.isAllowCraftingSpeciesSelector()) {
            removeSpeciesSelectorRecipeIfPresent();
            this.getEventRegistry().register(
                    LoadedAssetsEvent.class,
                    CraftingRecipe.class,
                    this::onCraftingRecipesLoaded
            );
        }
        
        // Register species model system (runs after PlayerSpawnedSystem to re-apply models on spawn)
        SpeciesModelSystem speciesModelSystem = new SpeciesModelSystem();
        this.getEntityStoreRegistry().registerSystem(speciesModelSystem);
        LOGGER.atInfo().log("Registered Orbis Origins species model system");
        
        // Register species model maintenance system (periodically re-applies models to ensure persistence)
        SpeciesModelMaintenanceSystem maintenanceSystem = new SpeciesModelMaintenanceSystem();
        this.getEntityStoreRegistry().registerSystem(maintenanceSystem);
        LOGGER.atInfo().log("Registered Orbis Origins species model maintenance system");

        // Register sleeping raise system (raises player position when sleeping to prevent clipping)
        this.getEntityStoreRegistry().registerSystem(new SpeciesSleepingRaiseSystem());
        LOGGER.atInfo().log("Registered Orbis Origins species sleeping raise system");
        
        // Register damage resistance system
        SpeciesDamageResistanceSystem damageResistanceSystem = new SpeciesDamageResistanceSystem();
        Query<?> damageQuery = damageResistanceSystem.getQuery();
        LOGGER.atInfo().log("SpeciesDamageResistanceSystem query: %s (null: %s)", damageQuery, damageQuery == null);
        if (damageQuery == null) {
            LOGGER.atSevere().log("SpeciesDamageResistanceSystem.getQuery() returned null! Player.getComponentType() = %s", 
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        }
        this.getEntityStoreRegistry().registerSystem(damageResistanceSystem);
        LOGGER.atInfo().log("Registered Orbis Origins damage resistance system");
        
        // Register commands
        this.getCommandRegistry().registerCommand(new OriginsCommand(this));
        LOGGER.atInfo().log("Registered Orbis Origins commands");
    }

    /**
     * Removes the species selector recipe from the asset store if it is already loaded.
     * This triggers RemovedAssetsEvent and the built-in CraftingPlugin removes it from all benches.
     */
    private void removeSpeciesSelectorRecipeIfPresent() {
        if (CraftingRecipe.getAssetMap().getAsset(SPECIES_SELECTOR_RECIPE_ID) != null) {
            CraftingRecipe.getAssetStore().removeAssets(List.of(SPECIES_SELECTOR_RECIPE_ID));
            LOGGER.atInfo().log("Species selector recipe disabled by config (removed from crafting)");
        }
    }

    /**
     * Handles late-loaded crafting recipes so we can remove the species selector recipe
     * if it loads after plugin setup (e.g. due to asset load order).
     */
    private void onCraftingRecipesLoaded(
            @Nonnull LoadedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event) {
        if (event.getLoadedAssets().containsKey(SPECIES_SELECTOR_RECIPE_ID)) {
            removeSpeciesSelectorRecipeIfPresent();
        }
    }
    
    @Override
    protected void shutdown() {
        // Save all player data before shutdown
        com.hexvane.orbisorigins.data.PlayerDataStorage.saveAll();
        LOGGER.atInfo().log("Saved all player data on shutdown");
    }
}
