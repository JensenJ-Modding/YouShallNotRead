package net.youshallnotread.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.youshallnotread.YouShallNotReadClient;
import net.youshallnotread.outline.Outliner;

public class YouShallNotReadFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        YouShallNotReadClient.init();
        WorldRenderEvents.AFTER_ENTITIES.register(
                (context) -> Outliner.processOutlines(context.world(), context.matrixStack()));
    }
}
