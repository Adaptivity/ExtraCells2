package extracells.network.packet.part;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import extracells.container.ContainerFluidStorage;
import extracells.gui.GuiFluidStorage;
import extracells.network.AbstractPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class PacketFluidStorage extends AbstractPacket {

    private IItemList<IAEFluidStack> fluidStackList;
    private Fluid currentFluid;

    @SuppressWarnings("unused")
    public PacketFluidStorage() {
    }

    public PacketFluidStorage(EntityPlayer _player, IItemList<IAEFluidStack> _list) {
        super(_player);
        mode = 0;
        fluidStackList = _list;
    }

    public PacketFluidStorage(EntityPlayer _player, Fluid _currentFluid) {
        super(_player);
        mode = 1;
        currentFluid = _currentFluid;
    }

    public PacketFluidStorage(EntityPlayer _player) {
        super(_player);
        mode = 2;
    }

    @Override
    public void writeData(ByteBuf out) {
        switch (mode) {
            case 0:
                for (IAEFluidStack stack : fluidStackList) {
                    FluidStack fluidStack = stack.getFluidStack();
                    writeFluid(fluidStack.getFluid(), out);
                    out.writeLong(fluidStack.amount);
                }
                break;
            case 1:
                writeFluid(currentFluid, out);
                break;
            case 2:
                break;
        }
    }

    @Override
    public void readData(ByteBuf in) {
        switch (mode) {
            case 0:
                fluidStackList = AEApi.instance().storage().createFluidList();
                while (in.readableBytes() > 0) {
                    Fluid fluid = readFluid(in);
                    long fluidAmount = in.readLong();
                    if (fluid != null) {
                        IAEFluidStack stack = AEApi.instance().storage().createFluidStack(new FluidStack(fluid, 1));
                        stack.setStackSize(fluidAmount);
                        fluidStackList.add(stack);
                    }
                }
                break;
            case 1:
                currentFluid = readFluid(in);
                break;
            case 2:
                break;
        }
    }

    @Override
    public void execute() {
        switch (mode) {
            case 0:
                if (player != null && player.isClientWorld()) {
                    Gui gui = Minecraft.getMinecraft().currentScreen;
                    if (gui instanceof GuiFluidStorage) {
                        ContainerFluidStorage container = (ContainerFluidStorage) ((GuiFluidStorage) gui).inventorySlots;
                        container.updateFluidList(fluidStackList);
                    }
                }
                break;
            case 1:
                if (player != null && player.openContainer instanceof ContainerFluidStorage) {
                    ((ContainerFluidStorage) player.openContainer).receiveSelectedFluid(currentFluid);
                }
                break;
            case 2:
                if (player != null) {
                    if (!player.worldObj.isRemote) {
                        if (player.openContainer instanceof ContainerFluidStorage) {
                            ((ContainerFluidStorage) player.openContainer).forceFluidUpdate();
                            ((ContainerFluidStorage) player.openContainer).doWork();
                        }
                    }
                }
                break;
        }
    }
}
