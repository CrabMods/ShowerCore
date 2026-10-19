package mod.crabmod.showercore.client.screen;

import mod.crabmod.showercore.material.BathtubMaterialFace;
import mod.crabmod.showercore.menu.BathtubMaterialMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native slots keep normal inventory, keyboard and ingredient-overlay behavior. */
public class BathtubMaterialScreen extends AbstractContainerScreen<BathtubMaterialMenu> {
    private Button faceButton;

    public BathtubMaterialScreen(BathtubMaterialMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 190;
        inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
        faceButton = addRenderableWidget(Button.builder(faceLabel(), button -> {
            int selected = menu.selectedFace() == BathtubMaterialFace.SIDE ? 1 : 0;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
        }).bounds(leftPos + 20, topPos + 63, 136, 20).build());
    }

    private Component faceLabel() {
        return Component.translatable("menu.showercore.material.face." + menu.selectedFace().serializedName());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        faceButton.setMessage(faceLabel());
        faceButton.active = false;
        var source = menu.getSlot(1).getItem();
        if (source.getItem() instanceof net.minecraft.world.item.BlockItem item) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(item.getBlock());
            var side = mod.crabmod.showercore.client.model.DynamicBathtubMaterialModel.resolveMaterialSprite(id, BathtubMaterialFace.SIDE);
            var top = mod.crabmod.showercore.client.model.DynamicBathtubMaterialModel.resolveMaterialSprite(id, BathtubMaterialFace.TOP);
            faceButton.active = side != null && top != null && side != top;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373431);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + 3, 0xFFFFFFFF);
        for (var slot : menu.slots) {
            int sx = x + slot.x, sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            g.fill(sx, sy, sx + 17, sy + 17, 0xFFFFFFFF);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
        }
        g.drawString(font, "+", x + 56, y + 39, 0xFF404040, false);
        g.drawString(font, "→", x + 108, y + 39, 0xFF404040, false);
        drawLabel(g, Component.translatable("menu.showercore.material.bathtub"), 34, 23);
        drawLabel(g, Component.translatable("menu.showercore.material.block"), 84, 23);
        drawLabel(g, Component.translatable("menu.showercore.material.result"), 142, 23);
        drawLabel(g, Component.translatable("menu.showercore.material.cost", menu.materialCost()),
                imageWidth / 2, 86);
    }

    private void drawLabel(GuiGraphics graphics, Component label, int centerX, int y) {
        graphics.drawString(font, label, leftPos + centerX - font.width(label) / 2,
                topPos + y, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
