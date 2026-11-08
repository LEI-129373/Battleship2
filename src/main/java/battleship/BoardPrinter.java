package battleship;

import java.util.List;

/**
 * Responsável por desenhar o tabuleiro do jogo na consola.
 * Extraída da classe {@link Game} (refabricação Extract Class) para reduzir
 * a complexidade da classe Game, que tinha sido detetada como God Class.
 */
public final class BoardPrinter
{
	private static final char EMPTY_MARKER = '.';
	private static final char SHIP_MARKER = '#';
	private static final char SHOT_SHIP_MARKER = '*';
	private static final char SHOT_WATER_MARKER = 'o';
	private static final char SHIP_ADJACENT_MARKER = '-';

	private BoardPrinter()
	{
	}

	/**
	 * Prints the game board by representing the positions of ships, adjacent tiles,
	 * shots, and other game elements onto the console. The method also optionally
	 * displays shot positions and a legend explaining the symbols used on the board.
	 *
	 * @param fleet       the fleet of ships to be displayed on the board.
	 * @param moves       the list of moves containing shots.
	 * @param show_shots  if true, displays the shots taken during the game.
	 * @param showLegend  if true, displays an explanatory legend of the symbols used.
	 */
	public static void printBoard(IFleet fleet, List<IMove> moves, boolean show_shots, boolean showLegend) {

		assert fleet != null;
		assert moves != null;

		char[][] map = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];

		for (int r = 0; r < Game.BOARD_SIZE; r++)
			for (int c = 0; c < Game.BOARD_SIZE; c++)
				map[r][c] = EMPTY_MARKER;

		printShips(fleet, map);

		if (show_shots)
			printBoardShots(moves, map);

		printBoardFrame(map);

		if (showLegend) {
			System.out.println("          LEGENDA");
			System.out.println("'" + SHIP_MARKER + "'->navio, '" + SHIP_ADJACENT_MARKER + "'->adjacente a navio, '" + EMPTY_MARKER + "'->água");
			System.out.println("'" + SHOT_SHIP_MARKER + "'->Tiro certeiro, '" + SHOT_WATER_MARKER + "'->Tiro na água");
		}
		System.out.println();
	}

	private static void printShips(IFleet fleet, char[][] map) {
		for (IShip ship : fleet.getShips()) {
			for (IPosition ship_pos : ship.getPositions())
				map[ship_pos.getRow()][ship_pos.getColumn()] = SHIP_MARKER;
			if (!ship.stillFloating())
				for (IPosition adjacent_pos : ship.getAdjacentPositions())
					map[adjacent_pos.getRow()][adjacent_pos.getColumn()] = SHIP_ADJACENT_MARKER;
		}
	}

	private static void printBoardShots(List<IMove> moves, char[][] map) {
		for (IMove move : moves)
			for (IPosition shot : move.getShots()) {
				if (shot.isInside()){
					int row = shot.getRow();
					int col = shot.getColumn();
					if (map[row][col] == SHIP_MARKER)
						map[row][col] = SHOT_SHIP_MARKER;
					if (map[row][col] == EMPTY_MARKER || map[row][col] == SHIP_ADJACENT_MARKER)
						map[row][col] = SHOT_WATER_MARKER;
				}
			}
	}

	private static void printBoardFrame(char[][] map) {
		System.out.println();
		System.out.print("    ");
		for (int col = 0; col < Game.BOARD_SIZE; col++) {
			System.out.print(" " + (col + 1));
		}
		System.out.println();

		System.out.print("   +-");
		for (int col = 0; col < Game.BOARD_SIZE; col++) {
			System.out.print("--");
		}
		System.out.println("+");

		for (int row = 0; row < Game.BOARD_SIZE; row++) {
			Position pos = new Position(row, 0);
			char rowLabel = pos.getClassicRow();
			System.out.print(" " + rowLabel + " |");
			for (int col = 0; col < Game.BOARD_SIZE; col++)
				System.out.print(" " + map[row][col]);
			System.out.println(" |");
		}

		System.out.print("   +");
		for (int col = 0; col < Game.BOARD_SIZE; col++)
			System.out.print("--");
		System.out.println("-+");
	}
}
