package main;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.pqc.math.linearalgebra.Permutation;

import data.Ballot;
import data.ElGamalTuple;
import data.MixerResult;
import data.ParallelMixerResult;
import data.ParallelProofOfShuffle;
import data.ProofOfShuffle;
import tools.Crypto;

public class Mixer {
	private final SecureRandom random;
	public final int id;

	public Mixer(int id) {
		random = new SecureRandom();
		this.id = id;
	}

	/**
	 * The simple mixing algorithm
	 * @param votes the ElGamal tuples to be mixed
	 * @param Y the encryption key
	 * @return the mixed set of ElGamal tuples plus a proof of Correct Shuffle
	 */
	public MixerResult mixVotes(List<ElGamalTuple> votes, ECPoint Y) {
		int ell = votes.size();
		ElGamalTuple[] votes_array = votes.toArray(new ElGamalTuple[ell]);

		Permutation pi = new Permutation(ell, random);
		int[] pi_of = pi.getVector();
		Permutation pi_inverse = pi.computeInverse();
		int[] pi_inverse_of = pi_inverse.getVector();
		// System.out.println(Arrays.toString(pi_of));

		BigInteger[] s = new BigInteger[ell];
		for (int i = 0; i < ell; i++)
			s[i] = new BigInteger(256, random).mod(Crypto.curve.getN());

		ElGamalTuple[] mixed_votes_array = new ElGamalTuple[ell];
		for (int i = 0; i < ell; i++)
			mixed_votes_array[i] = Crypto.reEncryptElgamalTuple(votes_array[pi_of[i]], s[pi_of[i]], Y);

		List<ElGamalTuple> mixed_votes = Arrays.asList(mixed_votes_array);
		// here you have the new board ----------------------------------------

		// start my proof of shuffle
		BigInteger k = new BigInteger(256, random).mod(Crypto.curve.getN());
		BigInteger[] m = new BigInteger[ell];
		for (int i = 0; i < ell; i++)
			m[i] = new BigInteger(256, random).mod(Crypto.curve.getN());

		ECPoint A = Crypto.curve.getG().multiply(k), B = Y.multiply(k);
		for (int i = 0; i < ell; i++) {
			A = A.add(votes_array[i].R.multiply(m[i]));
			B = B.add(votes_array[i].C.multiply(m[i]));
		}

		// compute challenge e_i
		BigInteger[] e = new BigInteger[ell];
		for (int i = 0; i < ell; i++)
			e[i] = Crypto.hash(Arrays.asList(votes_array[i].R, votes_array[i].C, A, B));

		BigInteger[] r = new BigInteger[ell];
		BigInteger t = k;
		for (int i = 0; i < ell; i++) {
			r[i] = m[i].add(e[pi_inverse_of[i]]).mod(Crypto.curve.getN());
			t = t.add(e[i].multiply(s[pi_of[i]]));
		}
		t = t.mod(Crypto.curve.getN());

		ProofOfShuffle proof = new ProofOfShuffle(A, B, e, r, t);

		return new MixerResult(mixed_votes, proof);
	}

	/**
	 * The parallel mixing algorithm
	 * @param ballots the sequence of 3 ElGamal tuples to be mixed
	 * @param Y the encryption key
	 * @return the mixed set of Ballots (3 ElGamal tuples) plus a proof of Correct Parallel Shuffle
	 */
	public ParallelMixerResult mixBallots(List<Ballot> ballots, ECPoint Y) {
		int ell = ballots.size();
		Ballot[] ballots_array = ballots.toArray(new Ballot[ell]);

		Permutation pi = new Permutation(ell, random);
		int[] pi_of = pi.getVector();
		Permutation pi_inverse = pi.computeInverse();
		int[] pi_inverse_of = pi_inverse.getVector();
		// System.out.println(Arrays.toString(pi_of));

		BigInteger[][] s = new BigInteger[ell][3];
		for (int i = 0; i < ell; i++) {
			s[i][0] = new BigInteger(256, random).mod(Crypto.curve.getN());
			s[i][1] = new BigInteger(256, random).mod(Crypto.curve.getN());
			s[i][2] = new BigInteger(256, random).mod(Crypto.curve.getN());
		}

		Ballot[] mixed_ballots_array = new Ballot[ell];
		for (int i = 0; i < ell; i++)
			mixed_ballots_array[i] = new Ballot(
					Crypto.reEncryptElgamalTuple(ballots_array[pi_of[i]].color_enc, s[pi_of[i]][0], Y),
					Crypto.reEncryptElgamalTuple(ballots_array[pi_of[i]].eID_enc, s[pi_of[i]][1], Y),
					Crypto.reEncryptElgamalTuple(ballots_array[pi_of[i]].vote_enc, s[pi_of[i]][2], Y));

		List<Ballot> mixed_ballots = Arrays.asList(mixed_ballots_array);
		// here you have the new board ----------------------------------------

		// start my proof of shuffle
		BigInteger[] k = new BigInteger[3];
		for (int j = 0; j < 3; j++)
			k[j] = new BigInteger(256, random).mod(Crypto.curve.getN());

		BigInteger[] m = new BigInteger[ell];
		for (int i = 0; i < ell; i++)
			m[i] = new BigInteger(256, random).mod(Crypto.curve.getN());

		ECPoint[] A = new ECPoint[3], B = new ECPoint[3];

		for (int j = 0; j < 3; j++) {
			A[j] = Crypto.curve.getG().multiply(k[j]);
			B[j] = Y.multiply(k[j]);
		}

		for (int i = 0; i < ell; i++) {
			A[0] = A[0].add(ballots_array[i].color_enc.R.multiply(m[i]));
			B[0] = B[0].add(ballots_array[i].color_enc.C.multiply(m[i]));

			A[1] = A[1].add(ballots_array[i].eID_enc.R.multiply(m[i]));
			B[1] = B[1].add(ballots_array[i].eID_enc.C.multiply(m[i]));

			A[2] = A[2].add(ballots_array[i].vote_enc.R.multiply(m[i]));
			B[2] = B[2].add(ballots_array[i].vote_enc.C.multiply(m[i]));
		}

		// compute challenge e_i
		BigInteger[] e = new BigInteger[ell];
		for (int i = 0; i < ell; i++)
			e[i] = Crypto.hash(Arrays.asList(ballots_array[i].color_enc.R, ballots_array[i].eID_enc.R,
					ballots_array[i].vote_enc.R, ballots_array[i].color_enc.C, ballots_array[i].eID_enc.C,
					ballots_array[i].vote_enc.C, A[0], A[1], A[2], B[0], B[1], B[2]));

		BigInteger[] r = new BigInteger[ell];
		BigInteger[] t = new BigInteger[3];
		for (int j = 0; j < 3; j++)
			t[j] = k[j];

		for (int i = 0; i < ell; i++) {
			r[i] = m[i].add(e[pi_inverse_of[i]]).mod(Crypto.curve.getN());
			for (int j = 0; j < 3; j++)
				t[j] = t[j].add(e[i].multiply(s[pi_of[i]][j]));
		}
		for (int j = 0; j < 3; j++)
			t[j] = t[j].mod(Crypto.curve.getN());

		ParallelProofOfShuffle proof = new ParallelProofOfShuffle(A, B, e, r, t);

		return new ParallelMixerResult(mixed_ballots, proof);
	}
}
