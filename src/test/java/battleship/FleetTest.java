package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testes de Fleet com o objetivo de cobrir 100% dos branches (apenas JUnit 5).
 *
 * Notas:
 *  - os "assert x != null" de Fleet contam como branches; o caso em que falham so e coberto com -ea,
 *    por isso esses testes sao ignorados (assumeTrue) quando as assercoes estao desligadas;
 *  - o limite "ships.size() <= FLEET_SIZE" so fica falso com 12 navios na frota, o que exige 12 navios
 *    afastados entre si (barcas numa grelha com espacamento 2);
 *  - em createRandom(), o ramo "ship == null" nao e alcancavel: os tipos de navio estao fixos no codigo
 *    e Ship.buildShip so devolve null para tipos desconhecidos.
 */
class FleetTest {
/**agora a 100%**/
	private Fleet fleet;
	private PrintStream stdoutOriginal;
	private ByteArrayOutputStream saida;

	@BeforeEach
	void preparar() {
		fleet = new Fleet();
		stdoutOriginal = System.out;
		saida = new ByteArrayOutputStream();
		System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void limpar() {
		System.setOut(stdoutOriginal);
	}

	// ------------------------------------------------------- construtor / getShips

	@Test
	@DisplayName("Construtor: a frota comeca vazia")
	void construtor() {
		assertNotNull(fleet);
		assertTrue(fleet.getShips().isEmpty());
	}

	@Test
	@DisplayName("getShips: devolve os navios adicionados")
	void getShips() {
		IShip navio = barca(1, 1);
		fleet.addShip(navio);

		assertEquals(1, fleet.getShips().size());
		assertSame(navio, fleet.getShips().get(0));
	}

	// ------------------------------------------------------------- createRandom

	@Test
	@DisplayName("createRandom: gera frotas completas e validas (varias vezes, para cobrir tentativas falhadas)")
	void createRandom() {
		int esperado = Fleet.FLEET_SIZE;
		for (int i = 0; i < 50; i++) {
			IFleet r = Fleet.createRandom();

			assertNotNull(r);
			assertEquals(esperado, r.getShips().size());
			for (IShip s : r.getShips()) assertTrue(s.isInsideBoard());
		}
	}

	// ------------------------------------------------------------------ addShip

	@Test
	@DisplayName("addShip: navio valido e adicionado")
	void addShipValido() {
		assertTrue(fleet.addShip(barca(1, 1)));
		assertEquals(1, fleet.getShips().size());
	}

	@Test
	@DisplayName("addShip: recusa o navio quando a frota ja tem mais do que FLEET_SIZE navios")
	void addShipLimiteDaFrota() {
		int adicionados = 0;
		for (int r = 0; r < Game.BOARD_SIZE && adicionados < 12; r += 2) {
			for (int c = 0; c < Game.BOARD_SIZE && adicionados < 12; c += 2) {
				assertTrue(fleet.addShip(barca(r, c)), "barca em " + r + "," + c);
				adicionados++;
			}
		}
		assertEquals(12, fleet.getShips().size());

		assertFalse(fleet.addShip(barca(8, 8)), "a 13.a barca excede o limite");
		assertEquals(12, fleet.getShips().size());
	}

	@Test
	@DisplayName("addShip: recusa navio fora do tabuleiro")
	void addShipForaDoTabuleiro() {
		assertFalse(fleet.addShip(barca(99, 99)));
		assertTrue(fleet.getShips().isEmpty());
	}

	@Test
	@DisplayName("addShip: recusa navio sobreposto ou adjacente a outro (risco de colisao)")
	void addShipComColisao() {
		assertTrue(fleet.addShip(barca(1, 1)));

		assertFalse(fleet.addShip(barca(1, 1)), "mesma posicao");
		assertFalse(fleet.addShip(barca(2, 2)), "posicao adjacente (diagonal)");
		assertEquals(1, fleet.getShips().size());
	}

	@Test
	@DisplayName("addShip: navio nulo viola a pre-condicao (assert)")
	void addShipNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> fleet.addShip(null));
	}

	// ------------------------------------------------------------- getShipsLike

	@Test
	@DisplayName("getShipsLike: devolve so os navios da categoria pedida")
	void getShipsLike() {
		IShip barca = barca(1, 1);
		IShip nau = new Carrack(Compass.NORTH, new Position(4, 4));
		fleet.addShip(barca);
		fleet.addShip(nau);

		assertEquals(List.of(barca), fleet.getShipsLike("Barca"));
		assertEquals(List.of(nau), fleet.getShipsLike("Nau"));
		assertTrue(fleet.getShipsLike("Galeao").isEmpty());
	}

	@Test
	@DisplayName("getShipsLike: categoria nula viola a pre-condicao (assert)")
	void getShipsLikeNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> fleet.getShipsLike(null));
	}

	// --------------------------------------------- getFloatingShips / getSunkShips

	@Test
	@DisplayName("getFloatingShips e getSunkShips: separam navios a flutuar e afundados")
	void floatingESunk() {
		IShip a = barca(1, 1);
		IShip b = barca(4, 4);
		fleet.addShip(a);
		fleet.addShip(b);

		assertEquals(2, fleet.getFloatingShips().size());
		assertTrue(fleet.getSunkShips().isEmpty());

		a.sink();

		assertEquals(List.of(b), fleet.getFloatingShips());
		assertEquals(List.of(a), fleet.getSunkShips());
	}

	@Test
	@DisplayName("getFloatingShips e getSunkShips: frota vazia devolve listas vazias")
	void floatingESunkFrotaVazia() {
		assertTrue(fleet.getFloatingShips().isEmpty());
		assertTrue(fleet.getSunkShips().isEmpty());
	}

	// ------------------------------------------------------------------ shipAt

	@Test
	@DisplayName("shipAt: devolve o navio na posicao ou null se estiver vazia")
	void shipAt() {
		IShip barca = barca(1, 1);
		fleet.addShip(barca);

		assertSame(barca, fleet.shipAt(new Position(1, 1)));
		assertNull(fleet.shipAt(new Position(5, 5)));
	}

	@Test
	@DisplayName("shipAt: posicao nula viola a pre-condicao (assert)")
	void shipAtNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> fleet.shipAt(null));
	}

	// -------------------------------------------------------------- colisionRisk

	@Test
	@DisplayName("colisionRisk (privado): frota vazia, colisao e sem colisao")
	void colisionRisk() throws Exception {
		Method m = metodoColisionRisk();

		assertFalse((Boolean) m.invoke(fleet, barca(1, 1)), "frota vazia");

		fleet.addShip(barca(1, 1));
		assertTrue((Boolean) m.invoke(fleet, barca(1, 1)), "sobreposto");
		assertFalse((Boolean) m.invoke(fleet, barca(5, 5)), "afastado");
	}

	@Test
	@DisplayName("colisionRisk (privado): navio nulo viola a pre-condicao (assert)")
	void colisionRiskNulo() throws Exception {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		Method m = metodoColisionRisk();

		InvocationTargetException e = assertThrows(InvocationTargetException.class,
				() -> m.invoke(fleet, new Object[]{null}));
		assertInstanceOf(AssertionError.class, e.getCause());
	}

	// ------------------------------------------------------------------- impressao

	@Test
	@DisplayName("printShips: lista vazia nao imprime nada; lista com navios imprime-os")
	void printShips() {
		fleet.printShips(List.of());
		assertEquals("", saida.toString(StandardCharsets.UTF_8));

		fleet.addShip(barca(1, 1));
		fleet.printShips(fleet.getShips());
		assertTrue(saida.toString(StandardCharsets.UTF_8).contains("Barca"));
	}

	@Test
	@DisplayName("printShips: lista nula viola a pre-condicao (assert)")
	void printShipsNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> fleet.printShips(null));
	}

	@Test
	@DisplayName("printStatus: mostra o numero de navios a flutuar e afundados")
	void printStatus() {
		IShip a = barca(1, 1);
		fleet.addShip(a);
		fleet.addShip(barca(4, 4));
		a.sink();

		fleet.printStatus();

		assertTrue(saida.toString(StandardCharsets.UTF_8).contains("Estado da Frota: 1 a flutuar, 1 afundados!"));
	}

	@Test
	@DisplayName("printShipsByCategory, printFloatingShips e printAllShips: imprimem os navios respetivos")
	void printVariantes() {
		IShip a = barca(1, 1);
		fleet.addShip(a);
		fleet.addShip(new Carrack(Compass.NORTH, new Position(4, 4)));
		a.sink();

		fleet.printShipsByCategory("Nau");
		String aposCategoria = saida.toString(StandardCharsets.UTF_8);
		assertTrue(aposCategoria.contains("Nau"));
		assertFalse(aposCategoria.contains("Barca"));

		saida.reset();
		fleet.printFloatingShips();
		String aposFlutuantes = saida.toString(StandardCharsets.UTF_8);
		assertTrue(aposFlutuantes.contains("Nau"));
		assertFalse(aposFlutuantes.contains("Barca"));

		saida.reset();
		fleet.printAllShips();
		String aposTodos = saida.toString(StandardCharsets.UTF_8);
		assertTrue(aposTodos.contains("Nau"));
		assertTrue(aposTodos.contains("Barca"));
	}

	@Test
	@DisplayName("printShipsByCategory: categoria nula viola a pre-condicao (assert)")
	void printShipsByCategoryNulo() {
		assumeTrue(assercoesAtivas(), "requer assercoes ativas (-ea)");
		assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null));
	}

	// ---------------------------------------------------------------- auxiliares

	private static Barge barca(int linha, int coluna) {
		return new Barge(Compass.NORTH, new Position(linha, coluna));
	}

	private static Method metodoColisionRisk() throws NoSuchMethodException {
		Method m = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
		m.setAccessible(true);
		return m;
	}

	/** true apenas se a JVM correr com -ea (o efeito lateral do assert so acontece nesse caso). */
	private static boolean assercoesAtivas() {
		boolean ativas = false;
		assert ativas = true;
		return ativas;
	}

	@Test
	@DisplayName("createRandom: repete a tentativa quando Ship.buildShip devolve null (ramo 'ship == null')")
	@SuppressWarnings("unchecked")
	void createRandomQuandoBuildShipDevolveNull() {
		Object[] tabela = obterTabelaInternaDoMapa();
		assumeTrue(tabela != null, "nao foi possivel aceder ao array interno de Ship.SHIP_CONSTRUCTORS nesta JVM");

		int indice = -1;
		for (int i = 0; i < tabela.length; i += 2) {
			if ("galeao".equals(tabela[i])) {
				indice = i;
				break;
			}
		}
		assumeTrue(indice >= 0, "entrada 'galeao' nao encontrada no mapa");

		BiFunction<Compass, IPosition, Ship> original = (BiFunction<Compass, IPosition, Ship>) tabela[indice + 1];
		AtomicInteger chamadas = new AtomicInteger();
		BiFunction<Compass, IPosition, Ship> comFalhas =
				(bearing, pos) -> chamadas.getAndIncrement() < 3 ? null : original.apply(bearing, pos);

		tabela[indice + 1] = comFalhas;
		try {
			int esperado = Fleet.FLEET_SIZE;
			IFleet frota = Fleet.createRandom();

			assertEquals(esperado, frota.getShips().size());
			assertTrue(chamadas.get() > 3, "as 3 primeiras chamadas devolveram null e foram repetidas");
		} finally {
			tabela[indice + 1] = original; // repoe sempre o construtor original
		}
	}

	/** Devolve o array [chave, valor, chave, valor, ...] interno do Map.of usado em Ship, ou null se nao for possivel. */
	private static Object[] obterTabelaInternaDoMapa() {
		try {
			Field campoMapa = Ship.class.getDeclaredField("SHIP_CONSTRUCTORS");
			campoMapa.setAccessible(true);
			Object mapa = campoMapa.get(null);

			Class<?> classeUnsafe = Class.forName("sun.misc.Unsafe");
			Field theUnsafe = classeUnsafe.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);

			Method objectFieldOffset = classeUnsafe.getMethod("objectFieldOffset", Field.class);
			Method getObject = classeUnsafe.getMethod("getObject", Object.class, long.class);

			Field campoTabela = mapa.getClass().getDeclaredField("table");
			long deslocamento = (Long) objectFieldOffset.invoke(unsafe, campoTabela);
			return (Object[]) getObject.invoke(unsafe, mapa, deslocamento);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}