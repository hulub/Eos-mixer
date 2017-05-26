package data;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.math.ec.ECPoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import tools.Crypto;
import tools.Printer;

public class ParallelProofOfShuffle {
	public final ECPoint[] A, B;
	public final BigInteger[] e, r;
	public final BigInteger[] t;

	public ParallelProofOfShuffle(ECPoint[] A, ECPoint[] B, BigInteger[] e, BigInteger[] r, BigInteger[] t) {
		this.A = A;
		this.B = B;
		this.e = e;
		this.r = r;
		this.t = t;
	}

	public ParallelProofOfShuffle(JsonObject json) {
		JsonArray A_json = json.get("A").getAsJsonArray();
		A = new ECPoint[A_json.size()];
		int i=0;
		for (JsonElement item : A_json)
			A[i++] = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(item.getAsString()));
		
		JsonArray B_json = json.get("B").getAsJsonArray();
		B = new ECPoint[B_json.size()];
		i=0;
		for (JsonElement item : B_json)
			B[i++] = Crypto.curve.getCurve().decodePoint(Printer.hexToBytes(item.getAsString()));
		
		JsonArray e_json = json.get("e").getAsJsonArray();
		e = new BigInteger[e_json.size()];
		i=0;
		for (JsonElement item : e_json)
			e[i++] = new BigInteger(1, Printer.hexToBytes(item.getAsString()));
		
		JsonArray r_json = json.get("r").getAsJsonArray();
		r = new BigInteger[r_json.size()];
		i=0;
		for (JsonElement item : r_json)
			r[i++] = new BigInteger(1, Printer.hexToBytes(item.getAsString()));
		
		JsonArray t_json = json.get("t").getAsJsonArray();
		t = new BigInteger[t_json.size()];
		i=0;
		for (JsonElement item : t_json)
			t[i++] = new BigInteger(1, Printer.hexToBytes(item.getAsString()));
	}
	
	/**
	 * Verification of the proof
	 * @param input the input board (a_{i,j}, b_{i,j}) to check the proof against
	 * @param output the output board (c_{i,j}, d_{i,j}) to check the proof against
	 * @param Y the encryption key
	 * @return true if the proof verifies successfully, false otherwise
	 */
	public boolean check(List<Ballot> input, List<Ballot> output, ECPoint Y) {
		if (input.size() != output.size()) {
			System.out.println("sizes don't match");
			return false;
		}

		int ell = input.size();
		if (e.length != ell || r.length != ell) {
			System.out.println("sizes don't match");
			return false;
		}

		if (A.length != 3 || B.length != 3 || t.length != 3) {
			System.out.println("sizes don't match");
			return false;
		}

		Ballot[] input_array = input.toArray(new Ballot[ell]), output_array = output.toArray(new Ballot[ell]);

		for (int i = 0; i < ell; i++)
			if (!e[i].equals(Crypto.hash(Arrays.asList(input_array[i].color_enc.R, input_array[i].eID_enc.R,
					input_array[i].vote_enc.R, input_array[i].color_enc.C, input_array[i].eID_enc.C,
					input_array[i].vote_enc.C, A[0], A[1], A[2], B[0], B[1], B[2])))) {
				System.out.println("wrong challenge e_" + i);
				return false;
			}

		// ECPoint totalReEncryption_R = Crypto.curve.getG().multiply(t),
		// totalReEncryption_C = Y.multiply(t);
		ECPoint[] inputToResponse_R = new ECPoint[3], inputToResponse_C = new ECPoint[3],
				outputToChallenge_R = new ECPoint[3], outputToChallenge_C = new ECPoint[3];
		for (int j = 0; j < 3; j++) {
			inputToResponse_R[j] = Crypto.curve.getCurve().getInfinity();
			inputToResponse_C[j] = Crypto.curve.getCurve().getInfinity();
			outputToChallenge_R[j] = Crypto.curve.getCurve().getInfinity();
			outputToChallenge_C[j] = Crypto.curve.getCurve().getInfinity();
		}

		for (int i = 0; i < ell; i++) {
			inputToResponse_R[0] = inputToResponse_R[0].add(input_array[i].color_enc.R.multiply(r[i]));
			inputToResponse_C[0] = inputToResponse_C[0].add(input_array[i].color_enc.C.multiply(r[i]));
			outputToChallenge_R[0] = outputToChallenge_R[0].add(output_array[i].color_enc.R.multiply(e[i]));
			outputToChallenge_C[0] = outputToChallenge_C[0].add(output_array[i].color_enc.C.multiply(e[i]));

			inputToResponse_R[1] = inputToResponse_R[1].add(input_array[i].eID_enc.R.multiply(r[i]));
			inputToResponse_C[1] = inputToResponse_C[1].add(input_array[i].eID_enc.C.multiply(r[i]));
			outputToChallenge_R[1] = outputToChallenge_R[1].add(output_array[i].eID_enc.R.multiply(e[i]));
			outputToChallenge_C[1] = outputToChallenge_C[1].add(output_array[i].eID_enc.C.multiply(e[i]));

			inputToResponse_R[2] = inputToResponse_R[2].add(input_array[i].vote_enc.R.multiply(r[i]));
			inputToResponse_C[2] = inputToResponse_C[2].add(input_array[i].vote_enc.C.multiply(r[i]));
			outputToChallenge_R[2] = outputToChallenge_R[2].add(output_array[i].vote_enc.R.multiply(e[i]));
			outputToChallenge_C[2] = outputToChallenge_C[2].add(output_array[i].vote_enc.C.multiply(e[i]));
		}

		for (int j = 0; j < 3; j++) {
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

		JsonArray A_json = new JsonArray();
		for (ECPoint item : A)
			A_json.add(new JsonPrimitive(Printer.bytesToHex(item.getEncoded(true))));
		json.add("A", A_json);
		
		JsonArray B_json = new JsonArray();
		for (ECPoint item : B)
			B_json.add(new JsonPrimitive(Printer.bytesToHex(item.getEncoded(true))));		
		json.add("B", B_json);

		JsonArray e_json = new JsonArray();
		for (BigInteger item : e)
			e_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("e", e_json);

		JsonArray r_json = new JsonArray();
		for (BigInteger item : r)
			r_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("r", r_json);

		JsonArray t_json = new JsonArray();
		for (BigInteger item : t)
			t_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("t", t_json);

		return json;
	}
}
