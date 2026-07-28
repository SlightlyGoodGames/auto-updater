package name.autoupdater.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class AutoUpdaterClient implements ClientModInitializer{
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (Keybindings.OPEN_MENU.consumeClick()) {
				client.setScreenAndShow(new UpdaterQueryScreen());
			}
		});
		KeyMappingHelper.registerKeyMapping(Keybindings.OPEN_MENU);
	}
}