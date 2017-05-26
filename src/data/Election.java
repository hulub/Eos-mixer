package data;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.math.ec.ECPoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import tools.Crypto;
import tools.Printer;

public class Election {
	public final ECPoint Y;
	public final List<ECPoint> L;
	public final List<String> candidates;

	public Election(JsonObject json) {
		Y = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(json.get("Y").getAsString()));
		
		L = new ArrayList<>();
		JsonArray L_json = json.get("L").getAsJsonArray();
		for (JsonElement item : L_json)
			L.add(Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(item.getAsString())));
		
		candidates = new ArrayList<>();
		JsonArray candidates_json = json.get("candidates").getAsJsonArray();
		for (JsonElement item : candidates_json)
			candidates.add(item.getAsString());
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();

		json.addProperty("Y", Printer.bytesToHex(Y.getEncoded(true)));

		JsonArray L_json = new JsonArray();
		for (ECPoint Y : L)
			L_json.add(new JsonPrimitive(Printer.bytesToHex(Y.getEncoded(true))));
		json.add("L", L_json);

		JsonArray candidates_json = new JsonArray();
		for (String c : candidates)
			candidates_json.add(new JsonPrimitive(c));
		json.add("candidates", candidates_json);

		return json;
	}
}
