package name.autoupdater.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybindings {
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.autoupdater.openmenu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Q,
            KeyMapping.Category.MISC
    );
}