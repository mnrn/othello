package othello;

import java.awt.Point;

/**
 * @brief 盤面を一手戻すための情報をまとめる
 */
public class OthelloState {
	public int x, y;     // 石を打つ場所の座標(x, y)
	public int count;    // ひっくり返った石の数
	public Point[] pos;  // ひっくり返った石の場所
	
	public OthelloState(int x, int y) {
		this.x = x; this.y = y;
		count = 0;
		pos = new Point[MainPanel.GRID * MainPanel.GRID];
	}
}