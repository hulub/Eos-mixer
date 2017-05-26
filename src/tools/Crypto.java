package tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import data.ElGamalTuple;

public class Crypto {
	public static ECParameterSpec curve = ECNamedCurveTable.getParameterSpec("secp256k1");
	
	public static BigInteger randomBigInteger () {
		byte[] bytes = new byte[256];
		new SecureRandom().nextBytes(bytes);
		return new BigInteger(1, bytes).mod(curve.getN());
	}
	
	/**
	 * Adds all points from the list.
	 * @param list of points to be added
	 * @return one point as the sum of all points
	 */
	public static ECPoint sumOfPoints(List<ECPoint> list) {
		ECPoint Response = curve.getCurve().getInfinity();
		if (list != null)
			for (ECPoint P : list)
				Response = Response.add(P);
		return Response.normalize();
	}
	
	// not used
//	public static ECPoint sumOfPointsStream(List<ECPoint> list) {
//		return list.stream().reduce(curve.getCurve().getInfinity(), (p_i, p_j) -> p_i.add(p_j)).normalize();
//	}

	/**
	 * Computes the hash as a BigInteger. Concatenates the lists and returns the result
	 * @param list1
	 * @param list2
	 * @return the hash as a BigInteger
	 */
	public static BigInteger hash(List<ECPoint> list1, List<ECPoint> list2) {
		List<ECPoint> list = new ArrayList<ECPoint>();
		list.addAll(list1);
		list.addAll(list2);
		return hash(list);
	}
	
	/**
	 * Computes the hash as a BigInteger. Concatenates both points from the tuple
	 * @param an Elgamal tuple
	 * @return the hash as a BigInteger
	 */
	public static BigInteger hash(ElGamalTuple tuple) {
		return hash(Arrays.asList(tuple.R, tuple.C));
	}
	
	// used in LSAG generation
	private static BigInteger hash(BigInteger m, ECPoint g_cypher, ECPoint phi_cypher) {
		ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
		try {
			byte[] m_bytes = m.toByteArray();
			m_bytes = m_bytes.length > 32 ? Arrays.copyOfRange(m_bytes, 1, 33) : m_bytes;
			bytesOutputStream.write(m_bytes);
			bytesOutputStream.write(g_cypher.getEncoded(false));
			bytesOutputStream.write(phi_cypher.getEncoded(false));
		} catch (IOException e) {
			bytesOutputStream = new ByteArrayOutputStream();
		}
		return hash(bytesOutputStream.toByteArray()).mod(curve.getN());
	}

	/**
	 * Computes the hash as a BigInteger.
	 * @param points the list to be hashed
	 * @return the hash as a BigInteger
	 */
	public static BigInteger hash(List<ECPoint> points) {
		if (points == null)
			return BigInteger.ZERO;

		ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
		try {
			for (ECPoint point : points)
				bytesOutputStream.write(point.getEncoded(false));
		} catch (IOException e) {
			bytesOutputStream = new ByteArrayOutputStream();
		}
		return hash(bytesOutputStream.toByteArray()).mod(curve.getN());
	}

	/**
	 * Computes the hash as a BigInteger from a Point
	 * @param Point
	 * @return the hash as a BigInteger
	 */
	public static BigInteger hash(ECPoint Point) {
		return hash(Point.getEncoded(false)).mod(curve.getN());
	}

	/**
	 * Computes the hash as a BigInteger from a String
	 * @param s 
	 * @return the hash as a BigInteger
	 */
	public static BigInteger hash(String s) {
		return hash(s.getBytes()).mod(curve.getN());
	}

	private static BigInteger hash(byte[] bytes) {
		// turn message in BigInteger m
		byte[] mhash = new byte[32];
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			mhash = digest.digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return BigInteger.ONE;
		}
		return new BigInteger(1, mhash);
	}

	/**
	 * Returns {n}^r_Y, the encryption of scalar n, for the public key Y, with the randomness r
	 * @param n
	 * @param Y
	 * @param random
	 * @return the encryption pair
	 */
