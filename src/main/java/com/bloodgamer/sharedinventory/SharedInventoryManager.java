package com.bloodgamer.sharedinventory;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public final class SharedInventoryManager {
	private static final String STATE_KEY = SharedInventoryMod.MOD_ID;
	private static SharedInventoryManager instance;

	private final MinecraftServer server;
	private final SharedInventoryState state;
	private boolean syncPending;

	private SharedInventoryManager(MinecraftServer server) {
		this.server = server;
		this.state = SharedInventoryState.get(server);
	}

	public static SharedInventoryManager get(MinecraftServer server) {
		if (instance == null || instance.server != server) {
			instance = new SharedInventoryManager(server);
		}

		return instance;
	}

	public void bindPlayer(ServerPlayerEntity player) {
		PlayerInventoryAccess inventory = (PlayerInventoryAccess) player.getInventory();
		inventory.sharedinventory$bind(state.main, state.armor, state.offHand);
		player.playerScreenHandler.sendContentUpdates();
		if (player.currentScreenHandler != player.playerScreenHandler) {
			player.currentScreenHandler.sendContentUpdates();
		}
		syncEquipment(player);
	}

	public void markDirtyAndScheduleSync() {
		state.markDirty();
		syncPending = true;
	}

	public void flushPendingSync() {
		if (!syncPending) {
			return;
		}

		syncPending = false;
		syncAllPlayers();
	}

	public void syncAllPlayers() {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.playerScreenHandler.sendContentUpdates();
			if (player.currentScreenHandler != player.playerScreenHandler) {
				player.currentScreenHandler.sendContentUpdates();
			}
			syncEquipment(player);
		}
	}

	public static void onInventoryMutated(ServerPlayerEntity player) {
		if (player.getServer() == null) {
			return;
		}

		SharedInventoryManager.get(player.getServer()).markDirtyAndScheduleSync();
	}

	private void syncEquipment(ServerPlayerEntity target) {
		EntityEquipmentUpdateS2CPacket packet = new EntityEquipmentUpdateS2CPacket(
			target.getId(),
			List.of(
				Pair.of(EquipmentSlot.MAINHAND, target.getEquippedStack(EquipmentSlot.MAINHAND).copy()),
				Pair.of(EquipmentSlot.OFFHAND, target.getEquippedStack(EquipmentSlot.OFFHAND).copy()),
				Pair.of(EquipmentSlot.HEAD, target.getEquippedStack(EquipmentSlot.HEAD).copy()),
				Pair.of(EquipmentSlot.CHEST, target.getEquippedStack(EquipmentSlot.CHEST).copy()),
				Pair.of(EquipmentSlot.LEGS, target.getEquippedStack(EquipmentSlot.LEGS).copy()),
				Pair.of(EquipmentSlot.FEET, target.getEquippedStack(EquipmentSlot.FEET).copy())
			)
		);

		for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
			viewer.networkHandler.sendPacket(packet);
		}
	}

	private static final class SharedInventoryState extends PersistentState {
		private static final int MAIN_SIZE = 36;
		private static final int ARMOR_SIZE = 4;
		private static final int OFFHAND_SIZE = 1;
		private static final Type<SharedInventoryState> TYPE = new Type<>(
			SharedInventoryState::new,
			SharedInventoryState::fromNbt,
			null
		);

		private final DefaultedList<ItemStack> main;
		private final DefaultedList<ItemStack> armor;
		private final DefaultedList<ItemStack> offHand;

		private SharedInventoryState() {
			this(DefaultedList.ofSize(MAIN_SIZE, ItemStack.EMPTY), DefaultedList.ofSize(ARMOR_SIZE, ItemStack.EMPTY), DefaultedList.ofSize(OFFHAND_SIZE, ItemStack.EMPTY));
		}

		private SharedInventoryState(DefaultedList<ItemStack> main, DefaultedList<ItemStack> armor, DefaultedList<ItemStack> offHand) {
			this.main = main;
			this.armor = armor;
			this.offHand = offHand;
		}

		public static SharedInventoryState get(MinecraftServer server) {
			PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
			return stateManager.getOrCreate(TYPE, STATE_KEY);
		}

		private static SharedInventoryState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
			DefaultedList<ItemStack> main = DefaultedList.ofSize(MAIN_SIZE, ItemStack.EMPTY);
			DefaultedList<ItemStack> armor = DefaultedList.ofSize(ARMOR_SIZE, ItemStack.EMPTY);
			DefaultedList<ItemStack> offHand = DefaultedList.ofSize(OFFHAND_SIZE, ItemStack.EMPTY);

			readList(nbt.getList("main", 10), main, registries);
			readList(nbt.getList("armor", 10), armor, registries);
			readList(nbt.getList("offhand", 10), offHand, registries);
			return new SharedInventoryState(main, armor, offHand);
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
			nbt.put("main", writeList(main, registries));
			nbt.put("armor", writeList(armor, registries));
			nbt.put("offhand", writeList(offHand, registries));
			return nbt;
		}

		private static NbtList writeList(DefaultedList<ItemStack> items, RegistryWrapper.WrapperLookup registries) {
			NbtList list = new NbtList();
			for (ItemStack stack : items) {
				list.add(stack.encodeAllowEmpty(registries));
			}
			return list;
		}

		private static void readList(NbtList source, DefaultedList<ItemStack> target, RegistryWrapper.WrapperLookup registries) {
			for (int i = 0; i < target.size(); i++) {
				if (i < source.size()) {
					target.set(i, ItemStack.fromNbtOrEmpty(registries, source.getCompound(i)));
				} else {
					target.set(i, ItemStack.EMPTY);
				}
			}
		}
	}
}
