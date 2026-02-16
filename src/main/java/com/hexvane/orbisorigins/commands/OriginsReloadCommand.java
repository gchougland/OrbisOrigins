package com.hexvane.orbisorigins.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hexvane.orbisorigins.OrbisOriginsPlugin;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hexvane.orbisorigins.util.SpeciesStatUtil;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Command to reload species files and reapply species to all online players.
 * Usage: /origins reload
 */
public class OriginsReloadCommand extends CommandBase {
    private static final Logger LOGGER = Logger.getLogger(OriginsReloadCommand.class.getName());
    private final OrbisOriginsPlugin plugin;

    public OriginsReloadCommand(@Nonnull OrbisOriginsPlugin plugin) {
        super("reload", "Reloads all species files and reapplies species to all online players");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("§a[Orbis Origins] Reloading species files..."));

        try {
            // Reload species registry
            SpeciesRegistry.initialize(plugin.getDataDirectory());
            int speciesCount = SpeciesRegistry.getAllSpecies().size();
            context.sendMessage(Message.raw("§a[Orbis Origins] Loaded " + speciesCount + " species"));

            // Reapply species to all online players
            // We need to execute on each world's thread, so we'll collect the work and execute it
            for (World world : Universe.get().getWorlds().values()) {
                // Collect player refs first (this is safe to do off-thread)
                java.util.List<PlayerRef> playerRefs = new java.util.ArrayList<>(world.getPlayerRefs());
                
                // Execute the reload for each world on its thread
                world.execute(() -> {
                    var store = world.getEntityStore().getStore();
                    int playersUpdated = 0;
                    
                    for (PlayerRef playerRef : playerRefs) {
                        if (!playerRef.isValid()) {
                            continue;
                        }

                        var ref = playerRef.getReference();
                        if (ref == null || !ref.isValid()) {
                            continue;
                        }

                        // Check if player has chosen a species
                        if (!PlayerSpeciesData.hasChosenSpecies(ref, store, world)) {
                            continue;
                        }

                        // Use effective species so that removed species fall back to default without breaking
                        String speciesId = PlayerSpeciesData.getEffectiveSpeciesId(ref, store, world);
                        int variantIndex = PlayerSpeciesData.getEffectiveVariantIndex(ref, store, world);

                        if (speciesId == null) {
                            continue;
                        }

                        SpeciesData species = SpeciesRegistry.getSpeciesOrDefault(speciesId);
                        if (species == null) {
                            continue;
                        }

                        String storedId = PlayerSpeciesData.getSelectedSpeciesId(ref, store, world);
                        if (storedId != null && !storedId.equals(speciesId)) {
                            LOGGER.info("OriginsReloadCommand: Player " + playerRef.getUuid() + " had removed species '" + storedId + "', reapplying as " + speciesId);
                        }

                        // Reapply stats
                        SpeciesStatUtil.applySpeciesStats(ref, store, species);

                        // Reapply model (if not orbian)
                        if (!species.usesPlayerModel()) {
                            if (species.isVersion2()) {
                                java.util.Map<String, String> attachmentSelections = PlayerSpeciesData.getAttachmentSelections(ref, store, world);
                                String textureSelection = PlayerSpeciesData.getTextureSelection(ref, store, world);
                                ModelUtil.applyModelToPlayerV2(ref, store, species, variantIndex, textureSelection, attachmentSelections);
                            } else {
                                String modelName = species.getModelName(variantIndex);
                                float eyeHeightModifier = species.getEyeHeightModifier(modelName);
                                float hitboxHeightModifier = species.getHitboxHeightModifier(modelName);
                                ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier);
                            }
                        } else {
                            ModelUtil.resetToPlayerSkin(ref, store);
                        }

                        playersUpdated++;
                    }
                    
                    LOGGER.info("OriginsReloadCommand: Reloaded species for world " + world.getName() + " and updated " + playersUpdated + " player(s)");
                });
            }

            context.sendMessage(Message.raw("§a[Orbis Origins] Reload complete! Species will be reapplied to players on their world threads."));
            LOGGER.info("OriginsReloadCommand: Reloaded species registry, queued player updates for all worlds");
        } catch (Exception e) {
            LOGGER.severe("OriginsReloadCommand: Error during reload: " + e.getMessage());
            e.printStackTrace();
            context.sendMessage(Message.raw("§c[Orbis Origins] Error during reload: " + e.getMessage()));
        }
    }
}
