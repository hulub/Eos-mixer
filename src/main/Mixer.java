package main;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.pqc.math.linearalgebra.Permutation;

import data.ElGamalTuple;
import data.MixerResult;
import data.ProofOfShuffle;
import tools.Crypto;

public class Mixer {
	public static MixerResult mix(ElGamalTuple[][] board, ECPoint Y) {
		SecureRandom random = new SecureRandom();

		int n = board.length;
		int ell = board[0].length;

		Permutation pi = new Permutation(n, random);
		int[] pi_of = pi.getVector();
		Permutation pi_inverse = pi.computeInverse();
		int[] pi_inverse_of = pi_inverse.getVector();
		// System.out.println(Arrays.toString(pi_of));

		BigInteger[][] s = new BigInteger[n][ell];
		for (int i = 0; i < n; i++)
			for (int j = 0; j < ell; j++)
				s[i][j] = new BigInteger(256, random).mod(Crypto.curve.getN());

		ElGamalTuple[][] mixed_board = new ElGamalTuple[n][ell];
		for (int i = 0; i < n; i++)
			for (int j = 0; j < ell; j++)
				mixed_board[i][j] = Crypto.reEncryptElgamalTuple(board[pi_of[i]][j], s[pi_of[i]][j], Y);

		// List<ElGamalTuple> mixed_votes = Arrays.asList(mixed_board);
		// here you have the new board ----------------------------------------

		// start my proof of shuffle
		BigInteger[] k = new BigInteger[ell];
		for (int j = 0; j < ell; j++)
			k[j] = new BigInteger(256, random).mod(Crypto.curve.getN());
		BigInteger[] m = new BigInteger[n];
		for (int i = 0; i < n; i++)
			m[i] = new BigInteger(256, random).mod(Crypto.curve.getN());

		ECPoint[] A = new ECPoint[ell];
		ECPoint[] B = new ECPoint[ell];

		for (int j = 0; j < ell; j++) {
			A[j] = Crypto.curve.getG().multiply(k[j]);
			B[j] = Y.multiply(k[j]);
		}

		for (int i = 0; i < n; i++)
			for (int j = 0; j < ell; j++) {
				A[j] = A[j].add(board[i][j].R.multiply(m[i]));
				B[j] = B[j].add(board[i][j].C.multiply(m[i]));
			}

		for (int j = 0; j < ell; j++) {
			A[j] = A[j].normalize();
			B[j] = B[j].normalize();
		}

		// compute challenge e_i
		BigInteger[] e = new BigInteger[n];
		for (int i = 0; i < n; i++) {
			Stream<ECPoint> stream = Arrays.stream(board[i]).map(et -> et.R);
			stream = Stream.concat(stream, Arrays.stream(board[i]).map(et -> et.C));
			stream = Stream.concat(stream, Arrays.stream(A));
			stream = Stream.concat(stream, Arrays.stream(B));
			
			e[i] = Crypto.hash(stream.collect(Collectors.toList()));
		}

		BigInteger[] r = new BigInteger[n];
		BigInteger[] t = new BigInteger[ell];
		for (int j = 0; j < ell; j++)
			t[j] = k[j];

		for (int i = 0; i < n; i++) {
			r[i] = m[i].add(e[pi_inverse_of[i]]).mod(Crypto.curve.getN());
			for (int j = 0; j < ell; j++)
				t[j] = t[j].add(e[i].multiply(s[pi_of[i]][j]));
		}
		for (int j = 0; j < ell; j++)
			t[j] = t[j].mod(Crypto.curve.getN());

		ProofOfShuffle proof = new ProofOfShuffle(A, B, e, r, t);

		return new MixerResult(mixed_board, proof);
	}
}
