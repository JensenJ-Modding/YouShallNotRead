package net.youshallnotread.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.youshallnotread.YouShallNotRead;
import net.youshallnotread.YouShallNotReadClient;
import net.youshallnotread.outline.Outliner;

@Mod(value = YouShallNotRead.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = YouShallNotRead.MOD_ID)
public class YouShallNotReadNeoForgeClient {

    public YouShallNotReadNeoForgeClient(ModContainer container, IEventBus bus) {
        YouShallNotReadClient.init();
    }

    @SubscribeEvent
    public static void handleWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            Outliner.processOutlines(event.getCamera().getEntity().level());
        }
    }
}
