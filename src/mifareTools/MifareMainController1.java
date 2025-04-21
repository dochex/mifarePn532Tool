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
import mifareTools.DumpDialog.Results;

import java.util.function.Consumer;

public class MifareMainController1 {


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
	private final static Object dataLock = new Object();
	private final Map<Integer, String> writeConditions = new HashMap<>(); 
	private final Map<Integer, byte[]> keyBMap = new HashMap<>(); 

	public MifareMainController1() {}
	
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
		final MenuItem item1 = new MenuItem("Sauvegarder les modifications");
		item1.setOnAction(new EventHandler<ActionEvent>() {
		    public void handle(ActionEvent e) {
		        System.out.println("Sauvegarde les modifications");
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
		//System.out.println("Received data: " + Util.getByteHexString(frame));
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
			    synchronized(dataLock) {
			            // Set data here...
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
			    }
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
	
	private void waitForResponse(long initialDelay) {
	    synchronized(dataLock) {
	        data = new byte[0];
	    }	    
	    try {
	        // Initial delay to allow device processing time
	        Thread.sleep(initialDelay);	        
	        long startTime = System.currentTimeMillis();
	        long maxWaitTime = 1000; // 1 second max wait	        
	        while (System.currentTimeMillis() - startTime < maxWaitTime) {
	            int available = serialPort.bytesAvailable();
	            if (available > 0) {
	                byte[] read = new byte[available];
	                int bytesRead = serialPort.readBytes(read, available);	                
	                if (bytesRead > 0) {
	                    //System.out.println("Received bytes: " + Util.getByteHexString(read));
	                    processReceivedData(read, bytesRead);	                    
	                    // Check if we have complete data
	                    synchronized(dataLock) {
	                        if (data.length > 0) {
	                            return; // We've got our data, exit the wait loop
	                        }
	                    }
	                }
	            }	            
	            // Small delay to prevent busy waiting
	            Thread.sleep(10);
	        }        
	        System.out.println("Timeout waiting for response");
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
	}
	
	/*****************************  READING  *********************************/
	
	@FXML
	public void readWithNewKeys() {
		String keyA = sKeyA.getText();
		String keyB = sKeyB.getText();
		readWithKeys(Util.decodeHexString(keyA), Util.decodeHexString(keyB));
	}
		
	@FXML
	public void readWithDefaultKeys() {
	    readWithKeys(DEFAULT_KEY_A, DEFAULT_KEY_B);
	}

	public void readWithKeys(byte[] keyA, byte[] keyB) {
	    if (!Arrays.equals(uid, new byte[4])) {
	        Platform.runLater(() -> textArea.appendText("Starting to read card...\n"));
	        readSectors(0, keyA, keyB);
	    } else {
	        Platform.runLater(() -> textArea.appendText("Card UID not available. Please scan card first.\n"));
	    }
	}

	private void readSectors(int sectorIndex, byte[] keyA, byte[] keyB) {
	    if (sectorIndex >= 16) { // Assuming 16 sectors
	        Platform.runLater(() -> textArea.appendText("Finished reading all sectors.\n"));
	        return;
	    }	    
	    final int sector = sectorIndex * 4 + 3; // Sector trailer block    
	    // Try key A first
	    authenticateBlock((byte)sector, uid, keyA, CMD_MIFARE_AUTH_A, authResult -> {
	        if (authResult) {
	            Platform.runLater(() -> textArea.appendText("Authentication OK with Key A for sector " + sectorIndex + "\n"));
	            readSectorBlocks(sectorIndex, sector, () -> readSectors(sectorIndex + 1, keyA, keyB));
	        } else {
	            // Try key B
	            authenticateBlock((byte)sector, uid, keyB, CMD_MIFARE_AUTH_B, authBResult -> {
	                if (authBResult) {
	                    Platform.runLater(() -> textArea.appendText("Authentication OK with Key B for sector " + sectorIndex + "\n"));
	                    readSectorBlocks(sectorIndex, sector, () -> readSectors(sectorIndex + 1, keyA, keyB));
	                } else {
	                    Platform.runLater(() -> textArea.appendText("Failed to authenticate sector " + sectorIndex + " with either key\n"));
	                    // Move to next sector
	                    readSectors(sectorIndex + 1, keyA, keyB);
	                }
	            });
	        }
	    });
	}

	private void readSectorBlocks(int sectorIndex, int sectorEnd, Runnable onComplete) {
	    // Calculate the starting block of this sector
	    int startBlock = sectorEnd - 3;	
	    // Debug message
	    System.out.println("Starting to read blocks " + startBlock + " to " + sectorEnd + " for sector " + sectorIndex);
	    Platform.runLater(() -> textArea.appendText("Reading blocks for sector " + sectorIndex + "...\n"));	    
	    // Read blocks from sector start to sector end
	    readBlocksSequentially(startBlock, sectorEnd, onComplete);
	}

	private void readBlocksSequentially(int currentBlock, int endBlock, Runnable onComplete) {
	    if (currentBlock > endBlock) {
	        // All blocks read, run completion callback
	        if (onComplete != null) {
	            onComplete.run();
	        }
	        return;
	    }	    
	   // System.out.println("Reading block " + currentBlock);	    
	    // Read current block
	    readMifareBlock((byte)currentBlock, response -> {
	        Platform.runLater(() -> textArea.appendText(response));   
	        try {
	            Thread.sleep(50);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }    
	        // Block read, move to next block
	        readBlocksSequentially(currentBlock + 1, endBlock, onComplete);
	    });
	}

	private void readMifareBlock(byte blockNumber, Consumer<String> responseHandler) {
	    SerialPortThreadFactory.submit(() -> {
	        try {
	            byte[] command = new byte[3];
	            command[0] = 0x01; // card 1
	            command[1] = CMD_MIFAREREAD; // 0x30
	            command[2] = blockNumber;	            
	            //System.out.println("Sending read command for block " + blockNumber + ": " +  Util.getByteHexString(command));            
	            // Clear data before sending command
	            synchronized(dataLock) {
	                data = new byte[0];
	            }            
	            callFunction(CMD_INDATAEXCHANGE, command);          
	            waitForResponse(70);
	            String response;
	            synchronized(dataLock) {
	                //System.out.println("Read response for block " + blockNumber + ": " + Util.getByteHexString(data));              
	                if (data.length > 0) {
	                    response = processReadResponse(blockNumber);
	                } else {
	                    response = "Block " + blockNumber + ": No response received\n";
	                }
	            }
	            if (responseHandler != null) {
	                final String finalResponse = response;
	                Platform.runLater(() -> responseHandler.accept(finalResponse));
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            String errorMsg = "Error reading block " + blockNumber + ": " + e.getMessage() + "\n";
	            if (responseHandler != null) {
	                Platform.runLater(() -> responseHandler.accept(errorMsg));
	            }
	        }
	    });
	}	
	
	private String processReadResponse(byte blockNumber) {
		StringBuilder builder = new StringBuilder();
		// Check if we have valid data
		if (data.length == 0) {
			return "Block " + blockNumber + ": No data received\n";
		}
		// System.out.println("Processing read response for block "+blockNumber+": "+ Util.getByteHexString(data));
		// First byte should be 0x41 (response to INDATAEXCHANGE)
		if (data[0] != 0x41) {
			return "Block " + blockNumber + ": Invalid response header: " + String.format("0x%02X", data[0] & 0xFF)
					+ "\n";
		}
		// Second byte is status - 0x00 means success
		if (data[1] == 0x00) {
			builder.append("Block " + blockNumber + " : ");
			// Check if we have enough data (16 bytes for a Mifare block + 2 bytes for
			// header)
			if (data.length < 18) {
				return "Block " + blockNumber + ": Incomplete data received\n";
			}
			// Display the data bytes (16 bytes for Mifare Classic)
			for (int i = 0; i < 16; i++) {
				builder.append(String.format("%02X", data[i + 2] & 0xFF));
				savingBuffer[i + blockNumber * 16] = (byte) (data[i + 2] & 0xFF);
				builder.append(" ");
			}
			builder.append("\n");
			// For sector trailer blocks (block % 4 == 3), decode access conditions
			if (blockNumber % 4 == 3) {
				try {
					String[] accessConditions = SectorTrailerUtil
							.decodeAccessConditions(Arrays.copyOfRange(data, 2, 18));
					for (int i = 0; i < accessConditions.length; i++) {
						builder.append(accessConditions[i] + "\n");
					}
					keyBMap.put((int) blockNumber, Arrays.copyOfRange(data, 12, 18));
					writeAccessMap(blockNumber, Arrays.copyOfRange(data, 9, 11));	
				} catch (Exception e) {
					builder.append("Error decoding access conditions: " + e.getMessage() + "\n");
				}
			}
		} else {
			builder.append(
					String.format("Block " + blockNumber + ": Read failed with error code: 0x%02X\n", data[1] & 0xFF));
		}
		return builder.toString();
	}
	
	public void writeAccessMap(byte blockNumber, byte[] bytesAccess) {
		//System.out.println(Util.getByteHexString(bytesAccess));
		boolean c1 = SectorTrailerUtil.getBitByPos(bytesAccess[0], 7);
		boolean c2 = SectorTrailerUtil.getBitByPos(bytesAccess[1], 3);
		boolean c3 = SectorTrailerUtil.getBitByPos(bytesAccess[1], 7);
		String trailerAccess = SectorTrailerUtil.decodeTrailerAccess(c1, c2, c3);
		//System.out.println(SectorTrailerUtil.isWritingKey(c1, c2, c3));
		writeConditions.put((int) blockNumber, SectorTrailerUtil.isWritingKey(c1, c2, c3));
	}
	
/****************************  Authentication  *********************************/
	
	private void authenticateBlock(byte block, byte[] uid, byte[] key, byte cmd, Consumer<Boolean> resultHandler) {
	    SerialPortThreadFactory.submit(() -> {
	        try {
	            byte[] command = new byte[13];
	            command[0] = 0x01; // card 1
	            command[1] = cmd;
	            command[2] = block;
	            System.arraycopy(key, 0, command, 3, key.length);
	            System.arraycopy(uid, 0, command, 9, uid.length);	            
	            //System.out.println("Auth " + cmd + " for block " + block + ": " + Util.getByteHexString(command));
	            callFunction(CMD_INDATAEXCHANGE, command);            
	            waitForResponse(100);	            
	            //System.out.println("Auth response: " + Util.getByteHexString(data));	            
	            boolean authResult = false;
	            synchronized(dataLock) {
	                if (data.length > 0 && data[0] == 0x41) {
	                    if (data[1] == 0x00) {
	                        authResult = true;
	                        //System.out.println("Authentication" + cmd + " successful for block " + block);
	                    } else {
	                        //System.out.println("Authentication " + cmd + " failed for block " + block + " with code: " +  String.format("0x%02X", data[1] & 0xFF));
	                        final String message = "Sector " + block/4 + " :Authentication A failed with code: " + String.format("0x%02X", data[1] & 0xFF) + "\n";
	                        Platform.runLater(() -> textArea.appendText(message));
	                    }
	                } else {
	                    //System.out.println("Invalid or no response for Authentication " + cmd + " on block " + block);
	                    final String message = "Sector " + block/4 + " :No valid Authentication A response\n";
	                    Platform.runLater(() -> textArea.appendText(message));
	                }
	            }	            
	            final boolean finalResult = authResult;
	            if (resultHandler != null) {
	                Platform.runLater(() -> resultHandler.accept(finalResult));
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            if (resultHandler != null) {
	                Platform.runLater(() -> resultHandler.accept(false));
	            }
	        }
	    });
	}
	
	/*****************************  WRITING  *********************************/
	
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
		String sKey = tfKeyForWriting.getText();
		byte[] key = Util.decodeHexString(sKey);
		System.out.println("block = " + block);
		// Get data to write
		String sBytes = tfBytesToAdd.getText();
		byte[] bytesToAdd = Util.decodeHexString(sBytes);
		// First authenticate, then write
		authenticateBlock(block, uid, key, CMD_MIFARE_AUTH_A, authResult -> {
			if (authResult) {
				Platform.runLater(
						() -> textArea.appendText("Authentication OK, proceeding to write block " + block + "\n"));
				writeToBlock(bytesToAdd, block, writeResult -> {
					Platform.runLater(() -> textArea.appendText(writeResult));
				});
			} else { // Try authenticate B
				authenticateBlock(block, uid, key, CMD_MIFARE_AUTH_B, authBResult2 -> {
					if (authBResult2) {
						Platform.runLater(() -> textArea
								.appendText("Authentication OK, proceeding to write block " + block + "\n"));
						writeToBlock(bytesToAdd, block, writeResult -> {
							Platform.runLater(() -> textArea.appendText(writeResult));
						});
					} else {
						Platform.runLater(() -> textArea.appendText("Authentication failed before writing\n"));
					}
				});
			}
		});
	}

	private void writeToBlock(byte[] dataToWrite, byte block, Consumer<String> resultHandler) {
		SerialPortThreadFactory.submit(() -> {
			try {
				byte[] command = new byte[3 + 16];
				command[0] = 0x01; // card 1
				command[1] = CMD_MIFAREWRITE; // 0xA0
				command[2] = block;
				// Make sure we have 16 bytes (pad if necessary)
				byte[] bytesToAdd = new byte[16];
				System.arraycopy(dataToWrite, 0, bytesToAdd, 0, Math.min(dataToWrite.length, 16));
				System.arraycopy(bytesToAdd, 0, command, 3, bytesToAdd.length);
				System.out.println("Write Command = " + Util.getByteHexString(command));
				// Clear previous data
				synchronized (dataLock) {
					data = new byte[0];
				}
				callFunction(CMD_INDATAEXCHANGE, command);
				waitForResponse(200);
				String result;
				synchronized (dataLock) {
					if (data.length > 0 && data[0] == 0x41) {
						if (data[1] == 0x00) {
							result = "Block " + block + " : Write successful\n";
							System.out.println("Write successful for block " + block);
						} else {
							result = "Block " + block + " : Write failed with error code: "
									+ String.format("0x%02X", data[1] & 0xFF) + "\n";
							System.out.println("Write failed for block " + block + " with code: "
									+ String.format("0x%02X", data[1] & 0xFF));
						}
					} else {
						result = "Block " + block + " : Invalid or no response received\n";
						System.out.println("Invalid or no response for write on block " + block);
					}
				}
				// Return result to callback
				if (resultHandler != null) {
					final String finalResult = result;
					Platform.runLater(() -> resultHandler.accept(finalResult));
				}
			} catch (Exception e) {
				e.printStackTrace();
				String errorMsg = "Error writing to block " + block + ": " + e.getMessage() + "\n";
				if (resultHandler != null) {
					Platform.runLater(() -> resultHandler.accept(errorMsg));
				}
			}
		});
	}
	
	@FXML
	private void writeNewTag() {
	    FileChooser fileChooser = new FileChooser();
	    fileChooser.setInitialDirectory(new File(currentDir + "/nfc-bin64/sauvegardes"));
	    File selectedFile = fileChooser.showOpenDialog(null);
	    if (selectedFile != null) {
	        try {
	            byte[] dumpData = Files.readAllBytes(selectedFile.toPath());
	            // Start with first block and process sequentially
	            writeNextBlock(dumpData, 0);
	        } catch (IOException e) {
	            e.printStackTrace();
	            Platform.runLater(() -> textArea.appendText("Error reading dump file: " + e.getMessage() + "\n"));
	        }
	    }
	}

	private void writeNextBlock(byte[] dumpData, int blockNumber) {
	    // Check if we've processed all blocks
	    if (blockNumber >= 64) {
	        Platform.runLater(() -> textArea.appendText("Card writing completed successfully!\n"));
	        return;
	    }	    
	    int j = blockNumber * 16;
	    // copy the block from the dump
	    byte[] frame = Arrays.copyOfRange(dumpData, j, j + 16);
	    if (blockNumber % 4 == 3) { // for trailer block change conditions
	        System.arraycopy(DEFAULT_CONDITIONS, 0, frame, 6, 4);
	    }	    
	    authenticateBlock((byte) blockNumber, uid, DEFAULT_KEY_A, CMD_MIFARE_AUTH_A, authResult -> {
	        if (authResult) {
	            Platform.runLater(() -> textArea.appendText("Authentication OK for block " + blockNumber + "\n"));            
	            writeToBlock(frame, (byte) blockNumber, writeResult -> {
	                Platform.runLater(() -> textArea.appendText(writeResult));
	                // Wait a moment before processing next block
	                try {
	                    Thread.sleep(200);
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }
	                // Process next block
	                writeNextBlock(dumpData, blockNumber + 1);
	            });
	        } else {
	            Platform.runLater(() -> textArea.appendText("Authentication failed for block " + blockNumber + "\n"));
	            // Try next block anyway
	            writeNextBlock(dumpData, blockNumber + 1);
	        }
	    });
	}	
	
	@FXML
	private void formatTag() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(currentDir + "/nfc-bin64/sauvegardes"));
		File selectedFile = fileChooser.showOpenDialog(null);
		if (selectedFile != null) {
			SerialPortThreadFactory.submit(() -> {
				try {
					byte[] dumpData = Files.readAllBytes(selectedFile.toPath());
					Platform.runLater(() -> {
						FormatDialog dialog = new FormatDialog();
						Optional<mifareTools.FormatDialog.Results> optionalResult = dialog.showAndWait();
						optionalResult.ifPresent((mifareTools.FormatDialog.Results results) -> {
							if (results.authKeyA.isEmpty() || keyBMap == null) {
								textArea.appendText("Please enter KeyA in the Format dialog , and read the Tag before trying to format, then restart Format dialog\n");
								return;
							}
							byte[] authKeyA = Util.decodeHexString(results.authKeyA);
							SerialPortThreadFactory.submit(() -> {
								try {
									writeAllTrailers(dumpData, authKeyA,
											() -> {
												Platform.runLater(() -> textArea.appendText("Formatting trailer blocks ended\n"));
											});
								} catch (Exception e) {
									e.printStackTrace();
									Platform.runLater(
											() -> textArea.appendText("Error writing dump: " + e.getMessage() + "\n"));
								}
							});
						});
					});
				} catch (IOException e) {
					e.printStackTrace();
					Platform.runLater(() -> textArea.appendText("Error reading dump file: " + e.getMessage() + "\n"));
				}
			});
		}
					
	}
	
	private void writeAllTrailers(byte[] dumpData, byte[] authKeyA, Runnable onComplete) {
		 authAndWriteTrailer(0, dumpData, authKeyA, onComplete);
	}
	
	private void authAndWriteTrailer(int blockIndex, byte[] dumpData, byte[] authKeyA, Runnable onComplete) {
		// Only process sector trailer blocks (block % 4 == 3)
		if (blockIndex >= 64) {
			// All sectors processed, run completion callback
			if (onComplete != null) {
				onComplete.run();
			}
			return;
		}
		if (blockIndex % 4 == 3) {
			int j = blockIndex * 16;
			byte[] frame = Arrays.copyOfRange(dumpData, j, j + 16);
			byte[]keyB = keyBMap.get(blockIndex);
			// Authenticate with provided keys
			System.out.println(writeConditions.get(blockIndex));
			if (writeConditions.get(blockIndex).equals("")) {
				textArea.appendText("Write forbidden for block " + blockIndex + "\n");
				authAndWriteTrailer(blockIndex + 1, dumpData, authKeyA, onComplete);
			} else if (writeConditions.get(blockIndex).equals("KeyA")) {
				authenticateBlock((byte) blockIndex, uid, authKeyA, CMD_MIFARE_AUTH_A, authResult -> {
					if (authResult) {
						// Authentication with Key A successful
						formatTrailerBlock(blockIndex, () -> {
							// Continue with next block
							authAndWriteTrailer(blockIndex + 1, dumpData, authKeyA, onComplete);
						});
					} else {
						Platform.runLater(() -> textArea.appendText("Authentication failed for trailer block " + blockIndex + "\n"));
						// Skip this block and continue
						authAndWriteTrailer(blockIndex + 1, dumpData, authKeyA, onComplete);
					}
				});
			} else if (writeConditions.get(blockIndex).equals("KeyB")) {
				authenticateBlock((byte) blockIndex, uid, keyB, CMD_MIFARE_AUTH_B, authBResult -> {
					if (authBResult) {
						// Authentication with Key B successful
						formatTrailerBlock(blockIndex, () -> {
							// Continue with next block
							authAndWriteTrailer(blockIndex + 1, dumpData, authKeyA, onComplete);
						});
					} else {
						Platform.runLater(() -> textArea.appendText("Authentication failed for trailer block " + blockIndex + "\n"));
						// Skip this block and continue
						authAndWriteTrailer(blockIndex + 1, dumpData, authKeyA, onComplete);
					}
				});
			}
		} else {
			//System.out.println("Not a trailer block, skip to next block");
			authAndWriteTrailer(blockIndex + 1, dumpData, authKeyA, onComplete);
		}
	}
	
	private void formatTrailerBlock(int block, Runnable onComplete) {
	    writeToBlock(DEFAULT_TRAILER, (byte)block, result -> {
	        Platform.runLater(() -> textArea.appendText(result));
	        // Continue with next operation
	        if (onComplete != null) {
	            onComplete.run();
	        }
	    });
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
		
	/****************************************************************************/
	
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
