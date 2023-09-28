package smartin.miapi.modules.properties.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import smartin.miapi.modules.properties.material.palette.MaterialPalette;
import smartin.miapi.modules.properties.util.ModuleProperty;
import smartin.miapi.registries.RegistryInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMaterial implements Material {
    public String key;
    protected JsonElement rawJson;

    public JsonMaterial(JsonObject element) {
        rawJson = element;
        key = element.get("key").getAsString();
    }

    @Environment(EnvType.CLIENT)
    public static JsonMaterial getClient(JsonObject element) {
        return new ClientJsonMaterial(element);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public List<String> getGroups() {
        List<String> groups = new ArrayList<>();
        groups.add(key);
        if (rawJson.getAsJsonObject().has("groups")) {
            JsonArray groupsJson = rawJson.getAsJsonObject().getAsJsonArray("groups");
            for (JsonElement groupElement : groupsJson) {
                String group = groupElement.getAsString();
                groups.add(group);
            }
        }
        return groups;
    }

    @Override
    public Map<ModuleProperty, JsonElement> materialProperties(String key) {
        JsonElement element = rawJson.getAsJsonObject().get(key);
        Map<ModuleProperty, JsonElement> propertyMap = new HashMap<>();
        if (element != null) {
            element.getAsJsonObject().entrySet().forEach(stringJsonElementEntry -> {
                ModuleProperty property = RegistryInventory.moduleProperties.get(stringJsonElementEntry.getKey());
                if (property != null) {
                    propertyMap.put(property, stringJsonElementEntry.getValue());
                }
            });
        }
        return propertyMap;
    }

    @Override
    public JsonElement getRawElement(String key) {
        return rawJson.getAsJsonObject().get(key);
    }

    @Override
    public double getDouble(String property) {
        String[] keys = property.split("\\.");
        JsonElement jsonData = rawJson;
        for (String k : keys) {
            jsonData = jsonData.getAsJsonObject().get(k);
            if (jsonData == null) {
                break;
            }
        }
        if (jsonData != null) {
            return jsonData.getAsDouble();
        }
        return 0;
    }

    @Override
    public String getData(String property) {
        String[] keys = property.split("\\.");
        JsonElement jsonData = rawJson;
        for (String key : keys) {
            jsonData = jsonData.getAsJsonObject().get(key);
            if (jsonData == null) {
                break;
            }
        }
        if (jsonData != null) {
            return jsonData.getAsString();
        }
        return "";
    }

    @Override
    public List<String> getTextureKeys() {
        return null;
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public MaterialPalette getPalette() {
        return null;
    }

    @Override
    public double getValueOfItem(ItemStack item) {
        JsonArray items = rawJson.getAsJsonObject().getAsJsonArray("items");

        for (JsonElement element : items) {
            JsonObject itemObj = element.getAsJsonObject();

            if (itemObj.has("item")) {
                String itemId = itemObj.get("item").getAsString();
                if (Registries.ITEM.getId(item.getItem()).toString().equals(itemId)) {
                    try {
                        return itemObj.get("value").getAsDouble();
                    } catch (Exception surpressed) {
                        return 1;
                    }
                }
            } else if (itemObj.has("tag")) {
                String tagId = itemObj.get("tag").getAsString();
                TagKey<Item> tag = TagKey.of(Registries.ITEM.getKey(), new Identifier(tagId));
                if (tag != null && item.isIn(tag)) {
                    try {
                        return itemObj.get("value").getAsDouble();
                    } catch (Exception suppressed) {
                        return 1;
                    }
                }
            }
        }
        return 0;
    }
}
