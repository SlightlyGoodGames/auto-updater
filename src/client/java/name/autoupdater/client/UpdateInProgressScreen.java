package name.autoupdater.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import name.autoupdater.AutoUpdater;

import java.io.IOException;

public class UpdateInProgressScreen extends Screen {
    private int development = 0;
    private EditBox editBox;
    private Button submitButton;
    private Button okButton;

    public UpdateInProgressScreen(){
        super(Component.empty());
    }

    @Override
    protected void init(){
        AutoUpdater.LOGGER.info("Beginning to update mods...");
        Updater.startFunction();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick){
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        String text;

        if(development == 0){
            text = "Downloading "+Updater.getCurrentDownloading()+".jar...";
            guiGraphics.text(this.font,text,getCentredX(text,2), height/5, 0xFFFFFFFF, true);
            try {
                development = Updater.mainFunction();
            } catch (Exception e){
                e.printStackTrace();
            }
        } else if(development == 1){
            text = "Finished downloading!";
            int y = height/5;
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            y += this.font.lineHeight+10;
            text = "A folder called \"updated\" has been created in your mods folder.";
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            y += this.font.lineHeight+10;
            text = "Please move the contents of this folder into the \"mods\" directory.";
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Keep playing"), button -> {
                        this.minecraft.setScreenAndShow(null);
                    }).bounds(this.width / 4 - 50,this.height * 4 / 5,100,20).build()
            );
            this.addRenderableWidget(Button.builder(
                    Component.literal("Quit game"), button -> {
                        Minecraft.getInstance().close();
                    }).bounds(this.width * 3 / 4 - 50,this.height * 4 / 5,100,20).build()
            );
            development = 2;
        } else if(development == 2){
            text = "Finished downloading!";
            int y = height/5;
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            y += this.font.lineHeight+10;
            text = "A folder called \"updated\" has been created in your mods folder.";
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            y += this.font.lineHeight+10;
            text = "Please move the contents of this folder into the \"mods\" directory.";
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
        } else if(development == 3){
            text = "Please enter the Modrinth project ID for "+Updater.getCurrentDownloading()+".jar";
            int y = height/5;
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            y += this.font.lineHeight+10;
            text = "(You should only need to do this once for "+Updater.getCurrentDownloading();
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);

            editBox = this.addRenderableWidget(new EditBox(
                    this.font,
                    width/2-50,
                    height*2/5,
                    100,
                    20,
                    Component.literal("Enter mod ID...")
            ));
            submitButton = this.addRenderableWidget(Button.builder(Component.literal("Submit"),
                    button -> {
                        development = 0;
                        this.removeWidget(editBox);
                        this.removeWidget(submitButton);
                        Updater.updateCurrentProject(editBox.getValue());
                    }
            ).bounds(width/2-50,height*3/5,100,20).build());
            development = 4;
        } else if(development == 4){
            text = "Please enter the Modrinth project ID for "+Updater.getCurrentDownloading()+".jar";
            int y = height/5;
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            y += this.font.lineHeight+10;
            text = "(You should only need to do this once for "+Updater.getCurrentDownloading()+".jar)";
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
        } else if(development == 5){
            text = "It seems that "+Updater.getCurrentDownloading()+" does not support the latest version.";
            int y = height/5;
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
            okButton = this.addRenderableWidget(Button.builder(Component.literal("OK"),
                    button -> {
                        development = 0;
                        this.removeWidget(okButton);
                    }
            ).bounds(width/2-50,height*3/5,100,20).build());
            development = 6;
        } else if(development == 6){
            text = "It seems that "+Updater.getCurrentDownloading()+" does not support the latest version.";
            int y = height/5;
            guiGraphics.text(this.font,text,getCentredX(text,2), y, 0xFFFFFFFF, true);
        }
    }

    private int getCentredX(String str,int placeOnScreen) {
        return ((this.width - this.font.width(str)) / placeOnScreen);
    }
}