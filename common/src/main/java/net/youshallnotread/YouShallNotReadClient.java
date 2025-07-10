package net.youshallnotread;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.youshallnotread.outline.Outline;
import net.youshallnotread.outline.Outliner;

public class YouShallNotReadClient {

    public static void init() {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(localPlayer -> Outliner.removeAllOutlines());

        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, interactionHand, entityHitResult) -> {
            if (!level.isClientSide()) return EventResult.pass();
            Outliner.addOutline(new Outline.Builder()
                    .key("test entity")
                    .duration(3)
                    .bounds(entity)
                    .build());
            return EventResult.pass();
        });

        BlockEvent.BREAK.register(((level, blockPos, blockState, serverPlayer, intValue) -> {
            if (!level.isClientSide()) return EventResult.pass();
            Outline outline = new Outline.Builder()
                    .key("test block")
                    .duration(-1)
                    .bounds(blockPos, level.dimension())
                    .merge(true, "test key")
                    .build();

            Outliner.addOutline(outline);
            return EventResult.pass();
        }));
    }
}
