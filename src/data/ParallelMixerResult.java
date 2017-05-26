package data;

import java.util.List;

public class ParallelMixerResult {
	public List<Ballot> mixed_board;
	public ParallelProofOfShuffle proof_of_shuffle;

	public ParallelMixerResult(List<Ballot> mixed_board, ParallelProofOfShuffle proof_of_shuffle) {
		this.mixed_board = mixed_board;
		this.proof_of_shuffle = proof_of_shuffle;
	}
}
