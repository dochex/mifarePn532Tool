package mifareTools;

public class SectorTrailerUtil {

	private static final int[] mask = { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80 };

	public static void main(String[] args) {
		String[] access = decodeAccessConditions(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x07, (byte) 0x80, (byte) 0x69, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
		for (int i = 0; i < 4; i++) {
			System.out.println(access[i]);
		}
	}
	
	public static String[] decodeAccessConditions(byte byte6, byte byte7, byte byte8) {
		if (byte6 == 0x00 && byte7 == 0x00 && byte8 == 0x00) {
			return new String[] { "Invalid access bytes " };
		}
		String[] accessConditions = new String[4];
		accessConditions[0] = "Block 0: "
				+ decodeDataBlockAccess(getBitByPos(byte7, 4), getBitByPos(byte8, 0), getBitByPos(byte8, 4));
		accessConditions[1] = "Block 1: "
				+ decodeDataBlockAccess(getBitByPos(byte7, 5), getBitByPos(byte8, 1), getBitByPos(byte8, 5));
		accessConditions[2] = "Block 2: "
				+ decodeDataBlockAccess(getBitByPos(byte7, 6), getBitByPos(byte8, 2), getBitByPos(byte8, 6));
		accessConditions[3] = "Trailer: "
				+ decodeTrailerAccess(getBitByPos(byte7, 7), getBitByPos(byte8, 3), getBitByPos(byte8, 7));
		return accessConditions;
	}

	public static String[] decodeAccessConditions(byte[] sectorTrailer) {
		if (sectorTrailer == null || sectorTrailer.length != 16) {
			return new String[] { "Invalid sector trailer" };
		}
		byte byte6 = sectorTrailer[6];
		byte byte7 = sectorTrailer[7];
		byte byte8 = sectorTrailer[8];
		return decodeAccessConditions(byte6, byte7, byte8);
	}

	public static boolean getBitByPos(byte b, int pos) {
		return (b & mask[pos]) > 0;

	}

	private static String decodeDataBlockAccess(boolean c1, boolean c2, boolean c3) {
		if (!c1 && !c2 && !c3)
			return "R:A|B W:A|B I:A|B D:A|B"; // 000 - Transport configuration
		if (!c1 && c2 && !c3)
			return "R:A|B W:- I:- D:-"; // 010
		if (c1 && !c2 && !c3)
			return "R:A|B W:B I:- D:-"; // 100
		if (c1 && c2 && !c3)
			return "R:A|B W:B I:B D:A|B"; // 110
		if (!c1 && !c2 && c3)
			return "R:A|B W:- I:- D:A|B"; // 001
		if (!c1 && c2 && c3)
			return "R:B W:B I:- D:-"; // 011
		if (c1 && !c2 && c3)
			return "R:B W:- I:- D:-"; // 101
		if (c1 && c2 && c3)
			return "R:- W:- I:- D:-"; // 111

		return "Unknown";
	}

	private static String decodeTrailerAccess(boolean c1, boolean c2, boolean c3) {
		if (!c1 && !c2 && !c3)
			return "KeyA (R:- W:A) Access bits (R:A W:-) KeyB (R:A W:A)"; // 000
		if (!c1 && c2 && !c3)
			return "KeyA (R:- W:-) Access bits (R:A W:-) KeyB (R:A W:-)"; // 010
		if (c1 && !c2 && !c3)
			return "KeyA (R:- W:B) Access bits (R:A|B W:-) KeyB (R:- W:B)"; // 100
		if (c1 && c2 && !c3)
			return "KeyA (R:- W:-) Access bits (R:A|B W:-) KeyB (R:- W:-)"; // 110
		if (!c1 && !c2 && c3)
			return "KeyA (R:- W:A) Access bits (R:A W:A) KeyB (R:A W:A)"; // 001 - Transport configuration
		if (!c1 && c2 && c3)
			return "KeyA (R:- W:B) Access bits (R:A|B W:B) KeyB (R:- W:B)"; // 011
		if (c1 && !c2 && c3)
			return "KeyA (R:- W:-) Access bits (R:A|B W:B) KeyB (R:- W:-)"; // 101
		if (c1 && c2 && c3)
			return "KeyA (R:- W:-) Access bits (R:A|B W:-) KeyB (R:- W:-)"; // 111

		return "Unknown";
	}

	/**
	 * Creates a new sector trailer with custom access conditions
	 * 
	 * @param keyA       Key A (6 bytes)
	 * @param keyB       Key B (6 bytes)
	 * @param accessBits Access bits (4 bytes)
	 * @return 16-byte sector trailer
	 */
	public static byte[] createSectorTrailer(byte[] keyA, byte[] keyB, byte[] accessBits) {
		byte[] trailer = new byte[16];
		// Copy key A
		System.arraycopy(keyA, 0, trailer, 0, 6);
		// Copy access bits
		System.arraycopy(accessBits, 0, trailer, 6, 4);
		// Copy key B
		System.arraycopy(keyB, 0, trailer, 10, 6);
		return trailer;
	}

	/**
	 * Creates default access bits for a sector (read/write with both keys)
	 * 
	 * @return 4-byte access bits
	 */
	public static byte[] createDefaultAccessBits() {
		// Default access bits for open access
		return new byte[] { (byte) 0xFF, (byte) 0x07, (byte) 0x80, (byte) 0x69 };
	}

	/**
	 * Creates secure access bits (read-only with key A, read/write with key B)
	 * 
	 * @return 4-byte access bits
	 */
	public static byte[] createSecureAccessBits() {
		// Secure access bits (read-only with key A, read/write with key B)
		return new byte[] { (byte) 0x7F, (byte) 0x07, (byte) 0x88, (byte) 0x40 };
	}

}