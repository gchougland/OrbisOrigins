package com.hexvane.orbisorigins.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for managing player model changes.
 */
public class ModelUtil {
    private static final Logger LOGGER = Logger.getLogger(ModelUtil.class.getName());
    
    /**
     * Gets a model by name.
     */
    @Nullable
    public static Model getModel(@Nonnull String modelName) {
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelName);
        if (modelAsset == null) {
            LOGGER.warning("ModelUtil: Model asset not found: " + modelName);
            // Try to find similar model names
            var allModels = ModelAsset.getAssetMap().getAssetMap().keySet();
            LOGGER.info("ModelUtil: Available models (first 20): " + 
                allModels.stream().sorted().limit(20).collect(java.util.stream.Collectors.joining(", ")));
            // Try common variations
            String[] variations = {
                "NPC/Intelligent/" + modelName,
                "NPC/" + modelName,
                modelName.replace("_", "/"),
                modelName.toLowerCase(),
                modelName.toUpperCase()
            };
            for (String variation : variations) {
                ModelAsset altAsset = ModelAsset.getAssetMap().getAsset(variation);
                if (altAsset != null) {
                    LOGGER.info("ModelUtil: Found model with variation: " + variation);
                    return Model.createScaledModel(altAsset, 1.0f);
                }
            }
            return null;
        }
        return Model.createScaledModel(modelAsset, 1.0f);
    }

    /**
     * Applies a model to a player entity.
     */
    public static void applyModelToPlayer(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String modelName
    ) {
        Model model = getModel(modelName);
        if (model == null) {
            LOGGER.warning("ModelUtil: Failed to get model: " + modelName);
            return;
        }
        
        store.putComponent(playerRef, ModelComponent.getComponentType(), new ModelComponent(model));
        LOGGER.fine("ModelUtil: Applied model to player: " + modelName);
    }

    /**
     * Resets a player to their default skin model.
     */
    public static void resetToPlayerSkin(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store
    ) {
        PlayerSkinComponent skinComponent = store.getComponent(playerRef, PlayerSkinComponent.getComponentType());
        if (skinComponent == null) {
            return;
        }

        Model newModel = CosmeticsModule.get().createModel(skinComponent.getPlayerSkin());
        store.putComponent(playerRef, ModelComponent.getComponentType(), new ModelComponent(newModel));
        skinComponent.setNetworkOutdated();
    }
}
