package com.bloodgamer.sharedinventory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class SharedInventoryMod implements ModInitializer {
	public static final String MOD_ID = "sharedinventory";

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> SharedInventoryManager.get(server));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SharedInventoryManager manager = SharedInventoryManager.get(server);
			manager.bindPlayer(handler.player);
			manager.syncAllPlayers();
		});
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (newPlayer.getServer() == null) {
				return;
			}

			SharedInventoryManager manager = SharedInventoryManager.get(newPlayer.getServer());
			manager.bindPlayer(newPlayer);
			manager.markDirtyAndScheduleSync();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> SharedInventoryManager.get(server).flushPendingSync());
	}
}
