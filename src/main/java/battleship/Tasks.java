
package battleship;

import java.util.Scanner;
import java.nio.file.Path;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Tasks {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final String GOODBYE_MESSAGE = "Bons ventos!";
	private static final String AJUDA = "ajuda";
	private static final String GERAFROTA = "gerafrota";
	private static final String LEFROTA = "lefrota";
	private static final String DESISTIR = "desisto";
	private static final String RAJADA = "rajada";
	private static final String TIROS = "tiros";
	private static final String MAPA = "mapa";
	private static final String STATUS = "estado";
	private static final String SIMULA = "simula";
	private static final String PDF = "pdf";
	private static final String GUARDAR = "guardar";
	private static final String GRAFICO = "grafico";
	private static final String CLASSIFICACAO = "classificacao";

	// Mantém o estado da frota e do jogo entre comandos.
	private static class MenuState {
		IFleet myFleet;
		IGame game;
	}

	public static void menu() {
		MenuState state = new MenuState();

		menuHelp();

		System.out.print("> ");
		Scanner in = new Scanner(System.in);
		ClassificacaoJogos classificacao = new ClassificacaoJogos();
		String command = in.next();

		while (!command.equals(DESISTIR)) {
			executeCommand(command, state, in);

			System.out.print("> ");
			command = in.next();
		}

		System.out.println(GOODBYE_MESSAGE);
	}

	private static void executeCommand(
			String command, MenuState state, Scanner in) {

		switch (command) {
			case GERAFROTA:
				state.myFleet = Fleet.createRandom();
				state.game = new Game(state.myFleet);
				state.game.printMyBoard(false, true);
				break;

			case LEFROTA:
				state.myFleet = buildFleet(in);
				state.game = new Game(state.myFleet);
				state.game.printMyBoard(false, true);
				break;

			case STATUS:
				if (state.myFleet != null) {
					state.myFleet.printStatus();
				}
				break;
		}
	}

	private static void handleEnemyFire(
			IGame game, IFleet myFleet, Scanner in) {

		if (game != null) {
			game.readEnemyFire(in);
			myFleet.printStatus();
			game.printMyBoard(true, false);

			if (game.getRemainingShips() == 0) {
				game.over();
				System.exit(0);
			}
		}
	}

	private static void simulateGame(IGame game, IFleet myFleet) {
		if (game != null) {
			while (game.getRemainingShips() > 0) {
				game.randomEnemyFire();
				myFleet.printStatus();
				game.printMyBoard(true, false);

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			if (game.getRemainingShips() == 0) {
				game.over();
				System.exit(0);
			}
		}
	}

	private static void exportPdf(IGame game) {
		if (game != null) {
			try {
				HistoricoPdf.exportar(game, Path.of("historico.pdf"));
				System.out.println("PDF criado com sucesso!");
			} catch (IOException e) {
				System.out.println("Erro ao criar PDF: " + e.getMessage());
			}
		} else {
			System.out.println("Primeiro tens de criar uma frota!");
		}
	}

	private static void saveGame(IGame game) {
		if (game != null) {
			try {
				HistoricoSqlite bd = new HistoricoSqlite(Path.of("partidas.db"));
				long id = bd.guardar(game, "Jogador", "Adversario", null);
				System.out.println("Partida guardada com sucesso! ID: " + id);
			} catch (SQLException e) {
				System.out.println("Erro ao guardar: " + e.getMessage());
			}
		} else {
			System.out.println("Primeiro tens de criar uma partida!");
		}
	}

	public static void menuHelp() {
		System.out.println("======================= AJUDA DO MENU =========================");
		System.out.println("Digite um dos comandos abaixo para interagir com o jogo:");
		System.out.println("- " + GERAFROTA + ": Gera uma frota aleatória de navios.");
		System.out.println("- " + LEFROTA + ": Permite criar e carregar uma frota personalizada.");
		System.out.println("- " + STATUS + ": Mostra o status atual da frota.)");
		System.out.println("- " + MAPA + ": Exibe o mapa da frota.");
		System.out.println("- " + RAJADA + ": Realiza uma rajada de disparos.");
		System.out.println("- " + SIMULA + ": Simula um jogo completo.");
		System.out.println("- " + TIROS + ": Lista os tiros válidos realizados (* = tiro em navio, o = tiro na água)");
		System.out.println("- " + DESISTIR + ": Encerra o jogo.");
		System.out.println("- " + PDF + ": Exporta o histórico das jogadas para PDF.");
		System.out.println("- " + GUARDAR + ": Guardar na Base de Dados.");
		System.out.println("- " + GRAFICO + ": Fazer a GUI.");
		System.out.println("- " + CLASSIFICACAO + ": Mostrar a classificação dos jogadores.");
		System.out.println("===============================================================");
	}

	public static Fleet buildFleet(Scanner in) {
		assert in != null;

		Fleet fleet = new Fleet();
		int i = 0;

		while (i < Fleet.FLEET_SIZE) {
			IShip s = readShip(in);

			if (s != null) {
				boolean success = fleet.addShip(s);

				if (success) {
					i++;
				} else {
					LOGGER.info(
							"Falha na criacao de {} {} {}",
							s.getCategory(),
							s.getBearing(),
							s.getPosition()
					);
				}
			} else {
				LOGGER.info("Navio desconhecido!");
			}
		}

		LOGGER.info("{} navios adicionados com sucesso!", i);
		return fleet;
	}

	public static Ship readShip(Scanner in) {
		assert in != null;

		String shipKind = in.next();
		Position pos = readPosition(in);
		char c = in.next().charAt(0);
		Compass bearing = Compass.charToCompass(c);

		return Ship.buildShip(shipKind, bearing, pos);
	}

	public static Position readPosition(Scanner in) {
		assert in != null;

		int row = in.nextInt();
		int column = in.nextInt();

		return new Position(row, column);
	}

	public static IPosition readClassicPosition(@NotNull Scanner in) {
		if (!in.hasNext()) {
			throw new IllegalArgumentException("Nenhuma posição válida encontrada!");
		}

		String part1 = in.next();
		String part2 = null;

		if (in.hasNextInt()) {
			part2 = in.next();
		}

		String input = (part2 != null) ? part1 + part2 : part1;
		input = input.toUpperCase();

		if (input.matches("[A-Z]\\d+")) {
			char column = input.charAt(0);
			int row = Integer.parseInt(input.substring(1));
			return new Position(column, row);
		} else if (part2 != null && part1.matches("[A-Z]") && part2.matches("\\d+")) {
			char column = part1.charAt(0);
			int row = Integer.parseInt(part2);
			return new Position(column, row);
		} else {
			throw new IllegalArgumentException(
					"Formato inválido. Use 'A3', 'A 3' ou similar."
			);
		}
	}
}
