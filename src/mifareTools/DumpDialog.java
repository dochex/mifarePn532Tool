package mifareTools;

import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DumpDialog extends Dialog<mifareTools.DumpDialog.Results>{
	
	public TextField accessBitsField;
	public TextField authKeyAField;
	public TextField authKeyBField;
	
	public static class Results {
		String accessBits;
		String authKeyA;
		String authKeyB;

		public Results(String accessBits,  String authKeyA, String authKeyB) {
			this.accessBits = accessBits;
			this.authKeyA = authKeyA;
			this.authKeyB = authKeyB;
		}
	}
	
	public DumpDialog() {
		super();
		DialogPane dialogPane = this.getDialogPane();
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		setTitle("Clonage d'un Tag Mifare");
		setHeaderText("Explication : \n" +
				"Les clés A et B sont les clés d'authentification du tag \n"
				+ "sur lequel on va copier le dump.\n" +
				"Les bits d'accès sont les bits de configuration pour le clone.\n" 
				+ "L'écriture des trailers avec les accessBits est faite \n"
				+ "en première passe pour donner les droits d'accès \n"
				+ "puis les blocks sont écris en seconde passe");

		setResizable(true);
		VBox vbox = new VBox(10);
		Label writingKeys = new Label("Clés du Tag pour l'authentification avant l'écriture :");
		authKeyAField = new TextField("FFFFFFFFFFFF");
		authKeyAField.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,12}$") ? change : null));
		CustomHBox box1 = new CustomHBox("Key A : ", authKeyAField);
		authKeyBField = new TextField("FFFFFFFFFFFF");
		authKeyBField.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,12}$") ? change : null));
		CustomHBox box2 = new CustomHBox("Key B : ", authKeyBField);
		Label variables = new Label("Droits d'accès pour le clone");
		accessBitsField = new TextField("FF078000");
		accessBitsField.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,8}$") ? change : null));
		CustomHBox box4 = new CustomHBox("Access Bits : ", accessBitsField);
		vbox.getChildren().addAll(writingKeys, box1, box2, variables, box4);
		dialogPane.setContent(vbox);
        this.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(accessBitsField.getText(), authKeyAField.getText(), authKeyBField.getText());
            }
            return null;
        });
        
	}
	
	private class CustomHBox extends HBox{
		
		private final static double spacing = 20;
		private Label label;
		
		public CustomHBox(String textLabel, TextField textField) {
			super(spacing);
			this.setPrefSize(200, 20);
			label = new Label(textLabel);
			label.setPrefWidth(80);
			label.setAlignment(Pos.CENTER_RIGHT);
			this.getChildren().addAll(label, textField);
		}

		
	}
	
	
	


}
