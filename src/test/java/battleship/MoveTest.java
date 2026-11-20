package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários (caixa-branca) da classe {@link Move}.
 */
@DisplayName("Testes da classe Move")
class MoveTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOut;

    private final IShip caravela = new Caravel(Compass.NORTH, new Position(0, 0));
    private final IShip barca = new Barge(Compass.NORTH, new Position(5, 5));

    @BeforeEach
    void redirectSystemOut() {
        originalOut = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreSystemOut() {
        System.setOut(originalOut);
    }

    // ------------------------------------------------------------------
    // Construtor, getters e toString
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Construtor, getters e toString")
    class Basicos {

        @Test
        @DisplayName("O construtor guarda o número, os tiros e os resultados")
        void constructorStoresValues() {
            List<IPosition> shots = List.of(new Position(1, 1), new Position(2, 2));
            List<IGame.ShotResult> results = List.of(agua(), agua());

            Move move = new Move(7, shots, results);

            assertEquals(7, move.getNumber());
            assertSame(shots, move.getShots());
            assertSame(results, move.getShotResults());
        }

        @Test
        @DisplayName("toString mostra o número da jogada e o tamanho das listas")
        void toStringShowsSizes() {
            Move move = new Move(3, List.of(new Position(0, 0)), List.of(agua(), agua()));

            assertEquals("Move{number=3, shots=1, results=2}", move.toString());
        }
    }

    // ------------------------------------------------------------------
    // processEnemyFire: resumo impresso (verbose = true)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("processEnemyFire com verbose = true (resumo impresso)")
    class ResumoImpresso {

        @Test
        @DisplayName("Três tiros na água")
        void threeMisses() {
            processar(1, true, agua(), agua(), agua());

            assertEquals("Jogada nº1 -> 3 tiros válidos: 3 tiros na água", summaryLine());
        }

        @Test
        @DisplayName("Um tiro válido na água e dois tiros exteriores (singular e plural)")
        void oneMissAndTwoOutside() {
            processar(2, true, agua(), invalido(), invalido());

            assertEquals("Jogada nº2 -> 1 tiro válido: 1 tiro na água, 2 tiros exteriores", summaryLine());
        }

        @Test
        @DisplayName("Tiros num navio que não afundou, sem tiros na água")
        void hitsWithoutSinking() {
            processar(3, true, acerto(caravela), acerto(caravela), invalido());

            assertEquals("Jogada nº3 -> 2 tiros válidos: 2 tiros num(a) Caravela , 1 tiro exterior", summaryLine());
        }

        @Test
        @DisplayName("Um tiro num navio que não afundou, sem tiros na água")
        void singleHitWithoutSinking() {
            processar(4, true, acerto(caravela), invalido(), invalido());

            assertEquals("Jogada nº4 -> 1 tiro válido: 1 tiro num(a) Caravela , 2 tiros exteriores", summaryLine());
        }

        @Test
        @DisplayName("Navio afundado, tiro noutro navio e tiro na água")
        void sunkHitAndMiss() {
            processar(5, true, afundou(barca), acerto(caravela), agua());

            assertEquals("Jogada nº5 -> 3 tiros válidos: 1 Barca ao fundo + 1 tiro num(a) Caravela + 1 tiro na água",
                    summaryLine());
        }

        @Test
        @DisplayName("Dois navios do mesmo tipo afundados (plural) e nenhum tiro na água")
        void twoSunkSameType() {
            processar(6, true, afundou(barca), afundou(barca), invalido());

            assertEquals("Jogada nº6 -> 2 tiros válidos: 2 Barcas ao fundo , 1 tiro exterior", summaryLine());
        }

        @Test
        @DisplayName("Só tiros repetidos")
        void onlyRepeated() {
            processar(7, true, repetido(), repetido(), repetido());

            assertEquals("Jogada nº7 -> 3 tiros repetidos", summaryLine());
        }

        @Test
        @DisplayName("Um tiro repetido e dois tiros exteriores")
        void oneRepeatedAndOutside() {
            processar(8, true, repetido(), invalido(), invalido());

            assertEquals("Jogada nº8 -> 1 tiro repetido, 2 tiros exteriores", summaryLine());
        }

        @Test
        @DisplayName("Tiro válido, tiro repetido e tiro exterior")
        void validRepeatedAndOutside() {
            processar(9, true, agua(), repetido(), invalido());

            assertEquals("Jogada nº9 -> 1 tiro válido: 1 tiro na água, 1 tiro repetido, 1 tiro exterior", summaryLine());
        }

        @Test
        @DisplayName("Dois tiros válidos e dois repetidos (resultados para além de NUMBER_SHOTS)")
        void validAndTwoRepeated() {
            processar(10, true, agua(), agua(), repetido(), repetido());

            assertEquals("Jogada nº10 -> 2 tiros válidos: 2 tiros na água, 2 tiros repetidos", summaryLine());
        }

        @Test
        @DisplayName("Só tiros exteriores")
        void onlyOutside() {
            processar(11, true, invalido(), invalido(), invalido());

            assertEquals("Jogada nº11 -> 3 tiros exteriores", summaryLine());
        }

        @Test
        @DisplayName("Um único tiro exterior quando os outros dois são válidos")
        void singleOutside() {
            processar(12, true, agua(), agua(), invalido());

            assertEquals("Jogada nº12 -> 2 tiros válidos: 2 tiros na água, 1 tiro exterior", summaryLine());
        }
    }

    // ------------------------------------------------------------------
    // processEnemyFire: resposta em JSON
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("processEnemyFire: resposta em JSON")
    class RespostaJson {

        @Test
        @DisplayName("Com verbose = false não imprime o resumo, mas devolve o JSON")
        void notVerboseDoesNotPrintSummary() throws Exception {
            String json = processar(1, false, agua(), agua(), agua());

            assertFalse(output.toString(StandardCharsets.UTF_8).contains("Jogada nº"));
            JsonNode node = new ObjectMapper().readTree(json);
            assertEquals(3, node.get("validShots").asInt());
            assertEquals(3, node.get("missedShots").asInt());
        }

        @Test
        @DisplayName("O JSON também é impresso na consola")
        void jsonIsPrinted() {
            String json = processar(1, false, agua(), agua(), agua());

            assertTrue(output.toString(StandardCharsets.UTF_8).contains(json));
        }

        @Test
        @DisplayName("JSON com contagens de tiros válidos, exteriores, repetidos e na água")
        void jsonCounters() throws Exception {
            JsonNode node = new ObjectMapper().readTree(processar(1, false, agua(), repetido(), invalido()));

            assertEquals(1, node.get("validShots").asInt());
            assertEquals(1, node.get("outsideShots").asInt());
            assertEquals(1, node.get("repeatedShots").asInt());
            assertEquals(1, node.get("missedShots").asInt());
        }

        @Test
        @DisplayName("JSON lista os navios afundados e os atingidos que não afundaram")
        void jsonSunkAndHitBoats() throws Exception {
            JsonNode node = new ObjectMapper().readTree(
                    processar(1, false, afundou(barca), acerto(caravela), acerto(caravela)));

            JsonNode sunk = node.get("sunkBoats");
            assertEquals(1, sunk.size());
            assertEquals("Barca", sunk.get(0).get("type").asText());
            assertEquals(1, sunk.get(0).get("count").asInt());

            JsonNode hits = node.get("hitsOnBoats");
            assertEquals(1, hits.size(), "A Barca afundada não aparece nos navios atingidos");
            assertEquals("Caravela", hits.get(0).get("type").asText());
            assertEquals(2, hits.get(0).get("hits").asInt());
        }

        @Test
        @DisplayName("JSON sem navios afundados nem atingidos tem listas vazias")
        void jsonEmptyLists() throws Exception {
            JsonNode node = new ObjectMapper().readTree(processar(1, false, agua(), agua(), agua()));

            assertEquals(0, node.get("sunkBoats").size());
            assertEquals(0, node.get("hitsOnBoats").size());
        }
    }

    // ------------------------------------------------------------------
    // Ramos que não são alcançáveis pela interface pública (reflexão)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Ramos defensivos (testados por reflexão)")
    class RamosDefensivos {

        @Test
        @DisplayName("appendRepeatedShotsAfterValid sem tiros válidos não acrescenta a vírgula")
        void repeatedAfterValidWithoutValidShots() throws Exception {
            Move move = new Move(1, List.of(), List.of());
            Object summary = newFireSummary(0, 2);
            StringBuilder out = new StringBuilder();

            Method m = Move.class.getDeclaredMethod("appendRepeatedShotsAfterValid",
                    StringBuilder.class, summary.getClass());
            m.setAccessible(true);
            m.invoke(move, out, summary);

            assertEquals("2 tiros repetidos", out.toString());
        }

        @Test
        @DisplayName("serializeResponse lança RuntimeException se o JSON não puder ser gerado")
        void serializeResponseFailure() throws Exception {
            Move move = new Move(1, List.of(), List.of());
            Method m = Move.class.getDeclaredMethod("serializeResponse", Map.class);
            m.setAccessible(true);

            Map<String, Object> invalida = Map.of("objeto", new Object());

            InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                    () -> m.invoke(move, invalida));
            assertInstanceOf(RuntimeException.class, ex.getCause());
            assertEquals("Erro ao serializar o JSON dos resultados da jogada", ex.getCause().getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private String processar(int numero, boolean verbose, IGame.ShotResult... resultados) {
        List<IPosition> shots = new ArrayList<>();
        for (int i = 0; i < resultados.length; i++)
            shots.add(new Position(i, i));
        Move move = new Move(numero, shots, Arrays.asList(resultados));
        return move.processEnemyFire(verbose);
    }

    private String summaryLine() {
        return Arrays.stream(output.toString(StandardCharsets.UTF_8).split("\\R"))
                .filter(l -> l.startsWith("Jogada nº"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Resumo da jogada não foi impresso"));
    }

    private static IGame.ShotResult agua() {
        return new IGame.ShotResult(true, false, null, false);
    }

    private static IGame.ShotResult invalido() {
        return new IGame.ShotResult(false, false, null, false);
    }

    private static IGame.ShotResult repetido() {
        return new IGame.ShotResult(true, true, null, false);
    }

    private static IGame.ShotResult acerto(IShip ship) {
        return new IGame.ShotResult(true, false, ship, false);
    }

    private static IGame.ShotResult afundou(IShip ship) {
        return new IGame.ShotResult(true, false, ship, true);
    }

    private static Object newFireSummary(int validShots, int repeatedShots) throws Exception {
        Class<?> cls = Class.forName("battleship.Move$FireSummary");
        Constructor<?> c = cls.getDeclaredConstructor();
        c.setAccessible(true);
        Object summary = c.newInstance();
        setField(summary, "validShots", validShots);
        setField(summary, "repeatedShots", repeatedShots);
        return summary;
    }

    private static void setField(Object target, String name, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(target, value);
    }
}