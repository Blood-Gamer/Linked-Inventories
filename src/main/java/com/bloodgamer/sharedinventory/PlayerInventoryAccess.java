package com.bloodgamer.sharedinventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface PlayerInventoryAccess {
	void sharedinventory$bind(DefaultedList<ItemStack> main, DefaultedList<ItemStack> armor, DefaultedList<ItemStack> offHand);
}
