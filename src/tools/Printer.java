package tools;


public class Printer {
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[(v >>> 4) & 0x0F];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static byte[] hexToBytes(String s) {
	    final int len = s.length();

	    // "111" is not a valid hex encoding.
	    if( len%2 != 0 )
	        throw new IllegalArgumentException("hexBinary needs to be even-length: "+s);

	    byte[] out = new byte[len/2];

	    for( int i=0; i<len; i+=2 ) {
	        int h = hexToBin(s.charAt(i  ));
	        int l = hexToBin(s.charAt(i+1));
	        if( h==-1 || l==-1 )
	            throw new IllegalArgumentException("contains illegal character for hexBinary: "+s);

	        out[i/2] = (byte)(h*16+l);
	    }

	    return out;
	}

	private static int hexToBin( char ch ) {
	    if( '0'<=ch && ch<='9' )    return ch-'0';
	    if( 'A'<=ch && ch<='F' )    return ch-'A'+10;
	    if( 'a'<=ch && ch<='f' )    return ch-'a'+10;
	    return -1;
	}

	public static String numeralString(int n) {
		String ending;
		if      (n % 10 == 1 && n != 11)
			ending = "st";
		else if (n % 10 == 2 && n != 12)
			ending = "nd";
		else if (n % 10 == 3 && n != 13)
			ending = "rd";
		else
			ending = "th";
		return String.format("%3d", n) + ending;
	}
}
