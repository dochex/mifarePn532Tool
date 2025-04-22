package mifareTools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fazecast.jSerialComm.SerialPort;

import hexEditor.HexEditor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;

public class MifareMainController {


@FXML
Button connectBtn;
@FXML
TextArea textArea;
@FXML
ChoiceBox<?> choiceSearchKeys;
@FXML
TextField sKeyA;
@FXML
TextField sKeyB;
@FXML
TextField tfBytesToAdd;
@FXML
TextField tfBlock;
@FXML
TextField tfKeyForWriting;
@FXML
TextField tfAccessBits;
@FXML
HexEditor hexEditor;

	SerialPort serialPort;
	private static final int DEFAULT_BAUD_RATE = 115200;
	private static final byte[] WAKE_UP= { 0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF, 0x05, (byte) 0xFB, (byte) 0xD4, 0x14, 0x01, 0x17, 0x00, 0x00};
	// SAMCONFIGURATION : // Normal mode, ?50ms timeout, IRQ disabled
	private static final byte CMD_SAMCONFIGURATION = 0x14;
	private static final byte CMD_GET_VERSION = 0x02;
	private static final byte CMD_GET_STATUS = 0x04;
	private static final byte CMD_INLIST_PASSIVE_TARGET = 0x4A;
	private static final byte CMD_INDATAEXCHANGE = 0x40;
	private static final byte HOST_TO_PN532 = (byte) 0xD4;
	static final byte[] START_CODE = {0x00, 0x00,(byte) 0xFF};
	static final byte[] ACK_FRAME = { 0x00, 0x00, (byte) 0xFF, 0x00, (byte) 0xFF, 0x00 };
	private static final byte CMD_MIFAREREAD = 0x30;
	private static final byte CMD_MIFAREWRITE = (byte) 0xA0; 
	private static final byte CMD_MIFARE_AUTH_B = 0x61; 
	private static final byte CMD_MIFARE_AUTH_A = 0x60; 
	private static final byte[] DEFAULT_KEY_B = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	private static final byte[] DEFAULT_KEY_A = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	private static final byte[] DEFAULT_CONDITIONS = {(byte) 0xFF, 0x07, (byte) 0x80, 0x00};
	private static final byte[] DEFAULT_TRAILER = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x07, (byte) 0x80, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	private static byte LEN = 0, LCS = 0, TFI = 0, DCS = 0;
	
	// Packet handling state
	private static final int STATE_WAIT_PREAMBLE = 0;
	private static final int STATE_WAIT_LEN = 10;
	private static final int STATE_WAIT_LEN_CHECKSUM = 20;
	private static final int STATE_WAIT_ACK_POSTAMBLE = 21;
	private static final int STATE_WAIT_TFI = 30;
	private static final int STATE_SAVE_BYTES = 40;
	private static final int STATE_CHECK_CHECKSUM = 50;
	
	private static byte[] data = new byte[0];
	private static byte[] previous = new byte[1];
	private static int bi = 0;
	private static int state = STATE_WAIT_PREAMBLE;
	private static byte[] uid = new byte[4];
	byte[] savingBuffer = new byte[1024];
	private static String currentDir = "";
	
	// Helper maps for decoding responses
	private static final Map<Integer, String> BITRATES = new HashMap<>();
	private static final Map<Integer, String> MODULATION_TYPES = new HashMap<>();
	
	private static final Map<Integer, String> writeConditions = new HashMap<>(); 
	private final Map<Integer, byte[]> keyBMap = new HashMap<>(); 

	static {
		// Initialize bitrates map
		BITRATES.put(0, "106kbps");
		BITRATES.put(1, "212kbps");
		BITRATES.put(2, "424kbps");

		// Initialize modulation types map
		MODULATION_TYPES.put(0x00,
				"Mifare, ISO/IEC14443-3 Type A, ISO/IEC14443-3 Type B, ISO/IEC18092 passive 106 kbps");
		MODULATION_TYPES.put(0x10, "FeliCa, ISO/IEC18092 passive 212/424 kbps");
		MODULATION_TYPES.put(0x01, "ISO/IEC18092 Active mode");
		MODULATION_TYPES.put(0x02, "Innovision Jewel tag");
		
		currentDir = Path.of("").toAbsolutePath().toString();
	}

