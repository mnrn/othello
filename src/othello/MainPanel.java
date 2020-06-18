package othello;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.applet.*;


public class MainPanel extends JPanel implements MouseListener {
	// マスのサイズ
	private static final int GS = 32;
	// マスの数
	public static final int GRID = 8;
	// ボードの大きさ
	private static final int WIDTH = GS * GRID;
	private static final int HEIGHT = WIDTH;
	// 空白は0, 黒は1, 白は-1とする
	public static final int BLANK = 0;
	public static final int BLACK = 1;
	public static final int WHITE = -1;
	// 黒のインデックスは0、白のインデックスは1
	public static final int BLACKIDX = 0;
	public static final int WHITEIDX = 1;
	
	// 描画スレッドを休ませる時間(miliseconds)
	private static final int SLEEPTIME = 500;
	// 終了までの手数(8x8-4の60手で終了)
	private static final int FINISH = 60;
	// ボード
	private int[][] board = new int[GRID][GRID];
	// 白の番か黒の番か
	private int color;
	// 石を打つ音
	private AudioClip putSE;
	// 今までの手数
	private int moves;
	// ゲームの状態
	private Scene scene;
	// 情報パネルへの参照
	private InfoPanel infoPanel;
	// パスが続いた回数
	private int passcnt;
	// AI
	private AI ai;
	
	// ゲームの状態を列挙する
	private enum Scene {
		START, PLAY, YOUWIN, YOULOSE, DRAW,;
	}
	
	
	/**
	 *  @brief コンストラクタ
	 */
	public MainPanel(InfoPanel _infoPanel) {
		// Othelloでpack()するときに必要になる
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		infoPanel = _infoPanel;
		
		// 盤面の初期化
		initBoard();
		// サウンドのロード
		putSE = Applet.newAudioClip(getClass().getResource("putSE.wav"));
		// マウス操作を受け付けるようにする
		addMouseListener(this);
		// AIを生成
		ai = new AI(this);
		//開始状態
		scene = Scene.START;
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		// 盤面を描く
		drawBoard(g);
		switch (scene) {
		case START:
			drawTextCentering(g, "オセロやで");
			break;
		case PLAY:
			drawStone(g);  // 石を描く
			Counter counter = countStone();
			// ラベルに表示
			infoPanel.setBlackLabel(counter.count[BLACKIDX]);
			infoPanel.setWhiteLabel(counter.count[WHITEIDX]);
			break;
		case YOUWIN:
			drawTextCentering(g, "YOU WIN!!");
			break;
		case YOULOSE:
			drawTextCentering(g, "YOU LOSE...");
			break;
		case DRAW:
			drawTextCentering(g, "DRAW!");
			break;
		}
	}
	
	/**
	 * @brief マウスをクリックしたとき石を打つ
	 */
	public void mouseClicked(MouseEvent e) {
		switch(scene) {
		case START:
			scene = Scene.PLAY;  // スタート画面でクリックされたらゲーム開始
			break;
		case PLAY:
			int x = e.getX() / GS, y = e.getY() / GS;
			// パスかどうか確認
			if (!passProc()) {
				// (x, y)に石が打てる場合にのみ打つ
				if (!canPutDown(x, y)) { return; }
				// 戻せるように記憶する
				OthelloState othello = new OthelloState(x, y);
				// その場所に石を打つ
				putDownStone(x, y, false);
				// ひっくり返す
				reverse(othello, false);
			}
			// 手番を変える
			nextTurn();
			// ゲームが終了したかどうか調べる
			endGame();
			 // AIの番になる前に再度パスの確認を行う
			 if (!passProc()) {
				// パスでなければAIが石を打つ
				ai.compute();
			 }
			 // 手番を変える
			 nextTurn();
			 // ゲームが終了したか調べる
			 endGame();
			break;
		case YOUWIN: case YOULOSE: case DRAW:
			scene = Scene.START;  // ゲーム終了画面でクリックされたらゲーム開始
			initBoard(); break;       // 盤面の初期化
		}
		// 再描画を行う
		repaint();
	}
	
