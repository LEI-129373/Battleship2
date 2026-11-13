package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testes de Tasks (apenas JUnit 5, sem Mockito e sem alterar qualquer classe de producao).
 *
 * Estrategia:
 *  - o System.in/System.out sao substituidos para dirigir e observar Tasks.menu();
 *  - para ter uma frota conhecida usa-se "lefrota" com 11 barcas (1 posicao cada), afastadas entre si,
 *    o que permite afundar tudo com 4 rajadas e cobrir o ramo de vitoria;
 *  - "simula" corre com a thread interrompida, para o Thread.sleep(3000) nao esperar;
 *  - os erros de PDF e SQLite sao provocados criando uma PASTA com o nome do ficheiro de destino
 *    (historico.pdf / partidas.db) na pasta de trabalho. Qualquer ficheiro pre-existente com esses
 *    nomes e guardado antes e reposto no fim de cada teste.
 *
 * Branches de Tasks que NAO e possivel cobrir sem alterar o codigo de producao:
 *  1. readClassicPosition: o corpo do "else if (part2 != null && part1.matches(...) && part2.matches(...))"
 *     nunca e executado, porque quando isso se verifica o "if (input.matches(...))" anterior ja foi verdadeiro;
 *  2. menu/SIMULA: o ramo falso de "if (game.getRemainingShips() == 0)" a seguir ao while, porque o ciclo
 *     so termina quando o valor deixa de ser > 0 e Game.getRemainingShips() nunca e negativo.
 */
class TasksCoberturaTest {

	private static final String PDF = "historico.pdf";
	private static final String BD = "partidas.db";

	/** 11 barcas (FLEET_SIZE) em A1, A3, A5, A7, A9, C1, C3, C5, C7, C9 e E1 (linha/coluna a partir de 0). */
	private static final String ONZE_BARCAS =
			"barca 0 0 n barca 0 2 n barca 0 4 n barca 0 6 n barca 0 8 n "
					+ "barca 2 0 n barca 2 2 n barca 2 4 n barca 2 6 n barca 2 8 n "
					+ "barca 4 0 n ";

	private InputStream stdinOriginal;
	private PrintStream stdoutOriginal;
	private ByteArrayOutputStream saida;
	private final List<Path[]> copias = new ArrayList<>();

	@BeforeEach
	void preparar() throws IOException {
		stdinOriginal = System.in;
		stdoutOriginal = System.out;
		saida = new ByteArrayOutputStream();
		System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
		guardarCopia(PDF);
		guardarCopia(BD);
	}

	@AfterEach
	void limpar() throws IOException {
		Thread.interrupted(); // a flag de interrupcao nao deve vazar para outros testes
		System.setIn(stdinOriginal);
		System.setOut(stdoutOriginal);
		for (Window w : Window.getWindows()) w.dispose();
		Files.deleteIfExists(Path.of(PDF));
		Files.deleteIfExists(Path.of(BD));
		for (Path[] c : copias) Files.move(c[1], c[0], StandardCopyOption.REPLACE_EXISTING);
	}

	// ------------------------------------------------------------------ menu()

	@Test
	@DisplayName("Construtor por omissao de Tasks")
	void construtorPorOmissao() {
		assertNotNull(new Tasks());
	}

