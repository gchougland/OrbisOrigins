package com.hexvane.orbisorigins.species;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles JSON deserialization for species data.
 * Supports version 1 (legacy) and version 2 (new variant/attachment format).
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
        Integer version; // 1 = legacy, 2 = new format; null/missing = 1
        String id;
        String displayName;
        String displayNameKey;
        String description;
        String descriptionKey;
        String modelBaseName;
        String modelNamespace;
        List<Object> variants; // v1: String or {modelName, namespace}; v2: variant objects
        int healthModifier;
        int staminaModifier;
        int manaModifier;
        Boolean enabled;
        Boolean usePlayerModel;
        Boolean enableAttachmentDiscovery;
        Map<String, Object> attachments; // Manual attachment definitions (v1 only)
        Map<String, Float> eyeHeightModifiers; // Per-variant eye height modifiers (v1 only)
        Map<String, Float> hitboxHeightModifiers; // Per-variant hitbox height modifiers (v1 only)
        List<String> starterItems;
        Map<String, Float> damageResistances;

        @Nonnull
        SpeciesData toSpeciesData() {
            int ver = (version != null && version == 2) ? 2 : 1;

            // Validate required fields
            if (id == null || id.isEmpty()) {
                throw new JsonParseException("Missing required field: id");
            }
            if (displayName == null || displayName.isEmpty()) {
                throw new JsonParseException("Missing required field: displayName");
            }
            if (description == null) {
                throw new JsonParseException("Missing required field: description");
            }
            if (variants == null) {
                throw new JsonParseException("Missing required field: variants");
            }

            if (ver == 2) {
                return toSpeciesDataV2(ver);
            } else {
                return toSpeciesDataV1(ver);
            }
        }

        @Nonnull
        private SpeciesData toSpeciesDataV1(int ver) {
            if (modelBaseName == null) {
                throw new JsonParseException("Missing required field: modelBaseName");
            }
            if (variants.isEmpty() && (modelBaseName == null || modelBaseName.isEmpty())) {
                // Valid - orbian / usePlayerModel
            } else if (variants.isEmpty()) {
                throw new JsonParseException("Variants array cannot be empty unless modelBaseName is also empty or usePlayerModel is true");
            }

            List<String> variantList = new ArrayList<>();
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

            validateDamageResistances();
            List<String> items = starterItems != null ? starterItems : new ArrayList<>();
            Map<String, Float> resistances = damageResistances != null ? damageResistances : new HashMap<>();
            Map<String, Float> eyeHeightMods = eyeHeightModifiers != null ? eyeHeightModifiers : new HashMap<>();
            Map<String, Float> hitboxHeightMods = hitboxHeightModifiers != null ? hitboxHeightModifiers : new HashMap<>();
            boolean isEnabled = enabled != null ? enabled : true;
            boolean usePlayerModelFlag = Boolean.TRUE.equals(usePlayerModel)
                    || (variantList.isEmpty() && (modelBaseName == null || modelBaseName.isEmpty()));
            boolean isAttachmentDiscoveryEnabled = enableAttachmentDiscovery != null ? enableAttachmentDiscovery : false;
            Map<String, Map<String, AttachmentOption>> manualAttachments = parseManualAttachments(attachments);

            return new SpeciesData(
                    ver,
                    id,
                    displayName,
                    displayNameKey,
                    modelBaseName,
                    variantList,
                    null,
                    description,
                    descriptionKey,
                    healthModifier,
                    staminaModifier,
                    manaModifier,
                    isEnabled,
                    usePlayerModelFlag,
                    isAttachmentDiscoveryEnabled,
                    manualAttachments,
                    eyeHeightMods,
                    hitboxHeightMods,
                    items,
                    resistances
            );
        }

        @Nonnull
        private SpeciesData toSpeciesDataV2(int ver) {
            List<SpeciesVariantData> variantDataList = new ArrayList<>();
            String baseModel = modelBaseName != null ? modelBaseName : "";

            for (Object variant : variants) {
                if (!(variant instanceof Map)) {
                    throw new JsonParseException("Version 2 variants must be objects");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> variantMap = (Map<String, Object>) variant;
                SpeciesVariantData v = parseV2Variant(variantMap);
                variantDataList.add(v);
                if (baseModel.isEmpty() && !variantDataList.isEmpty()) {
                    baseModel = v.getParentModel();
                }
            }

            boolean usePlayerModelFlag = Boolean.TRUE.equals(usePlayerModel);
            if (variantDataList.isEmpty() && !baseModel.isEmpty() && !usePlayerModelFlag) {
                throw new JsonParseException("Version 2: variants array cannot be empty unless usePlayerModel is true");
            }

            validateDamageResistances();
            List<String> items = starterItems != null ? starterItems : new ArrayList<>();
            Map<String, Float> resistances = damageResistances != null ? damageResistances : new HashMap<>();
            boolean isEnabled = enabled != null ? enabled : true;

            return new SpeciesData(
                    ver,
                    id,
                    displayName,
                    displayNameKey,
                    baseModel,
                    Collections.emptyList(),
                    variantDataList,
                    description,
                    descriptionKey,
                    healthModifier,
                    staminaModifier,
                    manaModifier,
                    isEnabled,
                    usePlayerModelFlag,
                    false,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    items,
                    resistances
            );
        }

        @Nonnull
        private SpeciesVariantData parseV2Variant(@Nonnull Map<String, Object> variantMap) {
            Object parentObj = variantMap.get("ParentModel");
            Object modelObj = variantMap.get("Model");
            if (parentObj == null || modelObj == null) {
                throw new JsonParseException("Version 2 variant must have ParentModel and Model");
            }
            String parentModel = parentObj.toString();
            String model = modelObj.toString();
            String variantName = getString(variantMap, "VariantName", "Unknown");

            List<String> textures = parseStringList(variantMap.get("Textures"));
            Float eyeHeight = getFloat(variantMap, "EyeHeight");
            Float crouchOffset = getFloat(variantMap, "CrouchOffset");
            Box hitBox = parseHitBox(variantMap.get("HitBox"));

            List<SpeciesVariantData.DefaultAttachmentDef> defaultAttachments = parseDefaultAttachments(variantMap.get("defaultAttachments"));
            Map<String, SpeciesVariantData.AttachmentSlotDef> attachments = parseV2Attachments(variantMap.get("attachments"));

            return new SpeciesVariantData(
                    variantName,
                    parentModel,
                    model,
                    textures,
                    eyeHeight,
                    crouchOffset,
                    hitBox,
                    defaultAttachments,
                    attachments
            );
        }

        @Nullable
        private String getString(Map<String, Object> map, String key, @Nullable String fallback) {
            Object v = map.get(key);
            return v != null ? v.toString() : fallback;
        }

        @Nullable
        private Float getFloat(Map<String, Object> map, String key) {
            Object v = map.get(key);
            if (v == null) return null;
            if (v instanceof Number) return ((Number) v).floatValue();
            try {
                return Float.parseFloat(v.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Nonnull
        private List<String> parseStringList(@Nullable Object obj) {
            List<String> result = new ArrayList<>();
            if (obj instanceof List) {
                for (Object item : (List<?>) obj) {
                    if (item != null) result.add(item.toString());
                }
            }
            return result;
        }

        @Nullable
        private Box parseHitBox(@Nullable Object obj) {
            if (obj == null || !(obj instanceof Map)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            Object minObj = map.get("min");
            Object maxObj = map.get("max");
            if (minObj == null || maxObj == null) return null;
            if (!(minObj instanceof Map) || !(maxObj instanceof Map)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> minMap = (Map<String, Object>) minObj;
            @SuppressWarnings("unchecked")
            Map<String, Object> maxMap = (Map<String, Object>) maxObj;
            double minX = getDoubleAnyCase(minMap, "x", 0);
            double minY = getDoubleAnyCase(minMap, "y", 0);
            double minZ = getDoubleAnyCase(minMap, "z", 0);
            double maxX = getDoubleAnyCase(maxMap, "x", 1);
            double maxY = getDoubleAnyCase(maxMap, "y", 1);
            double maxZ = getDoubleAnyCase(maxMap, "z", 1);
            return new Box(new Vector3d(minX, minY, minZ), new Vector3d(maxX, maxY, maxZ));
        }

        private double getDouble(Map<String, Object> map, String key, double fallback) {
            Object v = map.get(key);
            if (v == null) return fallback;
            if (v instanceof Number) return ((Number) v).doubleValue();
            try {
                return Double.parseDouble(v.toString());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private double getDoubleAnyCase(Map<String, Object> map, String key, double fallback) {
            Object v = map.get(key);
            if (v == null) v = map.get(key.toUpperCase());
            if (v == null) return fallback;
            if (v instanceof Number) return ((Number) v).doubleValue();
            try {
                return Double.parseDouble(v.toString());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        @Nonnull
        private List<SpeciesVariantData.DefaultAttachmentDef> parseDefaultAttachments(@Nullable Object obj) {
            List<SpeciesVariantData.DefaultAttachmentDef> result = new ArrayList<>();
            if (!(obj instanceof List)) return result;
            for (Object item : (List<?>) obj) {
                if (!(item instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) item;
                Object modelObj = m.get("Model");
                Object textureObj = m.get("Texture");
                if (modelObj != null && textureObj != null) {
                    result.add(new SpeciesVariantData.DefaultAttachmentDef(modelObj.toString(), textureObj.toString()));
                }
            }
            return result;
        }

        @Nonnull
        private Map<String, SpeciesVariantData.AttachmentSlotDef> parseV2Attachments(@Nullable Object obj) {
            Map<String, SpeciesVariantData.AttachmentSlotDef> result = new HashMap<>();
            if (!(obj instanceof Map)) return result;
            @SuppressWarnings("unchecked")
            Map<String, Object> slots = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : slots.entrySet()) {
                if (!(entry.getValue() instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> slotMap = (Map<String, Object>) entry.getValue();
                boolean allowsNone = Boolean.TRUE.equals(slotMap.get("allowsNone"));
                List<AttachmentOption> options = new ArrayList<>();
                Object opts = slotMap.get("options");
                if (opts instanceof List) {
                    for (Object o : (List<?>) opts) {
                        if (!(o instanceof Map)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> optMap = (Map<String, Object>) o;
                        Object nameObj = optMap.get("Name");
                        Object modelObj = optMap.get("Model");
                        Object textureObj = optMap.get("Texture");
                        if (modelObj != null && textureObj != null) {
                            String name = nameObj != null ? nameObj.toString() : null;
                            options.add(new AttachmentOption(modelObj.toString(), textureObj.toString(), name));
                        }
                    }
                }
                result.put(entry.getKey(), new SpeciesVariantData.AttachmentSlotDef(allowsNone, options));
            }
            return result;
        }

        private void validateDamageResistances() {
            if (damageResistances != null) {
                for (Map.Entry<String, Float> entry : damageResistances.entrySet()) {
                    float value = entry.getValue();
                    if (value < 0.0f || value > 2.0f) {
                        throw new JsonParseException("Invalid damage resistance value for " + entry.getKey() +
                                ": " + value + " (must be between 0.0 and 2.0)");
                    }
                }
            }
        }

        /**
         * Parses manual attachment definitions from JSON.
         * Format: { "Hair": { "Option1": { "model": "...", "texture": "..." }, ... } }
         */
        @Nonnull
        private Map<String, Map<String, AttachmentOption>> parseManualAttachments(@Nullable Map<String, Object> attachmentsJson) {
            Map<String, Map<String, AttachmentOption>> result = new HashMap<>();
            
            if (attachmentsJson == null) {
                return result;
            }

            for (Map.Entry<String, Object> attachmentTypeEntry : attachmentsJson.entrySet()) {
                String attachmentType = attachmentTypeEntry.getKey();
                Object attachmentTypeValue = attachmentTypeEntry.getValue();
                
                if (!(attachmentTypeValue instanceof Map)) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> options = (Map<String, Object>) attachmentTypeValue;
                Map<String, AttachmentOption> optionMap = new HashMap<>();
                
                for (Map.Entry<String, Object> optionEntry : options.entrySet()) {
                    String optionName = optionEntry.getKey();
                    Object optionValue = optionEntry.getValue();
                    
                    if (!(optionValue instanceof Map)) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> optionData = (Map<String, Object>) optionValue;
                    
                    Object modelObj = optionData.get("model");
                    Object textureObj = optionData.get("texture");
                    Object displayNameObj = optionData.get("displayName");
                    
                    if (modelObj != null && textureObj != null) {
                        String model = modelObj.toString();
                        String texture = textureObj.toString();
                        String displayName = displayNameObj != null ? displayNameObj.toString() : null;
                        optionMap.put(optionName, new AttachmentOption(model, texture, displayName));
                    }
                }
                
                if (!optionMap.isEmpty()) {
                    result.put(attachmentType, optionMap);
                }
            }
            
            return result;
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

            data.version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : null;
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
            data.enableAttachmentDiscovery = jsonObject.has("enableAttachmentDiscovery") ? jsonObject.get("enableAttachmentDiscovery").getAsBoolean() : null;

            // Deserialize manual attachments
            if (jsonObject.has("attachments")) {
                data.attachments = context.deserialize(jsonObject.get("attachments"), 
                        new com.nimbusds.jose.shaded.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
            }

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
