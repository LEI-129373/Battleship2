package battleship;

import java.util.Scanner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.io.IOException;

/**
 * The type Tasks.
 */
public class Tasks {

	/**
	 * The constant GOODBYE_MESSAGE.
	 */
	private static final String GOODBYE_MESSAGE = "Bons ventos!";

	/**
	 * Strings to be used by the user
	 */
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
	private static final String MSG_SEM_PARTIDA = "Primeiro tens de criar uma partida!";

	/**
	 * This task also tests the fighting element of a round of three shots
	 */
	public static void menu() {

		IFleet myFleet = null;
		IGame game = null;
		menuHelp();

		System.out.print("> ");
		Scanner in = new Scanner(System.in);
		ClassificacaoJogos classificacao = new ClassificacaoJogos();
		String command = in.next();
		while (!command.equals(DESISTIR)) {

			switch (command) {
				case GERAFROTA:
					myFleet = Fleet.createRandom();
					game = new Game(myFleet);
					game.printMyBoard(false, true);
					break;
				case LEFROTA:
					myFleet = LeitorFrota.buildFleet(in);
					game = new Game(myFleet);
					game.printMyBoard(false, true);
					break;
				case STATUS:
					if (myFleet != null)
						myFleet.printStatus();
					break;
				case MAPA:
					if (myFleet != null)
						game.printMyBoard(false, true);
					break;
				case RAJADA:
					if (game != null) {
						game.readEnemyFire(in);
						myFleet.printStatus();
						game.printMyBoard(true, false);

						game = terminarPartidaSeAcabou(game, classificacao);
					}
					break;
				case SIMULA:
					if (game != null) {
						while (game.getRemainingShips() > 0){
							game.randomEnemyFire();
							myFleet.printStatus();
							game.printMyBoard(true, false);
							try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt(); // Best practice: restore interrupt status
							}
						}

						game = terminarPartidaSeAcabou(game, classificacao);
					}
					break;
				case TIROS:
					if (game != null)
						game.printMyBoard(true, true);
					break;
				case AJUDA:
					menuHelp();
					break;
				case PDF:
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
					break;
				case GUARDAR:
					guardarPartida(game);
					break;
				case GRAFICO:
					if (game != null) {
						TabuleiroGrafico.mostrar(game, true);
					} else {
						System.out.println(MSG_SEM_PARTIDA);
					}
					break;
				case CLASSIFICACAO:
					System.out.println(classificacao.apresentar());
					break;
				default:
					System.out.println("Que comando é esse??? Repete ...");
			}
			System.out.print("> ");
			command = in.next();
		}
		System.out.println(GOODBYE_MESSAGE);
	}

	private static @Nullable IGame terminarPartidaSeAcabou(IGame game, ClassificacaoJogos classificacao) {
		if (game.getRemainingShips() == 0) {
			game.over();
			classificacao.registarVitoria("Adversario");
			System.out.println("Partida terminada! Vitória registada.");
			game = null;
		}
		return game;
	}

	private static void guardarPartida(IGame game) {
		if (game != null) {
			try {
				HistoricoSqlite bd = new HistoricoSqlite(
						Path.of("partidas.db")
				);

				long id = bd.guardar(
						game,
						new HistoricoSqlite.Participantes("Jogador", "Adversario", null));

				System.out.println("Partida guardada com sucesso! ID: " + id);

			} catch (java.sql.SQLException e) {
				System.out.println("Erro ao guardar: " + e.getMessage());
			}
		} else {
			System.out.println(MSG_SEM_PARTIDA);
		}
	}

	/**
	 * This function provides help information about the menu commands.
	 */
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

	/**
	 * This operation allows reading a position in the map
	 *
	 * @param in The scanner to read from
	 * @return The classic position that has been read
	 */
	public static IPosition readClassicPosition(@NotNull Scanner in) {
		// Verifica se ainda há tokens disponíveis
		if (!in.hasNext()) {
			throw new IllegalArgumentException("Nenhuma posição válida encontrada!");
		}

		String part1 = in.next(); // Primeiro token
		String part2 = null;

		if (in.hasNextInt()) {
			part2 = in.next(); // Segundo token, se disponível
		}

		String input = (part2 != null) ? part1 + part2 : part1;

		// Normalizar o input para tratar letras maiúsculas e minúsculas
		input = input.toUpperCase();

		// Verificar os dois formatos possíveis: compactos e com espaço
		if (input.matches("[A-Z]\\d+")) {
			char column = input.charAt(0); // Extrair a coluna
			int row = Integer.parseInt(input.substring(1)); // Extrair a linha
			return new Position(column, row);
		} else if (part2 != null && part1.matches("[A-Z]") && part2.matches("\\d+")) {
			char column = part1.charAt(0); // Extrair a coluna
			int row = Integer.parseInt(part2); // Extrair a linha
			return new Position(column, row);
		} else {
			throw new IllegalArgumentException("Formato inválido. Use 'A3', 'A 3' ou similar.");
		}
	}

}