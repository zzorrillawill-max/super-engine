package com.example.superengine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

public class SuperEngineBlockEntity extends BlockEntity {
    // Same feel as Eureka's engine
    private static final int FUEL_MULTIPLIER = 2;
    private static final int CAPACITY = 20000;
    private static final float HEAT_GAIN = 0.03f;
    private static final float HEAT_LOSS = 0.01f;
    private static final float HEAT_EXPONENT = 0.1f;
    private static final float MAX_EFFECTIVE_HEAT = 100f - HEAT_GAIN;

    private int fuelLeft = 0;
    private float heat = 0f;

    public SuperEngineBlockEntity(BlockPos pos, BlockState state) {
        super(SuperEngineMod.SUPER_ENGINE_BE.get(), pos, state);
    }

    public int fuelPercent() {
        return Math.min(100, fuelLeft * 100 / CAPACITY);
    }

    public int heatPercent() {
        return Math.round(heat);
    }

    /** Puts fuel items from the stack into the engine. Returns true if anything was added. */
    public boolean addFuel(Player player, ItemStack stack, boolean fillAsMuchAsPossible) {
        if (stack.isEmpty()) return false;
        int burn = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) * FUEL_MULTIPLIER;
        if (burn <= 0) return false;

        boolean added = false;
        while (!stack.isEmpty() && fuelLeft + burn <= CAPACITY) {
            ItemStack remainder = stack.getCraftingRemainingItem();
            stack.shrink(1);
            fuelLeft += burn;
            added = true;
            if (!remainder.isEmpty()) {
                player.getInventory().placeItemBackInInventory(remainder);
            }
            if (!fillAsMuchAsPossible) break;
        }
        if (added) setChanged();
        return added;
    }

    public void tick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        boolean powered = level.hasNeighborSignal(worldPosition);

        if (!powered && fuelLeft > 0) {
            fuelLeft--;
            if (heat <= MAX_EFFECTIVE_HEAT) {
                heat += (100f * HEAT_EXPONENT - heat * HEAT_EXPONENT + 1f) * HEAT_GAIN;
            }
        }

        BlockState state = getBlockState();
        int heatLevel = Math.min((int) Math.ceil(heat * 4f / 100f), 4);
        if (state.getValue(SuperEngineBlock.HEAT) != heatLevel) {
            level.setBlock(worldPosition, state.setValue(SuperEngineBlock.HEAT, heatLevel), 11);
        }

        if (heat > 0) {
            float effective = heat < MAX_EFFECTIVE_HEAT ? heat / 100f : 1f;
            double linear = lerp(Config.POWER_LINEAR_MIN.get(), Config.POWER_LINEAR.get(), effective);
            double angular = lerp(0.0, Config.POWER_ANGULAR.get(), effective);

            float consumed = ShipBridge.addPower((ServerLevel) level, worldPosition, linear, angular);
            heat -= consumed;
            heat = Math.max(heat - (heat * HEAT_EXPONENT + 1f) * HEAT_LOSS, 0f);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("FuelLeft", fuelLeft);
        tag.putFloat("Heat", heat);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fuelLeft = tag.getInt("FuelLeft");
        heat = tag.getFloat("Heat");
    }
}
