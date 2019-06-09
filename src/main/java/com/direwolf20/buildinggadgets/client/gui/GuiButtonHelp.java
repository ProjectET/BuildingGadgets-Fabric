package com.direwolf20.buildinggadgets.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public class GuiButtonHelp extends GuiButtonSelect {

    public GuiButtonHelp(int x, int y, @Nullable IPressable action) {
        super(x, y, 12, 12, "?", "", action);
    }

    public String getHoverText() {
        return I18n.format(GuiMod.getLangKeyButton("help", selected ? "help.exit" : "help.enter"));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!visible)
            return;

        GlStateManager.color4f(1, 1, 1, 1);

        isHovered = isHovered(mouseX, mouseY);

        float x = this.x + 5.5F;
        int y = this.y + 6;
        double radius = 6;
        int red, green, blue;
        if (selected) {
            red = blue = 0;
            green = 200;
        } else {
            red = green = blue = 120;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(red, green, blue, 255).endVertex();
        double s = 30;
        for(int k = 0; k <= s; k++)  {
            double angle = (Math.PI * 2 * k / s) + Math.toRadians(180);
            buffer.pos(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius, 0).color(red, green, blue, 255).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();

        int colorText = -1;
        if (this.packedFGColor != 0)
            colorText = this.packedFGColor;
        else if (! this.active)
            colorText = 10526880;
        else if (isHovered)
            colorText = 16777120;

        Minecraft.getInstance().fontRenderer.drawString(getMessage(), this.x + width / 2 - Minecraft.getInstance().fontRenderer.getStringWidth(getMessage()) / 2, this.y + (height - 8) / 2, colorText);
    }

}