package extracells.definitions;

import appeng.api.util.AEItemDefinition;
import extracells.api.definitions.IBlockDefinition;
import extracells.registries.BlockEnum;
import extracells.tileentity.TileEntityCertusTank;
import extracells.tileentity.TileEntityFluidCrafter;
import extracells.tileentity.TileEntityWalrus;

public class BlockDefinition implements IBlockDefinition {
	
	public static final BlockDefinition instance = new BlockDefinition();

	@Override
	public AEItemDefinition certusTank() {
		return new BlockItemDefinitions(BlockEnum.CERTUSTANK.getBlock(), TileEntityCertusTank.class);
	}

	@Override
	public AEItemDefinition walrus() {
		return new BlockItemDefinitions(BlockEnum.WALRUS.getBlock(), TileEntityWalrus.class);
	}

	@Override
	public AEItemDefinition fluidCrafter() {
		return new BlockItemDefinitions(BlockEnum.FLUIDCRAFTER.getBlock(), TileEntityFluidCrafter.class);
	}

}