	public MifareMainController() {}
	
	public void initialize() {
		Platform.runLater(() -> {
		    SkinBase<ChoiceBox<String>> skin = (SkinBase<ChoiceBox<String>>) choiceSearchKeys.getSkin();
		    for (Node child : skin.getChildren()) {
		        if (child instanceof Label) {
		            Label label = (Label) child;
		            if (label.getText().isEmpty()) {
		                label.setText("Search keys with :");
		            }
		            return;
		        }
		    }
		});
		tfBytesToAdd.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,32}$") ? change : null));
		sKeyA.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,12}$") ? change : null));
		sKeyB.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,12}$") ? change : null));
		tfBlock.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9]{0,2}$") ? change : null));
		tfAccessBits.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,6}$") ? change : null));
		tfKeyForWriting.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,12}$") ? change : null));
		final MenuItem item1 = new MenuItem("Save the changes");
		item1.setOnAction(new EventHandler<ActionEvent>() {
		    public void handle(ActionEvent e) {
		        saveModifiedDump();
		    }
		});
		final ContextMenu contextMenu = new ContextMenu();
		contextMenu.getItems().addAll(item1);
		hexEditor.getArea().setContextMenu(contextMenu);
		SerialPort comPorts[] = SerialPort.getCommPorts();
		int port = -1;
		for (int i = 0; i < comPorts.length; i++) {
			if (comPorts[i].getDescriptivePortName().contains("USB")) {
				textArea.appendText("find device at port comPorts[" + i + "]  " + comPorts[i].getDescriptivePortName() + "\n");
				port = i;			
			}
		}
		if (port == -1) {
			textArea.appendText("No device found, Is an USB-to-Serial converter connected?\n");
        }else {
        	serialPort = comPorts[port];
        	configureSerialPort();
        }
	}
	
	private void configureSerialPort() {
		serialPort.setBaudRate(DEFAULT_BAUD_RATE);
		serialPort.setNumDataBits(8);
		serialPort.setNumStopBits(1);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 50);
	}
	
	/**********************  Processing response  *********************************/
	
	private static boolean processReceivedData(byte[] frame, int dataLength) {
		boolean ackFrame = false;
		System.out.println("Received data: " + Util.getByteHexString(frame));
		for (int dataIndex = 0; dataIndex < dataLength; dataIndex++) {
			byte currentByte = frame[dataIndex];
			if (bi >= 10000) {
				System.out.printf("ERROR: bi=%d which is too big. Starting over.\n", bi);
				bi = 0;
				state = STATE_WAIT_PREAMBLE;
				continue;
			}
			// Look ahead in the data array to identify patterns when possible
			switch (state) {
			case STATE_WAIT_PREAMBLE:
				// Look for 00 FF sequence (preamble)
				if (previous[0] == 0 && currentByte == (byte) 0xFF) {
					state = STATE_WAIT_LEN;
				}
				break;
			case STATE_WAIT_LEN:
				LEN = currentByte;
				state = STATE_WAIT_LEN_CHECKSUM;
				break;
			case STATE_WAIT_LEN_CHECKSUM:
				// Check for special cases in length and checksum
				if ((LEN == (byte) 0xFF && currentByte == (byte) 0xFF)
						|| (LEN == (byte) 0x00 && currentByte == (byte) 0xFF)
						|| (LEN == (byte) 0xFF && currentByte == (byte) 0x00)) {
					// Handle special case patterns
					if (LEN == (byte) 0xFF && currentByte == (byte) 0xFF) {
						System.out.println("ERROR: BIG PACKET. Bye.");
						System.exit(-2);
					} else {
						state = STATE_WAIT_ACK_POSTAMBLE;
					}
				} else {
					// Normal path - validate length checksum
					LCS = (byte) (currentByte + LEN);
					if (LCS != 0) {
						System.out.printf("ERROR: Length checksum failed! 0x%02X\n", currentByte & 0xFF);
					}
					state = STATE_WAIT_TFI;
				}
				break;
			case STATE_WAIT_ACK_POSTAMBLE:
				state = STATE_WAIT_PREAMBLE;
				if (currentByte == 0x00) {
					if (previous[0] == (byte) 0xFF) {
						if(dataIndex + 1 ==dataLength) ackFrame = true;
					}
					if (previous[0] == 0x00) {
						System.out.println("NACK received!");
					}
				} else {
					System.out.println("ERROR: Invalid length, or ACK/NACK missing postamble.");
				}
				break;
			case STATE_WAIT_TFI:
				TFI = currentByte;
				DCS = TFI;
				bi = 0;
				state = STATE_SAVE_BYTES;
				break;
			case STATE_SAVE_BYTES:
				data = new byte[LEN + 1];
				// Check if we can process multiple bytes at once
				int remainingBytes = LEN - bi;
				int availableBytes = dataLength - dataIndex;
				if (remainingBytes <= availableBytes) {
					// Copy all remaining bytes and calculate checksum
					for (int i = 0; i < remainingBytes; i++) {
						data[bi++] = frame[dataIndex + i];
						DCS = (byte) (DCS + frame[dataIndex + i]);
					}
					//System.out.println("data received : " + Util.getByteHexString(frame));
					// Jump ahead in the data array
					dataIndex += remainingBytes - 1; // -1 because the loop will increment dataIndex
					// Move to checksum state
					state = STATE_CHECK_CHECKSUM;
				} else {
					// We don't have all bytes yet, store what we have
					data[bi++] = currentByte;
					DCS = (byte) (DCS + currentByte);
				}				
				break;
			case STATE_CHECK_CHECKSUM:
				state = STATE_WAIT_PREAMBLE;
				DCS = (byte) (DCS + currentByte);
				if (DCS != 0) {
					System.out.printf("ERROR: Data Checksum Failed! (0x%02X)\n", DCS & 0xFF);
				} else {
					//int bile = bi+ 1;;
					//buffin = Arrays.copyOfRange(buffin, 0, bile);
					//processResponse();
				}
				break;
			default:
				System.out.println("Error: Invalid state!");
				state = STATE_WAIT_PREAMBLE;
				break;
			}
			// Save current byte for next iteration
			previous[0] = currentByte;
		}
		return ackFrame;
	}
	
	private void waitForResponse(long timeout) {
		data = new byte[0];
		try {
			Thread.sleep(timeout); // Ajouter un délai de timeout ms
			while (data.length == 0) {
				int available = serialPort.bytesAvailable();
				if (available > 0) {
					byte[] read = new byte[available];
					serialPort.readBytes(read, available);
					// System.out.println(Util.getByteHexString(read) );
					processReceivedData(read, available);
					// System.out.println(Util.getByteHexString(data));
				}
			}
		} catch (InterruptedException e) {
		}	
	}
	
	/*****************************  READING  *********************************/
	
	@FXML
	public void readWithDefaultKeys() {
		readWithKeys(DEFAULT_KEY_A, DEFAULT_KEY_B); 
	}
	
	@FXML
	public void readWithNewKeys() {
		String keyA = sKeyA.getText();
		String keyB = sKeyB.getText();
		readWithKeys(Util.decodeHexString(keyA), Util.decodeHexString(keyB));
	}
	
	private void readMifareBlock(byte blockNumber) {
		byte[] command = new byte[3];
		command[0] = 0x01; // card 1
		command[1] = CMD_MIFAREREAD; // 0x30
		command[2] = blockNumber;
		// System.out.println("readCommand = " + Util.getByteHexString(command));
		callFunction(CMD_INDATAEXCHANGE, command);
		data = new byte[0];
		try {
			Thread.sleep(50); // Ajouter un délai de 50ms
		} catch (InterruptedException e) {
		}
		while (data.length == 0) {
			int available = serialPort.bytesAvailable();
			if (available > 0) {
				byte[] read = new byte[available];
				serialPort.readBytes(read, available);
				// System.out.println(Util.getByteHexString(read) );
				processReceivedData(read, available);
				//System.out.println(Util.getByteHexString(data));
				textArea.appendText(processReadResponse(blockNumber));
			}
		}
	}
	
	private String processReadResponse(byte blockNumber) {
		StringBuilder builder = new StringBuilder();
		// System.out.println("Read response received: " + Util.getByteHexString(buffin));
		if (data[1] == 0x00) {
			builder.append("Block " + blockNumber + " : ");
			
			// System.out.printf("Block " + blockNumber + " : ");
			// Afficher les données du bloc (16 octets pour Mifare Classic)
			for (int i = 0; i < 16; i++) {
				builder.append(String.format("%02X", data[i + 2] & 0xFF));
				savingBuffer[i + blockNumber*16] = (byte) (data[i + 2] & 0xFF);
				// System.out.printf("%02X ", data[i + 2] & 0xFF);
				builder.append(" ");
				// System.out.print(" ");
			}
			builder.append("\n");
			System.out.println();
			if (blockNumber % 4 == 3) {
				String[] accessConditions = SectorTrailerUtil.decodeAccessConditions(Arrays.copyOfRange(data, 2, 18)) ;
				for (int i = 0; i < 4; i++) {
					builder.append(accessConditions[i] + "\n");
				}
				keyBMap.put((int) blockNumber, Arrays.copyOfRange(data, 12, 18));
				setWriteMap(blockNumber, Arrays.copyOfRange(data, 9, 11));			
			}
		} else {
			return String.format("Read failed with error code: 0x%02X\n", data[1] & 0xFF);
		}
		return builder.toString();
	}
	
	public void setWriteMap(byte blockNumber, byte[] bytesAccess) {
		boolean c1 = SectorTrailerUtil.getBitByPos(bytesAccess[0], 7);
		boolean c2 = SectorTrailerUtil.getBitByPos(bytesAccess[1], 3);
		boolean c3 = SectorTrailerUtil.getBitByPos(bytesAccess[1], 7);
		writeConditions.put((int) blockNumber, SectorTrailerUtil.isWritingKey(c1, c2, c3));
	}
	
	public void readTrailers(byte[]keyA, byte[] keyB) {
		System.out.println(Util.getByteHexString(keyA));
		data = new byte[0];   //                                                                           
		boolean auth = false;
		if (!Arrays.equals(uid, new byte[4])) {
			for (int sector = 3; sector < 64; sector = sector + 4) {
				auth = authenticateBlock((byte) (sector), uid, keyA, CMD_MIFARE_AUTH_A);
				if (!auth) {
					auth = authenticateBlock((byte) (sector), uid, keyB, CMD_MIFARE_AUTH_B);
				}
				if (auth) {
					textArea.appendText("Authentication OK, reading trailer of sector " + (sector)/4 + "\n");
					readMifareBlock((byte) (sector));
				}
			}
		}
	}
	
	public void readWithKeys(byte[] keyA, byte[] keyB) {
		System.out.println(Util.getByteHexString(keyA));
		data = new byte[0];   //                                                                           
		boolean auth = false;
		if (!Arrays.equals(uid, new byte[4])) {
			for (int sector = 3; sector < 64; sector = sector + 4) {
				auth = authenticateBlock((byte) (sector), uid, keyA, CMD_MIFARE_AUTH_A);
				if (!auth) {
					auth = authenticateBlock((byte) (sector), uid, keyB, CMD_MIFARE_AUTH_B);
				}
				if (auth) {
					textArea.appendText("Authentication OK, reading sector " + (sector)/4 + "\n");
					for (int block = sector -3; block <= sector; block++) {
						readMifareBlock((byte) (block));
					}
				}
			}
		}
    }
	
	/****************************  Authentication  *********************************/
	
	private boolean authenticateBlock(byte block, byte[] uid, byte[] key, byte cmd) {
		boolean ret = false;
		byte[] command = new byte[13];
		command[0] =  0x01; // card 1
        command[1] = cmd;
        command[2] = block;
		System.arraycopy(key, 0, command, 3, key.length);
		System.arraycopy(uid, 0, command, 9, uid.length);
	    System.out.println("authCommand = " + Util.getByteHexString(command));
	    callFunction(CMD_INDATAEXCHANGE, command);
	    waitForResponse(50);			
		if (data.length > 0) {
			if (data[0] == 0x41) {
				if (data[1] == 0x14) {
					textArea.appendText("Sector " + block/4 + " :Authentication " + cmd + " failed \n");
					ret = false;
				}
				if (data[1] == 0x00) {
					ret = true;
				}
			}
		}
		return ret;
	}
	
	/*****************************  WRITING  *********************************/
	
	@FXML
	private void writeNewTag() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(currentDir + "/nfc-bin64/sauvegardes"));
		File selectedFile = fileChooser.showOpenDialog(null);
		try {
			byte[] dumpData = Files.readAllBytes(selectedFile.toPath());
			for (int block = 0; block < 64; block++) {
				int j = block * 16;
				// copy the block from the dump
				byte[] frame = Arrays.copyOfRange(dumpData, j, j + 16);
				if (block % 4 == 3) { // for trailer block change conditions
					System.arraycopy(DEFAULT_CONDITIONS, 0, frame, 6, 4);
				}
				boolean auth = authenticateBlock((byte) (block), uid, DEFAULT_KEY_A, CMD_MIFARE_AUTH_A);
				if (!auth) {
					textArea.appendText("Authentication failed, block " + block + "\n");
				} else {
					writeToBlock(frame, (byte) (block));
					textArea.appendText("Writing Block " + block + " " + Util.getByteHexString(frame) + "\n ");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void formatTag() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(currentDir + "/nfc-bin64/sauvegardes"));
		File selectedFile = fileChooser.showOpenDialog(null);
		if (selectedFile != null) {
			FormatDialog dialog = new FormatDialog();
			Optional<mifareTools.FormatDialog.Results> optionalResult = dialog.showAndWait();
			optionalResult.ifPresent((mifareTools.FormatDialog.Results results) -> {
				if (results.authKeyA.isEmpty() || keyBMap == null) {
					textArea.appendText(
							"Please enter KeyA in the Format dialog , and read the Tag before trying to format, then restart Format dialog\n");
					return;
				}
				byte[] authKeyA = Util.decodeHexString(results.authKeyA);
				for (int block = 3; block < 64; block = block + 4) {
					if (writeConditions.get(block).equals("")) {
						textArea.appendText("No access conditions for block " + block + "\n");
						return;
					} else if (writeConditions.get(block).equals("KeyA")) {
						boolean auth = authenticateBlock((byte) (block), uid, authKeyA, CMD_MIFARE_AUTH_A);
						if (auth) {
							writeToBlock(DEFAULT_TRAILER, (byte) (block));
						} else {
							textArea.appendText("Authentication block " + block + "failed, \n");
						}
					} else if (writeConditions.get(block).equals("KeyB")) {
						byte[] keyB = keyBMap.get(block);
						boolean auth = authenticateBlock((byte) (block), uid, keyB, CMD_MIFARE_AUTH_A);
						if (auth) {
							writeToBlock(DEFAULT_TRAILER, (byte) (block));
						} else {
							textArea.appendText("Authentication block " + block + "failed, \n");
						}
					}
				}
			});
		}
	}
		

	

	
	@FXML
	private void writeToBlock() {
		if (tfBlock.getText().isEmpty()) {
			textArea.appendText("Please enter block number before trying to write\n");
			return;
		}
		if (tfBytesToAdd.getText().isEmpty()) {
			textArea.appendText("Please enter bytes to add before trying to write\n");
			return;
		}
		if (tfKeyForWriting.getText().isEmpty()) {
			textArea.appendText("Please enter key to add before trying to write\n");
			return;
		}
		String sBlock = tfBlock.getText();
		byte block = (byte) Integer.parseInt(sBlock);
		String sKeyA = tfKeyForWriting.getText();
		byte[] keyA = Util.decodeHexString(sKeyA);
		System.out.println("block = " + block);
		writeToBlockWithAuth(block, keyA);
		
	}
	
	private void writeToBlockWithAuth(byte block, byte[] keyA) {
		if (authenticateBlock(block, uid, keyA, CMD_MIFARE_AUTH_A)) {
			textArea.appendText("authenticate before writing : OK \n");
			byte[] command = new byte[3 + 16];
			command[0] = 0x01; // card 1
			command[1] = CMD_MIFAREWRITE; // 0xA0
			command[2] = block;
			String sBytes = tfBytesToAdd.getText();
			byte[] bytesToAdd = Util.decodeHexString(sBytes);
			System.arraycopy(bytesToAdd, 0, command, 3, bytesToAdd.length);
			System.out.println("writeCommand = " + Util.getByteHexString(command));
			callFunction(CMD_INDATAEXCHANGE, command);
			waitForResponse(200);
			if (data.length > 0) {
				if (data[0] == 0x41) {
					if (data[1] != 0x00) {
						textArea.appendText("Block " + block + " : Write fail with error code : " + data[1] + "\n");
					}
					if (data[1] == 0x00) {
						textArea.appendText("Block " + block + " : Write OK \n");
					}
				}
			}
		} else {
			textArea.appendText("Authentication failed before writing\n");
			return;
		}
		
	}
	
	private void writeToBlock(byte[] eightennthByte, byte block) {
		byte[] command = new byte[3 + 16];
		command[0] = 0x01; // card 1
		command[1] = CMD_MIFAREWRITE; // 0xA0
		command[2] = block;
		byte[] bytesToAdd = eightennthByte;
		System.arraycopy(bytesToAdd, 0, command, 3, bytesToAdd.length);
		// System.out.println("writeCommand = " + Util.getByteHexString(command));
		callFunction(CMD_INDATAEXCHANGE, command);
		waitForResponse(200);
		if (data.length > 0) {
			if (data[0] == 0x41) {
				if (data[1] != 0x00) {
					textArea.appendText("Block " + block + " : Write fail with error code : " + data[1] + "\n");
				}
				if (data[1] == 0x00) {
					textArea.appendText("Block " + block + " : Write OK \n");
				}
			}
		}
	}
	
	/****************************  Utility  ******************************************/
	
	
	@FXML
	public void mfcuk() {
		textArea.appendText("Searching a key with mfcuk, waiting for response....\n");
		serialPort.closePort();
		serialPort.closePort();
		Thread thread =new Thread(() -> {
                ProcessBuilder processBuilder = new ProcessBuilder();
                String basePath = currentDir + "/nfc-bin64/";
                processBuilder.directory(new File(basePath));
                processBuilder.command(basePath + "mfcuk.exe", "-C", "-R", "0:A", "-s 250", "-S 250", "-v 2");
                try {
                    Process process = processBuilder.start();
                    InputStream inputStream = process.getInputStream();
                    InputStream errorStream = process.getErrorStream();
                    printStream(inputStream);
                    printStream(errorStream);
                    process.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
		});
        thread.setDaemon(true);
        thread.start();
	}
	
	@FXML
	public void searchKeys() {
		String keysFile = (String) choiceSearchKeys.getSelectionModel().getSelectedItem();
		textArea.appendText("Searching keys, waiting for response....\n");
		serialPort.closePort();
		Thread thread =new Thread(() -> {
                ProcessBuilder processBuilder = new ProcessBuilder();
                String basePath = currentDir + "/nfc-bin64/";
                processBuilder.directory(new File(basePath));
                processBuilder.command(basePath + "mfoc-hardnested.exe", "-f", basePath + keysFile, "-O", basePath + "sauvegardes/saved.mfd");
                try {
                    Process process = processBuilder.start();
                    InputStream inputStream = process.getInputStream();
                    InputStream errorStream = process.getErrorStream();
                    printStream(inputStream);
                    printStream(errorStream);
                    process.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
	
		});
        thread.setDaemon(true);
        thread.start();
	}
	
	private void printStream(InputStream inputStream) {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
			 String line;
		        while ((line = bufferedReader.readLine()) != null) {
		            final String finalLine = line; // Declare as final
		            if (finalLine.endsWith("[................]")) {
		            	int i = 0;
		            	 Platform.runLater(() -> textArea.appendText("."));
							if (i > 100) {
								Platform.runLater(() -> textArea.appendText("\n"));
								i = 0;
							}
		            } else {
		                Platform.runLater(() -> textArea.appendText(finalLine + "\n"));
		            }
		        }
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
		
	public void saveModifiedDump() {
		String classChanged = hexEditor.getContent();
		try {
			File saved = new File(currentDir +"/nfc-bin64/sauvegardes/modified.mfd");
			byte[] data = Util.decodeHexString(classChanged);
			FileOutputStream destFile = new FileOutputStream(saved);
			try {
				ByteArrayInputStream dataBytes = new ByteArrayInputStream(data);
				try {
					byte buffer[] = new byte[1024];
					int n;
					while ((n = dataBytes.read(buffer)) != -1) {
						destFile.write(buffer, 0, n);
					}
				} finally {
					dataBytes.close();
				}
			} finally {
				destFile.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void saveDump() {
		File outputFile = new File( currentDir + "/nfc-bin64/sauvegardes/outputFile.mfd");
		try {
			Files.write(outputFile.toPath(), savingBuffer);
			hexEditor.setData(outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void openDump() {
		hexEditor.getArea().clear();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File( currentDir + "/nfc-bin64/sauvegardes"));
		File selectedFile = fileChooser.showOpenDialog(null);
		hexEditor.setData(selectedFile);
	}
	
	@FXML
	public void testAccessBits() {
		if (tfAccessBits.getText().isEmpty()) {
			textArea.appendText("Please enter three byte before trying to decode\n");
			return;
		}
		byte[] accessBits = Util.decodeHexString(tfAccessBits.getText());
		String[] decoded = SectorTrailerUtil.decodeAccessConditions(accessBits[0], accessBits[1], accessBits[2]);
		for (int i = 0; i < 4; i++) {
			textArea.appendText(decoded[i] + "\n");
		}
	}

	@FXML
	public void getUid() {
		data = new byte[0];
		callFunction(CMD_INLIST_PASSIVE_TARGET, new byte[] { 0x02, 0x00 });
		waitForResponse(100);
		processCardDetection();
		data = new byte[0];
	}
	
	/*************************  TAG CONNEXION  ***************************/
	
	@FXML
	public void connect() {
		boolean opened = serialPort.openPort();
		if (!opened) {
			textArea.appendText("Error opening COM3\n");
			System.exit(-1);
		}else {
			textArea.appendText("Welcome to Pn532 \n");
		}
		serialPort.writeBytes(WAKE_UP, WAKE_UP.length);
		try {
			Thread.sleep(100); // Ajouter un délai de 50ms
		} catch (InterruptedException e) {
		}
		waitForResponse(10);	
		if (data[0] == CMD_SAMCONFIGURATION + 1) {
			textArea.appendText("PN532 is awake \n");
		}
		callFunction(CMD_GET_VERSION, new byte[] {});
		waitForResponse(50);
		System.out.println("data : " + Util.getByteHexString(data) );
		if (data[0] == CMD_GET_VERSION + 0x01) {
			textArea.appendText("PN532 Version: "+ data[2] +"." + data[3]+ ", features: " + data[4] +"\n");
		}
		callFunction(CMD_GET_STATUS, new byte[0]);
		waitForResponse(50);
		processStatusInfo();
		data = new byte[0];
	}
	
	private void processCardDetection() {
		int cardCount = data[1];
		textArea.appendText(String.format("FOUND %d CARDS!\n", cardCount) + "\n");
		// Process each detected card (only valid for Mifare/ISO type A 106KBPS)
		int bufferPosition = 2; // Start position for the first card data
		if (cardCount == 1) {
			// Get target number
			int targetNumber = data[bufferPosition++];
			textArea.appendText(String.format("Target #%d: ", targetNumber) + "\n");
			// Get SENS_RES (2 bytes)
			int sensResHighByte = data[bufferPosition] & 0xFF;
			int sensResLowByte = data[bufferPosition + 1] & 0xFF;
			textArea.appendText(String.format("SENS_RES=0x%02X%02X, ", sensResHighByte, sensResLowByte) + "\n");
			bufferPosition += 2;
			// Get SEL_RES (1 byte)
			int selRes = data[bufferPosition++] & 0xFF;
			textArea.appendText(String.format("SEL_RES=0x%02X, ", selRes) + "\n");
			// Get NFCID length and data
			int nfcidLength = data[bufferPosition++] & 0xFF;
			textArea.appendText(String.format("NFCIDLength=%d, ", nfcidLength) + "\n");
			textArea.appendText(String.format("NFCID="));
			// Format and display the NFCID (UID) as a colon-separated hex string
			for (int nfcidByteIndex = 0; nfcidByteIndex < nfcidLength; nfcidByteIndex++) {
				uid[nfcidByteIndex] = data[bufferPosition + nfcidByteIndex];
				textArea.appendText(String.format("%02X", data[bufferPosition + nfcidByteIndex] & 0xFF));
				if (nfcidByteIndex < nfcidLength - 1) {
					textArea.appendText(String.format(":"));
				}
			}
			// Move buffer position past the NFCID data
			bufferPosition += nfcidLength;
			textArea.appendText("\n");
			textArea.appendText(String.format("uid = " + Util.getByteHexString(uid))+"\n\n");
		}
	}
	
	private void callFunction(byte command, byte[] params) {
		byte[] data = new byte[2 + params.length];
		data[0] = HOST_TO_PN532;
		data[1] = command;
		System.arraycopy(params, 0, data, 2, params.length);
		writeFrame(data);
	}
	
	private void writeFrame(byte[] data) {
    	int length = data.length;
    	int count = 0;
    	byte[] frame = new byte[length + 1024];
    	while (count <= 8) {
			frame[count] = (byte) 0xFF;
			count++;
		}
    	frame[count++] = START_CODE[0];
		frame[count++] = START_CODE[1];
		frame[count++] = START_CODE[2];
        LEN = (byte) (length & 0xFF);
        frame[count++] = LEN;
        LCS = (byte) -LEN;
        frame[count++] = LCS;
         System.arraycopy(data, 0, frame, count, length);
        count = count + length;
		DCS = (byte) 0xFF;
		for (byte b : data) {
			DCS = Util.addBytes(DCS, b);
		}
		frame[count++] = (byte) ~DCS;
		//frame[count++] = 0x00;
		frame = Arrays.copyOfRange(frame, 0,  count);
		serialPort.writeBytes(frame, frame.length);
		LEN = 0; LCS = 0;DCS = 0;
		//System.out.println("function : " + Util.getByteHexString(frame));
    }
	
	private  void processStatusInfo() {
		String formatted = String.format("Status: Last Error: %d, Field: %d, Targets: %d, SAM Status: 0x%02X\n", data[1], data[2],
				data[3], data[LEN - 2] & 0xFF);
		textArea.appendText(formatted + "\n");
		// Process target information if available
		if (data[3] >= 1) {
			processTargetInfo(1, 4);
		}
		if (data[3] >= 2) {
			processTargetInfo(2, 8);
		}
	}
	
	private void processTargetInfo(int targetNum, int startIndex) {
		String rxBitrate = getBitrateString(data[startIndex + 1] & 0xFF);
		String txBitrate = getBitrateString(data[startIndex + 2] & 0xFF);
		String modType = getModulationTypeString(data[startIndex + 3] & 0xFF);
		String formatted = String.format("Target %d: rx bps: %s, tx bps: %s, modulation type: %s\n", data[startIndex], rxBitrate,
				txBitrate, modType);
		textArea.appendText(formatted + "\n");
	}
	
	/**
	 * Get bitrate string from code
	 */
	private static String getBitrateString(int code) {
		return BITRATES.getOrDefault(code, "Unknown");
	}

	/**
	 * Get modulation type string from code
	 */
	private static String getModulationTypeString(int code) {
		return MODULATION_TYPES.getOrDefault(code, "Unknown");
	}


}
