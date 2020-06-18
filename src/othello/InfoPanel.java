package othello;

import javax.swing.*;

public class InfoPanel extends JPanel {
	private JLabel black;
	private JLabel white;
	
	public InfoPanel() {
		add(new JLabel("BLACK:"));
		black = new JLabel("0");
		add(black);
		
		add(new JLabel("WHITE:"));
		white = new JLabel("0");
		add(white);
	}
	
	/**
	 * @brief BLACKラベルに値を設定する
	 * @param count 設定する値
	 */
	public void setBlackLabel(int count) {
		black.setText(count + "");
	}
	
	/**
	 * @brief WHITEラベルに値を設定する
	 * @param count
	 */
	public void setWhiteLabel(int count) {
		white.setText(count + "");
	}
}
