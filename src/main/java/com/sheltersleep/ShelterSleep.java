package com.sheltersleep;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ShelterSleep implements ModInitializer {
    public static final String MOD_ID = "shelter_sleep";

    @Override
    public void onInitialize() {
        MidnightConfig.init(MOD_ID, ShelterConfig.class);
        // Scatta DOPO i controlli di Minecraft (ora del giorno, mostri vicini): qui aggiungiamo solo il nostro.
        EntitySleepEvents.ALLOW_SLEEPING.register(ShelterSleep::onAllowSleeping);
    }

    private static PlayerEntity.SleepFailureReason onAllowSleeping(PlayerEntity player, BlockPos bedPos) {
        World world = player.getEntityWorld();
        if (world.isClient()) {
            return null;
        }
        if (!(world.getBlockState(bedPos).getBlock() instanceof BedBlock)) {
            return null;
        }
        if (ShelterConfig.bypassInCreative && player.isCreative()) {
            return null;
        }

        ShelterCheck.Result result = ShelterCheck.check(
                new McBlocks(world), bedPos.getX(), bedPos.getY(), bedPos.getZ(), ShelterConfig.settings());

        String message = switch (result) {
            case OK -> null;
            case NO_ROOF -> ShelterConfig.msgNoRoof;
            case NO_SPACE -> ShelterConfig.msgNoSpace;
            case NO_WALLS -> ShelterConfig.msgNoWalls;
            case NO_DOOR -> ShelterConfig.msgNoDoor;
        };
        // Il messaggio viene mostrato sopra la barra degli oggetti, come quelli di Minecraft.
        return message == null ? null : new PlayerEntity.SleepFailureReason(Text.literal(message));
    }
}
