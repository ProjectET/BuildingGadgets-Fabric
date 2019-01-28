package com.direwolf20.buildinggadgets.common.blocks.templatemanager;

import com.direwolf20.buildinggadgets.common.BuildingObjects;
import com.direwolf20.buildinggadgets.common.items.ITemplate;
import com.direwolf20.buildinggadgets.common.items.Template;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.tools.*;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateManagerCommands {
    private static final Set<Item> allowedItemsRight = Stream.of(Items.PAPER, BuildingObjects.template).collect(Collectors.toSet());

    public static void loadTemplate(TemplateManagerContainer container, EntityPlayer player) {
        ItemStack itemStack0 = container.getSlot(0).getStack();
        ItemStack itemStack1 = container.getSlot(1).getStack();
        if (!(itemStack0.getItem() instanceof ITemplate) || !(allowedItemsRight.contains(itemStack1.getItem()))) {
            return;
        }
        ITemplate template = (ITemplate) itemStack0.getItem();
        if (itemStack1.getItem().equals(Items.PAPER)) return;
        World world = player.world;

        BlockPos startPos = template.getStartPos(itemStack1);
        BlockPos endPos = template.getEndPos(itemStack1);
        Multiset<UniqueItem> tagMap = template.getItemCountMap(itemStack1);
        String UUIDTemplate = ((Template) BuildingObjects.template).getUUID(itemStack1);
        if (UUIDTemplate == null) return;

        WorldSave worldSave = WorldSave.getWorldSave(world);
        WorldSave templateWorldSave = WorldSave.getTemplateWorldSave(world);
        NBTTagCompound tagCompound;

        template.setStartPos(itemStack0, startPos);
        template.setEndPos(itemStack0, endPos);
        template.setItemCountMap(itemStack0, tagMap);
        String UUID = template.getUUID(itemStack0);

        if (UUID == null) return;

        NBTTagCompound templateTagCompound = templateWorldSave.getCompoundFromUUID(UUIDTemplate);
        tagCompound = templateTagCompound.copy();
        template.incrementCopyCounter(itemStack0);
        tagCompound.setInt("copycounter", template.getCopyCounter(itemStack0));
        tagCompound.setString("UUID", template.getUUID(itemStack0));
        tagCompound.setString("owner", player.getName().getString());
        if (template.equals(BuildingObjects.gadgetCopyPaste)) {
            worldSave.addToMap(UUID, tagCompound);
        } else {
            templateWorldSave.addToMap(UUID, tagCompound);
            Template.setName(itemStack0, Template.getName(itemStack1));
        }
        container.putStackInSlot(0, itemStack0);
// @todo: reimplement @since 1.13.x
        //        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(tagCompound), (EntityPlayerMP) player);
    }

    public static void saveTemplate(TemplateManagerContainer container, EntityPlayer player, String templateName) {
        ItemStack itemStack0 = container.getSlot(0).getStack();
        ItemStack itemStack1 = container.getSlot(1).getStack();

        if (itemStack0.isEmpty() && itemStack1.getItem() instanceof Template && !templateName.isEmpty()) {
            Template.setName(itemStack1, templateName);
            container.putStackInSlot(1, itemStack1);
            return;
        }

        if (!(itemStack0.getItem() instanceof ITemplate) || !(allowedItemsRight.contains(itemStack1.getItem()))) {
            return;
        }
        ITemplate template = (ITemplate) itemStack0.getItem();
        World world = player.world;
        ItemStack templateStack;
        if (itemStack1.getItem().equals(Items.PAPER)) {
            templateStack = new ItemStack(BuildingObjects.template, 1);
            container.putStackInSlot(1, templateStack);
        }
        if (!(container.getSlot(1).getStack().getItem().equals(BuildingObjects.template))) return;
        templateStack = container.getSlot(1).getStack();
        WorldSave worldSave = WorldSave.getWorldSave(world);
        WorldSave templateWorldSave = WorldSave.getTemplateWorldSave(world);
        NBTTagCompound templateTagCompound;

        String UUID = template.getUUID(itemStack0);
        String UUIDTemplate = ((Template) BuildingObjects.template).getUUID(templateStack);
        if (UUID == null) return;
        if (UUIDTemplate == null) return;

        boolean isTool = itemStack0.getItem().equals(BuildingObjects.gadgetCopyPaste);
        NBTTagCompound tagCompound = isTool ? worldSave.getCompoundFromUUID(UUID) : templateWorldSave.getCompoundFromUUID(UUID);
        templateTagCompound = tagCompound.copy();
        template.incrementCopyCounter(templateStack);
        templateTagCompound.setInt("copycounter", template.getCopyCounter(templateStack));
        templateTagCompound.setString("UUID", ((Template) BuildingObjects.template).getUUID(templateStack));

        templateWorldSave.addToMap(UUIDTemplate, templateTagCompound);
        BlockPos startPos = template.getStartPos(itemStack0);
        BlockPos endPos = template.getEndPos(itemStack0);
        Multiset<UniqueItem> tagMap = template.getItemCountMap(itemStack0);
        template.setStartPos(templateStack, startPos);
        template.setEndPos(templateStack, endPos);
        template.setItemCountMap(templateStack, tagMap);
        if (isTool) {
            Template.setName(templateStack, templateName);
        } else {
            if (templateName.equals("")) {
                Template.setName(templateStack, Template.getName(itemStack0));
            } else {
                Template.setName(templateStack, templateName);
            }
        }
        container.putStackInSlot(1, templateStack);
        // @todo: reimplement @since 1.13.x
        //        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(templateTagCompound), (EntityPlayerMP) player);
    }

    public static void pasteTemplate(TemplateManagerContainer container, EntityPlayer player, NBTTagCompound sentTagCompound, String templateName) {
        ItemStack itemStack1 = container.getSlot(1).getStack();

        if (!(allowedItemsRight.contains(itemStack1.getItem()))) {
            return;
        }

        World world = player.world;
        ItemStack templateStack;
        if (itemStack1.getItem().equals(Items.PAPER)) {
            templateStack = new ItemStack(BuildingObjects.template, 1);
            container.putStackInSlot(1, templateStack);
        }
        if (!(container.getSlot(1).getStack().getItem().equals(BuildingObjects.template))) return;
        templateStack = container.getSlot(1).getStack();

        WorldSave templateWorldSave = WorldSave.getTemplateWorldSave(world);
        Template template = (Template) BuildingObjects.template;
        String UUIDTemplate = template.getUUID(templateStack);
        if (UUIDTemplate == null) return;

        NBTTagCompound templateTagCompound;

        templateTagCompound = sentTagCompound.copy();
        BlockPos startPos = GadgetUtils.getPOSFromNBT(templateTagCompound, "startPos");
        BlockPos endPos = GadgetUtils.getPOSFromNBT(templateTagCompound, "endPos");
        template.incrementCopyCounter(templateStack);
        templateTagCompound.setInt("copycounter", template.getCopyCounter(templateStack));
        templateTagCompound.setString("UUID", template.getUUID(templateStack));
        //GadgetUtils.writePOSToNBT(templateTagCompound, startPos, "startPos", 0);
        //GadgetUtils.writePOSToNBT(templateTagCompound, endPos, "startPos", 0);
        //Map<UniqueItem, Integer> tagMap = GadgetUtils.nbtToItemCount((NBTTagList) templateTagCompound.getTag("itemcountmap"));
        //templateTagCompound.removeTag("itemcountmap");

        NBTTagList MapIntStateTag = (NBTTagList) templateTagCompound.getTag("mapIntState");

        BlockMapIntState mapIntState = new BlockMapIntState();
        mapIntState.getIntStateMapFromNBT(MapIntStateTag);
        mapIntState.makeStackMapFromStateMap(player);
        templateTagCompound.setTag("mapIntStack", mapIntState.putIntStackMapIntoNBT());
        templateTagCompound.setString("owner", player.getName().getString());

        Multiset<UniqueItem> itemCountMap = HashMultiset.create();
        Map<IBlockState, UniqueItem> intStackMap = mapIntState.intStackMap;
        List<BlockMap> blockMapList = GadgetCopyPaste.getBlockMapList(templateTagCompound);
        for (BlockMap blockMap : blockMapList) {
            UniqueItem uniqueItem = intStackMap.get(blockMap.state);
            if (!(uniqueItem == null)) {
                NonNullList<ItemStack> drops = NonNullList.create();
                blockMap.state.getBlock().getDrops(blockMap.state, drops, world, new BlockPos(0, 0, 0), 0);
                int neededItems = 0;
                for (ItemStack drop : drops) {
                    if (drop.getItem().equals(uniqueItem.item)) {
                        neededItems++;
                    }
                }
                if (neededItems == 0) {
                    neededItems = 1;
                }
                if (uniqueItem.item != Items.AIR) {
                    itemCountMap.add(uniqueItem,neededItems);
                }
            }
        }

        templateWorldSave.addToMap(UUIDTemplate, templateTagCompound);


        template.setStartPos(templateStack, startPos);
        template.setEndPos(templateStack, endPos);

        template.setItemCountMap(templateStack, itemCountMap);
        Template.setName(templateStack, templateName);
        container.putStackInSlot(1, templateStack);
        // @todo: reimplement @since 1.13.x
        //        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(templateTagCompound), (EntityPlayerMP) player);
    }

    public static void copyTemplate(TemplateManagerContainer container) {
        ItemStack itemStack0 = container.getSlot(0).getStack();
        if (itemStack0.getItem() instanceof GadgetCopyPaste) {
            NBTTagCompound tagCompound = PasteToolBufferBuilder.getTagFromUUID(((GadgetCopyPaste) BuildingObjects.gadgetCopyPaste).getUUID(itemStack0));
            if (tagCompound == null) {
                Minecraft.getInstance().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.copyfailed").getUnformattedComponentText()), false);
                return;
            }
            NBTTagCompound newCompound = new NBTTagCompound();
            newCompound.setIntArray("stateIntArray", tagCompound.getIntArray("stateIntArray"));
            newCompound.setIntArray("posIntArray", tagCompound.getIntArray("posIntArray"));
            newCompound.setTag("mapIntState", tagCompound.getTag("mapIntState"));
            GadgetUtils.writePOSToNBT(newCompound, GadgetUtils.getPOSFromNBT(tagCompound, "startPos"), "startPos", 0);
            GadgetUtils.writePOSToNBT(newCompound, GadgetUtils.getPOSFromNBT(tagCompound, "endPos"), "endPos", 0);
            //Map<UniqueItem, Integer> tagMap = GadgetCopyPaste.getItemCountMap(itemStack0);
            //NBTTagList tagList = GadgetUtils.itemCountToNBT(tagMap);
            //newCompound.setTag("itemcountmap", tagList);
            String jsonTag = newCompound.toString();

            Minecraft.getInstance().keyboardListener.setClipboardString(jsonTag);
            Minecraft.getInstance().player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.copysuccess").getUnformattedComponentText()), false);
        }
    }
}
