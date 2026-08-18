package name.autoupdater;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoUpdater implements ModInitializer {
	public static final String MOD_ID = "auto-updater";
	public static final String MOD_VERSION = "1.2.0+26.2";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {}

	public static void log(String msg){
		LOGGER.info(msg);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
