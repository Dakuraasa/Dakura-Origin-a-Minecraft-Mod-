package com.dakura.origins.power;

/**
 * Ponto de extensão para outros mods (addons).
 *
 * Um addon declara no próprio fabric.mod.json:
 * <pre>
 * "entrypoints": { "dakura_origins": [ "com.exemplo.MeuAddon" ] }
 * </pre>
 * e registra seus tipos aqui. As raças do addon ficam em resources/dakura_races/*.json
 * e são encontradas automaticamente.
 */
public interface DakuraOriginsPlugin {
	void registerTypes();
}
