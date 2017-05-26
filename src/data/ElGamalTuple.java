package data;

import org.bouncycastle.math.ec.ECPoint;

import com.google.gson.JsonObject;

import tools.Crypto;
import tools.Printer;

public class ElGamalTuple {
	public final ECPoint R;
	public final ECPoint C;

	public ElGamalTuple(ECPoint R, ECPoint C) {
		this.R = R;
		this.C = C;
	}

	public ElGamalTuple(JsonObject json) {
		this.R = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(json.get("R").getAsString()));
		this.C = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(json.get("C").getAsString()));
	}

	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();

		json.addProperty("R", Printer.bytesToHex(R.getEncoded(true)));
		json.addProperty("C", Printer.bytesToHex(C.getEncoded(true)));

		return json;
	}
}
