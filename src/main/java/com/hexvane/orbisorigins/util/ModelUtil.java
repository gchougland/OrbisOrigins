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
        applyModelToPlayer(playerRef, store, modelName, 0.0f);
    }

    /**
     * Applies a model to a player entity with an eye height modifier.
     * @param eyeHeightModifier Additive modifier to the model's base eye height (in blocks)
     */
    public static void applyModelToPlayer(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String modelName,
            float eyeHeightModifier
    ) {
        applyModelToPlayer(playerRef, store, modelName, eyeHeightModifier, 0.0f);
    }

    /**
     * Applies a model to a player entity with modifiers.
     * @param eyeHeightModifier Additive modifier to the model's base eye height (in blocks)
     * @param hitboxHeightModifier Additive modifier to the bounding box height (in blocks, modifies max.y)
     */
    public static void applyModelToPlayer(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull String modelName,
            float eyeHeightModifier,
            float hitboxHeightModifier
    ) {
        Model baseModel = getModel(modelName);
        if (baseModel == null) {
            LOGGER.warning("ModelUtil: Failed to get model: " + modelName);
            return;
        }
        
        Model modelToApply = baseModel;
        boolean needsNewModel = false;
        float newEyeHeight = baseModel.getEyeHeight();
        com.hypixel.hytale.math.shape.Box newBoundingBox = baseModel.getBoundingBox();
        
        // Check if we need to modify eye height
        if (Math.abs(eyeHeightModifier) > 0.001f) {
            needsNewModel = true;
            newEyeHeight = baseModel.getEyeHeight() + eyeHeightModifier;
        }
        
        // Check if we need to modify hitbox height
        if (Math.abs(hitboxHeightModifier) > 0.001f && baseModel.getBoundingBox() != null) {
            needsNewModel = true;
            // Clone the bounding box and modify max.y to change height
            newBoundingBox = baseModel.getBoundingBox().clone();
            newBoundingBox.max.y += hitboxHeightModifier;
            // Ensure height is still positive
            if (newBoundingBox.height() <= 0.0) {
                LOGGER.warning("ModelUtil: Hitbox height modifier would result in invalid height, clamping");
                newBoundingBox.max.y = newBoundingBox.min.y + 0.1; // Minimum height of 0.1 blocks
            }
        }
        
        // Create a new Model if we modified anything
        if (needsNewModel) {
            modelToApply = new Model(
                    baseModel.getModelAssetId(),
                    baseModel.getScale(),
                    baseModel.getRandomAttachmentIds(),
                    baseModel.getAttachments(),
                    newBoundingBox, // Modified bounding box (or original)
                    baseModel.getModel(),
                    baseModel.getTexture(),
                    baseModel.getGradientSet(),
                    baseModel.getGradientId(),
                    newEyeHeight, // Modified eye height (or original)
                    baseModel.getCrouchOffset(),
                    baseModel.getAnimationSetMap(),
                    baseModel.getCamera(),
                    baseModel.getLight(),
                    baseModel.getParticles(),
                    baseModel.getTrails(),
                    baseModel.getPhysicsValues(),
                    baseModel.getDetailBoxes(),
                    baseModel.getPhobia(),
                    baseModel.getPhobiaModelAssetId()
            );
            LOGGER.fine("ModelUtil: Applied model with modifiers: " + modelName + 
                    " (eyeHeight: " + baseModel.getEyeHeight() + " -> " + newEyeHeight + 
                    ", hitboxHeight: " + (baseModel.getBoundingBox() != null ? baseModel.getBoundingBox().height() : "null") + 
                    " -> " + (newBoundingBox != null ? newBoundingBox.height() : "null") + ")");
        }
        
        store.putComponent(playerRef, ModelComponent.getComponentType(), new ModelComponent(modelToApply));
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
