package com.hexvane.orbisorigins.species;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles JSON deserialization for species data.
 * Supports flexible variant formats (string or object with namespace).
 */
public class SpeciesJsonCodec {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SpeciesJsonData.class, new SpeciesDeserializer())
            .create();

    /**
     * Deserializes a JSON string into a SpeciesData object.
     */
    @Nonnull
    public static SpeciesData fromJson(@Nonnull String json) throws JsonParseException {
        SpeciesJsonData jsonData = GSON.fromJson(json, SpeciesJsonData.class);
        return jsonData.toSpeciesData();
    }

    /**
     * Intermediate JSON data structure.
     */
    private static class SpeciesJsonData {
        String id;
        String displayName;
        String displayNameKey;
        String description;
        String descriptionKey;
        String modelBaseName;
        String modelNamespace;
        List<Object> variants; // Can be String or Map
        int healthModifier;
        int staminaModifier;
        int manaModifier;
        Boolean enabled;
        Map<String, Float> eyeHeightModifiers; // Per-variant eye height modifiers
        Map<String, Float> hitboxHeightModifiers; // Per-variant hitbox height modifiers
        List<String> starterItems;
        Map<String, Float> damageResistances;

        @Nonnull
        SpeciesData toSpeciesData() {
            // Validate required fields
            if (id == null || id.isEmpty()) {
                throw new JsonParseException("Missing required field: id");
            }
            if (modelBaseName == null) {
                throw new JsonParseException("Missing required field: modelBaseName");
            }
            // Variants can be empty for orbian (no model), but must be present
            if (variants == null) {
                throw new JsonParseException("Missing required field: variants");
            }
            if (displayName == null || displayName.isEmpty()) {
                throw new JsonParseException("Missing required field: displayName");
            }
            if (description == null) {
                throw new JsonParseException("Missing required field: description");
            }

            // Process variants - convert to list of strings
            // Empty variants are allowed for orbian (no model)
            List<String> variantList = new ArrayList<>();
            String defaultNamespace = modelNamespace != null ? modelNamespace : "base";
            
            // If variants is empty and modelBaseName is empty, that's valid (orbian)
            if (variants.isEmpty() && (modelBaseName == null || modelBaseName.isEmpty())) {
                // Valid - orbian species with no model
            } else if (variants.isEmpty()) {
                throw new JsonParseException("Variants array cannot be empty unless modelBaseName is also empty (for orbian)");
            }
            
            for (Object variant : variants) {
                if (variant instanceof String) {
                    variantList.add((String) variant);
                } else if (variant instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> variantMap = (Map<String, Object>) variant;
                    Object modelNameObj = variantMap.get("modelName");
                    if (modelNameObj == null) {
                        throw new JsonParseException("Variant object missing 'modelName' field");
                    }
                    variantList.add(modelNameObj.toString());
                } else {
                    throw new JsonParseException("Invalid variant format: expected String or Object with 'modelName'");
                }
            }

            // Validate damage resistances
            if (damageResistances != null) {
                for (Map.Entry<String, Float> entry : damageResistances.entrySet()) {
                    float value = entry.getValue();
                    if (value < 0.0f || value > 2.0f) {
                        throw new JsonParseException("Invalid damage resistance value for " + entry.getKey() + 
                                ": " + value + " (must be between 0.0 and 2.0)");
                    }
                }
            }

            // Use empty lists/maps if null
            List<String> items = starterItems != null ? starterItems : new ArrayList<>();
            Map<String, Float> resistances = damageResistances != null ? damageResistances : new HashMap<>();
            Map<String, Float> eyeHeightMods = eyeHeightModifiers != null ? eyeHeightModifiers : new HashMap<>();
            Map<String, Float> hitboxHeightMods = hitboxHeightModifiers != null ? hitboxHeightModifiers : new HashMap<>();
            
            // Default enabled to true if not specified (for backward compatibility)
            boolean isEnabled = enabled != null ? enabled : true;

            return new SpeciesData(
                    id,
                    displayName,
                    displayNameKey,
                    modelBaseName,
                    variantList,
                    description,
                    descriptionKey,
                    healthModifier,
                    staminaModifier,
                    manaModifier,
                    isEnabled,
                    eyeHeightMods,
                    hitboxHeightMods,
                    items,
                    resistances
            );
        }
    }

    /**
     * Custom deserializer for SpeciesJsonData.
     */
    private static class SpeciesDeserializer implements JsonDeserializer<SpeciesJsonData> {
        @Override
        public SpeciesJsonData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            SpeciesJsonData data = new SpeciesJsonData();

            data.id = jsonObject.has("id") ? jsonObject.get("id").getAsString() : null;
            data.displayName = jsonObject.has("displayName") ? jsonObject.get("displayName").getAsString() : null;
            data.displayNameKey = jsonObject.has("displayNameKey") ? jsonObject.get("displayNameKey").getAsString() : null;
            data.description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : null;
            data.descriptionKey = jsonObject.has("descriptionKey") ? jsonObject.get("descriptionKey").getAsString() : null;
            data.modelBaseName = jsonObject.has("modelBaseName") ? jsonObject.get("modelBaseName").getAsString() : null;
            data.modelNamespace = jsonObject.has("modelNamespace") ? jsonObject.get("modelNamespace").getAsString() : null;
            data.healthModifier = jsonObject.has("healthModifier") ? jsonObject.get("healthModifier").getAsInt() : 0;
            data.staminaModifier = jsonObject.has("staminaModifier") ? jsonObject.get("staminaModifier").getAsInt() : 0;
            data.manaModifier = jsonObject.has("manaModifier") ? jsonObject.get("manaModifier").getAsInt() : 0;
            data.enabled = jsonObject.has("enabled") ? jsonObject.get("enabled").getAsBoolean() : null;

            // Deserialize eye height modifiers (per-variant)
            if (jsonObject.has("eyeHeightModifiers")) {
                data.eyeHeightModifiers = context.deserialize(jsonObject.get("eyeHeightModifiers"), 
                        new com.nimbusds.jose.shaded.gson.reflect.TypeToken<Map<String, Float>>(){}.getType());
            }

            // Deserialize hitbox height modifiers (per-variant)
            if (jsonObject.has("hitboxHeightModifiers")) {
                data.hitboxHeightModifiers = context.deserialize(jsonObject.get("hitboxHeightModifiers"), 
                        new com.nimbusds.jose.shaded.gson.reflect.TypeToken<Map<String, Float>>(){}.getType());
            }

            // Deserialize variants - can be array of strings or objects
            if (jsonObject.has("variants")) {
                data.variants = context.deserialize(jsonObject.get("variants"), List.class);
            }

            // Deserialize starter items
            if (jsonObject.has("starterItems")) {
                data.starterItems = context.deserialize(jsonObject.get("starterItems"), List.class);
            }

            // Deserialize damage resistances
            if (jsonObject.has("damageResistances")) {
                data.damageResistances = context.deserialize(jsonObject.get("damageResistances"), 
                        new com.nimbusds.jose.shaded.gson.reflect.TypeToken<Map<String, Float>>(){}.getType());
            }

            return data;
        }
    }
}
