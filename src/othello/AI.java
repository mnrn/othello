package othello;

public class AI {
	private static final int MAXDEPTH = 7;    // 深読みするレベル
	private MainPanel mainPanel;              // メインパネルへの参照
	private static final int score2D[][] = {  // 盤面における各マス目評価
		{ 100, -40,  20,   5,   5,  20, -40, 100, },
		{ -40, -80,  -1,  -1,  -1,  -1, -80, -40, },
		{  20,  -1,   5,   1,   1,   5,  -1,  20, },
		{   5,  -1,   1,   0,   0,   1,  -1,  5,  },
		{   5,  -1,   1,   0,   0,   1,  -1,  5,  },
		{  20,  -1,   5,   1,   1,   5,  -1,  20, },
		{ -40, -80,  -1,  -1,  -1,  -1, -80, -40, },
		{ 100, -40,  20,   5,   5,  20, -40, 100, },
	};
	
	/**
	 * @brief コンストラクタ
	 * @param panel メインパネルへの参照
	 */
	public AI(MainPanel panel) {
		mainPanel = panel;
	}
	
	public void compute() {
		int best = alphaBeta(true, MAXDEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE);
		// プレイヤーにとって最良のゲームになるような手を取得する
		int bestX = best % MainPanel.GRID;
		int bestY = best / MainPanel.GRID;
		
		// 打った場所、ひっくり返した石の位置を記録
		OthelloState othello = new OthelloState(bestX, bestY);
		// その場所に実際に石を打つ
		mainPanel.putDownStone(bestX, bestY, false);
		// 実際にひっくり返す
		mainPanel.reverse(othello, false);
	}
	
	/**
	 * @brief  ミニマックスアルゴリズム
	 * 
	 * @note   ゲーム木のサイズは、各ゲーム状態での可能な手の数で決まる. 最初のゲーム状態で可能な手がb個あり、
	 *         ある手を打つと、相手がそこに打てなくなると仮定する. 読みの深さがdならば、調べられるゲーム状態の全個数は、
	 *           Σ[d, i = 1](b! / (b - i)!)
	 *         である. ここで、b!はbの階乗を表す. 規模の例を挙げると、b = 10かつd = 6で、評価されなければならない
	 *         ゲーム状態の総数は187,300となる
	 * 
	 * @note   このアルゴリズムは、すぐに、再帰探索中のゲーム状態の爆発に困ることになる. チェスでは、盤面での平均的な手の個数が
	 *         30と考えられているので、5手先を読むだけなのに(すなわち、b = 30, d = 5) 25,137,931個もの盤面位置を評価する
	 *         必要がある. この値は、次の式で決まる
	 *           Σ[d, i = 0] b^i
	 *         ミニマックスは、過去に調べた状態(及びその得点)を一時的に蓄えておき、ゲームの対称性(盤面の回転や反転など)を
	 *         利用することで計算を節約できるが、どれだけ節約できるかは、ゲームによって異なる
	 *         
	 * @note   ゲーム木の深さは固定であり、読みの深さ分の手順で生成できるゲーム状態はすべて生成される
	 *         各ゲーム状態で固定個数bの手があるなら(可能な手の個数が各層で1ずつ減るにしても)、読みの深さがdのミニマックスで
	 *         探索されるゲーム状態の総数は、指数的に増大するΟ(b^d)となる
	 * 
	 * @param  originalplayer AI自身か否か(trueならAI, falseなら相手)
	 * @param  plyDepth 読みの深さ
	 * @return 有効な手の中から、評価関数によって決定される、プレイヤーにとって最良のゲームになるような手
	 */
	private int miniMax(boolean originalPlayer, int plyDepth) {
		int bestX = 0, bestY = 0;  // 最良の手となる座標(x, y)
		int bestScore = 0;         // ゲーム状態の評価点
		
		if (plyDepth == 0 || !mainPanel.hasValidMoves()) {  // 再帰が底をついた場合、または一手も打てない場合
			return evaluation();   // 評価関数から得た評価値を返す(葉節点はAIの位置から評価する)
		}
		// AIの手番ならば最大の評価値を見つけたいので最初に最小値を、違うならば、最小の評価値を見つけたいので最大値を代入する
		bestScore = originalPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		// このゲーム状態での可能な手をすべて試す
		for (int y = 0; y < MainPanel.GRID; y++) {
			for (int x = 0; x < MainPanel.GRID; x++) {
				if (!mainPanel.canPutDown(x, y)) { continue; }  // おける場所でないならばcontinue
				OthelloState state = new OthelloState(x, y);
				// 実際に打ってみる
				mainPanel.putDownStone(x, y, true);
				// ひっくり返す
				mainPanel.reverse(state,  true);
				// 手番を変える
				mainPanel.nextTurn();
				// 再帰的に位置を評価する. miniMaxを計算してMINとMAX、およびプレイヤーを同時に入れ替える
				int score = miniMax(!originalPlayer, plyDepth - 1);
				// 盤面を元に戻す
				mainPanel.undoBoard(state);
				if (originalPlayer) {  // AIの手番ならば、最大再帰評価を選ぶ
					if (score > bestScore) { bestScore = score; bestX = x; bestY = y; }
				}
				else {                 // 相手の手番ならば、最小再帰評価を選ぶ
					if (score < bestScore) { bestScore = score; bestX = x; bestY = y; }
				}
			}
		}
		// 探索元であるゲーム部分木の根ならば、最大評価を持つ場所を返す
		if (plyDepth == MAXDEPTH) { return bestX + bestY * MainPanel.GRID; }
		// そうでない場合は、最大(最小)評価値を返す
		else { return bestScore; }
	}
	
