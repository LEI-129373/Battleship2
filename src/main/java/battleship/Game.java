package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

public class Game implements IGame
{
    private final ShotStatistics shotStatistics = new ShotStatistics();

    /**
	 * Prints the game board. The drawing logic lives in {@link BoardPrinter}.
	 *
	 * @param fleet       the fleet of ships to be displayed on the board.
	 * @param moves       the list of moves containing shots.
	 * @param show_shots  if true, displays the shots taken during the game.
	 * @param showLegend  if true, displays the legend of the symbols used.
	 */
	public static void printBoard(IFleet fleet, List<IMove> moves, boolean show_shots, boolean showLegend) {
		BoardPrinter.printBoard(fleet, moves, show_shots, showLegend);
	}

	/**
	 * Serializes a list of shot positions into a JSON string. Each shot is represented
	 * with its classic row and column values. The method uses the Jackson library for
	 * JSON serialization.
	 *
	 * @param shots a list of shot positions to be serialized. Each position is represented
	 *              by an implementation of the {@code IPosition} interface. The list must
	 *              not be null.
	 * @return a formatted JSON string containing the shot positions. Each shot includes
	 *         its classic row and column.
	 * @throws RuntimeException if an error occurs during JSON serialization.
	 */
	public static String jsonShots(List<IPosition> shots) {

		assert shots != null;

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		// 1. Create a simplified list containing only the desired data
		List<Map<String, Object>> simplifiedShots = new ArrayList<>();
		for (IPosition shot : shots) {
			Map<String, Object> simplePos = new LinkedHashMap<>();
			simplePos.put("row", String.valueOf(shot.getClassicRow()));
			simplePos.put("column", shot.getClassicColumn());
			simplifiedShots.add(simplePos);
		}

		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(simplifiedShots);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Erro ao serializar o JSON", e);
		}

		System.out.println(jsonString);
		System.out.println();