	/**
	 *  @brief 盤面の初期化を行う
	 */
	private void initBoard() {
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				board[y][x] = BLANK;
			}
		} 
		board[3][3] = board[4][4] = WHITE;
		board[3][4] = board[4][3] = BLACK;
		color = BLACK;   // 黒番から始まるよ
		moves = 0;       // 現在4つ石が置かれているがそれらは数えないこととする
		passcnt = 0;     // パスが続いた回数を初期化
	}
	
	
	/**
	 * @brief  盤面の描画を行う
	 * @param  Graphics g 描画オブジェクト
	 */
	private void drawBoard(Graphics g) {
		// マスを塗りつぶす
		g.setColor(new Color(0, 255, 0));
		g.fillRect(0, 0, WIDTH, HEIGHT);	
		// マス枠を描画
		g.setColor(Color.black);
		// 線の描画
		for (int i = 1; i < GRID; i++) {
			g.drawLine(i * GS, 1, i * GS, HEIGHT);  // 横線
			g.drawLine(0,  i * GS, WIDTH, i * GS);  // 縦線
		}
		g.drawRect(0, 0, WIDTH, HEIGHT);  // 外枠
	}
	
	/**
	 * @brief 石描きます
	 * @param g 描画オブジェクト　
	 */
	private void drawStone(Graphics g) {
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				if (board[y][x] == BLANK) { continue; }	 // 石が置かれていなければ飛ばす			
				g.setColor(board[y][x] == BLACK ? Color.BLACK : Color.WHITE);
				g.fillOval(x * GS + 3, y * GS + 3, GS - 6, GS - 6);  // 石描きます
			}
		}
	}
	
	/**
	 * 盤面に石を打つ
	 * @param x 石を打つ場所のx座標
	 * @param y 石を打つ場所のy座標
	 * @param thinking コンピュータの思考実験中か否か
	 */
	public void putDownStone(int x, int y, boolean thinking) {
		board[y][x] = color;    // 手番が白ならば白、違うならば黒を打つ
		if (!thinking) {        // 思考中ではなく実際に石を打つ場合
			moves = moves + 1;      // 置いてある石を1つ増やす
			putSE.play();           // SE再生
			update(getGraphics());  // ボードの更新を反映
			sleep();                // 小休止を入れる(入れないとすぐにひっくり返しが始まる)
		}
	}
	
	/**
	 * @brief 石が打てるかどうか調べます
	 * @param x 石を打ちたい場所のx座標
	 * @param y 石を打ちたい場所のy座標
	 * @return 石が打てるならばtrue, 打てないならば、falseを返す
	 */
	public boolean canPutDown(int x, int y) {
		// (x, y)がボードの外だったら打てない
		if (x >= GRID || y >= GRID) { return false; }
		// (x, y)にすでに石があったら打てない
		if (board[y][x] != BLANK) { return false; }
		// 8方向のうち一箇所でもひっくり返せればよい
		for (int vy = -1; vy <= 1; vy++) {
			for (int vx = -1; vx <= 1; vx++) {
				if (canPutDown(x, y, vx, vy)) { return true; }
			}
		}
		return false;  // ここにきたということはあ、打てないってことだよ^^！
	}
	/**
	 * @brief (vx, vy)の方向に引っ繰り返せる石があるか返す
	 * @param x   石を打とうとしている場所のx座標
	 * @param y   石を打とうとしている場所のy座標
	 * @param vx  調べる方向を示す方向ベクトルのx成分
	 * @param vy  調べる方向を示す方向ベクトルのy成分
	 * @return 石が打てるならばtrue, 打てないならば、falseを返します
	 */
	private boolean canPutDown(int x, int y, int vx, int vy) {
		// 位置ベクトルの更新をまず1回行う
		x += vx; y += vy;
		// ボードの外だったら打てません
		if (x < 0 || x >= GRID || y < 0 ||y >= GRID) { return false; }
		// 隣が自分と同じ色の石の場合は打てません
		if (board[y][x] == color) { return false; }
		// 隣に石がなくては打てません
		if (board[y][x] == BLANK) { return false; }
		// 再び、位置ベクトルの更新を行う
		x += vx; y += vy;
		// 隣に石が存在するまでループを回る
		while (0 <= x && x < GRID && 0 <= y && y < GRID) {
			// 石が存在しない場合、打てません
			if (board[y][x] == BLANK) { return false; }
			// 自分と同じ色の石が存在する場合、打てます
			if (board[y][x] == color) { return true; }
			// 位置ベクトルの更新
			x += vx; y += vy;
		}
		return false;  // 相手の石しかない場合、ボード外に出るのでfalse
	}
	
	/**
	 * @brief 石をひっくり返します
	 * @param ひっくり返す石の情報
	 * @param thinking コンピュータ験実験中か否か
	 */
	public void reverse(OthelloState othello, boolean thinking) {
		for (int vy = -1; vy <= 1; vy++) {
			for (int vx = -1; vx <= 1; vx++) {
				if (canPutDown(othello.x, othello.y, vx, vy)) { reverse(othello, vx, vy, thinking); }
			}
		}
	}
	
	/**
	 * @brief 石をひっくり返します
	 * @param othello  ひっくり返す石の情報
	 * @param vx       ひっくり返す方向を示す方向ベクトルのx成分
	 * @param vy       ひっくり返す方向を示す方向ベクトルのy成分
	 * @param thinking コンピュータ思考実験中か否か
	 */
	private void reverse(OthelloState othello, int vx, int vy, boolean thinking) {
		int x = othello.x, y = othello.y;
		// 相手の石が存在する間ひっくり返し続ける
		// NOTE : (x, y)に打てるのは確認済みなので相手の石は必ず存在する
		x += vx; y += vy;
		while (board[y][x] != color) {
			board[y][x] = color;                       // ひっくり返す
			othello.pos[othello.count++] = new Point(x, y);  // ひっくり返した場所を記憶する
			if (!thinking) {   // 思考実験中でない場合、
				putSE.play();          // SE再生
				update(getGraphics()); // 盤面が更新されたので再描画
				sleep();               // 小休止を入れる(入れない場合、一斉に石がひっくり返る)
			}
			x += vx; y += vy;  // 位置ベクトルの更新
		}
	}
	
	/**
	 * @brief 盤面を元に戻す
	 * @param othello 戻す石の情報
	 */
	public void undoBoard(OthelloState othello) {
		int i = 0;
		while (othello.pos[i] != null) {
			// ひっくり返した位置を取得
			int x = othello.pos[i].x, y = othello.pos[i].y;
			// 元に戻すには白(-1)を黒(1), 黒(1)を白(-1)にすればよいから、
			board[y][x] = ~board[y][x] + 1;  // 符号を反転させる
			i++;  // イテレータを1つ進める
		}
		// 石を打つ前に戻す
		board[othello.y][othello.x] = BLANK;
		// 手番も元に戻す
		nextTurn();
	}
	
	/**
	 * @brief SLEEPTIMEだけ休みます
	 */
	private void sleep() {
		try { Thread.sleep(SLEEPTIME); }
		catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	/**
	 * @brief 文字列を中央揃えで描画する
	 * @param g 描画オブジェクト
	 * @param s 描画したい文字列
	 */
	private void drawTextCentering(Graphics g, String s) {
		Font f = new Font("SansSerif", Font.BOLD, 20);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(Color.BLUE);
		g.drawString(s, (WIDTH - fm.stringWidth(s)) / 2,
				HEIGHT / 2 + fm.getDescent());
	}
	
	/**
	 * @brief ゲームが終了していた場合、適切な処理を行う
	 */
	private boolean endGame() {
		if (passcnt < 2 && moves < FINISH) { return false; }  // パスが2回続くか石が置けない状況でないと実行されない
		Counter counter = countStone();    // 黒白両方の石を数える
		// 適切なゲーム状態に遷移する
		if (counter.count[BLACKIDX] == counter.count[WHITEIDX])           { scene = Scene.DRAW;    }
		else if (counter.count[BLACKIDX] > counter.count[WHITEIDX])       { scene = Scene.YOUWIN;  }
		else /* if (counter.count[BLACKIDX] < counter.count[WHITEIDX]) */ { scene = Scene.YOULOSE; }
		return true;
	}
	
	/**
	 * @brief  盤上の石の数を数える
	 * @return 石を数えたCounterオブジェクト
	 */
	public Counter countStone() {
		Counter counter = new Counter();
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				if (board[y][x] == BLACK) { counter.count[BLACKIDX]++; }
				if (board[y][x] == WHITE) { counter.count[WHITEIDX]++; }
			}
		}
		return counter;
	}
	
	/**
	 * @brief  現在、石が置ける状況か確認する
	 * @return 置くことが可能なマスの数
	 */
	private int checkPosition() {
		int count = 0;
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				if (canPutDown(x, y)) { count++; }
			}
		}
		return count;
	}
	
	/**
	 * @brief  一手を打つことができるか否か返す
	 * @return 一手を打てるならばtrue、打てないならばfalse
	 */
	public boolean hasValidMoves()
	{
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				if (canPutDown(x, y)) { return true; }
			}
		}
		return false;
	}
	
	/**
	 * @brief 手番を変えます
	 */
	public void nextTurn()
	{
		color = ~color + 1;
	}
	
	/**
	 * @brief パスの処理を行う
	 * @return パスが2度続き、ゲームが終了するかどうか返す
	 */
	private boolean passProc()
	{
		if (hasValidMoves()) {  // 置ける場所が存在する場合
			passcnt = 0;  // パスが続かなかったのでカウントをゼロに
			return false;
		}
		// 置ける場所がない場合
		passcnt++; // パスカウントを1つ増やす
		return true;
	}
	
	/**
	 * @brief 盤面座標系における(x, y)の値を返す
	 * @param x
	 * @param y
	 * @return 0ならBLANK, -1ならWHITE, 1ならBLACK
	 */
	public int getBoard(int x, int y) {
		return board[y][x];
	}
	
	public void mousePressed(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}

}
