package battleship;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Game.
 * Author: britoeabreu
 * Date: 2024-03-19
 * Time: 15:30
 * Cyclomatic Complexity for each method:
 * - Game (constructor): 1
 * - fire: 7
 * - getShots: 1
 * - getRepeatedShots: 1
 * - getInvalidShots: 1
 * - getHits: 1
 * - getSunkShips: 1
 * - getRemainingShips: 1
 * - validShot: 3
 * - repeatedShot: 2
 * - printBoard: 1
 * - printValidShots: 1
 * - printFleet: 1
 */
public class GameTest {

	private Game game;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet()); // Assuming Fleet is a concrete implementation of IFleet
	}

	@AfterEach
	void tearDown() {
		game = null;
	}

	@Test
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}


	@Test
	@DisplayName("Deve identificar um disparo válido na água")
	void testDisparoNaAgua() {

		// Criar posição válida num tabuleiro sem navios
		Position posicao = new Position(3, 3);

		// Executar o disparo
		IGame.ShotResult resultado =
				game.fireSingleShot(posicao, false);

		// Verificar o resultado
		assertTrue(resultado.valid());
		assertFalse(resultado.repeated());
		assertNull(resultado.ship());
		assertFalse(resultado.sunk());

		// Verificar os contadores
		assertEquals(0, game.getHits());
		assertEquals(0, game.getSunkShips());
		assertEquals(0, game.getInvalidShots());
		assertEquals(0, game.getRepeatedShots());
	}


	@Test
	@DisplayName("Deve acertar num navio sem o afundar")
	void testAcertarNavio() {

		Frigate fragata = new Frigate(
				Compass.EAST, new Position(2, 2)
		);

		game.getMyFleet().addShip(fragata);

		IGame.ShotResult resultado =
				game.fireSingleShot(new Position(2, 2), false);

		assertTrue(resultado.valid());
		assertFalse(resultado.repeated());
		assertNotNull(resultado.ship());
		assertFalse(resultado.sunk());

		assertEquals(1, game.getHits());
		assertEquals(0, game.getSunkShips());
	}


	@Test
	@DisplayName("Deve afundar um navio e atualizar os contadores")
	void testAfundarNavio() {

		Barge barco = new Barge(
				Compass.NORTH, new Position(4, 4)
		);

		game.getMyFleet().addShip(barco);

		IGame.ShotResult resultado =
				game.fireSingleShot(new Position(4, 4), false);

		assertTrue(resultado.valid());
		assertNotNull(resultado.ship());
		assertTrue(resultado.sunk());

		assertEquals(1, game.getHits());
		assertEquals(1, game.getSunkShips());
		assertEquals(0, game.getRemainingShips());
	}


	@Test
	@DisplayName("Deve rejeitar jogadas com quantidade incorreta de disparos")
	void testQuantidadeInvalidaDisparos() {

		List<IPosition> poucosDisparos = List.of(
				new Position(1, 1),
				new Position(2, 2)
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> game.fireShots(poucosDisparos)
		);

		assertTrue(game.getAlienMoves().isEmpty());
	}


	@Test
	@DisplayName("Deve identificar posições repetidas na mesma jogada")
	void testDisparosRepetidosNaMesmaJogada() {

		List<IPosition> posicoes = List.of(
				new Position(1, 1),
				new Position(1, 1),
				new Position(2, 2)
		);

		game.fireShots(posicoes);

		assertEquals(1, game.getRepeatedShots());
		assertEquals(1, game.getAlienMoves().size());
	}

	@Test
	@DisplayName("Deve ler três posições com coluna e linha separadas")
	void testReadEnemyFirePosicoesSeparadas() {

		Scanner scanner = new Scanner("A 3 B 4 C 5");

		String resultado = game.readEnemyFire(scanner);

		assertNotNull(resultado);
		assertTrue(resultado.contains("row"));
		assertTrue(resultado.contains("column"));

		assertEquals(1, game.getAlienMoves().size());
		assertEquals(3, game.getAlienMoves().get(0).getShots().size());
	}

	@Test
	@DisplayName("Deve rejeitar uma coluna sem número")
	void testReadEnemyFirePosicaoIncompleta() {

		Scanner scanner = new Scanner("A B4 C5");

		IllegalArgumentException erro = assertThrows(
				IllegalArgumentException.class,
				() -> game.readEnemyFire(scanner)
		);

		assertTrue(erro.getMessage().contains("Posição incompleta"));
	}


	@Test
	@DisplayName("Deve rejeitar menos de três posições")
	void testReadEnemyFirePoucasPosicoes() {

		Scanner scanner = new Scanner("A3 B4");

		IllegalArgumentException erro = assertThrows(
				IllegalArgumentException.class,
				() -> game.readEnemyFire(scanner)
		);

		assertTrue(erro.getMessage().contains("exatamente"));
	}



	@Test
	@DisplayName("Deve ler três posições no formato compacto")
	void testReadEnemyFirePosicoesCompactas() {

		Scanner scanner = new Scanner("A3 B4 C5");

		String resultado = game.readEnemyFire(scanner);

		assertNotNull(resultado);
		assertTrue(resultado.contains("row"));
		assertTrue(resultado.contains("column"));

		assertEquals(1, game.getAlienMoves().size());
	}


	@Test
	@DisplayName("Deve converter posições para JSON")
	void testJsonShots() {

		List<IPosition> posicoes = List.of(
				new Position(0, 0),
				new Position(1, 1),
				new Position(2, 2)
		);

		String json = Game.jsonShots(posicoes);

		assertNotNull(json);
		assertTrue(json.contains("row"));
		assertTrue(json.contains("column"));
	}

	@Test
	@DisplayName("Deve apresentar o tabuleiro do jogador")
	void testPrintMyBoard() {

		assertDoesNotThrow(() -> {
			game.printMyBoard(false, false);
		});

		assertDoesNotThrow(() -> {
			game.printMyBoard(true, true);
		});
	}

	@Test
	@DisplayName("Deve apresentar o tabuleiro do adversário")
	void testPrintAlienBoard() {

		assertDoesNotThrow(() -> {
			game.printAlienBoard(false, false);
		});

		assertDoesNotThrow(() -> {
			game.printAlienBoard(true, true);
		});
	}

	@Test
	@DisplayName("Deve apresentar a mensagem de fim de jogo")
	void testOver() {

		PrintStream original = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			System.setOut(new PrintStream(output, true,
					StandardCharsets.UTF_8));

			game.over();

		} finally {
			System.setOut(original);
		}

		String mensagem = output.toString(StandardCharsets.UTF_8);

		assertTrue(
				mensagem.contains("Maldito sejas, Java Sparrow")
		);

		assertTrue(
				mensagem.contains("glub glub glub")
		);
	}


	@Test
	@DisplayName("Deve devolver false quando existem jogadas mas a posição não foi disparada")
	void testRepeatedShotPosicaoNaoEncontrada() {

		// Registar uma jogada com três posições
		List<IPosition> posicoes = List.of(
				new Position(1, 1),
				new Position(2, 2),
				new Position(3, 3)
		);

		game.fireShots(posicoes);

		// Procurar uma posição diferente das anteriores
		Position novaPosicao = new Position(5, 5);

		// Existem jogadas, mas nenhuma contém esta posição
		assertFalse(game.repeatedShot(novaPosicao));

		// Confirmar que a jogada foi registada
		assertEquals(1, game.getAlienMoves().size());
	}


	@Test
	@DisplayName("Deve lançar AssertionError quando a posição é null")
	void testRepeatedShotNull() {

		assertThrows(
				AssertionError.class,
				() -> game.repeatedShot(null)
		);
	}


	@Test
	@DisplayName("Deve gerar três disparos aleatórios válidos")
	void testRandomEnemyFire() {

		String json = game.randomEnemyFire();

		assertNotNull(json);
		assertTrue(json.contains("row"));
		assertTrue(json.contains("column"));

		assertEquals(1, game.getAlienMoves().size());

		List<IPosition> disparos =
				game.getAlienMoves().get(0).getShots();

		assertEquals(3, disparos.size());

		// Confirmar que os disparos são diferentes
		assertEquals(3, new java.util.HashSet<>(disparos).size());

		// Confirmar que são posições válidas
		for (IPosition pos : disparos) {
			assertTrue(pos.isInside());
		}
	}


	@Test
	@DisplayName("Não deve repetir disparos de jogadas anteriores")
	void testRandomEnemyFireSemRepeticoes() {

		// Primeira jogada
		game.randomEnemyFire();

		List<IPosition> primeira =
				game.getAlienMoves().get(0).getShots();

		// Segunda jogada
		game.randomEnemyFire();

		List<IPosition> segunda =
				game.getAlienMoves().get(1).getShots();

		assertEquals(2, game.getAlienMoves().size());

		// Verificar que nenhuma posição foi repetida
		for (IPosition pos : segunda) {
			assertFalse(primeira.contains(pos));
		}
	}

	@Test
	@DisplayName("Deve completar três disparos quando restam apenas duas posições")
	void testRandomEnemyFirePoucasPosicoes() {

		List<IPosition> anteriores = new java.util.ArrayList<>();

		// Ocupar 98 posições do tabuleiro
		for (int r = 0; r < Game.BOARD_SIZE; r++) {
			for (int c = 0; c < Game.BOARD_SIZE; c++) {

				if (r == 9 && (c == 8 || c == 9)) {
					continue;
				}

				anteriores.add(new Position(r, c));
			}
		}

		// Adicionar os disparos anteriores ao histórico
		game.getAlienMoves().add(
				new Move(1, anteriores, List.of())
		);

		// Gerar nova jogada
		String json = game.randomEnemyFire();

		assertNotNull(json);

		// Obter a jogada gerada
		List<IMove> jogadas = game.getAlienMoves();

		List<IPosition> disparos =
				jogadas.get(jogadas.size() - 1).getShots();

		// Deve gerar exatamente 3 disparos
		assertEquals(3, disparos.size());

		// Só existem 2 posições diferentes disponíveis
		assertEquals(
				2,
				new java.util.HashSet<>(disparos).size()
		);

		// O último disparo deve repetir o anterior
		assertEquals(disparos.get(1), disparos.get(2));
	}


	@Test
	@DisplayName("Deve excluir posições adjacentes a navios afundados")
	void testRandomEnemyFireNavioAfundado() {

		// Criar um navio numa posição do tabuleiro
		Barge barco = new Barge(
				Compass.NORTH,
				new Position(4, 4)
		);

		// Adicionar o navio à frota
		game.getMyFleet().addShip(barco);

		// Afundar o navio
		barco.sink();

		// Confirmar que está na lista de navios afundados
		assertEquals(1, game.getMyFleet().getSunkShips().size());

		// Obter as posições que devem ser excluídas
		List<IPosition> adjacentes = barco.getAdjacentPositions();

		// Gerar os disparos aleatórios
		String json = game.randomEnemyFire();

		assertNotNull(json);

		// Obter os disparos gerados
		List<IMove> jogadas = game.getAlienMoves();

		List<IPosition> disparos =
				jogadas.get(jogadas.size() - 1).getShots();

		// Nenhum disparo pode estar nas posições excluídas
		for (IPosition pos : disparos) {
			assertFalse(
					adjacentes.contains(pos),
					"Não deveria disparar numa posição adjacente ao navio afundado"
			);
		}
	}

	@Test
	@DisplayName("Deve tratar tabuleiro sem posições disponíveis")
	void testRandomEnemyFireSemPosicoesDisponiveis() {

		// Criar uma lista com as 100 posições do tabuleiro
		List<IPosition> todasPosicoes = new java.util.ArrayList<>();

		for (int r = 0; r < Game.BOARD_SIZE; r++) {
			for (int c = 0; c < Game.BOARD_SIZE; c++) {
				todasPosicoes.add(new Position(r, c));
			}
		}

		// Registar todas as posições como já utilizadas
		game.getAlienMoves().add(
				new Move(1, todasPosicoes, List.of())
		);

		// Não existem posições disponíveis.
		// O método tentará disparar em posições null.
		assertThrows(
				AssertionError.class,
				() -> game.randomEnemyFire()
		);
	}

	@Test
	@DisplayName("Deve lançar AssertionError quando a lista de disparos é null")
	void testJsonShotsNull() {

		assertThrows(
				AssertionError.class,
				() -> Game.jsonShots(null)
		);
	}



	@Test
	@DisplayName("Deve detetar disparo repetido de uma jogada anterior")
	void testFireSingleShotRepetidoDeJogadaAnterior() {

		Position posicao = new Position(2, 3);

		// Registar uma jogada anterior com esta posição
		game.fireShots(List.of(
				posicao,
				new Position(2, 4),
				new Position(2, 5)
		));

		assertTrue(game.repeatedShot(posicao));

		int repetidosAntes = game.getRepeatedShots();

		// isRepeated = false, mas repeatedShot(posicao) = true
		game.fireSingleShot(posicao, false);

		// Deve contabilizar mais um disparo repetido
		assertEquals(repetidosAntes + 1, game.getRepeatedShots());
	}


	@Test
	@DisplayName("readEnemyFire deve rejeitar um Scanner null")
	void testReadEnemyFireNull() {

		assertThrows(
				AssertionError.class,
				() -> game.readEnemyFire(null)
		);
	}


	@Test
	@DisplayName("Deve devolver a frota adversária")
	void testGetAlienFleet() {

		IFleet frotaAdversaria = game.getAlienFleet();

		assertNotNull(frotaAdversaria);
		assertTrue(frotaAdversaria.getShips().isEmpty());
	}


	@Test
	@DisplayName("Deve devolver a lista de jogadas do jogador")
	void testGetMyMoves() {

		List<IMove> jogadas = game.getMyMoves();

		assertNotNull(jogadas);
		assertTrue(jogadas.isEmpty());
	}

	@Test
	@DisplayName("fireShots deve lançar AssertionError quando shots é null")
	void testFireShotsNull() {

		assertThrows(
				AssertionError.class,
				() -> game.fireShots(null)
		);
	}

}