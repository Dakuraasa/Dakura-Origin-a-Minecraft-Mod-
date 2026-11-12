package com.dakura.origins.power;

/**
 * Entrypoint for add-on mods that bring their own power or ability types.
 * <p>
 * Declare it in the add-on's {@code fabric.mod.json}:
 * <pre>
 * "entrypoints": { "dakura_origins": [ "com.example.MyAddon" ] }
 * </pre>
 * {@link #registerTypes()} runs once during startup, before race files are loaded. Race JSON files placed in the
 * add-on's {@code resources/dakura_races/} folder are discovered automatically and need no code.
 */
public interface DakuraOriginsPlugin {
	void registerTypes();
}
