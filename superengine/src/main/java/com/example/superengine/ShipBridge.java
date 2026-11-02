package com.example.superengine;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Talks to Eureka and Valkyrien Skies through reflection, so this mod
 * does not need either of them to compile.
 */
final class ShipBridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean ready = false;
    private static boolean failed = false;

    private static Constructor<?> engineCtor;
    private static Method engineTick;
    private static Method getShip;
    private static Method getAttachment;
    private static Class<?> controlClass;
    private static Method getPowerLinear;
    private static Method setPowerLinear;
    private static Method getPowerAngular;
    private static Method setPowerAngular;

    private ShipBridge() {}

    /** Eureka's own heat property. It must be the exact same object, not a copy. */
    static IntegerProperty heatProperty() {
        try {
            Class<?> props = Class.forName("org.valkyrienskies.eureka.EurekaProperties");
            Object instance = props.getField("INSTANCE").get(null);
            return (IntegerProperty) props.getMethod("getHEAT").invoke(instance);
        } catch (Throwable t) {
            LOGGER.error("Super Engine could not find Eureka's heat property.", t);
            return IntegerProperty.create("heat", 0, 4);
        }
    }

    private static boolean init() {
        if (ready) return true;
        if (failed) return false;
        try {
            Class<?> engineClass = Class.forName("org.valkyrienskies.eureka.blockentity.EngineBlockEntity");
            engineCtor = engineClass.getConstructor(BlockPos.class, BlockState.class);
            engineTick = engineClass.getMethod("tick");

            Class<?> util = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            for (Method m : util.getMethods()) {
                if (m.getName().equals("getLoadedShipManagingPos")
                        && m.getParameterCount() == 2
                        && m.getParameterTypes()[0].isAssignableFrom(ServerLevel.class)
                        && m.getParameterTypes()[1].isAssignableFrom(BlockPos.class)) {
                    getShip = m;
                    break;
                }
            }
            if (getShip == null) throw new NoSuchMethodException("getLoadedShipManagingPos");

            Class<?> shipClass = Class.forName("org.valkyrienskies.core.api.ships.LoadedServerShip");
            for (Method m : shipClass.getMethods()) {
                if (m.getName().equals("getAttachment") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == Class.class) {
                    getAttachment = m;
                    break;
                }
            }
            if (getAttachment == null) throw new NoSuchMethodException("getAttachment");

            controlClass = Class.forName("org.valkyrienskies.eureka.ship.EurekaShipControl");
            getPowerLinear = controlClass.getMethod("getPowerLinear");
            setPowerLinear = controlClass.getMethod("setPowerLinear", double.class);
            getPowerAngular = controlClass.getMethod("getPowerAngular");
            setPowerAngular = controlClass.getMethod("setPowerAngular", double.class);

            ready = true;
            return true;
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("Super Engine could not hook into Eureka. The engines will not work.", t);
            return false;
        }
    }

    /** Makes Eureka's engine block entity type accept our blocks. */
    static void allowBlocks(Block... blocks) {
        try {
            BlockEntityType<?> type = ForgeRegistries.BLOCK_ENTITY_TYPES
                    .getValue(new ResourceLocation("vs_eureka", "engine"));
            if (type == null) throw new IllegalStateException("vs_eureka:engine block entity not found");

            Field validBlocks = null;
            for (Field f : BlockEntityType.class.getDeclaredFields()) {
                if (Set.class.isAssignableFrom(f.getType())) {
                    validBlocks = f;
                    break;
                }
            }
            if (validBlocks == null) throw new IllegalStateException("valid blocks field not found");

            validBlocks.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<Block> old = (Set<Block>) validBlocks.get(type);
            Set<Block> merged = new HashSet<>(old);
            merged.addAll(Arrays.asList(blocks));
            validBlocks.set(type, merged);
        } catch (Throwable t) {
            LOGGER.error("Super Engine could not register with Eureka's engine block entity.", t);
        }
    }

    /** Creates Eureka's real engine block entity (same menu, fuel slot and fuel rules). */
    static BlockEntity newEngine(BlockPos pos, BlockState state) {
        if (!init()) return null;
        try {
            return (BlockEntity) engineCtor.newInstance(pos, state);
        } catch (Throwable t) {
            LOGGER.error("Super Engine could not create an engine block entity.", t);
            return null;
        }
    }

    /**
     * Runs Eureka's own engine tick, then adds extra push on top
     * so the engine is {@code multiplier} times as strong as a normal one.
     */
    static void tick(ServerLevel level, BlockPos pos, BlockEntity engine, double multiplier) {
        if (!init()) return;
        try {
            Object control = null;
            Object ship = getShip.invoke(null, level, pos);
            if (ship != null) control = getAttachment.invoke(ship, controlClass);

            double linearBefore = 0;
            double angularBefore = 0;
            if (control != null) {
                linearBefore = (Double) getPowerLinear.invoke(control);
                angularBefore = (Double) getPowerAngular.invoke(control);
            }

            engineTick.invoke(engine);

            if (control != null && multiplier != 1.0) {
                double linearAfter = (Double) getPowerLinear.invoke(control);
                double angularAfter = (Double) getPowerAngular.invoke(control);
                setPowerLinear.invoke(control, linearAfter + (linearAfter - linearBefore) * (multiplier - 1.0));
                setPowerAngular.invoke(control, angularAfter + (angularAfter - angularBefore) * (multiplier - 1.0));
            }
        } catch (Throwable t) {
            if (!failed) {
                failed = true;
                LOGGER.error("Super Engine failed while ticking.", t);
            }
        }
    }
}
