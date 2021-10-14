package com.direwolf20.buildinggadgets.client;

import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryHelper;
import com.direwolf20.buildinggadgets.common.tainted.inventory.MatchResult;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateHeader;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;

/**
 * This class was adapted from code written by Vazkii
 * Thanks Vazkii!!
 */
@Environment(EnvType.CLIENT)
public class EventUtil {
    private static final String PLACE_HOLDER = "\u00a77\u00a7r\u00a7r\u00a7r\u00a7r\u00a7r";
    public static final Comparator<Multiset.Entry<ItemVariant>> ENTRY_COMPARATOR = Comparator
            .<Multiset.Entry<ItemVariant>, Integer>comparing(Entry::getCount)
            .reversed()
            .thenComparing(e -> Registry.ITEM.getKey(e.getElement().getItem()));

    private static final int STACKS_PER_LINE = 8;

    public static void addTemplatePadding(ItemStack stack, List<Component> tooltip) {
        //This method extends the tooltip box size to fit the item's we will render in onDrawTooltip
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) //populateSearchTreeManager...
            return;

        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(mc.level).ifPresent((ITemplateProvider provider) -> BGComponent.TEMPLATE_KEY_COMPONENT.maybeGet(stack).ifPresent((ITemplateKey templateKey) -> {
            Template template = provider.getTemplateForKey(templateKey);
            IItemIndex index = InventoryHelper.index(stack, mc.player);

            BuildContext buildContext = BuildContext.builder()
                    .stack(stack)
                    .player(mc.player)
                    .build(mc.level);

            TemplateHeader header = template.getHeaderAndForceMaterials(buildContext);
            MaterialList list = header.getRequiredItems();
            if (list == null)
                list = MaterialList.empty();

            MatchResult match;

            try (Transaction transaction = Transaction.openOuter()) {
                match = index.match(list, transaction);
            }

            int count = match.isSuccess() ? match.getChosenOption().entrySet().size() : match.getChosenOption().entrySet().size() + 1;
            if (count > 0 && Screen.hasShiftDown()) {
                int lines = (((count - 1) / STACKS_PER_LINE) + 1) * 2;
                int width = Math.min(STACKS_PER_LINE, count) * 18;
                String spaces = PLACE_HOLDER;
                while (mc.font.width(spaces) < width)
                    spaces += " ";

                for (int j = 0; j < lines; j++)
                    tooltip.add(new TextComponent(spaces));
            }
        }));
    }

    public static void printUUID(ItemStack stack, TooltipFlag context, List<Component> lines) {
        BGComponent.TEMPLATE_KEY_COMPONENT.maybeGet(stack).ifPresent(iTemplateKey -> {
            CompoundTag tag = stack.getOrCreateTag();
            if(tag.hasUUID(NBTKeys.TEMPLATE_KEY_ID)) lines.add(new TextComponent(stack.getOrCreateTag().getUUID(NBTKeys.TEMPLATE_KEY_ID).toString()));
        });
    }

    public static void onDrawTooltip(PoseStack poseStack, ItemStack itemStack, int xin, int yin) {
        if (!Screen.hasShiftDown())
            return;

        //This method will draw items on the tooltip
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null)
            return;

        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(mc.level).ifPresent((ITemplateProvider provider) -> BGComponent.TEMPLATE_KEY_COMPONENT.maybeGet(itemStack).ifPresent((ITemplateKey templateKey) -> {
            Template template = provider.getTemplateForKey(templateKey);
            IItemIndex index = InventoryHelper.index(itemStack, mc.player);
            BuildContext buildContext = BuildContext.builder()
                    .stack(itemStack)
                    .player(mc.player)
                    .build(mc.level);
            TemplateHeader header = template.getHeaderAndForceMaterials(buildContext);
            MaterialList list = header.getRequiredItems();
            if (list == null)
                list = MaterialList.empty();

            MatchResult match;

            try (Transaction transaction = Transaction.openOuter()) {
                match = index.match(list, transaction);
            }

            Multiset<ItemVariant> existing = match.getFoundItems();
            List<Entry<ItemVariant>> sortedEntries = ImmutableList.sortedCopyOf(ENTRY_COMPARATOR, match.getChosenOption().entrySet());

            int by = yin;
            int j = 0;
            int totalMissing = 0;
            List<? extends FormattedText> tooltip = itemStack.getTooltipLines(mc.player, TooltipFlag.Default.NORMAL);
            Font fontRenderer = Minecraft.getInstance().font;
            for (FormattedText s : tooltip) {
                if (s.getString().trim().equals(PLACE_HOLDER))
                    break;
                by += fontRenderer.lineHeight;
            }
            //add missing offset because the Stack is 16 by 16 as a render, not 9 by 9
            //needs to be 8 instead of 7, so that there is a one pixel padding to the text, just as there is between stacks
            by += 8;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            for (Entry<ItemVariant> entry : sortedEntries) {
                int x = xin + (j % STACKS_PER_LINE) * 18;
                int y = by + (j / STACKS_PER_LINE) * 20;
                totalMissing += renderRequiredBlocks(poseStack, entry.getElement().toStack(), x, y, existing.count(entry.getElement()), entry.getCount());
                j++;
            }
        }));
    }

    private static int renderRequiredBlocks(PoseStack matrices, ItemStack itemStack, int x, int y, int count, int req) {
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer render = mc.getItemRenderer();

        String s1 = req == Integer.MAX_VALUE ? "\u221E" : Integer.toString(req);
        int w1 = mc.font.width(s1);

        boolean hasReq = req > 0;

        render.blitOffset += 500f;
        // TODO: fix this, this isn't correct
        render.renderAndDecorateItem(itemStack, x, y);
        render.renderGuiItemDecorations(mc.font, itemStack, x, y);
        render.blitOffset -= 500f;

        matrices.pushPose();
        matrices.translate(x + 8 - w1 / 4f, y + (hasReq ? 12 : 14), render.blitOffset + 800.0F);
        matrices.scale(.5f, .5f, 0);
        MultiBufferSource.BufferSource irendertypebuffer$impl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        mc.font.drawInBatch(s1, 0, 0, 0xFFFFFF, true, matrices.last().pose(), irendertypebuffer$impl, false, 0, 15728880);
        matrices.popPose();

        int missingCount = 0;
        if (hasReq) {
            if (count < req) {
                String fs = Integer.toString(req - count);
                String s2 = "(" + fs + ")";
                int w2 = mc.font.width(s2);

                matrices.pushPose();
                matrices.translate(x + 8 - w2 / 4f, y + 17, render.blitOffset + 800.0F);
                matrices.scale(.5f, .5f, 0);
                mc.font.drawInBatch(s2, 0, 0, 0xFF0000, true, matrices.last().pose(), irendertypebuffer$impl, false, 0, 15728880);
                matrices.popPose();

                missingCount = (req - count);
            }
        }

        irendertypebuffer$impl.endBatch();
        return missingCount;
    }
}
