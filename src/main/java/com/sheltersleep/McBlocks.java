package com.sheltersleep;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Collega la logica pura al mondo di Minecraft. */
final class McBlocks implements ShelterCheck.Blocks {
    private final World world;
    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    McBlocks(World world) {
        this.world = world;
    }

    @Override
    public ShelterCheck.Kind kindAt(int x, int y, int z) {
        this.pos.set(x, y, z);
        BlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof DoorBlock) {
            return ShelterCheck.Kind.DOOR;
        }
        if (state.getBlock() instanceof BedBlock) {
            return ShelterCheck.Kind.BED;
        }
        // Tutto cio' che ha una collisione (muri, vetri, bauli, scale...) e' "solido"; aria, torce, acqua no.
        return state.getCollisionShape(this.world, this.pos).isEmpty()
                ? ShelterCheck.Kind.FREE
                : ShelterCheck.Kind.SOLID;
    }

    @Override
    public boolean outOfHeight(int y) {
        return this.world.isOutOfHeightLimit(y);
    }
}
