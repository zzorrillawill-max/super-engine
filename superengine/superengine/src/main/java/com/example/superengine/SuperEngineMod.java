package com.example.superengine;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SuperEngineMod.MODID)
public class SuperEngineMod {
    public static final String MODID = "superengine";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> SUPER_ENGINE =
            BLOCKS.register("super_engine", () -> new SuperEngineBlock(() -> Config.SUPER_MULTIPLIER.get()));
    public static final RegistryObject<Block> MEGA_ENGINE =
            BLOCKS.register("mega_engine", () -> new SuperEngineBlock(() -> Config.MEGA_MULTIPLIER.get()));
    public static final RegistryObject<Block> GIGA_ENGINE =
            BLOCKS.register("giga_engine", () -> new SuperEngineBlock(() -> Config.GIGA_MULTIPLIER.get()));

    public static final RegistryObject<Item> SUPER_ENGINE_ITEM =
            ITEMS.register("super_engine", () -> new BlockItem(SUPER_ENGINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MEGA_ENGINE_ITEM =
            ITEMS.register("mega_engine", () -> new BlockItem(MEGA_ENGINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GIGA_ENGINE_ITEM =
            ITEMS.register("giga_engine", () -> new BlockItem(GIGA_ENGINE.get(), new Item.Properties()));

    public SuperEngineMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Let Eureka's engine block entity live inside our blocks too
        event.enqueueWork(() -> ShipBridge.allowBlocks(
                SUPER_ENGINE.get(), MEGA_ENGINE.get(), GIGA_ENGINE.get()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SUPER_ENGINE_ITEM);
            event.accept(MEGA_ENGINE_ITEM);
            event.accept(GIGA_ENGINE_ITEM);
        }
    }
}
