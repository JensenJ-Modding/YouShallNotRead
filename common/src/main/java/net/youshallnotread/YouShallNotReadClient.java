package net.youshallnotread;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.youshallnotread.outline.Outline;
import net.youshallnotread.outline.Outliner;
import org.joml.Vector4f;

public class YouShallNotReadClient {

    public static void init() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(localPlayer -> Outliner.removeAllOutlines());

        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, interactionHand, entityHitResult) -> {
            if (!level.isClientSide()) return EventResult.pass();

            for (int x = 0; x < 1; x++) {
                for (int y = 0; y < 1; y++) {
                    for (int z = 0; z < 1; z++) {
                        Outline outline = new Outline.Builder()
                                .key("test entity:" + x + "," + y + "," + z)
                                .duration(-1)
                                .bounds(entity)
                                .colour(new Vector4f(0.5f, 0.7f, 0.7f, 0.5f))
                                .build();
                        if (!Outliner.outlineExists(outline)) {
                            Outliner.addOutline(outline);
                        }
                    }
                }
            }

            return EventResult.pass();
        });

        InteractionEvent.RIGHT_CLICK_BLOCK.register(((player, hand, blockPos, face) -> {
            Level level = player.level();
            if (!level.isClientSide()) return EventResult.pass();
            if (hand == InteractionHand.OFF_HAND) return EventResult.pass();
            if (!player.isShiftKeyDown()) return EventResult.pass();

            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 100; y++) {
                    for (int z = 0; z < 10; z++) {

                        BlockPos pos = blockPos.east(x * 2).above(y * 2).south(z * 2);
                        Outline outline = new Outline.Builder()
                                .key("test block" + pos)
                                .duration(-1)
                                .bounds(pos, level.dimension())
                                .merge(true, "test key")
                                .build();
                        Outliner.addOutline(outline);
                    }
                }
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

            Set<Outline> outlines = Outliner.getDiscardedOutlines(entity);
            if (outlines == null) return EventResult.pass();
            for (Outline outline : outlines) {
                outline.setEntity(entity);
                Outliner.addOutline(outline);
            }
            return EventResult.pass();
        }));
    }
}
