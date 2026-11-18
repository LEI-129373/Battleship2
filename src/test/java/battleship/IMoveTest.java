package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários (caixa-branca) do método estático {@link IMove#readMove(int, Scanner)}.
 * É o único código executável da interface IMove (os restantes métodos são abstratos
 * e são testados através da classe Move).
 */
@DisplayName("Testes da interface IMove (readMove)")
class IMoveTest {

    @Test
    @DisplayName("Com 0 tiros devolve uma jogada sem tiros (o ciclo não é executado)")
    void readMoveWithZeroShots() {
        Scanner sc = new Scanner("0");

        Move move = IMove.readMove(1, sc);

        assertNotNull(move);
        assertEquals(1, move.getNumber());
        assertTrue(move.getShots().isEmpty());
        assertTrue(move.getShotResults().isEmpty());
    }

    @Test
    @DisplayName("Com 1 tiro lê a linha e a coluna corretas")
    void readMoveWithOneShot() {
        Scanner sc = new Scanner("1 4 7");

        Move move = IMove.readMove(2, sc);

        assertEquals(2, move.getNumber());
        List<IPosition> shots = move.getShots();
        assertEquals(1, shots.size());
        assertEquals(4, shots.get(0).getRow());
        assertEquals(7, shots.get(0).getColumn());
    }

    @Test
    @DisplayName("Com 3 tiros lê todos os tiros pela ordem indicada")
    void readMoveWithThreeShots() {
        Scanner sc = new Scanner("3  0 0  5 6  9 9");

        Move move = IMove.readMove(3, sc);

        assertEquals(3, move.getNumber());
        List<IPosition> shots = move.getShots();
        assertEquals(3, shots.size());
        assertPosition(0, 0, shots.get(0));
        assertPosition(5, 6, shots.get(1));
        assertPosition(9, 9, shots.get(2));
        assertTrue(move.getShotResults().isEmpty(), "Uma jogada acabada de ler ainda não tem resultados");
    }

    @Test
    @DisplayName("Lê apenas os valores necessários e deixa o resto do input no Scanner")
    void readMoveConsumesOnlyItsInput() {
        Scanner sc = new Scanner("1 2 3 99");

        IMove.readMove(1, sc);

        assertTrue(sc.hasNextInt());
        assertEquals(99, sc.nextInt());
    }

    @Test
    @DisplayName("Duas jogadas seguidas são lidas do mesmo Scanner")
    void readTwoMovesFromSameScanner() {
        Scanner sc = new Scanner("1 1 1\n2 2 2 3 3");

        Move first = IMove.readMove(1, sc);
        Move second = IMove.readMove(2, sc);

        assertEquals(1, first.getShots().size());
        assertPosition(1, 1, first.getShots().get(0));
        assertEquals(2, second.getShots().size());
        assertPosition(2, 2, second.getShots().get(0));
        assertPosition(3, 3, second.getShots().get(1));
    }

    @Test
    @DisplayName("Input incompleto lança NoSuchElementException")
    void readMoveWithIncompleteInputThrows() {
        Scanner sc = new Scanner("2 1 1 5");   // anuncia 2 tiros mas só traz 1 e meio

        assertThrows(NoSuchElementException.class, () -> IMove.readMove(1, sc));
    }

    private static void assertPosition(int expectedRow, int expectedColumn, IPosition actual) {
        assertEquals(expectedRow, actual.getRow(), "linha");
        assertEquals(expectedColumn, actual.getColumn(), "coluna");
    }
}