/**
 * @ref http://aidiary.hatenablog.com/entry/20040918/1251373370
 */

package othello;

import javax.swing.*;
import java.awt.*;

public class Othello extends JFrame {

	/**
	 * @brief コンストラクタ
	 */
	public Othello() {
		// タイトルを設定
		setTitle("決戦！ シロクロつけるぜ！!");
		// サイズを変更できなくする
		setResizable(false);
		// container取得
		Container contentPane = getContentPane();
		
		// 情報パネルを生成
		InfoPanel infoPanel = new InfoPanel();
		contentPane.add(infoPanel, BorderLayout.NORTH);
		// メインパネルを生成してフレームに追加
		MainPanel mainPanel = new MainPanel(infoPanel);
		contentPane.add(mainPanel, BorderLayout.CENTER);
		// パネルサイズに合わせてフレームサイズを自動設定
		pack();
	}
	
	public static void main(String[] args) {
		Othello frame = new Othello();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

}
