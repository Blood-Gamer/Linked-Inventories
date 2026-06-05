package com.bloodgamer.sharedinventory.mixin;

import com.bloodgamer.sharedinventory.PlayerInventoryAccess;
import com.bloodgamer.sharedinventory.SharedInventoryManager;
import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements PlayerInventoryAccess {
	@Shadow @Final public PlayerEntity player;
	@Shadow @Final @Mutable public DefaultedList<ItemStack> main;
	@Shadow @Final @Mutable public DefaultedList<ItemStack> armor;
	@Shadow @Final @Mutable public DefaultedList<ItemStack> offHand;
	@Shadow @Final @Mutable private List<DefaultedList<ItemStack>> combinedInventory;

	@Unique
	private boolean sharedinventory$bound;

	@Override
	public void sharedinventory$bind(DefaultedList<ItemStack> main, DefaultedList<ItemStack> armor, DefaultedList<ItemStack> offHand) {
		this.main = main;
		this.armor = armor;
		this.offHand = offHand;
		this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
		this.sharedinventory$bound = true;
	}

	@Inject(method = "markDirty", at = @At("TAIL"))
	private void sharedinventory$onMarkDirty(CallbackInfo ci) {
		if (!sharedinventory$bound || player.getWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		SharedInventoryManager.onInventoryMutated(serverPlayer);
	}
}