	@Test
	@DisplayName("menu: comando desconhecido mostra mensagem e 'desisto' termina o ciclo")
	void menuComandoDesconhecido() {
		String out = executar("foo\ndesisto\n");

		assertTrue(out.contains("AJUDA DO MENU"));
		assertTrue(out.contains("Que comando"));
		assertTrue(out.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("menu: comandos que dependem de frota/jogo quando ainda nao existe nenhum")
	void menuSemFrotaNemJogo() {
		String out = executar("estado mapa rajada simula tiros pdf guardar grafico classificacao ajuda desisto");

		assertEquals(2, contar(out, "AJUDA DO MENU"));                        // inicio + comando 'ajuda'
		assertEquals(1, contar(out, "Primeiro tens de criar uma frota!"));    // pdf
		assertEquals(2, contar(out, "Primeiro tens de criar uma partida!"));  // guardar + grafico
		assertTrue(out.contains("Ainda nao existem resultados."));            // classificacao vazia
		assertFalse(Files.exists(Path.of(PDF)));
		assertFalse(Files.exists(Path.of(BD)));
	}

	@Test
	@DisplayName("menu: gerafrota seguido de estado, mapa, tiros, pdf, guardar, grafico e classificacao")
	void menuGerafrotaComJogo() throws Exception {
		String out = executar("gerafrota estado mapa tiros pdf guardar grafico classificacao desisto");
		SwingUtilities.invokeAndWait(() -> { }); // deixa a EDT terminar o que o comando 'grafico' agendou

		assertTrue(out.contains("Estado da Frota: 11 a flutuar, 0 afundados!"));
		assertTrue(out.contains("LEGENDA"));
		assertTrue(out.contains("PDF criado com sucesso!"));
		assertTrue(Files.size(Path.of(PDF)) > 0);
		assertTrue(out.contains("Partida guardada com sucesso! ID: 1"));
		assertTrue(Files.exists(Path.of(BD)));
		assertTrue(out.contains("CLASSIFICACAO DE VITORIAS"));
	}

	@Test
	@DisplayName("menu: IOException ao exportar o PDF e tratada (destino e uma pasta)")
	void menuPdfComErro() throws IOException {
		Files.createDirectory(Path.of(PDF));

		String out = executar("gerafrota pdf desisto");

		assertTrue(out.contains("Erro ao criar PDF: "));
		assertFalse(out.contains("PDF criado com sucesso!"));
	}

	@Test
	@DisplayName("menu: SQLException ao guardar a partida e tratada (ficheiro da BD e uma pasta)")
	void menuGuardarComErro() throws IOException {
		Files.createDirectory(Path.of(BD));

		String out = executar("gerafrota guardar desisto");

		assertTrue(out.contains("Erro ao guardar: "));
		assertFalse(out.contains("Partida guardada com sucesso!"));
	}

	@Test
	@DisplayName("menu: lefrota constroi a frota a partir do input e cria o jogo")
	void menuLefrota() {
		String out = executar("lefrota " + ONZE_BARCAS + "estado mapa desisto");

		assertTrue(out.contains("Estado da Frota: 11 a flutuar, 0 afundados!"));
		assertTrue(out.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("menu: rajada sem vitoria (ainda ha navios a flutuar)")
	void menuRajadaSemVitoria() {
		String out = executar("lefrota " + ONZE_BARCAS + "rajada A1 A3 A5\n classificacao desisto");

		assertTrue(out.contains("Estado da Frota: 8 a flutuar, 3 afundados!"));
		assertFalse(out.contains("Partida terminada"));
		assertTrue(out.contains("Ainda nao existem resultados."));
	}

	@Test
	@DisplayName("menu: 4 rajadas afundam toda a frota, registam a vitoria e terminam o jogo")
	void menuRajadaComVitoria() {
		String out = executar("lefrota " + ONZE_BARCAS
				+ "rajada A1 A3 A5\n"
				+ "rajada A7 A9 C1\n"
				+ "rajada C3 C5 C7\n"
				+ "rajada C9 E1 J10\n"
				+ "tiros classificacao desisto");

		assertTrue(out.contains("Estado da Frota: 0 a flutuar, 11 afundados!"));
		assertEquals(1, contar(out, "Partida terminada"));
		assertTrue(out.contains("1. Adversario - 1 vitorias"));
	}

	@Test
	@DisplayName("menu: simula ate ao fim (cobre o ciclo, a interrupcao do sleep e a vitoria)")
	void menuSimula() {
		Thread.currentThread().interrupt(); // Thread.sleep lanca InterruptedException logo, sem esperar 3s

		String out;
		try {
			out = executar("gerafrota simula classificacao desisto");
			assertTrue(Thread.currentThread().isInterrupted(), "a flag de interrupcao deve ser restaurada");
		} finally {
			Thread.interrupted();
		}

		assertTrue(out.contains("Estado da Frota: 0 a flutuar, 11 afundados!"));
		assertEquals(1, contar(out, "Partida terminada"));
		assertTrue(out.contains("1. Adversario - 1 vitorias"));
	}

	// ------------------------------------------------------------- buildFleet()

	@Test
	@DisplayName("buildFleet: navio desconhecido e navio fora do tabuleiro sao ignorados ate completar a frota")
	void buildFleetComFalhas() {
		String entrada = "xpto 1 1 n "       // tipo desconhecido  -> buildShip devolve null
				+ "barca 20 20 n "           // fora do tabuleiro  -> addShip devolve false
				+ ONZE_BARCAS;                // 11 sucessos

		Fleet frota = Tasks.buildFleet(new Scanner(entrada));

		assertEquals(11, frota.getShips().size());
	}

	// -------------------------------------------------- readShip / readPosition

	@Test
	@DisplayName("readShip: le tipo, posicao e orientacao e constroi o navio")
	void readShipValido() {
		Ship s = Tasks.readShip(new Scanner("barca 3 4 n"));

		assertNotNull(s);
		assertEquals("Barca", s.getCategory());
		assertEquals(Compass.NORTH, s.getBearing());
		assertEquals(3, s.getPosition().getRow());
		assertEquals(4, s.getPosition().getColumn());
	}

	@Test
	@DisplayName("readShip: tipo de navio desconhecido devolve null")
	void readShipDesconhecido() {
		assertNull(Tasks.readShip(new Scanner("xpto 3 4 n")));
	}

	@Test
	@DisplayName("readPosition: le linha e coluna")
	void readPositionLeLinhaEColuna() {
		Position p = Tasks.readPosition(new Scanner("5 6"));

		assertEquals(5, p.getRow());
		assertEquals(6, p.getColumn());
	}

	// ---------------------------------------------------- readClassicPosition()

	@Test
	@DisplayName("readClassicPosition: formato compacto 'A3'")
	void classicaCompacta() {
		IPosition p = Tasks.readClassicPosition(new Scanner("A3"));

		assertEquals(0, p.getRow());      // 'A'
		assertEquals(2, p.getColumn());   // 3 -> indice 2
	}

	@Test
	@DisplayName("readClassicPosition: formato com espaco 'A 3'")
	void classicaComEspaco() {
		IPosition p = Tasks.readClassicPosition(new Scanner("A 3"));

		assertEquals(0, p.getRow());
		assertEquals(2, p.getColumn());
	}

	@Test
	@DisplayName("readClassicPosition: letras minusculas sao normalizadas")
	void classicaMinuscula() {
		IPosition p = Tasks.readClassicPosition(new Scanner("b 7"));

		assertEquals(1, p.getRow());      // 'B'
		assertEquals(6, p.getColumn());   // 7 -> indice 6
	}

	@Test
	@DisplayName("readClassicPosition: sem tokens lanca IllegalArgumentException")
	void classicaSemTokens() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("")));
		assertTrue(e.getMessage().startsWith("Nenhuma"));
	}

	@Test
	@DisplayName("readClassicPosition: token unico invalido ('ZZ') lanca excecao")
	void classicaTokenUnicoInvalido() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("ZZ")));
		assertTrue(e.getMessage().startsWith("Formato"));
	}

	@Test
	@DisplayName("readClassicPosition: primeiro token com mais de uma letra ('AB 3') lanca excecao")
	void classicaPrimeiroTokenInvalido() {
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("AB 3")));
	}

	@Test
	@DisplayName("readClassicPosition: segundo token inteiro mas nao so digitos ('A -3') lanca excecao")
	void classicaSegundoTokenNegativo() {
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("A -3")));
	}

	// ------------------------------------------- pre-condicoes (assert in != null)

	@Test
	@DisplayName("buildFleet: scanner nulo viola a pre-condicao (assert)")
	void buildFleetScannerNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> Tasks.buildFleet(null));
	}

	@Test
	@DisplayName("readShip: scanner nulo viola a pre-condicao (assert)")
	void readShipScannerNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> Tasks.readShip(null));
	}

	@Test
	@DisplayName("readPosition: scanner nulo viola a pre-condicao (assert)")
	void readPositionScannerNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> Tasks.readPosition(null));
	}

	// ---------------------------------------------------------------- auxiliares

	/** true apenas se a JVM correr com -ea (o efeito lateral do assert so acontece nesse caso). */
	private static boolean assercoesAtivas() {
		boolean ativas = false;
		assert ativas = true;
		return ativas;
	}

	private String executar(String entrada) {
		System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
		Tasks.menu();
		return saida.toString(StandardCharsets.UTF_8);
	}

	private static int contar(String texto, String procurado) {
		return texto.split(Pattern.quote(procurado), -1).length - 1;
	}

	/** Se ja existir um ficheiro/pasta com este nome, afasta-o para ser reposto no fim do teste. */
	private void guardarCopia(String nome) throws IOException {
		Path original = Path.of(nome);
		if (Files.exists(original)) {
			Path copia = Path.of(nome + ".copia-teste");
			Files.move(original, copia, StandardCopyOption.REPLACE_EXISTING);
			copias.add(new Path[]{original, copia});
		}
	}
}