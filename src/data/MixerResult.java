package data;

public class MixerResult {
	public ElGamalTuple[][] mixed_board;
	public ProofOfShuffle proof_of_shuffle;

	public MixerResult(ElGamalTuple[][] mixed_board, ProofOfShuffle proof_of_shuffle) {
		this.mixed_board = mixed_board;
		this.proof_of_shuffle = proof_of_shuffle;
	}
}
