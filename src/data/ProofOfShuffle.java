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

public class ProofOfShuffle {
	public final ECPoint A, B;
	public final BigInteger[] e, r;
	public final BigInteger t;

	public ProofOfShuffle(ECPoint A, ECPoint B, BigInteger[] e, BigInteger[] r, BigInteger t) {
		this.A = A;
		this.B = B;
		this.e = e;
		this.r = r;
		this.t = t;
	}

	/**
	 * Verification of the proof
	 * @param input the input board (a_i, b_i) to check the proof against
	 * @param output the output board (c_i, d_i) to check the proof against
	 * @param Y the encryption key
	 * @return true if the proof verifies successfully, false otherwise
	 */
	public boolean check(List<ElGamalTuple> input, List<ElGamalTuple> output, ECPoint Y) {
		if (input.size() != output.size()) {
			System.out.println("sizes don't match");
			return false;
		}

		int ell = input.size();
		if (e.length != ell || r.length != ell) {
			System.out.println("sizes don't match");
			return false;
		}

		ElGamalTuple[] input_array = input.toArray(new ElGamalTuple[ell]),
				output_array = output.toArray(new ElGamalTuple[ell]);

		for (int i = 0; i < ell; i++)
			if (!e[i].equals(Crypto.hash(
					Arrays.asList(input_array[i].R, input_array[i].C, output_array[i].R, output_array[i].C, A, B)))) {
				System.out.println("wrong challenge epsilon_" + i);
				return false;
			}

		ECPoint inputToResponse_R = Crypto.curve.getCurve().getInfinity(),
				inputToResponse_C = Crypto.curve.getCurve().getInfinity();
		ECPoint outputToChallenge_R = Crypto.curve.getCurve().getInfinity(),
				outputToChallenge_C = Crypto.curve.getCurve().getInfinity();
		for (int i = 0; i < ell; i++) {
			inputToResponse_R = inputToResponse_R.add(input_array[i].R.multiply(r[i]));
			inputToResponse_C = inputToResponse_C.add(input_array[i].C.multiply(r[i]));
			outputToChallenge_R = outputToChallenge_R.add(output_array[i].R.multiply(e[i]));
			outputToChallenge_C = outputToChallenge_C.add(output_array[i].C.multiply(e[i]));
		}

		if (!Crypto.curve.getG().multiply(t).add(inputToResponse_R).normalize().equals(A.add(outputToChallenge_R).normalize())) {
			System.out.println("wrong mixing of R");
			return false;
		}

		if (!Y.multiply(t).add(inputToResponse_C).normalize().equals(B.add(outputToChallenge_C).normalize())) {
			System.out.println("wrong mixing of C");
			return false;
		}

		return true;
	}

	public JsonElement toJsonObject() {
		JsonObject json = new JsonObject();

		json.addProperty("A", Printer.bytesToHex(A.getEncoded(true)));
		json.addProperty("B", Printer.bytesToHex(B.getEncoded(true)));

		JsonArray e_json = new JsonArray();
		for (BigInteger item : e)
			e_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("e", e_json);

		JsonArray r_json = new JsonArray();
		for (BigInteger item : r)
			r_json.add(new JsonPrimitive(Printer.bytesToHex(item.toByteArray())));
		json.add("r", r_json);

		json.addProperty("t", Printer.bytesToHex(t.toByteArray()));

		return json;
	}
}