	/**
	 * @brief アルファベータ法
	 * 
	 * @note  アルファベータ法は、性能的に大幅な節約をして、ミニマックスが選ぶであろう手を選ぶ
	 *        また、プレイヤーが間違いを犯さないものと仮定し、この仮定の下で、ゲームに影響を与えないゲーム木の部分を探索する手間を省いてしまう
	 * 
	 * @note  アルファベータ法は、ゲーム木を再帰的に探索し、αとβという2つの値を維持する. これらは、α < βである限り、「勝利の機会がある」ことを意味する
	 *        
	 *        値αは、これまでにプレイヤー(今回はAI)のために見つけたゲーム状態の下限を表し(1つも見つかっていないと-∞)、
	 *        プレイヤーが少なくともαだけの点数を得られる手を見つけたことを示す. αの値が高い場合は、プレイヤー(AI)が優位に立っていることを意味する
	 *        α = +∞ならば、プレイヤーが勝つ手を見つけて、探索が終了して良いことを意味する
	 *        
	 *        値βは、これまでのゲーム状態の上限を表し(1つも見つかっていないと+∞)、プレイヤーが達成できる最大の盤面を表す
	 *        βが低くなるということは、相手が優位で、プレイヤーの選択が制限を受けていることを意味する
	 *        
	 * @note  アルファベータ法には、最大の読みの深さがあり、それ以上は探索しないので、あらゆる決定は、この範囲に制限される
	 * 
	 * @note  各ゲーム状態で固定したb個の可能な手があるとすると、読みの深さdのアルファベータ法で探索される総ゲーム状態数は、Ο(b^d)となる
	 *        相手の手が、望ましさの順に並べられている(すなわち、自分にとって都合の良い手が先)なら、プレイヤーのために(最善手を選ぶには)全b個の評価をしなければならない
	 *        しかし、相手側の最初の手だけ評価すれば良い場合もある
	 *        
	 * @note  最良の場合、アルファベータ法は、各段階で最初のプレイヤーのためにb個の状態は評価するが、相手側のゲーム状態は1つしか評価しない
	 *        つまり、ゲーム木のd番目の深さで、ゲーム状態をb*b*b*...*b*b(全部でd回)展開する代わりに、アルファベータ法は、
	 *        b*1*b*...*b*1(全部でd回)の展開で済むかもしれない. 結果として、ゲーム状態の個数はb^(d/2)となり、効果的な削減が行われる
	 * 
	 * @param originalPlayer AI自身か否か(trueならAI, falseなら相手)
	 * @param plyDepth 読みの深さ
	 * @param alpha  これまでに見つけたゲーム状態の下限α
	 * @param beta   これまでに見つけたゲーム状態の上限β
	 * @return 有効な手の中から、評価関数によって決定される、プレイヤーにとって最良のゲームになるような手
	 */
	private int alphaBeta(boolean originalPlayer, int plyDepth, int alpha, int beta)
	{
		int bestX = 0, bestY = 0;  // 最良の手となる座標(x, y)
		int bestScore = 0;         // ゲーム状態の評価点
		
		if (plyDepth == 0 || !mainPanel.hasValidMoves()) {  // 再帰が底をついた場合、または一手も打てない場合
			return evaluation();   // 評価関数から得た評価値を返す(葉節点はAIの位置から評価する)
		}
		// AIの手番ならば最大の評価値を見つけたいので最初に最小値を、違うならば、最小の評価値を見つけたいので最大値を代入する
		bestScore = originalPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		// このゲーム状態での可能な手をすべて試す
		for (int y = 0; y < MainPanel.GRID; y++) {
			for (int x = 0; x < MainPanel.GRID; x++) {
				if (!mainPanel.canPutDown(x, y)) { continue; } // おける場所でないならばcontinue
				OthelloState state = new OthelloState(x, y);
				// 実際に打ってみる
				mainPanel.putDownStone(x, y, true);
				// ひっくり返す
				mainPanel.reverse(state, true);
				// 手番を変える
				mainPanel.nextTurn();
				// 再帰的に位置を評価する. alphaBetaを計算してMINとMAX、およびプレイヤーを同時に入れ替える
				int score = alphaBeta(!originalPlayer, plyDepth - 1, alpha, beta);
				// 盤面を元に戻す
				mainPanel.undoBoard(state);
				if (originalPlayer) { // AIの手番ならば、
					if (score > bestScore) { bestScore = score; bestX = x; bestY = y; alpha = score; }  // 最大再帰評価を選ぶ
					if (score >= beta)     { return beta;  }  // βカット
				}
				else {                // 相手の手番ならば、
					if (score < bestScore) { bestScore = score; bestX = x; bestY = y; beta = score;  }  // 最小再帰評価を選ぶ
					if (score <= alpha)    { return alpha; }  // αカット
				}
			}
		}
		// 探索元であるゲーム部分木の根ならば、最大評価を持つ場所を返す
		if (plyDepth == MAXDEPTH) { return bestX + bestY * MainPanel.GRID; }
		// そうでない場合は、最大(最小)評価値を返す
		else { return bestScore; }
	}
	
	/**
	 * @brief  オセロにおける評価関数
	 * @return ゲームの状態の評価を返す
	 */
	private int evaluation() {
		int score = 0;
		for (int y = 0; y < MainPanel.GRID; y++) {
			for (int x = 0; x < MainPanel.GRID; x++) {
				score += mainPanel.getBoard(x, y) * score2D[y][x];  // 盤面評価を施してゆく
			}
		}
		return (~score + 1);  // 現在AIは白石なので符号を反転させることで適切な評価値に変換する
	}
}
