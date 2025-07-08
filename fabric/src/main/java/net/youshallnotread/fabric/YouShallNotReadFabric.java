package net.youshallnotread.fabric;

import net.youshallnotread.YouShallNotRead;
import net.fabricmc.api.ModInitializer;

public class YouShallNotReadFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YouShallNotRead.init();
    }
}
