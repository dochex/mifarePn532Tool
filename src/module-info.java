module pn532 {

	requires transitive com.fazecast.jSerialComm;
	requires transitive javafx.graphics;
	requires javafx.fxml;
	requires transitive javafx.controls;
	requires javafx.base;
	
	opens mifareTools to javafx.fxml;
	exports mifareTools;
	exports hexEditor;
}