//	public static ElGamal_Scalar_encryption generateScalarEncryption(BigInteger n, ECPoint Y, Random random) {
//		// sanitize
//		n = n == null ? BigInteger.ONE : n;
//		Y = Y == null ? Curve.G : Y;
//		random = random == null ? new SecureRandom() : random;
//
//		BigInteger r = BigInteger.probablePrime(256, random).mod(Curve.n);
//		ECPoint R = Curve.G.multiply(r).normalize();
//		ECPoint S = Y.multiply(r).normalize();
//		BigInteger s = Crypto.hash(S);
//		BigInteger n_hat = n.multiply(s).mod(Curve.n);
//
//		return new ElGamal_Scalar_encryption(R, n_hat);
//	}

	/**
	 * Returns {P}^r_Y, the encryption of point P, for the public key Y, with the randomness r
	 * @param P
	 * @param Y
	 * @param random
	 * @return the encryption pair
	 */
	public static ElGamalTuple generatePointEncryption(ECPoint P, ECPoint Y, SecureRandom random) {
		// sanitize
		P = P == null ? curve.getG() : P;
		Y = Y == null ? curve.getG() : Y;
		random = random == null ? new SecureRandom() : random;

		BigInteger r = BigInteger.probablePrime(256, random).mod(curve.getN());
		ECPoint R = curve.getG().multiply(r).normalize();
		ECPoint P_hat = P.add(Y.multiply(r)).normalize();

		return new ElGamalTuple(R, P_hat);
	}

	/**
	 * Computes the decrypted P from P_cb={P}^r_Y, where Y=G*x
	 * @param P_cb the encryption pair 
	 * @param x the private key
	 * @return the decrypted Point
	 */
	public static ECPoint decryptPoint(ElGamalTuple P_cb, BigInteger x) {
		ECPoint S = P_cb.R.multiply(x).normalize();
		ECPoint P = P_cb.C.subtract(S).normalize();

		return P;
	}

	/**
	 * Computes the decrypted n from n_cb={n}^r_Y, where Y=G*x
	 * @param n_cb the encryption pair
	 * @param x the private key
	 * @return the decrypted scalar
	 */
//	public static BigInteger decryptScalar(ElGamal_Scalar_encryption n_cb, BigInteger x) {
//		// sanitize
//		n_cb = n_cb == null ? new ElGamal_Scalar_encryption(Curve.G, BigInteger.ONE) : n_cb;
//		x = x == null ? BigInteger.ONE : x;
//
//		ECPoint S = n_cb.R.multiply(x).normalize();
//		BigInteger s = hash(S);
//		BigInteger n = n_cb.n_hat.multiply(s.modInverse(Curve.n)).mod(Curve.n);
//
//		return n;
//	}

	/**
	 * Returns a PK{(x): Y = G*x} = (Commitment, challenge, response) = (C, c, z)
	 * @param G the generator
	 * @param x the private key
	 * @param random
	 * @return the proof of knowledge
	 */
//	public static DiscreteLogarithm generateDiscreteLogarithmProof(ECPoint G, BigInteger x, Random random) {
//		// sanitize
//		G = G == null ? Curve.G : G;
//		x = x == null ? BigInteger.ONE : x;
//		random = random == null ? new SecureRandom() : random;
//
//		BigInteger r = BigInteger.probablePrime(256, random).mod(Curve.n);
//		ECPoint C = G.multiply(r).normalize();
//		BigInteger c = Crypto.hash(Arrays.asList(G, C));
//		BigInteger z = r.add(x.multiply(c)).mod(Curve.n);
//
//		return new DiscreteLogarithm(C, c, z);
//	}

	/**
	 * Returns a PK{(x): AND_i^n Y_i = G_i*x} = (Commitment_list, challenge, response) = (C_list, c, z)
	 * @param G_list the list og generators
	 * @param x the private key
	 * @param random
	 * @return the proof of knowledge
	 */
