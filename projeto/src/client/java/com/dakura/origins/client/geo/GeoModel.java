package com.dakura.origins.client.geo;

import com.dakura.origins.DakuraOrigins;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Modelo de cubos lido de um arquivo do Blockbench (Bedrock Geometry 1.12.0) em
 * assets/dakura_origins/geo/&lt;nome&gt;.geo.json. Editar o arquivo no Blockbench muda o modelo do jogo.
 * Conversão: y do Bedrock (para cima, pescoço em 24) vira y do Java (para baixo); pivôs passam a ser
 * relativos ao osso pai; rotações em graus viram radianos (pitch, yaw, roll).
 */
public final class GeoModel {
	private static final Map<String, GeoModel> CACHE = new HashMap<>();

	public final ModelPart root;
	private final Map<String, ModelPart> bones;
	private final Map<String, float[]> rest;

	private GeoModel(ModelPart root, Map<String, ModelPart> bones, Map<String, float[]> rest) {
		this.root = root;
		this.bones = bones;
		this.rest = rest;
	}

	/** O modelo (carregado uma vez e guardado). */
	public static GeoModel get(String name) {
		return CACHE.computeIfAbsent(name, GeoModel::load);
	}

	/** Esquece os modelos carregados (para recarregar depois de editar no Blockbench, com F3+T). */
	public static void clear() {
		CACHE.clear();
	}

	public ModelPart bone(String name) {
		ModelPart p = bones.get(name);
		return p != null ? p : root;
	}

	public boolean has(String name) {
		return bones.containsKey(name);
	}

	/** Volta todos os ossos para a pose do arquivo. */
	public void reset() {
		for (Map.Entry<String, ModelPart> e : bones.entrySet()) {
			float[] r = rest.get(e.getKey());
			ModelPart p = e.getValue();
			p.pitch = r[0];
			p.yaw = r[1];
			p.roll = r[2];
			p.visible = true;
		}
	}

	/** Soma uma rotação (radianos) à pose de descanso do osso. */
	public void turn(String name, float pitch, float yaw, float roll) {
		ModelPart p = bones.get(name);
		if (p == null) return;
		float[] r = rest.get(name);
		p.pitch = r[0] + pitch;
		p.yaw = r[1] + yaw;
		p.roll = r[2] + roll;
	}

	/** Define a rotação absoluta do osso (radianos). */
	public void set(String name, float pitch, float yaw, float roll) {
		ModelPart p = bones.get(name);
		if (p == null) return;
		p.pitch = pitch;
		p.yaw = yaw;
		p.roll = roll;
	}

	public float[] restOf(String name) {
		return rest.getOrDefault(name, new float[3]);
	}

	public void render(MatrixStack m, VertexConsumer vc, int light) {
		root.render(m, vc, light, OverlayTexture.DEFAULT_UV);
	}

	public void render(MatrixStack m, VertexConsumer vc, int light, int argb) {
		root.render(m, vc, light, OverlayTexture.DEFAULT_UV, argb);
	}

	// ------------------------------------------------------------------ leitura

	private static GeoModel load(String name) {
		Identifier id = DakuraOrigins.id("geo/" + name + ".geo.json");
		try {
			Optional<Resource> res = MinecraftClient.getInstance().getResourceManager().getResource(id);
			if (res.isEmpty()) throw new IllegalStateException("arquivo não encontrado");
			try (Reader reader = res.get().getReader()) {
				return parse(JsonParser.parseReader(reader).getAsJsonObject());
			}
		} catch (Exception ex) {
			DakuraOrigins.LOGGER.error("[Dakura Origins] Não consegui ler o modelo {}: {}", id, ex.toString());
			ModelData md = new ModelData();
			return new GeoModel(TexturedModelData.of(md, 16, 16).createModel(), Map.of(), Map.of());
		}
	}

	private static float f(JsonArray a, int i) {
		return a.get(i).getAsFloat();
	}

	private static GeoModel parse(JsonObject json) {
		JsonObject geo = json.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
		JsonObject desc = geo.getAsJsonObject("description");
		int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
		ModelData md = new ModelData();
		Map<String, ModelPartData> datas = new HashMap<>();
		Map<String, float[]> absPivot = new HashMap<>();
		Map<String, String> parentOf = new LinkedHashMap<>();
		Map<String, float[]> rest = new HashMap<>();
		for (JsonElement el : geo.getAsJsonArray("bones")) {
			JsonObject b = el.getAsJsonObject();
			String name = b.get("name").getAsString();
			String parent = b.has("parent") ? b.get("parent").getAsString() : null;
			JsonArray pv = b.getAsJsonArray("pivot");
			float[] j = {f(pv, 0), 24 - f(pv, 1), f(pv, 2)};
			float[] pj = parent != null && absPivot.containsKey(parent) ? absPivot.get(parent) : new float[3];
			float[] rot = new float[3];
			if (b.has("rotation")) {
				JsonArray r = b.getAsJsonArray("rotation");
				for (int i = 0; i < 3; i++) rot[i] = (float) Math.toRadians(f(r, i));
			}
			ModelPartBuilder builder = ModelPartBuilder.create();
			if (b.has("cubes")) {
				for (JsonElement ce : b.getAsJsonArray("cubes")) {
					JsonObject c = ce.getAsJsonObject();
					JsonArray o = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size"), uv = c.getAsJsonArray("uv");
					float sx = f(s, 0), sy = f(s, 1), sz = f(s, 2);
					float fx = f(o, 0) - j[0], fy = (24 - f(o, 1) - sy) - j[1], fz = f(o, 2) - j[2];
					float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
					boolean mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
					builder.uv(uv.get(0).getAsInt(), uv.get(1).getAsInt()).mirrored(mirror)
							.cuboid(fx, fy, fz, sx, sy, sz, new Dilation(inflate));
				}
			}
			ModelPartData parentData = parent != null && datas.containsKey(parent) ? datas.get(parent) : md.getRoot();
			ModelPartData data = parentData.addChild(name, builder,
					ModelTransform.of(j[0] - pj[0], j[1] - pj[1], j[2] - pj[2], rot[0], rot[1], rot[2]));
			datas.put(name, data);
			absPivot.put(name, j);
			parentOf.put(name, parent);
			rest.put(name, rot);
		}
		ModelPart root = TexturedModelData.of(md, tw, th).createModel();
		Map<String, ModelPart> bones = new HashMap<>();
		for (String name : parentOf.keySet()) {
			List<String> path = new ArrayList<>();
			for (String n = name; n != null; n = parentOf.get(n)) path.add(0, n);
			ModelPart p = root;
			for (String n : path) p = p.getChild(n);
			bones.put(name, p);
		}
		return new GeoModel(root, bones, rest);
	}
}
