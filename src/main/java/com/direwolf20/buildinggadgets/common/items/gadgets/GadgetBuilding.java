package com.direwolf20.buildinggadgets.common.items.gadgets;

import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.building.BlockData;
import com.direwolf20.buildinggadgets.common.building.view.IBuildContext;
import com.direwolf20.buildinggadgets.common.building.view.SimpleBuildContext;
import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.inventory.MatchResult;
import com.direwolf20.buildinggadgets.common.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.inventory.materials.objects.IUniqueObject;
import com.direwolf20.buildinggadgets.common.items.gadgets.modes.AbstractMode;
import com.direwolf20.buildinggadgets.common.items.gadgets.modes.BuildingModes;
import com.direwolf20.buildinggadgets.common.items.gadgets.renderers.BaseRenderer;
import com.direwolf20.buildinggadgets.common.items.gadgets.renderers.BuildRender;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.packets.PacketBindTool;
import com.direwolf20.buildinggadgets.common.network.packets.PacketRotateMirror;
import com.direwolf20.buildinggadgets.common.save.Undo;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import com.direwolf20.buildinggadgets.common.util.lang.LangUtil;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import com.direwolf20.buildinggadgets.common.util.lang.TooltipTranslation;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.BlockReference.TagReference;
import com.direwolf20.buildinggadgets.common.world.MockBuilderWorld;
import com.google.common.collect.ImmutableMultiset;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static com.direwolf20.buildinggadgets.common.util.GadgetUtils.*;

public class GadgetBuilding extends AbstractGadget {

    private static final MockBuilderWorld fakeWorld = new MockBuilderWorld();

    public GadgetBuilding(Properties builder, IntSupplier undoLengthSupplier, String undoName) {
        super(builder, undoLengthSupplier, undoName, TagReference.WHITELIST_BUILDING, TagReference.BLACKLIST_BUILDING);
    }

    @Override
    public int getEnergyMax() {
        return Config.GADGETS.GADGET_BUILDING.maxEnergy.get();
    }

    @Override
    public int getEnergyCost(ItemStack tool) {
        return Config.GADGETS.GADGET_BUILDING.energyCost.get();
    }

    @Override
    protected Supplier<BaseRenderer> createRenderFactory() {
        return () -> new BuildRender(false);
    }

    public boolean placeAtop(ItemStack stack) {
        return shouldPlaceAtop(stack);
    }

    private static void setToolMode(ItemStack tool, BuildingModes mode) {
        //Store the tool's mode in NBT as a string
        CompoundNBT tagCompound = tool.getOrCreateTag();
        tagCompound.putString("mode", mode.toString());
    }

    public static BuildingModes getToolMode(ItemStack tool) {
        CompoundNBT tagCompound = tool.getOrCreateTag();
        return BuildingModes.getFromName(tagCompound.getString("mode"));
    }

    public static boolean shouldPlaceAtop(ItemStack stack) {
        return !stack.getOrCreateTag().getBoolean(NBTKeys.GADGET_PLACE_INSIDE);
    }

