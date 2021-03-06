package extracells.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import extracells.crafting.CraftingPattern;
import extracells.crafting.CraftingPattern2;

public class ItemInternalCraftingPattern extends Item implements
		ICraftingPatternItem {

	@Override
	public ICraftingPatternDetails getPatternForItem(ItemStack is, World w) {
		if (is == null || w == null)
			return null;
		System.out.println(is);
		switch (is.getItemDamage()) {
		case 0:
			if (is.hasTagCompound() && is.getTagCompound().hasKey("item")) {
				ItemStack s = ItemStack.loadItemStackFromNBT(is
						.getTagCompound().getCompoundTag("item"));
				if (s != null && s.getItem() instanceof ICraftingPatternItem)
					return new CraftingPattern(
							((ICraftingPatternItem) s.getItem())
									.getPatternForItem(s, w));
			}
			return null;
		case 1:
			if (is.hasTagCompound() && is.getTagCompound().hasKey("item")) {
				ItemStack s = ItemStack.loadItemStackFromNBT(is
						.getTagCompound().getCompoundTag("item"));
				if (s != null && s.getItem() instanceof ICraftingPatternItem)
					return new CraftingPattern2(
							((ICraftingPatternItem) s.getItem())
									.getPatternForItem(s, w));
			}
		default:
			return null;
		}
	}

}
