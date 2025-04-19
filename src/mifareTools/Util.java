package mifareTools;

import java.util.Arrays;
import java.util.HexFormat;

public final class Util {
	
	// STRUCTURE D'UN PAQUET
	// 00        | 00 0F      | LEN | LCS | TFI | PD ...      | DCS | 00
	// PREAMBLE  | START CODE |     |     |     | PACKET DATA |     | POSTAMBLE
	// LEN (1 byte) : NOMBRE DE BYTES DE TFI + PD
	// LCS (1 byte) : Packet Length Checksum Lower byte of [LEN + LCS] = 0x00
	// TFI (1 byte) : Frame Identifier D4: host -> PN532  D5: PN532 -> host
	// DCS (1 byte) : Data Checksum Lower byte of [TFI + PD0 + PD1 + … + PDn + DCS] = 0x00

	private static final byte[] ACK_FRAME = { 0x00, 0x00, (byte) 0xFF, 0x00, (byte) 0xFF, 0x00 };
	
	public static byte[] decodeHexString(String hexString) {
	    HexFormat hexFormat = HexFormat.of();
	    return hexFormat.parseHex(hexString);
	}

	public static String getByteHexString(byte[] bytes, int startIndex, int length) {
		var output = new StringBuilder();
		output.append('[');
		if (bytes != null) {
			boolean first = true;
			for (int i = startIndex; i < startIndex + length; i++) {
				if (!first) {
					output.append(' ');
				}
				first = false;
				output.append(String.format("%02X", bytes[i]));
			}
		}
		output.append(']');
		//System.out.println(output.toString());
		return output.toString();
	}

	public static String getByteHexString(byte[] bytes) {
		return getByteHexString(bytes, 0, bytes.length);
	}
	
	
	public static String getByteString(byte[] bytes) {
		return getByteString(bytes, 0, bytes.length);
	}


	public static String getByteString(byte[] bytes, int startIndex, int length) {
		var output = new StringBuilder();
		if (bytes != null) {
			for (int i = startIndex; i < startIndex + length; i++) {
				output.append(String.format("%02x", bytes[i]));
			}
		}
		//System.out.println(output.toString());
		return output.toString();
	}

	public static byte addBytes(byte a, byte b) {
		return (byte) ((a & 0xFF) + (b & 0xFF));
	}
	
	protected static byte[] readFrame(byte[] frame) {
		if (!Arrays.equals(ACK_FRAME, Arrays.copyOfRange(frame, 0, ACK_FRAME.length))) {
			System.out.println("Response = No ACK");
			return null;
		} else {
			System.out.print("Response = ACK + ");
			frame = Arrays.copyOfRange(frame, ACK_FRAME.length, frame.length);
			System.out.print(" " + Util.getByteHexString(frame));
			if (frame.length == 0) {
				System.out.println("  -> meaning no data");
			}else {
				System.out.println();
			}
		}
		int offset = 0;
		while (frame[offset] == 0x00) {
			offset++;
			if (offset >= frame.length) {
				System.out.println("Response frame preamble does not contain 0x00FF!");
				return null;
			}
		}
		if (frame[offset] != (byte) 0xFF) {
			System.out.println("Response frame preamble does not contain 0x00FF!");
			return null;
		}
		offset++;
		if (offset >= frame.length) {
			System.out.println("Response frame preamble does not contain 0x00FF!");
			return null;
		}
		byte frameLen = frame[offset];
		byte lengthCkeck = frame[offset + 1];
		if ((frameLen + lengthCkeck) != 0) {
			throw new RuntimeException("Response length checksum did not match length!");
		}
		byte dataChecksum = 0;
		for (int i = offset + 2; i < offset + 2 + frameLen + 1; i++) {
			dataChecksum = Util.addBytes(dataChecksum, frame[i]);
		}
		if (dataChecksum != 0) {
			throw new RuntimeException("Response checksum did not match expected value!");
		}
		return Arrays.copyOfRange(frame, offset + 2, offset + 2 + frameLen);
	}

}
