package net.technicpack.minecraftcore.mojang.version.io.argument;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.technicpack.minecraftcore.launch.ILaunchOptions;

import java.util.Collections;
import java.util.List;

public abstract class Argument {

	public boolean doesApply(ILaunchOptions opts) {
		return true;
	}

	public abstract List<String> getArgStrings();

	public abstract JsonElement serialize();

	public static Argument literal(final String arg) {
		return new Argument() {
			@Override
			public List<String> getArgStrings() {
				return Collections.singletonList(arg);
			}

			@Override
			public JsonElement serialize() {
				return new JsonPrimitive(arg);
			}
		};
	}


}
