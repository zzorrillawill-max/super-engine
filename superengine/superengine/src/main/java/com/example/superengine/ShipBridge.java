package com.example.superengine;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Talks to Eureka and Valkyrien Skies through reflection, so this mod
 * does not need either of them to compile.
 */
final class ShipBridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean ready = false;
    private static boolean failed = false;

    private static Method getShip;
    private static Method getAttachment;
    private static Class<?> controlClass;
    private static Method getPowerLinear;
    private static Method setPowerLinear;
    private static Method getPowerAngular;
    private static Method setPowerAngular;
    private static Method getConsumed;

    private ShipBridge() {}

    private static boolean init() {
        if (ready) return true;
        if (failed) return false;
        try {
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
            getConsumed = controlClass.getMethod("getConsumed");

            ready = true;
            return true;
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("Super Engine could not hook into Eureka. The engine will not push ships.", t);
            return false;
        }
    }

    /**
     * Adds engine power to the ship that contains pos.
     *
     * @return how much power the ship used last tick (the engine burns heat with it),
     *         or 0 if the block is not on a Eureka ship.
     */
    static float addPower(ServerLevel level, BlockPos pos, double linear, double angular) {
        if (!init()) return 0f;
        try {
            Object ship = getShip.invoke(null, level, pos);
            if (ship == null) return 0f;
            Object control = getAttachment.invoke(ship, controlClass);
            if (control == null) return 0f;

            setPowerLinear.invoke(control, ((Double) getPowerLinear.invoke(control)) + linear);
            setPowerAngular.invoke(control, ((Double) getPowerAngular.invoke(control)) + angular);
            return (Float) getConsumed.invoke(control);
        } catch (Throwable t) {
            if (!failed) {
                failed = true;
                LOGGER.error("Super Engine failed while pushing a ship.", t);
            }
            return 0f;
        }
    }
}
