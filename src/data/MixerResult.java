package data;

import java.util.List;

public class MixerResult {
	public List<ElGamalTuple> mixed_board;
	public ProofOfShuffle proof_of_shuffle;

	public MixerResult(List<ElGamalTuple> mixed_board, ProofOfShuffle proof_of_shuffle) {
		this.mixed_board = mixed_board;
		this.proof_of_shuffle = proof_of_shuffle;
	}
}
