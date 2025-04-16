package hexEditor;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class HexEditor extends VBox {
	
	private static int addrWidth;
	private FxHexView area = new FxHexView();
	private Label title = new Label();
	private FlowPane paneTitle = new FlowPane();
	private final String delimiteur = " : ";
	private final String Ox = "0x";
	private List<Integer> list = new ArrayList<Integer>();
	private Map<Integer, Byte> map = new HashMap<Integer, Byte>();
	
	public HexEditor() {
		super();
		//Button buttonSave = new Button("Save");
        title.getStyleClass().add("hex");
        paneTitle.getStyleClass().add("paneTitle");
        paneTitle.getChildren().add(title);
        this.getChildren().addAll(paneTitle, this.getArea());
	}
	
	public void setData(File fichier) {
		// ------- calcule la longueur de la string de l'adresse en hexa
        long fich = fichier.length();
		addrWidth = Long.toHexString(fich).length();
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	title.setText(getTitleText(getLeftMargin()));
				area.setAddrWidth(addrWidth);
				area.appendText(getData(fichier));
		    }
		});

		
	}
	
	private String getData(File fichier) {
		StringBuilder builder = new StringBuilder();
		try {
			int addr = 0;
			// ------------------------------------------------------------
			FileInputStream fis = new FileInputStream(fichier);
			while (fis.available() > 0) {
				char[] line = new char[16];
				// ------ formatte l'adresse hexa et la marge gauche
				builder.append(Ox);
				builder.append(String.format("%0" + (addrWidth - 1) + "X" + "0", addr));
				builder.append(delimiteur);
				// -----------------------------
				for (int i = 0; i < 16; i++) {
					int readByte = fis.read();
					String zeroPadding = (readByte < 16) ? "0" : "";
					if (readByte < 0) { // les derniers octets � afficher qui n'appartiennent pas au fichier
						builder.append("   ");
					} else {
						list.add(builder.length()); // stocke les adresses des hexa
						map.put(builder.length(), (byte) readByte);
						builder.append(zeroPadding + Integer.toHexString(readByte).toUpperCase() + " ");
					}
					if (readByte < 0) {
						line[i] = ' ';
					} else {
						line[i] = (readByte >= 33 && readByte <= 126) ? (char) readByte : '.';
					}
					//char ch = line[i];
				}
				builder.append(delimiteur + new String(line) + "\n");
				addr++;
			}
			fis.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return builder.toString();	
	}
	
	public int getLeftMargin() {
		return delimiteur.length() + Ox.length() + addrWidth;
	}


	private String getTitleText(int leftMargin) {
		StringBuilder builder = new StringBuilder();	
		for (int i = 0; i <= leftMargin; i++) {
			builder.append(" ");
		}
		for (int i = 0; i < 16; i++) {
			builder.append("0" + Integer.toHexString(i).toUpperCase() + " ");
		}
		return builder.toString();
	}


	public FxHexView getArea() {
		return area;
	}


	public void setArea(FxHexView area) {
		this.area = area;
	}
	
	public static int getAddrWidth() {
		return addrWidth;
	}
	
	public String getContent(){
		final int hexaWidth = 16 * 3;
		final int start = addrWidth + 4;
		StringBuilder saved = new StringBuilder();
		String text[] = area.getText().split("\n");
		int end = start + hexaWidth ;
		for (int i = 0; i < text.length; i++) {
			text[i] = text[i].substring(start + 1, end);
			saved.append(text[i]);
		}
		String hextext = saved.toString().replaceAll(" ", "");
		return hextext;		
	}
	
	
    
}