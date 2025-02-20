package smartin.miapi.forge;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import smartin.miapi.Environment;
import smartin.miapi.Miapi;
import smartin.miapi.attributes.AttributeRegistry;
import smartin.miapi.client.MiapiClient;
import smartin.miapi.config.MiapiConfig;
import smartin.miapi.datapack.ReloadEvents;
import smartin.miapi.forge.compat.epic_fight.EpicFightCompatProperty;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.modules.properties.AttributeProperty;
import smartin.miapi.modules.properties.compat.ht_treechop.TreechopUtil;
import smartin.miapi.registries.RegistryInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static smartin.miapi.Miapi.MOD_ID;
import static smartin.miapi.attributes.AttributeRegistry.SWIM_SPEED;

@Mod(MOD_ID)
public class TrulyModularForge {
    public TrulyModularForge() {
        // Submit our event bus to let architectury register our content on the right time
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(MOD_ID, bus);
        if (Environment.isClient()) {
            bus.register(new ClientEvents());
        }
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
        Miapi.init();

        RegistryInventory.moduleProperties.register(EpicFightCompatProperty.KEY, new EpicFightCompatProperty());


        LifecycleEvent.SERVER_STARTING.register((instance -> setupAttributes()));
        ReloadEvents.START.subscribe((isClient -> setupAttributes()));

        LifecycleEvent.SERVER_STARTED.register((minecraftServer -> {
            if (!Environment.isClient() && MiapiConfig.INSTANCE.server.other.forgeReloadMode) {
                Map<String, String> cacheDatapack = new HashMap<>(ReloadEvents.DATA_PACKS);
                Miapi.LOGGER.info("Truly Modular will now go onto reload twice.");
                Miapi.LOGGER.info("This is done because Forges classloading is buggy and stupid. Until we have a better fix, this is used");
                Miapi.LOGGER.info("This can be turned off in Miapis config.json");
                ReloadEvents.reloadCounter++;
                ReloadEvents.START.fireEvent(false);
                ReloadEvents.DataPackLoader.trigger(cacheDatapack);
                ReloadEvents.MAIN.fireEvent(false);
                ReloadEvents.END.fireEvent(false);
                ReloadEvents.reloadCounter--;
            }
        }));
        AttributeProperty.replaceMap.put("miapi:generic.reach", ForgeMod.BLOCK_REACH);
        AttributeProperty.replaceMap.put("miapi:generic.attack_range", ForgeMod.ENTITY_REACH);
        AttributeProperty.replaceMap.put("forge:block_reach", ForgeMod.BLOCK_REACH);
        AttributeProperty.replaceMap.put("forge:entity_reach", ForgeMod.ENTITY_REACH);
        AttributeProperty.replaceMap.put("reach-entity-attributes:reach", ForgeMod.BLOCK_REACH);
        AttributeProperty.replaceMap.put("reach-entity-attributes:attack_range", ForgeMod.ENTITY_REACH);
        AttributeProperty.replaceMap.put("miapi:generic.swim_speed", () -> SWIM_SPEED);

    }

    public static void setupAttributes() {
        AttributeRegistry.REACH = ForgeMod.BLOCK_REACH.get();
        AttributeRegistry.ATTACK_RANGE = ForgeMod.ENTITY_REACH.get();
        AttributeRegistry.SWIM_SPEED = ForgeMod.SWIM_SPEED.get();
        AttributeProperty.replaceMap.put("miapi:generic.reach", ForgeMod.BLOCK_REACH);
        AttributeProperty.replaceMap.put("miapi:generic.attack_range", ForgeMod.ENTITY_REACH);
        AttributeProperty.replaceMap.put("forge:block_reach", ForgeMod.BLOCK_REACH);
        AttributeProperty.replaceMap.put("forge:entity_reach", ForgeMod.ENTITY_REACH);
        AttributeProperty.replaceMap.put("reach-entity-attributes:reach", ForgeMod.BLOCK_REACH);
        AttributeProperty.replaceMap.put("reach-entity-attributes:attack_range", ForgeMod.ENTITY_REACH);
        AttributeProperty.replaceMap.put("miapi:generic.swim_speed", () -> SWIM_SPEED);
    }

    public static class ServerEvents {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void adjustAttributes(ItemAttributeModifierEvent event) {
            if (event.getItemStack().getItem() instanceof ModularItem) {
                //AttributeProperty.equipmentSlotMultimapMap(event.getItemStack())
                //        .get(event.getSlotType()).forEach((event::addModifier));
            }
        }

        @SubscribeEvent
        public static void enqueueIMC(InterModEnqueueEvent event) {
            if (Platform.isModLoaded("treechop")) {
                InterModComms.sendTo("treechop", "getTreeChopAPI", () -> (Consumer<Object>) response -> {
                    TreechopUtil.setTreechopApi(response);
                });
            }
        }
    }

    public static class ClientEvents {
        @SubscribeEvent
        public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            //dont ask me, but this fixes registration for client
            setupAttributes();
        }
        @SubscribeEvent
        public void registerBindings(RegisterKeyMappingsEvent event) {
            MiapiClient.KEY_BINDINGS.addCallback(event::register);
        }
    }
}