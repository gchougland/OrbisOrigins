package com.hexvane.orbisorigins.util;

import com.hexvane.orbisorigins.species.AttachmentOption;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility for discovering model attachments from Hytale model assets.
 * Reads RandomAttachmentSets from ModelAsset definitions using Hytale's asset system.
 */
public class AttachmentDiscoveryUtil {
    private static final Logger LOGGER = Logger.getLogger(AttachmentDiscoveryUtil.class.getName());
    
    // Cache for discovered attachments per model
    private static final Map<String, Map<String, Map<String, AttachmentOption>>> DISCOVERY_CACHE = new HashMap<>();

    /**
     * Discovers attachments for a given model name using Hytale's asset system.
     * Handles model inheritance (Parent field) by merging attachment sets.
     * 
     * @param modelName The model name (e.g., "Kweebec_Sapling_Pink")
     * @return Map of attachment type -> option name -> AttachmentOption, or null if not found
     */
    @Nullable
    public static Map<String, Map<String, AttachmentOption>> discoverAttachments(@Nonnull String modelName) {
        // Check cache first
        if (DISCOVERY_CACHE.containsKey(modelName)) {
            return DISCOVERY_CACHE.get(modelName);
        }

        try {
            Map<String, Map<String, AttachmentOption>> attachments = new HashMap<>();
            
            // Get the ModelAsset using Hytale's asset system
            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelName);
            if (modelAsset == null) {
                LOGGER.fine("AttachmentDiscoveryUtil: Model asset not found for: " + modelName);
                DISCOVERY_CACHE.put(modelName, null);
                return null;
            }

            // Get RandomAttachmentSets from the model asset
            // Note: Hytale's asset system automatically handles parent inheritance when loading assets,
            // so getRandomAttachmentSets() should already include inherited attachments
            Map<String, Map<String, ModelAttachment>> randomAttachmentSets = modelAsset.getRandomAttachmentSets();
            if (randomAttachmentSets != null && !randomAttachmentSets.isEmpty()) {
                LOGGER.info("AttachmentDiscoveryUtil: Found RandomAttachmentSets for " + modelName + " with " + randomAttachmentSets.size() + " attachment types");
                
                for (Map.Entry<String, Map<String, ModelAttachment>> attachmentTypeEntry : randomAttachmentSets.entrySet()) {
                    String attachmentType = attachmentTypeEntry.getKey();
                    Map<String, ModelAttachment> attachmentOptions = attachmentTypeEntry.getValue();
                    
                    Map<String, AttachmentOption> options = attachments.computeIfAbsent(attachmentType, k -> new HashMap<>());
                    
                    for (Map.Entry<String, ModelAttachment> optionEntry : attachmentOptions.entrySet()) {
                        String optionName = optionEntry.getKey();
                        ModelAttachment attachment = optionEntry.getValue();
                        
                        // Skip "null" options (they only have Weight, no Model/Texture)
                        if ("null".equals(optionName)) {
                            // Add a null option that represents "no attachment"
                            options.put("null", new AttachmentOption("", "", "None"));
                            continue;
                        }
                        
                        // Extract model and texture from ModelAttachment
                        String model = attachment.getModel();
                        String texture = attachment.getTexture();
                        if (model != null && texture != null) {
                            options.put(optionName, new AttachmentOption(model, texture, optionName));
                        }
                    }
                }
            } else {
                LOGGER.fine("AttachmentDiscoveryUtil: No RandomAttachmentSets found for " + modelName);
            }

            // Cache the result
            if (!attachments.isEmpty()) {
                LOGGER.info("AttachmentDiscoveryUtil: Discovered " + attachments.size() + " attachment types for " + modelName);
            }
            DISCOVERY_CACHE.put(modelName, attachments.isEmpty() ? null : attachments);
            return attachments.isEmpty() ? null : attachments;
            
        } catch (Exception e) {
            LOGGER.warning("AttachmentDiscoveryUtil: Failed to discover attachments for " + modelName + ": " + e.getMessage());
            e.printStackTrace();
            DISCOVERY_CACHE.put(modelName, null);
            return null;
        }
    }

    /**
     * Clears the discovery cache. Useful for reloading after model changes.
     */
    public static void clearCache() {
        DISCOVERY_CACHE.clear();
    }
}
