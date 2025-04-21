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

public class FormatDialog extends Dialog<mifareTools.FormatDialog.Results>{
	
	public TextField authKeyAField;
	
	public static class Results {
		String authKeyA;

		public Results(String authKeyA) {
			this.authKeyA = authKeyA;
		}
	}
	
	public FormatDialog() {
		super();
		DialogPane dialogPane = this.getDialogPane();
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		setTitle("Format a Mifare tag");
		setHeaderText("To format a Tag , we need to put access bits at the default conditions. \n"
				+ "For that we need to know KeyA and KeyB for each sectors.\n" 
				+ "Cancel this dialog and read the Tag. "
				+ "The keyB and the write access will be mapped during this reading.\n" 
				+ "The KeyA cannot never be read, only with mfoc.\n"
				+ "Restart this dialog and write KeyA here if you know it; "
				+ "for the KeyB the mapping will do the job. \n"
				+ "In a second time we need to write the Tag with the 'format.mfd'");

		setResizable(true);
		VBox vbox = new VBox(10);
		Label writingKeys = new Label("Clés du Tag pour l'authentification avant l'écriture :");
		authKeyAField = new TextField("");
		authKeyAField.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("^[0-9A-F]{0,12}$") ? change : null));
		CustomHBox box1 = new CustomHBox("Key A : ", authKeyAField);
		vbox.getChildren().addAll(writingKeys, box1);
		dialogPane.setContent(vbox);
        this.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(authKeyAField.getText());
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
