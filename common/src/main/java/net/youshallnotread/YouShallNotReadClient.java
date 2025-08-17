package net.youshallnotread;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.youshallnotread.outline.Outline;
import net.youshallnotread.outline.Outliner;
import org.joml.Vector3f;

public class YouShallNotReadClient {

    public static void init() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(localPlayer -> Outliner.removeAllOutlines());

        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, interactionHand, entityHitResult) -> {
            if (!level.isClientSide()) return EventResult.pass();

            AABB bb = entity.getBoundingBox();
            Vector3f inflation = new Vector3f((float) bb.getXsize(), (float) bb.getYsize(), (float) bb.getZsize())
                    .mul(0.55f)
                    .mul(0.5f);

            Outline outline = new Outline.Builder()
                    .key("test entity")
                    .boundsEntity(() -> entity)
                    .inflation(() -> inflation)
                    .greedy(() -> true)
                    .onChanged((oldOutline, newOutline) -> YouShallNotRead.LOGGER.info("Changed outline"))
                    .onRemoved(outline1 -> YouShallNotRead.LOGGER.info("Removed outline"))
                    .onSuspended(outline1 -> YouShallNotRead.LOGGER.info("Suspended outline"))
                    .onUnsuspended(outline1 -> YouShallNotRead.LOGGER.info("Unsuspended outline"))
                    .removeIf((outline1) -> outline1.entity().position().y < 0)
                    .regenerateIf((outline1) -> true)
                    .colour(() -> {
                        if (entity.isOnFire()) {
                            return Utils.RBGFromInt(0xDB4031);
                        }
                        return Utils.RBGFromInt(0x00FF00);
                    })
                    .build();

            if (!Outliner.outlineExists(outline)) {
                Outliner.addOutline(outline);
            }

            return EventResult.pass();
        });

        InteractionEvent.RIGHT_CLICK_BLOCK.register(((player, hand, blockPos, face) -> {
            Level level = player.level();
            if (!level.isClientSide()) return EventResult.pass();
            if (hand == InteractionHand.OFF_HAND) return EventResult.pass();
            if (!player.isShiftKeyDown()) return EventResult.pass();

            if (player.getItemInHand(hand).getItem() == Items.WOODEN_SWORD) {
                Outliner.addOutline(new Outline.Builder()
                        .key("red cube")
                        .boundsBlockGroup(
                                () -> {
                                    Set<BlockPos> blockPositions = new HashSet<>();
                                    for (int x = 0; x < 8; x++) {
                                        for (int y = 0; y < 8; y++) {
                                            for (int z = 0; z < 7; z++) {
                                                blockPositions.add(blockPos.offset(x, y, z));
                                            }
                                        }
                                    }
                                    return blockPositions;
                                },
                                level::dimension)
                        .greedy(() -> true)
                        .merge(() -> false, "test key")
                        .colour(() -> Utils.RBGFromInt(0xFF0000))
                        .regenerateIf((outline) -> false)
                        .thickness(() -> 0.1f)
                        .collisionThickness(() -> 0.15f)
                        .build());
            }

            if (player.getItemInHand(hand).getItem() == Items.WOODEN_HOE) {
                Outliner.addOutline(new Outline.Builder()
                        .key("green cube")
                        .boundsBlockGroup(
                                () -> {
                                    Set<BlockPos> blockPositions = new HashSet<>();
                                    for (int x = 0; x < 4; x++) {
                                        for (int y = 0; y < 4; y++) {
                                            for (int z = 0; z < 3; z++) {
                                                blockPositions.add(blockPos.offset(x, y, z));
                                            }
                                        }
                                    }
                                    return blockPositions;
                                },
                                level::dimension)
                        .greedy(() -> true)
                        .merge(() -> false, "test key")
                        .colour(() -> Utils.RBGFromInt(0x00FF00))
                        .regenerateIf((outline) -> false)
                        .thickness(() -> 0.15f)
                        .collisionThickness(() -> 0.05f)
                        .build());
            }

            if (player.getItemInHand(hand).getItem() == Items.WOODEN_PICKAXE) {
                Outliner.addOutline(new Outline.Builder()
                        .key("blue cube")
                        .boundsBlockGroup(
                                () -> {
                                    Set<BlockPos> blockPositions = new HashSet<>();
                                    for (int x = 0; x < 2; x++) {
                                        for (int y = 0; y < 3; y++) {
                                            for (int z = 0; z < 2; z++) {
                                                blockPositions.add(blockPos.offset(x, y, z));
                                            }
                                        }
                                    }
                                    return blockPositions;
                                },
                                level::dimension)
                        .greedy(() -> true)
                        .merge(() -> false, "test key")
                        .colour(() -> Utils.RBGFromInt(0x0000FF))
                        .regenerateIf((outline) -> false)
                        .thickness(() -> 0.2f)
                        .collisionThickness(() -> 0.05f)
                        .build());
            }
            return EventResult.interruptFalse();
        }));

        InteractionEvent.LEFT_CLICK_BLOCK.register(((player, hand, blockPos, face) -> {
            Level level = player.level();
            if (!level.isClientSide()) return EventResult.pass();
            if (hand == InteractionHand.OFF_HAND) return EventResult.pass();
            if (!player.isShiftKeyDown()) return EventResult.pass();

            Outliner.removeAllOutlines();
            return EventResult.interruptFalse();
        }));

        EntityEvent.ADD.register(((entity, level) -> {
            if (!level.isClientSide()) return EventResult.pass();

            Set<Outline> outlines = Outliner.getSuspendedOutlines(entity);
            if (outlines == null) return EventResult.pass();
            for (Outline outline : outlines) {
                outline.setEntity(() -> entity);
                Outliner.addOutline(outline);
            }
            return EventResult.pass();
        }));
    }
}
