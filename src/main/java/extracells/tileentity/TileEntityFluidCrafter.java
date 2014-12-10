package extracells.tileentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableSet;

import cpw.mods.fml.common.FMLCommonHandler;
import extracells.api.IECTileEntity;
import extracells.crafting.CraftingPatter;
import extracells.gridblock.ECBaseGridBlock;
import extracells.gridblock.ECFluidGridBlock;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingWatcher;
import appeng.api.networking.crafting.ICraftingWatcherHost;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class TileEntityFluidCrafter extends TileEntity implements IActionHost, ICraftingProvider, ICraftingWatcherHost, IECTileEntity {

	private ECFluidGridBlock gridBlock;
	private IGridNode node = null;
	private List<ICraftingPatternDetails> patternHandlers = new ArrayList<ICraftingPatternDetails>();
	private List<IAEItemStack> requestetItems = new ArrayList<IAEItemStack>();
	private boolean isBusy = false;
	private ICraftingWatcher watcher = null;
	
	private boolean isFirstGetGridNode = true;
	
	public final FluidCrafterInventory inventory;
	
	private Long finishCraftingTime = 0L;
	private ItemStack returnStack = null;
	
	private boolean update = false;
	
	private final TileEntityFluidCrafter instance;
	
	public TileEntityFluidCrafter(){
		super();
		gridBlock = new ECFluidGridBlock(this);
		inventory = new FluidCrafterInventory();
		instance = this;
	}
	
	@Override
	public IGridNode getActionableNode() {
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
			return null;
		if(node == null){
			node = AEApi.instance().createGridNode(gridBlock);
		}
		return node;
	}

	@Override
	public IGridNode getGridNode(ForgeDirection dir) {
		if(FMLCommonHandler.instance().getSide().isClient() && (getWorldObj() == null || getWorldObj().isRemote))
			return null;
		if(isFirstGetGridNode){
			isFirstGetGridNode = false;
			getActionableNode().updateState();
		}
		return node;
	}

	@Override
	public AECableType getCableConnectionType(ForgeDirection dir) {
		return AECableType.SMART;
	}

	@Override
	public void securityBreak() {
		
	}

	public double getPowerUsage() {
		return 0;
	}

	public DimensionalCoord getLocation() {
		return new DimensionalCoord(this);
	}

	public IGridNode getGridNode() {
		return getGridNode(ForgeDirection.UNKNOWN);
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails patternDetails,
			InventoryCrafting table) {
		if(isBusy)
			return false;
		if(patternDetails instanceof CraftingPatter){
			CraftingPatter patter = (CraftingPatter) patternDetails;
			HashMap<Fluid, Long> fluids = new HashMap<Fluid, Long>();
			for(IAEFluidStack stack : patter.getCondencedFluidInputs()){
				if(fluids.containsKey(stack.getFluid())){
					Long amount = fluids.get(stack.getFluid()) + stack.getStackSize();
					fluids.remove(stack.getFluid());
					fluids.put(stack.getFluid(), amount);
				}else{
					fluids.put(stack.getFluid(), stack.getStackSize());
				}
			}
			IGrid grid = node.getGrid();
			if(grid == null)
				return false;
			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if(storage == null)
				return false;
			for(Fluid fluid : fluids.keySet()){
				Long amount = fluids.get(fluid);
				IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid,  (int) (amount+0))), Actionable.SIMULATE, new MachineSource(this));
				if(extractFluid == null || extractFluid.getStackSize() != amount){
					return false;
				}
			}
			for(Fluid fluid : fluids.keySet()){
				Long amount = fluids.get(fluid);
				IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid,  (int) (amount+0))), Actionable.MODULATE, new MachineSource(this));
			}
			finishCraftingTime = System.currentTimeMillis() + 1000;
			
			returnStack = patter.getCondencedOutputs()[0].getItemStack();
			isBusy = true;
		}
		return true;
	}

	@Override
	public boolean isBusy() {
		return isBusy;
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper craftingTracker) {
		patternHandlers = new ArrayList<ICraftingPatternDetails>();
		
		for (ItemStack currentPatternStack : inventory.inv)
		{
			if (currentPatternStack != null && currentPatternStack.getItem() != null && currentPatternStack.getItem() instanceof ICraftingPatternItem)
			{
				ICraftingPatternItem currentPattern = (ICraftingPatternItem) currentPatternStack.getItem();

				if (currentPattern != null && currentPattern.getPatternForItem(currentPatternStack, getWorldObj()) != null && currentPattern.getPatternForItem(currentPatternStack, getWorldObj()).isCraftable())
				{
					ICraftingPatternDetails pattern = new CraftingPatter(currentPattern.getPatternForItem(currentPatternStack, getWorldObj()));
					patternHandlers.add(pattern);
					if(pattern.getCondencedInputs().length == 0){
						craftingTracker.setEmitable(pattern.getCondencedOutputs()[0]);
					}else{
						craftingTracker.addCraftingOption(this, pattern);
					}
				}
			}
		}
		updateWatcher();
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		inventory.writeToNBT(tagCompound);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		inventory.readFromNBT(tagCompound);
	}
	
	@Override
	public void updateEntity(){
		if(update){
			update = false;
			if(getGridNode() != null && getGridNode().getGrid() !=  null){
            	getGridNode().getGrid().postEvent(new MENetworkCraftingPatternChange(instance, getGridNode()));
            }
		}
		if(isBusy && finishCraftingTime <= System.currentTimeMillis() && getWorldObj() != null && !getWorldObj().isRemote){
			if(node == null || returnStack == null)
				return;
			IGrid grid = node.getGrid();
			if(grid == null)
				return;
			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if(storage == null)
				return;
			storage.getItemInventory().injectItems(AEApi.instance().storage().createItemStack(returnStack), Actionable.MODULATE, new MachineSource(this));
			isBusy = false;
			returnStack = null;
		}
		if(!isBusy && getWorldObj() != null && !getWorldObj().isRemote){
			if(!requestetItems.isEmpty()){
				for(IAEItemStack s : requestetItems){
					for(ICraftingPatternDetails details : patternHandlers){
						if(details.getCondencedOutputs()[0].equals(s)){
							CraftingPatter patter = (CraftingPatter) details;
							HashMap<Fluid, Long> fluids = new HashMap<Fluid, Long>();
							for(IAEFluidStack stack : patter.getCondencedFluidInputs()){
								if(fluids.containsKey(stack.getFluid())){
									Long amount = fluids.get(stack.getFluid()) + stack.getStackSize();
									fluids.remove(stack.getFluid());
									fluids.put(stack.getFluid(), amount);
								}else{
									fluids.put(stack.getFluid(), stack.getStackSize());
								}
							}
							IGrid grid = node.getGrid();
							if(grid == null)
								break;
							IStorageGrid storage = grid.getCache(IStorageGrid.class);
							if(storage == null)
								break;
							boolean doBreak = false;
							for(Fluid fluid : fluids.keySet()){
								Long amount = fluids.get(fluid);
								IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid,  (int) (amount+0))), Actionable.SIMULATE, new MachineSource(this));
								if(extractFluid == null || extractFluid.getStackSize() != amount){
									doBreak = true;
									break;
								}
							}
							if(doBreak)
								break;
							for(Fluid fluid : fluids.keySet()){
								Long amount = fluids.get(fluid);
								IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid,  (int) (amount+0))), Actionable.MODULATE, new MachineSource(this));
							}
							finishCraftingTime = System.currentTimeMillis() + 1000;
							
							returnStack = patter.getCondencedOutputs()[0].getItemStack();
							isBusy = true;
							return;
						}
					}
				}
			}
		}
	}

	
	public IInventory getInventory(){
		return inventory;
	}
	
	private class FluidCrafterInventory implements IInventory{

		private ItemStack[] inv = new ItemStack[9];

        @Override
        public int getSizeInventory() {
                return inv.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
                return inv[slot];
        }
        
        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
                inv[slot] = stack;
                if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                        stack.stackSize = getInventoryStackLimit();
                }
                update = true;
        }

        @Override
        public ItemStack decrStackSize(int slot, int amt) {
                ItemStack stack = getStackInSlot(slot);
                if (stack != null) {
                        if (stack.stackSize <= amt) {
                                setInventorySlotContents(slot, null);
                        } else {
                                stack = stack.splitStack(amt);
                                if (stack.stackSize == 0) {
                                        setInventorySlotContents(slot, null);
                                }
                        }
                }
                update = true;
                return stack;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
                return null;
        }
        
        @Override
        public int getInventoryStackLimit() {
                return 1;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
                return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}
        
        public void readFromNBT(NBTTagCompound tagCompound) {
                
                NBTTagList tagList = tagCompound.getTagList("Inventory", 10);
                for (int i = 0; i < tagList.tagCount(); i++) {
                        NBTTagCompound tag = (NBTTagCompound) tagList.getCompoundTagAt(i);
                        byte slot = tag.getByte("Slot");
                        if (slot >= 0 && slot < inv.length) {
                                inv[slot] = ItemStack.loadItemStackFromNBT(tag);
                        }
                }
        }

        public void writeToNBT(NBTTagCompound tagCompound) {
                                
                NBTTagList itemList = new NBTTagList();
                for (int i = 0; i < inv.length; i++) {
                        ItemStack stack = inv[i];
                        if (stack != null) {
                                NBTTagCompound tag = new NBTTagCompound();
                                tag.setByte("Slot", (byte) i);
                                stack.writeToNBT(tag);
                                itemList.appendTag(tag);
                        }
                }
                tagCompound.setTag("Inventory", itemList);
        }

		@Override
		public String getInventoryName() {
			return "inventory.fluidCrafter";
		}

		@Override
		public boolean hasCustomInventoryName() {
			return false;
		}

		@Override
		public void markDirty() {}

		@Override
		public boolean isItemValidForSlot(int slot, ItemStack stack) {
			if(stack.getItem() instanceof ICraftingPatternItem){
				ICraftingPatternDetails details =  ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, getWorldObj());
				return (details != null && details.isCraftable());
			}
			return false;
		}
		
	}

	@Override
	public void updateWatcher(ICraftingWatcher newWatcher) {
		watcher = newWatcher;
		updateWatcher();
	}

	@Override
	public void onRequestChange(ICraftingGrid craftingGrid, IAEItemStack what) {
		if(craftingGrid.isRequesting(what)){
			if(!requestetItems.contains(what)){
				requestetItems.add(what);
			}
		}else if(requestetItems.contains(what)){
			requestetItems.remove(what);
		}
		
	}
	
	private void updateWatcher(){
		requestetItems = new ArrayList<IAEItemStack>();
		IGrid grid = null;
		IGridNode node = getGridNode();
		ICraftingGrid crafting = null;
		if(node != null){
			grid = node.getGrid();
			if(grid != null){
				crafting = grid.getCache(ICraftingGrid.class);
			}
		}
		for(ICraftingPatternDetails patter : patternHandlers){
			watcher.clear();
			if(patter.getCondencedInputs().length == 0){
				watcher.add(patter.getCondencedOutputs()[0]);
				
				if(crafting != null){
					if(crafting.isRequesting(patter.getCondencedOutputs()[0])){
						requestetItems.add(patter.getCondencedOutputs()[0]);
					}
				}
			}
		}
	}
	
}
