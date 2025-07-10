package net.youshallnotread.fabric;

import net.fabricmc.api.ModInitializer;
import net.youshallnotread.YouShallNotRead;

public class YouShallNotReadFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YouShallNotRead.init();
    }
}