		return jsonString;
	}

	//------------------------------------------------------------------
	public static final int BOARD_SIZE = 10;
	public static final int NUMBER_SHOTS = 3;


	//------------------------------------------------------------------
	private final IFleet myFleet;
	private final List<IMove> alienMoves;

	private final IFleet alienFleet;
	private final List<IMove> myMoves;

    private int moveNumber;

	//------------------------------------------------------------------
	public Game(IFleet myFleet)
	{
		this.moveNumber = 1;

		this.alienMoves = new ArrayList<IMove>();
		this.myMoves = new ArrayList<IMove>();

		this.alienFleet = new Fleet();
		this.myFleet = myFleet;

        this.shotStatistics.countInvalidShots = 0;
        this.shotStatistics.countRepeatedShots = 0;
        this.shotStatistics.countHits = 0;
        this.shotStatistics.countSinks = 0;
	}

	@Override
	public IFleet getMyFleet()
	{
		return myFleet;
	}

	@Override
	public IFleet getAlienFleet()
	{
		return alienFleet;
	}

	@Override
	public List<IMove> getMyMoves()
	{
		return myMoves;
	}

	@Override
	public List<IMove> getAlienMoves()
	{
		return alienMoves;
	}

	/**
	 * Simulates a random enemy burst: picks up to {@code NUMBER_SHOTS} distinct positions that
	 * are still usable (not adjacent to a sunk ship and not shot before), fires them and
	 * returns them as JSON. If there are fewer candidates than {@code NUMBER_SHOTS},
	 * the last shot is repeated.
	 *
	 * @return a JSON string with the shots that were fired
	 * @throws RuntimeException if the JSON serialization fails
	 */
	public String randomEnemyFire() {

		Random random = new Random(System.currentTimeMillis());

		Set<IPosition> usablePositions = new HashSet<IPosition>();
		for (int r = 0; r < BOARD_SIZE; r++)
			for (int c = 0; c < BOARD_SIZE; c++)
				usablePositions.add(new Position(r, c));

		myFleet.removeUnavailablePositions(usablePositions, this);

		List<IPosition> candidateShots = new ArrayList<>(usablePositions);

		System.out.println();
		// Baralhar os candidatos e escolher os primeiros NUMBER_SHOTS (tiros únicos)
		Collections.shuffle(candidateShots, random);
		List<IPosition> shots = new ArrayList<>(candidateShots.subList(0, Math.min(NUMBER_SHOTS, candidateShots.size())));

		// Se não houver candidatos suficientes, repetir o último tiro
		IPosition lastShot = shots.isEmpty() ? null : shots.get(shots.size() - 1);
		while (shots.size() < NUMBER_SHOTS)
			shots.add(lastShot);

		System.out.print("rajada ");
		for (IPosition shot : shots)
			System.out.print(shot + " ");
		System.out.println();

		this.fireShots(shots);

		return Game.jsonShots(shots);
	}


	/**
	 * Reads the enemy fire input from the scanner, validates it and fires the shots.
	 * Each position may be a single token (e.g., "A3") or two tokens (e.g., "A" followed by "3").
	 *
	 * @param in the scanner to read the enemy fire positions from
	 * @return the JSON representation of the shots that were fired
	 * @throws IllegalArgumentException if the positions are incomplete, badly formatted,
	 *                                  or do not match {@code NUMBER_SHOTS}
	 */
	public String readEnemyFire(Scanner in) {

		assert in != null;

		String input = in.nextLine().trim();

		List<IPosition> shots = ShotParser.readShots(input);

		ShotParser.validateShotCount(shots);

		this.fireShots(shots);

		return Game.jsonShots(shots);
	}
	/**
	 * Fires a burst of enemy shots against my fleet, records the resulting move
	 * and advances the move counter.
	 *
	 * @param shots the positions to fire at; must contain exactly {@code NUMBER_SHOTS} positions
	 * @throws IllegalArgumentException if the list does not contain exactly {@code NUMBER_SHOTS} shots
	 */
	public void fireShots(List<IPosition> shots)
	{
		assert shots != null;

		List<ShotResult> shotResults = new ArrayList<ShotResult>();
		if (shots.size() != NUMBER_SHOTS) {
			throw new IllegalArgumentException("Must fire exactly " + NUMBER_SHOTS + " shots per move.");
		}

		List<IPosition> alreadyShot = new ArrayList<IPosition>();
		for (IPosition pos : shots) {
			shotResults.add(fireSingleShot(pos, alreadyShot.contains(pos)));
			alreadyShot.add(pos);
		}

		Move move = new Move(moveNumber, shots, shotResults);

		move.processEnemyFire(true);

		alienMoves.add(move);

		moveNumber++;
	}

	/**
	 * Fires a single shot and updates the shot statistics. Positions outside the board count
	 * as invalid shots and positions already shot count as repeated shots; otherwise the shot
	 * is a miss or a hit (possibly sinking a ship).
	 *
	 * @param pos        the position to fire at; positions outside the board are counted as invalid shots
	 * @param isRepeated true if the same position was already fired in the current burst
	 * @return the result of the shot
	 */
	public ShotResult fireSingleShot(IPosition pos, boolean isRepeated) {

		assert pos != null;

		if (!pos.isInside()) {
            shotStatistics.countInvalidShots++;
			return ShotResult.invalidShot();
		}

		if (isRepeated || repeatedShot(pos)) {
            shotStatistics.countRepeatedShots++;
			return ShotResult.repeatedShot();
		}

		IShip ship = myFleet.shipAt(pos);
		if (ship == null)
			return ShotResult.miss();
		else
		{
			ship.shoot(pos);
            shotStatistics.countHits++;
			if (!ship.stillFloating()) {
                shotStatistics.countSinks++;
			}
			return ShotResult.hit(ship);
		}
	}

	@Override
	public int getRepeatedShots()
	{
        return shotStatistics.getRepeatedShots();
    }

	@Override
	public int getInvalidShots()
	{
        return shotStatistics.getInvalidShots();
    }

	@Override
	public int getHits()
	{
        return shotStatistics.getHits();
    }

	@Override
	public int getSunkShips()
	{
        return shotStatistics.getSunkShips();
    }

	@Override
	public int getRemainingShips()
	{
		List<IShip> floatingShips = myFleet.getFloatingShips();
		return floatingShips.size();
	}

	public boolean repeatedShot(IPosition pos)
	{
		assert pos != null;

		return alienMoves.stream().anyMatch(move -> move.getShots().contains(pos));
	}

	public void printMyBoard(boolean show_shots, boolean show_legend)
	{
		Game.printBoard(this.myFleet, this.alienMoves, show_shots, show_legend);
	}

	public void printAlienBoard(boolean show_shots, boolean show_legend)
	{
		Game.printBoard(this.alienFleet, this.myMoves, show_shots, show_legend);
	}

	public void over() {
		System.out.println();
		System.out.println("+--------------------------------------------------------------+");
		System.out.println("| Maldito sejas, Java Sparrow, eu voltarei, glub glub glub ... |");
		System.out.println("+--------------------------------------------------------------+");
	}

	/**
	 * Constrói o mapa de caracteres de um tabuleiro para apresentação gráfica.
	 *
	 * @param jogo    o jogo em curso
	 * @param proprio {@code true} para o tabuleiro próprio, {@code false} para o do adversário
	 * @return a matriz com '.', '#', 'o' e 'X'
	 */
	static char[][] construir(IGame jogo, boolean proprio) {
		char[][] mapa = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];
		for (char[] linha : mapa) java.util.Arrays.fill(linha, '.');
		IFleet frota = proprio ? jogo.getMyFleet() : jogo.getAlienFleet();
		if (proprio) for (IShip navio : frota.getShips()) for (IPosition p : navio.getPositions())
			if (p.isInside()) mapa[p.getRow()][p.getColumn()] = '#';
		java.util.List<IMove> jogadas = proprio ? jogo.getAlienMoves() : jogo.getMyMoves();
		for (IMove jogada : jogadas) for (int i = 0; i < jogada.getShots().size(); i++) {
			IPosition p = jogada.getShots().get(i);
			if (!p.isInside()) continue;
			if (i >= jogada.getShotResults().size()) continue;
			IGame.ShotResult resultado = jogada.getShotResults().get(i);
			if (!resultado.valid() || resultado.repeated()) continue;
			mapa[p.getRow()][p.getColumn()] = resultado.ship() == null ? 'o' : 'X';
		}
		return mapa;
	}
}