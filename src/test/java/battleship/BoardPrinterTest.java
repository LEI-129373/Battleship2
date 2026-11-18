package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários (caixa-branca) da classe {@link BoardPrinter}.
 *
 * As dependências (IFleet, IShip, IMove, IPosition) são substituídas por
 * "stubs" criados com java.lang.reflect.Proxy, para controlar exatamente
 * o que cada método devolve e assim exercitar todos os ramos da classe,
 * sem depender da lógica das classes Fleet, Ship ou Move.
 */
@DisplayName("Testes da classe BoardPrinter")
class BoardPrinterTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOut;

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
    // Testes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Frota vazia, sem tiros e sem legenda: tabuleiro só com água e moldura correta")
    void emptyFleetWithoutShotsAndWithoutLegend() {
        BoardPrinter.printBoard(fleet(), Collections.emptyList(), false, false);

        List<String> lines = lines();
        assertTrue(lines.contains(headerLine()), "Cabeçalho com os números das colunas");
        assertTrue(lines.contains(topBorderLine()), "Moldura superior");
        assertTrue(lines.contains(bottomBorderLine()), "Moldura inferior");
        for (int row = 0; row < Game.BOARD_SIZE; row++)
            assertTrue(lines.contains(rowLine(row, emptyCells())), "Linha " + row + " só com água");
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("LEGENDA"), "Sem legenda");
    }

    @Test
    @DisplayName("Com showLegend = true a legenda é impressa")
    void legendIsPrintedWhenRequested() {
        BoardPrinter.printBoard(fleet(), Collections.emptyList(), false, true);

        List<String> lines = lines();
        assertTrue(lines.contains("          LEGENDA"));
        assertTrue(lines.contains("'#'->navio, '-'->adjacente a navio, '.'->água"));
        assertTrue(lines.contains("'*'->Tiro certeiro, 'o'->Tiro na água"));
    }

    @Test
    @DisplayName("Navio a flutuar: mostra '#' nas suas posições e não marca adjacentes")
    void floatingShipShowsOnlyItsPositions() {
        IShip ship = ship(true,
                List.of(pos(0, 0), pos(0, 1)),
                List.of(pos(1, 0), pos(1, 1)));   // adjacentes NÃO devem ser desenhadas

        BoardPrinter.printBoard(fleet(ship), Collections.emptyList(), false, false);

        char[] row0 = emptyCells();
        row0[0] = '#';
        row0[1] = '#';
        List<String> lines = lines();
        assertTrue(lines.contains(rowLine(0, row0)));
        assertTrue(lines.contains(rowLine(1, emptyCells())), "Adjacentes de navio a flutuar ficam como água");
    }

    @Test
    @DisplayName("Navio afundado: mostra '#' nas suas posições e '-' nas posições adjacentes")
    void sunkShipShowsAdjacentPositions() {
        IShip ship = ship(false,
                List.of(pos(2, 2)),
                List.of(pos(1, 2), pos(3, 2)));

        BoardPrinter.printBoard(fleet(ship), Collections.emptyList(), false, false);

        char[] row1 = emptyCells();
        row1[2] = '-';
        char[] row2 = emptyCells();
        row2[2] = '#';
        char[] row3 = emptyCells();
        row3[2] = '-';
        List<String> lines = lines();
        assertTrue(lines.contains(rowLine(1, row1)));
        assertTrue(lines.contains(rowLine(2, row2)));
        assertTrue(lines.contains(rowLine(3, row3)));
    }

    @Test
    @DisplayName("Com show_shots = true: tiro em navio '*', tiro na água 'o', tiro em adjacente 'o', tiro fora do tabuleiro ignorado")
    void shotsAreDrawnWhenRequested() {
        IShip floating = ship(true, List.of(pos(0, 0)), List.of());
        IShip sunk = ship(false, List.of(pos(5, 5)), List.of(pos(5, 6)));

        IMove move = move(
                pos(0, 0),        // acerta no navio            -> '*'
                pos(9, 9),        // água                       -> 'o'
                pos(5, 6),        // posição adjacente ('-')    -> 'o'
                outsidePos());    // fora do tabuleiro          -> ignorado

        BoardPrinter.printBoard(fleet(floating, sunk), List.of(move), true, false);

        char[] row0 = emptyCells();
        row0[0] = '*';
        char[] row5 = emptyCells();
        row5[5] = '#';
        row5[6] = 'o';
        char[] row9 = emptyCells();
        row9[9] = 'o';
        List<String> lines = lines();
        assertTrue(lines.contains(rowLine(0, row0)), "Tiro certeiro marcado com '*'");
        assertTrue(lines.contains(rowLine(5, row5)), "Tiro em posição adjacente marcado com 'o'");
        assertTrue(lines.contains(rowLine(9, row9)), "Tiro na água marcado com 'o'");
    }

    @Test
    @DisplayName("Com show_shots = false os tiros não são desenhados")
    void shotsAreNotDrawnWhenNotRequested() {
        IShip ship = ship(true, List.of(pos(4, 4)), List.of());
        IMove move = move(pos(4, 4), pos(7, 7));

        BoardPrinter.printBoard(fleet(ship), List.of(move), false, false);

        char[] row4 = emptyCells();
        row4[4] = '#';
        List<String> lines = lines();
        assertTrue(lines.contains(rowLine(4, row4)), "O navio continua '#' (tiro não desenhado)");
        assertTrue(lines.contains(rowLine(7, emptyCells())), "A água continua '.' (tiro não desenhado)");
    }

    @Test
    @DisplayName("Várias jogadas: os tiros de todas as jogadas são desenhados")
    void shotsFromSeveralMovesAreDrawn() {
        IMove first = move(pos(0, 0));
        IMove second = move(pos(0, 9));

        BoardPrinter.printBoard(fleet(), List.of(first, second), true, false);

        char[] row0 = emptyCells();
        row0[0] = 'o';
        row0[9] = 'o';
        assertTrue(lines().contains(rowLine(0, row0)));
    }

    @Test
    @DisplayName("Frota null provoca AssertionError (com asserções ativas, -ea)")
    void nullFleetFailsAssertion() {
        assertThrows(AssertionError.class,
                () -> BoardPrinter.printBoard(null, Collections.emptyList(), false, false));
    }

    @Test
    @DisplayName("Lista de jogadas null provoca AssertionError (com asserções ativas, -ea)")
    void nullMovesFailsAssertion() {
        assertThrows(AssertionError.class,
                () -> BoardPrinter.printBoard(fleet(), null, false, false));
    }

    // ------------------------------------------------------------------
    // Stubs (implementações mínimas das interfaces usando Proxy)
    // ------------------------------------------------------------------

    /** Cria uma implementação da interface que responde apenas aos métodos indicados no mapa. */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> answers) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (answers.containsKey(name))
                        return answers.get(name);
                    switch (name) {
                        case "toString":
                            return type.getSimpleName() + "Stub" + answers;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(
                                    "Método não esperado no stub de " + type.getSimpleName() + ": " + name);
                    }
                });
    }

    private static IPosition pos(int row, int column) {
        return stub(IPosition.class, Map.of(
                "getRow", row,
                "getColumn", column,
                "isInside", true));
    }

    /** Posição fora do tabuleiro: se o BoardPrinter pedir a linha/coluna, o stub lança exceção. */
    private static IPosition outsidePos() {
        return stub(IPosition.class, Map.of("isInside", false));
    }

    private static IShip ship(boolean stillFloating, List<IPosition> positions, List<IPosition> adjacent) {
        return stub(IShip.class, Map.of(
                "getPositions", positions,
                "getAdjacentPositions", adjacent,
                "stillFloating", stillFloating));
    }

    private static IFleet fleet(IShip... ships) {
        return stub(IFleet.class, Map.of("getShips", List.of(ships)));
    }

    private static IMove move(IPosition... shots) {
        return stub(IMove.class, Map.of("getShots", List.of(shots)));
    }

    // ------------------------------------------------------------------
    // Auxiliares para construir o output esperado
    // ------------------------------------------------------------------

    private List<String> lines() {
        return Arrays.asList(output.toString(StandardCharsets.UTF_8).split("\\R"));
    }

    private static char[] emptyCells() {
        char[] cells = new char[Game.BOARD_SIZE];
        Arrays.fill(cells, '.');
        return cells;
    }

    private static String rowLine(int row, char[] cells) {
        char label = new Position(row, 0).getClassicRow();
        StringBuilder sb = new StringBuilder(" " + label + " |");
        for (char cell : cells)
            sb.append(' ').append(cell);
        return sb.append(" |").toString();
    }

    private static String headerLine() {
        StringBuilder sb = new StringBuilder("    ");
        for (int col = 0; col < Game.BOARD_SIZE; col++)
            sb.append(' ').append(col + 1);
        return sb.toString();
    }

    private static String topBorderLine() {
        return "   +-" + "--".repeat(Game.BOARD_SIZE) + "+";
    }

    private static String bottomBorderLine() {
        return "   +" + "--".repeat(Game.BOARD_SIZE) + "-+";
    }
}