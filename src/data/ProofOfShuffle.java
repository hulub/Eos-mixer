package data;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.math.ec.ECPoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import tools.Crypto;
import tools.Printer;

public class ProofOfShuffle {
	public final ECPoint[] A, B;
	public final BigInteger[] e, r, t;

	public ProofOfShuffle(ECPoint[] A, ECPoint[] B, BigInteger[] e, BigInteger[] r, BigInteger[] t) {
		this.A = A;
		this.B = B;
		this.e = e;
		this.r = r;
		this.t = t;
	}

	public ProofOfShuffle(ECPoint A, ECPoint B, BigInteger[] e, BigInteger[] r, BigInteger t) {
		this.A = new ECPoint[] { A };
		this.B = new ECPoint[] { B };
		this.e = e;
		this.r = r;
		this.t = new BigInteger[] { t };
	}

	public ProofOfShuffle(JsonObject json) {
		int i;

		// read A -- check if array or only one value
		if (json.get("A") instanceof JsonArray) {
			JsonArray A_json = json.get("A").getAsJsonArray();
			A = new ECPoint[A_json.size()];
			i = 0;
			for (JsonElement item : A_json)
				A[i++] = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(item.getAsString()));
		} else {
			ECPoint A_point = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(json.get("A").getAsString()));
			A = new ECPoint[] { A_point };
		}

		// read B -- check if array or only one value
		if (json.get("B") instanceof JsonArray) {
			JsonArray B_json = json.get("B").getAsJsonArray();
			B = new ECPoint[B_json.size()];
			i = 0;
			for (JsonElement item : B_json)
				B[i++] = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(item.getAsString()));
		} else {
			ECPoint B_point = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(json.get("B").getAsString()));
			B = new ECPoint[] { B_point };
		}

		// read e
		JsonArray e_json = json.get("e").getAsJsonArray();
		e = new BigInteger[e_json.size()];
		i = 0;
		for (JsonElement item : e_json)
			e[i++] = new BigInteger(1, Printer.hexToBytes(item.getAsString()));

		// read r
		JsonArray r_json = json.get("r").getAsJsonArray();
		r = new BigInteger[r_json.size()];
		i = 0;
		for (JsonElement item : r_json)
			r[i++] = new BigInteger(1, Printer.hexToBytes(item.getAsString()));

		// read t -- check if array or only one value
		if (json.get("t") instanceof JsonArray) {
			JsonArray t_json = json.get("t").getAsJsonArray();
			t = new BigInteger[t_json.size()];
			i = 0;
			for (JsonElement item : t_json)
				t[i++] = new BigInteger(1, Printer.hexToBytes(item.getAsString()));
		} else {
			BigInteger t_bigint = new BigInteger(1, Printer.hexToBytes(json.get("t").getAsString()));
			t = new BigInteger[] { t_bigint };
		}
	}

	/**
	 * Verification of the proof
	 * @param input the input board (a_{i,j}, b_{i,j}) to check the proof against
	 * @param output the output board (c_{i,j}, d_{i,j}) to check the proof against
	 * @param Y the encryption key
	 * @return true if the proof verifies successfully, false otherwise
	 */
	public boolean check(ElGamalTuple[][] input, ElGamalTuple[][] output, ECPoint Y) {
		if (input.length != output.length || input[0].length != output[0].length) {
			System.out.println("sizes don't match");
			return false;
		}

		int n = input.length;
		int ell = input[0].length;
		if (e.length != n || r.length != n) {
			System.out.println("sizes don't match");
			return false;
		}

		if (A.length != ell || B.length != ell || t.length != ell) {
			System.out.println("sizes don't match");
			return false;
		}

		for (int i = 0; i < n; i++) {
			Stream<ECPoint> stream = Arrays.stream(input[i]).map(et -> et.R);
			stream = Stream.concat(stream, Arrays.stream(input[i]).map(et -> et.C));
			stream = Stream.concat(stream, Arrays.stream(A));
			stream = Stream.concat(stream, Arrays.stream(B));

			if (!e[i].equals(Crypto.hash(stream.collect(Collectors.toList())))) {
				System.out.println("wrong challenge e_" + i);
				return false;
			}
		}

		// ECPoint totalReEncryption_R = Crypto.curve.getG().multiply(t),
		// totalReEncryption_C = Y.multiply(t);
		ECPoint[] inputToResponse_R = new ECPoint[ell], inputToResponse_C = new ECPoint[ell],
				outputToChallenge_R = new ECPoint[ell], outputToChallenge_C = new ECPoint[ell];
		for (int j = 0; j < ell; j++) {
			inputToResponse_R[j] = Crypto.curve.getCurve().getInfinity();
			inputToResponse_C[j] = Crypto.curve.getCurve().getInfinity();
			outputToChallenge_R[j] = Crypto.curve.getCurve().getInfinity();
			outputToChallenge_C[j] = Crypto.curve.getCurve().getInfinity();
		}

		for (int i = 0; i < n; i++)
			for (int j = 0; j < ell; j++) {
				inputToResponse_R[j] = inputToResponse_R[j].add(input[i][j].R.multiply(r[i]));
				inputToResponse_C[j] = inputToResponse_C[j].add(input[i][j].C.multiply(r[i]));
				outputToChallenge_R[j] = outputToChallenge_R[j].add(output[i][j].R.multiply(e[i]));
				outputToChallenge_C[j] = outputToChallenge_C[j].add(output[i][j].C.multiply(e[i]));
			}

		for (int j = 0; j < ell; j++) {
			if (!Crypto.curve.getG().multiply(t[j]).add(inputToResponse_R[j]).normalize()
					.equals(A[j].add(outputToChallenge_R[j]).normalize())) {
				System.out.println("wrong mixing of R_" + j);
				return false;
			}

			if (!Y.multiply(t[j]).add(inputToResponse_C[j]).normalize()
					.equals(B[j].add(outputToChallenge_C[j]).normalize())) {
				System.out.println("wrong mixing of C_" + j);
				return false;
			}
		}

		return true;
	}

	public JsonElement toJsonObject() {
		JsonObject json = new JsonObject();

		if (A.length == 1)
			json.addProperty("A", Printer.bytesToHex(A[0].getEncoded(true)));
		else {
			JsonArray A_json = new JsonArray();
			for (ECPoint item : A)
				A_json.add(new JsonPrimitive(Printer.bytesToHex(item.getEncoded(true))));
			json.add("A", A_json);
		}

		if (B.length == 1)
			json.addProperty("B", Printer.bytesToHex(B[0].getEncoded(true)));
		else {
			JsonArray B_json = new JsonArray();
			for (ECPoint item : B)
				B_json.add(new JsonPrimitive(Printer.bytesToHex(item.getEncoded(true))));
			json.add("B", B_json);
		}

		JsonArray e_json = new JsonArray();
		for (BigInteger item : e)
			e_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("e", e_json);

		JsonArray r_json = new JsonArray();
		for (BigInteger item : r)
			r_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("r", r_json);

		if (t.length == 1)
			json.addProperty("t", Printer.bytesToHex(t[0].toByteArray()));
		else {
			JsonArray t_json = new JsonArray();
			for (BigInteger item : t)
				t_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
			json.add("t", t_json);
		}

		return json;
	}
}
