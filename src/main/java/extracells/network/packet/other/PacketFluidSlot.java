package extracells.network.packet.other;

import extracells.network.AbstractPacket;
import extracells.part.PartECBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;

import java.util.ArrayList;
import java.util.List;

public class PacketFluidSlot extends AbstractPacket {

    private int index;
    private Fluid fluid;
    private IFluidSlotPartOrBlock partOrBlock;
    private List<Fluid> filterFluids;

    public PacketFluidSlot() {
    }

    public PacketFluidSlot(IFluidSlotPartOrBlock _partOrBlock, int _index, Fluid _fluid, EntityPlayer _player) {
        super(_player);
        mode = 0;
        partOrBlock = _partOrBlock;
        index = _index;
        fluid = _fluid;
    }

    public PacketFluidSlot(List<Fluid> _filterFluids) {
        mode = 1;
        filterFluids = _filterFluids;
    }

    public void writeData(ByteBuf out) {
        switch (mode) {
            case 0:
            	if(partOrBlock instanceof PartECBase){
            		out.writeBoolean(true);
            		writePart((PartECBase) partOrBlock, out);
            	}else{
            		out.writeBoolean(false);
            		writeTileEntity((TileEntity) partOrBlock, out);
            	}
                out.writeInt(index);
                writeFluid(fluid, out);
                break;
            case 1:
                out.writeInt(filterFluids.size());
                for (int i = 0; i < filterFluids.size(); i++) {
                    writeFluid(filterFluids.get(i), out);
                }
                break;
        }
    }

    public void readData(ByteBuf in) {
        switch (mode) {
            case 0:
            	if(in.readBoolean())
            		partOrBlock = (IFluidSlotPartOrBlock) readPart(in);
            	else
            		partOrBlock = (IFluidSlotPartOrBlock) readTileEntity(in);
                index = in.readInt();
                fluid = readFluid(in);
                break;
            case 1:
                filterFluids = new ArrayList<Fluid>();
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    filterFluids.add(readFluid(in));
                }
                break;
        }
    }

    @Override
    public void execute() {
        switch (mode) {
            case 0:
            	if(partOrBlock != null)
            		partOrBlock.setFluid(index, fluid, player);
                break;
            case 1:
                Gui gui = Minecraft.getMinecraft().currentScreen;
                if (gui instanceof IFluidSlotGui) {
                    IFluidSlotGui partGui = (IFluidSlotGui) gui;
                    partGui.updateFluids(filterFluids);
                }
                break;
        }
    }
}
