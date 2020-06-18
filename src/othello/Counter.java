package othello;

/**
 * @brief 盤面に存在する石の数を数える
 */
public class Counter {
	private static final int COLORMAX = 2;
	public int[] count = new int[COLORMAX];
	public Counter() { for (int i = 0; i < COLORMAX; i++) count[i] = 0; }
}
