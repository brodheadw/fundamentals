package ai.gsmc.fundamentals.oxidation;

import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;

public class InertDrumBlockEntity extends BaseContainerBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 1000;
    public static final int TICKS_PER_MB = 2400;
    public static final int VENT = 25;
    private static final long DAY = 24000;
    private static final TagKey<Fluid> KEROSENE = TagKey.create(Registries.FLUID, ResourceLocation.parse("c:kerosene"));

    private static final long UNSET = Long.MIN_VALUE;

    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private long since = UNSET;
    private long leakSince = UNSET;
    private final Tank tank = new Tank();

    private class Tank extends FluidTank {
        Tank() {
            super(CAPACITY, stack -> Separation.reagent(stack.getFluid()) == Separation.fluid("argon") || stack.is(KEROSENE));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            settle();
            return super.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            settle();
            return super.drain(resource, action);
        }

        @Override
        public FluidStack drain(int max, FluidAction action) {
            settle();
            return super.drain(max, action);
        }

        void lose(int amount) {
            super.drain(amount, FluidAction.EXECUTE);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }

    public InertDrumBlockEntity(BlockPos pos, BlockState state) {
        super(Oxidation.drumEntity(), pos, state);
    }

    public IFluidHandler handler(@Nullable Direction side) {
        return tank;
    }

    public FluidStack gas() {
        settle();
        return tank.getFluid();
    }

    private boolean argon() {
        return Separation.reagent(tank.getFluid().getFluid()) == Separation.fluid("argon");
    }

    public void settle() {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        if (since == UNSET) {
            since = leakSince = now;
            setChanged();
            return;
        }
        if (since == now) {
            return;
        }
        long exposed;
        if (tank.isEmpty()) {
            exposed = now - since;
        } else if (argon()) {
            exposed = Math.max(0, now - Math.max(since, leakSince + (long) tank.getFluidAmount() * TICKS_PER_MB));
        } else {
            exposed = 0;
        }
        since = now;
        if (argon()) {
            int leaked = (int) Math.min(tank.getFluidAmount(), (now - leakSince) / TICKS_PER_MB);
            leakSince = tank.getFluidAmount() == leaked ? now : leakSince + (long) leaked * TICKS_PER_MB;
            tank.lose(leaked);
        } else {
            leakSince = now;
        }
        if (exposed > 0) {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, Oxidation.age(items.get(i), exposed, Moisture.AIR, level.random));
            }
        }
        setChanged();
    }

    public void backdate(long ticks) {
        settle();
        since -= ticks;
        leakSince -= ticks;
    }

    @Override
    public void startOpen(Player player) {
        settle();
        if (argon()) {
            tank.lose(VENT);
        }
    }

    public int gasAt(long now) {
        if (!argon() || leakSince == UNSET) {
            return tank.getFluidAmount();
        }
        return (int) Math.max(0, tank.getFluidAmount() - (now - leakSince) / TICKS_PER_MB);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String key = "goggles.fundamentals.inert_storage_drum.";
        int gas = level == null ? tank.getFluidAmount() : gasAt(level.getGameTime());
        tooltip.add(indent(getBlockState().getBlock().getName().withStyle(ChatFormatting.GRAY)));
        if (gas == 0) {
            tooltip.add(indent(Component.translatable(key + "no_gas").withStyle(ChatFormatting.GOLD)));
        } else if (argon()) {
            tooltip.add(indent(Component.translatable(key + "argon", gas, tank.getFluid().getHoverName(), DAY / TICKS_PER_MB)));
        } else {
            tooltip.add(indent(Component.translatable(key + "kerosene", gas, tank.getFluid().getHoverName())));
        }
        int stacks = 0, count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                stacks++;
                count += stack.getCount();
            }
        }
        tooltip.add(indent(Component.translatable(key + "holds", stacks, items.size(), count)));
        tooltip.add(indent(gas == 0 ? Component.translatable(key + "ageing").withStyle(ChatFormatting.RED)
                : Component.translatable(key + "kept").withStyle(ChatFormatting.DARK_GREEN)));
        return true;
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public Component reading() {
        FluidStack gas = gas();
        return gas.isEmpty() ? Component.translatable("block.fundamentals.inert_storage_drum.empty")
                : Component.translatable("block.fundamentals.inert_storage_drum.charged", gas.getAmount(), gas.getHoverName());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        settle();
        return super.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        settle();
        return super.removeItemNoUpdate(slot);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.fundamentals.inert_storage_drum");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("Since", since);
        tag.putLong("LeakSince", leakSince);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        since = tag.contains("Since") ? tag.getLong("Since") : UNSET;
        leakSince = tag.contains("LeakSince") ? tag.getLong("LeakSince") : UNSET;
    }
}
