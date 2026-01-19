package com.hexvane.orbisorigins;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hexvane.orbisorigins.gui.OrbisOriginsPageSupplier;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.systems.FirstJoinSystem;
import com.hexvane.orbisorigins.systems.SpeciesDamageResistanceSystem;
import com.hexvane.orbisorigins.systems.SpeciesModelSystem;
import com.hexvane.orbisorigins.systems.SpeciesModelMaintenanceSystem;
import com.hypixel.hytale.component.query.Query;

public class OrbisOriginsPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public OrbisOriginsPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        // Initialize species registry
        SpeciesRegistry.initialize();
        
        // Register the custom UI page supplier
        OpenCustomUIInteraction.PAGE_CODEC.register(
                "OrbisOriginsSpeciesSelection",
                OrbisOriginsPageSupplier.class,
                OrbisOriginsPageSupplier.CODEC
        );
        LOGGER.atInfo().log("Registered Orbis Origins custom UI page supplier");
        
        // Register first join system
        FirstJoinSystem firstJoinSystem = new FirstJoinSystem();
        Query<?> firstJoinQuery = firstJoinSystem.getQuery();
        LOGGER.atInfo().log("FirstJoinSystem query: %s (null: %s)", firstJoinQuery, firstJoinQuery == null);
        this.getEntityStoreRegistry().registerSystem(firstJoinSystem);
        LOGGER.atInfo().log("Registered Orbis Origins first join system");
        
        // Register species model system (runs after PlayerSpawnedSystem to re-apply models on spawn)
        SpeciesModelSystem speciesModelSystem = new SpeciesModelSystem();
        this.getEntityStoreRegistry().registerSystem(speciesModelSystem);
        LOGGER.atInfo().log("Registered Orbis Origins species model system");
        
        // Register species model maintenance system (periodically re-applies models to ensure persistence)
        SpeciesModelMaintenanceSystem maintenanceSystem = new SpeciesModelMaintenanceSystem();
        this.getEntityStoreRegistry().registerSystem(maintenanceSystem);
        LOGGER.atInfo().log("Registered Orbis Origins species model maintenance system");
        
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
    }
}
