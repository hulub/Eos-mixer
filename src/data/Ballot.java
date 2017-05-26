package data;

import com.google.gson.JsonObject;

public class Ballot {
public final ElGamalTuple color_enc, eID_enc, vote_enc;

	public Ballot(ElGamalTuple color_enc, ElGamalTuple eID_enc, ElGamalTuple vote_enc) {
		this.color_enc = color_enc;
		this.eID_enc = eID_enc;
		this.vote_enc = vote_enc;
	}
	
	public Ballot(JsonObject json) {
		color_enc = new ElGamalTuple(json.get("color_enc").getAsJsonObject());
		eID_enc = new ElGamalTuple(json.get("eID_enc").getAsJsonObject());
		vote_enc = new ElGamalTuple(json.get("vote_enc").getAsJsonObject());
	}

	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();

		json.add("color_enc", color_enc.toJsonObject());
		json.add("eID_enc", eID_enc.toJsonObject());
		json.add("vote_enc", vote_enc.toJsonObject());

		return json;
	}
}