    public static void togglePlaceAtop(PlayerEntity player, ItemStack stack) {
        stack.getOrCreateTag().putBoolean(NBTKeys.GADGET_PLACE_INSIDE, shouldPlaceAtop(stack));
        player.sendStatusMessage((shouldPlaceAtop(stack) ? MessageTranslation.PLACE_ATOP : MessageTranslation.PLACE_INSIDE).componentTranslation().setStyle(Styles.AQUA), true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        BuildingModes mode = getToolMode(stack);
        addEnergyInformation(tooltip, stack);

        tooltip.add(TooltipTranslation.GADGET_MODE
                .componentTranslation((mode == BuildingModes.SURFACE && getConnectedArea(stack) ? TooltipTranslation.GADGET_CONNECTED
                        .format(mode) : mode))
                .setStyle(Styles.AQUA));

        tooltip.add(TooltipTranslation.GADGET_BLOCK
                .componentTranslation(LangUtil.getFormattedBlockName(getToolBlock(stack).getState()))
                .setStyle(Styles.DK_GREEN));

        int range = getToolRange(stack);
        if (getToolMode(stack) != BuildingModes.BUILD_TO_ME)
            tooltip.add(TooltipTranslation.GADGET_RANGE
                    .componentTranslation(range, getRangeInBlocks(range, mode.getMode()))
                    .setStyle(Styles.LT_PURPLE));

        if (getToolMode(stack) == BuildingModes.SURFACE)
            tooltip.add(TooltipTranslation.GADGET_FUZZY
                    .componentTranslation(String.valueOf(getFuzzy(stack)))
                    .setStyle(Styles.GOLD));

        addInformationRayTraceFluid(tooltip, stack);

        tooltip.add(TooltipTranslation.GADGET_BUILDING_PLACE_ATOP
                .componentTranslation(String.valueOf(shouldPlaceAtop(stack)))
                .setStyle(Styles.YELLOW));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        //On item use, if sneaking, select the block clicked on, else build -- This is called when you right click a tool NOT on a block.
        ItemStack itemstack = player.getHeldItem(hand);

        player.setActiveHand(hand);
        if (!world.isRemote) {
            if (player.isShiftKeyDown()) {
                selectBlock(itemstack, player);
            } else if (player instanceof ServerPlayerEntity) {
                build((ServerPlayerEntity) player, itemstack);
            }
        } else {
            if (!player.isShiftKeyDown()) {
                BaseRenderer.updateInventoryCache();
            } else {
                if (Screen.hasControlDown())
                    PacketHandler.sendToServer(new PacketBindTool());
            }
        }
        return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
    }

    public void setMode(ItemStack heldItem, int modeInt) {
        //Called when we specify a mode with the radial menu
        BuildingModes mode = BuildingModes.values()[modeInt];
        setToolMode(heldItem, mode);
    }

    public static void rangeChange(PlayerEntity player, ItemStack heldItem) {
        //Called when the range change hotkey is pressed
        int range = getToolRange(heldItem);
        int changeAmount = (getToolMode(heldItem) != BuildingModes.SURFACE || (range % 2 == 0)) ? 1 : 2;
        if (player.isShiftKeyDown())
            range = (range == 1) ? Config.GADGETS.maxRange.get() : range - changeAmount;
        else
            range = (range >= Config.GADGETS.maxRange.get()) ? 1 : range + changeAmount;

        setToolRange(heldItem, range);
        player.sendStatusMessage(MessageTranslation.RANGE_SET.componentTranslation(range).setStyle(Styles.AQUA), true);
    }

    private void build(ServerPlayerEntity player, ItemStack stack) {
        //Build the blocks as shown in the visual render
        World world = player.world;
        ItemStack heldItem = getGadget(player);
        if (heldItem.isEmpty())
            return;

        List<BlockPos> coords = GadgetUtils.getAnchor(heldItem).orElse(new ArrayList<>());

        BlockData blockData = getToolBlock(heldItem);
        if (coords.size() == 0) {  //If we don't have an anchor, build in the current spot
            BlockRayTraceResult lookingAt = VectorHelper.getLookingAt(player, stack);
            if (world.isAirBlock(lookingAt.getPos())) //If we aren't looking at anything, exit
                return;

            Direction sideHit = lookingAt.getFace();
            coords = getToolMode(stack).getMode().getCollection(
                    new AbstractMode.UseContext(world, blockData.getState(), lookingAt.getPos(), heldItem, placeAtop(stack)),
                    player,
                    sideHit
            );
        }
        else  //If we do have an anchor, erase it (Even if the build fails)
            setAnchor(stack);

        Undo.Builder builder = Undo.builder();
        IItemIndex index = InventoryHelper.index(stack, player);
        if (blockData.getState() != Blocks.AIR.getDefaultState()) { //Don't attempt a build if a block is not chosen -- Typically only happens on a new tool.
            //TODO replace with a better TileEntity supporting Fake IWorld
            fakeWorld.setWorldAndState(player.world, blockData.getState(), coords); // Initialize the fake world's blocks
            for (BlockPos coordinate : coords) {
                //Get the extended block state in the fake world
                //Disabled to fix Chisel
                //state = state.getBlock().getExtendedState(state, fakeWorld, coordinate);
                placeBlock(world, player, index, builder, coordinate, blockData);
            }
        }
        pushUndo(stack, builder.build(world.getDimension().getType()));
    }

    private void placeBlock(World world, ServerPlayerEntity player, IItemIndex index, Undo.Builder builder, BlockPos pos, BlockData setBlock) {
        if ((pos.getY() > world.getMaxHeight() || pos.getY() < 0) || !player.isAllowEdit())
            return;

        ItemStack heldItem = getGadget(player);
        if (heldItem.isEmpty())
            return;

        boolean useConstructionPaste = false;

        IBuildContext buildContext = new SimpleBuildContext(world, player, heldItem);
        MaterialList requiredItems = setBlock.getRequiredItems(buildContext, null, pos);

        // #majorcode
        MatchResult match = index.tryMatch(requiredItems);
        if (! match.isSuccess()) {
            if (setBlock.getState().hasTileEntity())
                return;
            match = index.tryMatch(InventoryHelper.PASTE_LIST);
            if (! match.isSuccess())
                return;
            else
                useConstructionPaste = true;
        }

        BlockSnapshot blockSnapshot = BlockSnapshot.getBlockSnapshot(world, pos);
        if (ForgeEventFactory.onBlockPlace(player, blockSnapshot, Direction.UP) || ! world.isBlockModifiable(player, pos) || !this.canUse(heldItem, player))
            return;

        this.applyDamage(heldItem, player);

        if (index.applyMatch(match)) {
            ImmutableMultiset<IUniqueObject<?>> usedItems = match.getChosenOption();
            builder.record(world, pos, setBlock, usedItems, ImmutableMultiset.of());
            EffectBlock.spawnEffectBlock(world, pos, setBlock, EffectBlock.Mode.PLACE, useConstructionPaste);
        }
    }

    public static ItemStack getGadget(PlayerEntity player) {
        ItemStack stack = AbstractGadget.getGadget(player);
        if (!(stack.getItem() instanceof GadgetBuilding))
            return ItemStack.EMPTY;
        return stack;
    }

    @Override
    public boolean performRotate(ItemStack stack, PlayerEntity player) {
        GadgetUtils.rotateOrMirrorToolBlock(stack, player, PacketRotateMirror.Operation.ROTATE);
        return true;
    }

    @Override
    public boolean performMirror(ItemStack stack, PlayerEntity player) {
        GadgetUtils.rotateOrMirrorToolBlock(stack, player, PacketRotateMirror.Operation.MIRROR);
        return true;
    }
}
