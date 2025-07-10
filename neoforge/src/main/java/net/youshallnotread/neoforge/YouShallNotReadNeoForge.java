package net.youshallnotread.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.youshallnotread.YouShallNotRead;

@Mod(YouShallNotRead.MOD_ID)
public class YouShallNotReadNeoForge {
    public YouShallNotReadNeoForge(ModContainer container, IEventBus bus) {
        YouShallNotRead.init();
    }
}
