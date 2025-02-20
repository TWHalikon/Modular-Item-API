package smartin.miapi.modules.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import smartin.miapi.Environment;
import smartin.miapi.Miapi;
import smartin.miapi.client.model.ModelTransformer;
import smartin.miapi.datapack.ReloadEvents;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.item.modular.VisualModularItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ModularItemCache {
    protected static Map<String, CacheObjectSupplier> supplierMap = new HashMap<>();
    public static final long CACHE_SIZE = 1000;
    public static final long CACHE_LIFETIME = 2;
    public static final TimeUnit CACHE_LIFETIME_UNIT = TimeUnit.MINUTES;
    protected static final LoadingCache<UUID, Cache> cache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterAccess(CACHE_LIFETIME, CACHE_LIFETIME_UNIT)
            .build(new CacheLoader<>() {
                public @NotNull Cache load(@NotNull UUID key) {
                    return new Cache(key, net.minecraft.item.ItemStack.EMPTY);
                }
            });
    protected static Map<ItemStack, UUID> lookUpTable = new WeakHashMap<>();
    protected static final LoadingCache<NbtCompound, UUID> nbtCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterAccess(CACHE_LIFETIME, CACHE_LIFETIME_UNIT)
            .build(new CacheLoader<>() {
                public @NotNull UUID load(@NotNull NbtCompound key) {
                    return ModularItemCache.getMissingUUID();
                }
            });

    public static void setSupplier(String key, CacheObjectSupplier supplier) {
        supplierMap.put(key, supplier);
    }

    @Nullable
    public static <T> T getRaw(ItemStack stack, String key) {
        if (!ReloadEvents.isInReload() && !stack.isEmpty() && stack.getItem() instanceof VisualModularItem) {
            Cache itemCache = find(stack);
            return (T) itemCache.get(key);
        }
        return null;
    }

    public static <T> T get(ItemStack stack, String key, T fallback) {
        if (!ReloadEvents.isInReload() && !stack.isEmpty() && stack.getItem() instanceof ModularItem) {
            Cache itemCache = find(stack);
            T object = (T) itemCache.get(key);
            if (object == null) {
                return fallback;
            }
            return object;
        }
        return fallback;
    }

    public static <T> T getVisualOnlyCache(ItemStack stack, String key, T fallback) {
        if (!ReloadEvents.isInReload() && !stack.isEmpty() && stack.getItem() instanceof VisualModularItem) {
            Cache itemCache = find(stack);
            T object = (T) itemCache.get(key);
            if (object == null) {
                return fallback;
            }
            return object;
        }
        return fallback;
    }

    public static void discardCache() {
        cache.cleanUp();
        cache.invalidateAll();
        if (Environment.isClient()) {
            ModelTransformer.clearCaches();
        }
    }

    @Nullable
    public static UUID getUUIDFor(ItemStack stack) {
        try {
            if (stack.getItem() instanceof VisualModularItem) {
                return nbtCache.get(stack.getOrCreateNbt());
            }
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setUUIDFor(ItemStack stack, UUID uuid) {
        if (stack.getItem() instanceof VisualModularItem) {
            if (stack.hasNbt()) {
                nbtCache.put(stack.getOrCreateNbt().copy(), uuid);
            }
        } else {
            Miapi.LOGGER.error("this shouldnt not be called");
        }
    }

    public static void clearUUIDFor(ItemStack stack) {
        if (stack.getItem() instanceof VisualModularItem) {
            nbtCache.invalidate(stack.getOrCreateNbt());
            UUID uuid = getUUIDFor(stack);
            cache.invalidate(uuid);
        }
    }

    protected static Cache find(ItemStack stack) {
        UUID lookUpUUId = lookUpTable.get(stack);
        if (lookUpUUId == null) {
            lookUpUUId = getUUIDFor(stack);
        }
        if (lookUpUUId == null) {
            lookUpUUId = getMissingUUID();
        }
        UUID uuid = lookUpUUId;
        try {
            return cache.get(lookUpUUId, () -> new Cache(uuid, stack));
        } catch (ExecutionException ignored) {
            UUID uuid1 = getMissingUUID();
            Cache cache1 = new Cache(uuid1, stack);
            cache.put(uuid1, cache1);
            return cache1;
        }
    }

    protected static UUID getMissingUUID() {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while (cache.getIfPresent(uuid) != null);
        return uuid;
    }

    public interface CacheObjectSupplier extends Function<ItemStack, Object> {
        @Override
        Object apply(ItemStack stack);
    }

    protected static class Cache {
        protected Map<String, Object> map = new ConcurrentHashMap<>();
        public UUID uuid;
        public ItemStack stack;

        public Cache(UUID uuid, ItemStack stack) {
            this.uuid = uuid;
            this.stack = stack.copy();
            setUUIDFor(stack.copy(), uuid);
        }

        public void set(String key, Object object) {
            map.put(key, object);
        }

        public Object get(String key) {
            if (map.containsKey(key)) {
                return map.get(key);
            } else {
                CacheObjectSupplier supplier = supplierMap.get(key);
                if (supplier != null) {
                    Object cached = supplier.apply(stack);
                    if (cached != null) {
                        map.put(key, cached);
                    }
                    return cached;
                }
            }
            return null;
        }
    }
}
