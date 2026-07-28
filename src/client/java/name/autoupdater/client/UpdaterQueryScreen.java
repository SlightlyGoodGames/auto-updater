package name.autoupdater.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpdaterQueryScreen extends Screen {
    private final String TITLE_TEXT = "Would you like to update your mods?";
    private final String YES_TEXT = "Yes";
    private final String NO_TEXT = "No";

    public UpdaterQueryScreen(){
        super(Component.empty());
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 4;
        int y = (this.height - buttonHeight) * 4 / 5;

        this.addRenderableWidget(Button.builder(Component.literal(YES_TEXT), _ -> {
            this.minecraft.setScreenAndShow(new UpdateInProgressScreen());
        }).bounds(x, y, buttonWidth, buttonHeight).build());

        x = (this.width - buttonWidth) * 3 / 4;

        this.addRenderableWidget(Button.builder(Component.literal(NO_TEXT), _ -> {
            this.minecraft.setScreenAndShow(null);
        }).bounds(x, y, buttonWidth, buttonHeight).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.text(this.font, TITLE_TEXT,getCentredX(TITLE_TEXT,2), height/5, 0xFFFFFFFF, true);
    }

    private int getCentredX(String str,int placeOnScreen) {
        return ((this.width - this.font.width(str)) / placeOnScreen);
    }
}