//	public static DiscreteLogarithmEquality generateDiscreteLogarithmEqualityBetween(List<ECPoint> G_list, BigInteger x,
//			Random random) {
//		// sanitize
//		G_list = G_list == null ? new ArrayList<ECPoint>() : G_list;
//		x = x == null ? BigInteger.ONE : x;
//		random = random == null ? new SecureRandom() : random;
//
//		BigInteger r = BigInteger.probablePrime(256, random).mod(Curve.order);
//		List<ECPoint> C_list = new ArrayList<ECPoint>();
//		for (ECPoint G : G_list)
//			C_list.add(G.multiply(r).normalize());
//
//		BigInteger c = Crypto.hash(G_list, C_list);
//		BigInteger z = r.add(c.multiply(x)).mod(Curve.order);
//
//		return new DiscreteLogarithmEquality(C_list, c, z);
//	}

	/**
	 * The LSAG generation algorithm
	 * @param L the ring
	 * @param pi the position in the ring
	 * @param x the private key associated with one of the public keys in L
	 * @param m the message to be signed
	 * @param random
	 * @return the LSAG signature
	 */
//	public static LSAGSignature generateLSAG(List<ECPoint> L, int pi, BigInteger x, BigInteger m, Random random) {
//		int beta = L.size();
//		BigInteger c, s;
//		List<BigInteger> c_list = new ArrayList<>();
//		List<BigInteger> s_list = new ArrayList<>();
//
//		ECPoint H = Crypto.sumOfPoints(L);
//		ECPoint Y_tilde = H.multiply(x).normalize();
//
//		BigInteger mu = BigInteger.probablePrime(256, random);
//
//		ECPoint H_sb = H.multiply(mu).normalize();
//		ECPoint Y_tilde_sb = Y_tilde.multiply(mu);
//
//		BigInteger u = BigInteger.probablePrime(256, random);
//		ECPoint CG = Curve.G.multiply(u).normalize();
//		ECPoint CH = H_sb.multiply(u).normalize();
//		ECPoint C = CG.add(CH).multiply(m).normalize();
//		c = Crypto.hash(C);
//		c_list.add(c);
//
//		for (int i = 1; i < beta; i++) {
//			s = BigInteger.probablePrime(256, random);
//			s_list.add(s);
//			CG = Curve.G.multiply(s).add(L.get((pi + i) % beta).multiply(c)).normalize();
//			CH = H_sb.multiply(s).add(Y_tilde_sb.multiply(c)).normalize();
//			C = CG.add(CH).multiply(m).normalize();
//			c = hash(C);
//			c_list.add(c);
//		}
//		s = u.subtract(x.multiply(c)).mod(Curve.n);
//		s_list.add(s);
//		Collections.rotate(s_list, pi + 1);
//
//		return new LSAGSignature(c_list.get(beta - pi - 1), s_list, H, Y_tilde, mu);
//	}

	/**
	 * The LSAG verification algorithm
	 * @param c1 one cipher text
	 * @param s	the list of randomness
	 * @param L the ring 
	 * @param phi the malleable H
	 * @param theta the malleable Y_tilde
	 * @param m the signed message
	 * @return true is validation passes, false otherwise
	 */
	public static boolean verifyLSAG(BigInteger c1, List<BigInteger> s, List<ECPoint> L, ECPoint phi,
			ECPoint theta, BigInteger m) {
		if (s.size() != L.size())
			return false;
		
		int beta = L.size();
		BigInteger c = c1;
		for (int i = 0; i < beta; i++) {
			ECPoint g_cypher = curve.getG().multiply(s.get(i)).add(L.get(i).multiply(c)).normalize();
			ECPoint phi_cypher = phi.multiply(s.get(i)).add(theta.multiply(c)).normalize();
			
			c = hash(m, g_cypher, phi_cypher);
		}
		return c.equals(c1);
	}
	
	/**
	 * It performs a re-encryption of an Elgamal tuple
	 * @param elgamalTuple is the tuple to re-encrypt
	 * @param randomness is the re-encryption coefficient
	 * @param key is the encryption key 
	 * @return the re-encrypted ElGamal tuple
	 */
	public static ElGamalTuple reEncryptElgamalTuple(ElGamalTuple elgamalTuple, BigInteger randomness, ECPoint key) {
		ECPoint R = elgamalTuple.R.add(curve.getG().multiply(randomness)).normalize();
		ECPoint C = elgamalTuple.C.add(key.multiply(randomness)).normalize();

		return new ElGamalTuple(R, C);
	}
}
