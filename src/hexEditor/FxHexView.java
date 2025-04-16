package hexEditor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class FxHexView extends TextArea {

	private final String delimiteur = " : ";
	private final String Ox = "0x";
	private final int lenCode = 48;
	private final int rightMargin = 19;
	private int posInLine;
	private int addrWidth;
	public int position;
	public boolean back;
	List<Integer> list = new ArrayList<Integer>();
	Map<Integer, Byte> map = new HashMap<Integer, Byte>();


	public FxHexView() {
		super();
		this.setPrefColumnCount(80);
		this.setPrefRowCount(40);
		this.setWrapText(true);
		this.getStyleClass().add("hex");
		this.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent t) {
				position = getPosition();
				selectHex(position);
				t.consume();
			}
		});
		this.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent ke) {
				KeyCode code = ke.getCode();
				switch (code) {
				case RIGHT:
					position = getPosition();
					break;
				case LEFT:
					position = getPosition() - 2;
					back = true;
					break;
				case UP:
					position = getPosition() - getLineLength() - 2;
					break;
				case DOWN:
					position = getPosition() + getLineLength();
					break;
				default:
					break;
				}
				selectHex(position);
				ke.consume();
			}
		});	
		this.setOnKeyReleased(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent ke) {
		    	KeyCode code = ke.getCode();
		    	System.out.println(ke.getCode());
		    	if (isHexLetter(code) || code.isKeypadKey()){
					position = getPosition();
					selectHex(position);
				}				
				ke.consume();
		    }
		});
		
	}
	
	public int getPosition() {
		return this.getCaretPosition();
	}
	
	
	private boolean isHexLetter(KeyCode code){
		if (code==KeyCode.A || code==KeyCode.B ||code==KeyCode.C ||code==KeyCode.D ||code==KeyCode.E ||code==KeyCode.F){
			return true;
		}else return false;
	}
	
	public void selectHex(int pos) {
		int margeInf = getLeftMargin();
		int line = position / (getLineLength()+1);
		int calcMarge = position - (line*(getLineLength()+1));
		posInLine = calcMarge - margeInf;
		if (posInLine<0 || posInLine > lenCode-2) this.setEditable(false);
		else {
			this.setEditable(true);
			if (posInLine % 3 == 0) {
				this.selectRange(pos, pos + 1);
			}
			if (posInLine % 3 == 1) {
				this.selectRange(pos, pos + 1);
			}
			if (posInLine % 3 == 2) {
				if (back){
					this.selectRange(pos-1, pos);
				}else{
					this.selectRange(pos + 1, pos + 2);
				}
			}
			back = false;
		}
	}
	
	public int selectCol(String colStr) {
		int col = Integer.decode(Ox + colStr);
		col = getLeftMargin() + (3 * col);
		return col;
	}
	
	public void goTo(String addr) {
		addr = Ox + addr;
		int pos = addr.length()-1;
		String lineEnd = addr.substring(pos);
		int col = selectCol(lineEnd);
		String lineStart = addr.substring(0, pos);
		int line = this.getText().indexOf(lineStart + "0");	
		this.selectRange(line + col, line + col + 2);
	}
	
	public int getLeftMargin() {
		return delimiteur.length() + Ox.length() + addrWidth;
	}

	public int getLineLength() {
		return getLeftMargin() + lenCode + rightMargin;
	}

	public int numberOfLine() {
		return this.getText().split("\n").length;
	}


	public int getAddrWidth() {
		return addrWidth;
	}


	public void setAddrWidth(int addrWidth) {
		this.addrWidth = addrWidth;
	}

}
