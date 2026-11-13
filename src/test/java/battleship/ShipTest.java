package battleship;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test class for Ship achieving 100% branch and line coverage.
 */
public class ShipTest {

    private Ship ship, shipBorder;

    @BeforeEach
    void setUp() {
        ship = new Barge(Compass.NORTH, new Position(5, 5));
        shipBorder = new Barge(Compass.NORTH, new Position(0, 0));
    }

    /**
     * Cria um navio com as posições exatas indicadas, usando diretamente a
     * implementação de Ship (sem métodos redefinidos por Barge ou outras subclasses).
     */
    private Ship customShip(IPosition... pts) {
        Ship s = new Ship("Teste", Compass.NORTH, new Position(5, 5), pts.length) {
        };
        s.positions.addAll(Arrays.asList(pts));
        return s;
    }

    @Test
    @DisplayName("Test isInsideBoard returns true with ship touching all four board limits")
    void testIsInsideBoardAllLimitsExact() {
        int max = Game.BOARD_SIZE - 1;
        Ship s = customShip(new Position(0, 0), new Position(0, max),
                new Position(max, 0), new Position(max, max));
        assertTrue(s.isInsideBoard());
    }

    @Test
    @DisplayName("Test isInsideBoard false when only the left-most position is outside (multi-position)")
    void testIsInsideBoardMultiLeftOut() {
        Ship s = customShip(new Position(3, -1), new Position(3, 0), new Position(3, 1));
        assertFalse(s.isInsideBoard());
    }

    @Test
    @DisplayName("Test isInsideBoard false when only the right-most position is outside (multi-position)")
    void testIsInsideBoardMultiRightOut() {
        Ship s = customShip(new Position(3, Game.BOARD_SIZE - 1), new Position(3, Game.BOARD_SIZE));
        System.out.println(s.getPositions() + " right=" + s.getRightMostPos() + " BOARD_SIZE=" + Game.BOARD_SIZE);
        assertFalse(s.isInsideBoard());
    }

    @Test
    @DisplayName("Test isInsideBoard false when only the top-most position is outside (multi-position)")
    void testIsInsideBoardMultiTopOut() {
        Ship s = customShip(new Position(-1, 3), new Position(0, 3), new Position(1, 3));
        assertFalse(s.isInsideBoard());
    }

    @Test
    @DisplayName("Test isInsideBoard false when only the bottom-most position is outside (multi-position)")
    void testIsInsideBoardMultiBottomOut() {
        Ship s = customShip(new Position(Game.BOARD_SIZE - 1, 3), new Position(Game.BOARD_SIZE, 3));
        assertFalse(s.isInsideBoard());
    }

    @Test
    @DisplayName("Test shoot only hits the matching position in a multi-position ship")
    void testShootMultiPositionMixedLoop() {
        Ship s = customShip(new Position(1, 1), new Position(1, 2), new Position(1, 3));

        s.shoot(new Position(1, 2));
        assertFalse(s.getPositions().get(0).isHit(), "First position must not be hit.");
        assertTrue(s.getPositions().get(1).isHit(), "Second position must be hit.");
        assertFalse(s.getPositions().get(2).isHit(), "Third position must not be hit.");

        // Tiro fora do navio: nenhuma posição adicional é atingida
        s.shoot(new Position(1, 5));
        assertFalse(s.getPositions().get(0).isHit());
        assertFalse(s.getPositions().get(2).isHit());
    }

    @Test
    @DisplayName("Test stillFloating with partially hit and fully hit multi-position ship")
    void testStillFloatingPartialHits() {
        Ship s = customShip(new Position(2, 2), new Position(2, 3), new Position(2, 4));
        assertTrue(s.stillFloating());

        s.shoot(new Position(2, 2));
        assertTrue(s.stillFloating(), "Ship with remaining intact positions must still float.");

        s.shoot(new Position(2, 3));
        s.shoot(new Position(2, 4));
        assertFalse(s.stillFloating(), "Ship with all positions hit must not float.");
    }

    @Test
    @DisplayName("Test sink hits every position of a multi-position ship")
    void testSinkMultiPosition() {
        Ship s = customShip(new Position(4, 4), new Position(4, 5), new Position(4, 6));
        s.sink();
        for (IPosition p : s.getPositions())
            assertTrue(p.isHit(), "Every position must be hit after sink.");
        assertFalse(s.stillFloating());
    }

    @Test
    @DisplayName("Test getAdjacentPositions excludes own positions and duplicates (interior ship)")
    void testGetAdjacentPositionsNoDuplicatesNoOwn() {
        Ship s = customShip(new Position(4, 4), new Position(4, 5));
        List<IPosition> adj = s.getAdjacentPositions();

        // Bloco 3x4 em torno do navio menos as 2 posições do próprio navio
        assertEquals(10, adj.size());
        assertEquals(adj.size(), new HashSet<>(adj).size(), "No duplicates allowed.");
        for (IPosition p : adj)
            assertFalse(s.getPositions().contains(p), "Own position found among adjacents.");
    }

    @Test
    @DisplayName("Test getAdjacentPositions for a multi-position ship in the board corner")
    void testGetAdjacentPositionsCornerMulti() {
        Ship s = customShip(new Position(0, 0), new Position(0, 1));
        List<IPosition> adj = s.getAdjacentPositions();

        // Bloco 2x3 dentro do tabuleiro menos as 2 posições do navio
        assertEquals(4, adj.size());
        assertEquals(adj.size(), new HashSet<>(adj).size());
    }

    @Test
    @DisplayName("Test tooCloseTo IShip false when no position is adjacent (multi-position ships)")
    void testTooCloseToShipMultiFalse() {
        Ship a = customShip(new Position(1, 1), new Position(1, 2));
        Ship b = customShip(new Position(7, 7), new Position(7, 8));
        assertFalse(a.tooCloseTo(b));
    }

    @Test
    @DisplayName("Test tooCloseTo IShip true when only a later position of the other ship is adjacent")
    void testTooCloseToShipMultiLaterPosition() {
        Ship a = customShip(new Position(1, 1), new Position(1, 2));
        Ship b = customShip(new Position(8, 8), new Position(2, 3));
        assertTrue(a.tooCloseTo(b));
    }

    @Test
    @DisplayName("Test tooCloseTo IPosition checks all positions of a multi-position ship")
    void testTooCloseToPositionMulti() {
        Ship s = customShip(new Position(1, 1), new Position(1, 2), new Position(1, 3));
        assertTrue(s.tooCloseTo(new Position(2, 4)), "Adjacent to the last position only.");
        assertFalse(s.tooCloseTo(new Position(5, 5)));
    }

    @Test
    @DisplayName("Test buildShip returns the ship of the requested category for every kind")
    void testBuildShipCategories() {
        assertEquals("Barca", Ship.buildShip("barca", Compass.NORTH, new Position(2, 2)).getCategory());
        assertEquals("Caravela", Ship.buildShip("caravela", Compass.NORTH, new Position(2, 2)).getCategory());
        assertEquals("Nau", Ship.buildShip("nau", Compass.NORTH, new Position(2, 2)).getCategory());
        assertEquals("Fragata", Ship.buildShip("fragata", Compass.NORTH, new Position(2, 2)).getCategory());
        assertEquals("Galeao", Ship.buildShip("galeao", Compass.NORTH, new Position(2, 2)).getCategory());
    }

    @AfterEach
    void tearDown() {
        ship = null;
        shipBorder = null;
    }

    @Test
    @DisplayName("Test constructor with valid parameters")
    void testConstructor() {
        assertNotNull(ship, "Ship instance should not be null.");
        assertEquals("Barca", ship.getCategory(), "Ship category is incorrect.");
        assertEquals(Compass.NORTH, ship.getBearing(), "Ship bearing is incorrect.");
        assertEquals(1, ship.getSize(), "Ship size is incorrect.");
        assertFalse(ship.getPositions().isEmpty(), "Ship positions should not be empty.");
    }

    @Test
    @DisplayName("Test buildShip static method with all kinds and unknown kind")
    void testBuildShipAllTypes() {
        assertNotNull(Ship.buildShip("barca", Compass.NORTH, new Position(2, 2)));
        assertNotNull(Ship.buildShip("caravela", Compass.NORTH, new Position(2, 2)));
        assertNotNull(Ship.buildShip("nau", Compass.NORTH, new Position(2, 2)));
        assertNotNull(Ship.buildShip("fragata", Compass.NORTH, new Position(2, 2)));
        assertNotNull(Ship.buildShip("galeao", Compass.NORTH, new Position(2, 2)));

        // Testa o ramo em que o construtor é null (retorna null)
        assertNull(Ship.buildShip("desconhecido", Compass.NORTH, new Position(2, 2)));
    }

    @Test
    @DisplayName("Test getCategory method")
    void testGetCategory() {
        assertEquals("Barca", ship.getCategory());
    }

    @Test
    @DisplayName("Test getSize method")
    void testGetSize() {
        assertEquals(1, ship.getSize());
    }

    @Test
    @DisplayName("Test getBearing method")
    void testGetBearing() {
        assertEquals(Compass.NORTH, ship.getBearing());
    }

    @Test
    @DisplayName("Test getPositions method")
    void testGetPositions() {
        List<IPosition> positions = ship.getPositions();
        assertNotNull(positions);
        assertEquals(1, positions.size());
    }

    @Test
    @DisplayName("Test getPosition method")
    void testGetPosition() {
        assertNotNull(ship.getPosition());
    }

    @Test
    @DisplayName("Test stillFloating method branches (true and false)")
    void testStillFloatingBranches() {
        assertTrue(ship.stillFloating(), "Ship should be floating.");
        ship.getPositions().get(0).shoot();
        assertFalse(ship.stillFloating(), "Ship should not be floating.");
    }

    @Test
    @DisplayName("Test shoot method branches (matching and non-matching positions in loop)")
    void testShootBranches() {
        // Ramo false (posição fora do navio)
        ship.shoot(new Position(0, 0));
        assertFalse(ship.getPositions().get(0).isHit());

        // Ramo true (posição correspondente exata no loop)
        ship.shoot(new Position(5, 5));
        assertTrue(ship.getPositions().get(0).isHit());

        // Testar com um navio de tamanho maior (ex: Frigate) para garantir cobertura do ciclo com múltiplos elementos
        Ship multiShip = new Frigate(Compass.NORTH, new Position(2, 2));
        multiShip.shoot(new Position(2, 2)); // Dispara num que existe e noutros que não existem no mesmo navio
        multiShip.shoot(new Position(9, 9));
        assertNotNull(multiShip.getPositions());
    }

    @Test
    @DisplayName("Test sink method")
    void testSink() {
        ship.sink();
        assertFalse(ship.stillFloating());
    }

    @Test
    @DisplayName("Test occupies method branches")
    void testOccupiesBranches() {
        assertTrue(ship.occupies(new Position(5, 5)));
        assertFalse(ship.occupies(new Position(0, 0)));
    }

    @Test
    @DisplayName("Test tooCloseTo IShip branches (true and false with multi-position ships)")
    void testTooCloseToShipBranches() {
        Ship nearbyShip = new Barge(Compass.NORTH, new Position(5, 6));
        assertTrue(ship.tooCloseTo(nearbyShip));

        Ship farShip = new Barge(Compass.NORTH, new Position(9, 9));
        assertFalse(ship.tooCloseTo(farShip));

        // Fragatas orientadas a NORTH ocupam posições verticais na mesma coluna;
        // na coluna 3 ficam imediatamente adjacentes às da coluna 2.
        Ship multiShip = new Frigate(Compass.NORTH, new Position(2, 2));
        Ship nearbyMulti = new Frigate(Compass.NORTH, new Position(2, 3));
        assertTrue(multiShip.tooCloseTo(nearbyMulti));
    }

    @Test
    @DisplayName("Test tooCloseTo IPosition branches")
    void testTooCloseToPositionBranches() {
        assertTrue(ship.tooCloseTo(new Position(5, 6)));
        assertFalse(ship.tooCloseTo(new Position(8, 8)));
    }

    @Test
    @DisplayName("Test extremal position methods with multi-position ship")
    void testExtremalPositions() {
        assertEquals(5, ship.getTopMostPos());
        assertEquals(5, ship.getBottomMostPos());
        assertEquals(5, ship.getLeftMostPos());
        assertEquals(5, ship.getRightMostPos());

        // Navio vertical/horizontal maior para cobrir min/max com múltiplos elementos
        Ship frigate = new Frigate(Compass.SOUTH, new Position(2, 2));
        assertTrue(frigate.getBottomMostPos() >= frigate.getTopMostPos());
        assertTrue(frigate.getRightMostPos() >= frigate.getLeftMostPos());
    }

    @Test
    @DisplayName("Test getAdjacentPositions branches (middle and border ships)")
    void testGetAdjacentPositions() {
        assertEquals(8, ship.getAdjacentPositions().size());
        assertEquals(3, shipBorder.getAdjacentPositions().size());
    }

    @Test
    @DisplayName("Test isInsideBoard complete branch coverage (all 4 boundary conditions)")
    void testIsInsideBoardBranches() {
        assertTrue(ship.isInsideBoard());

        // Leftmost < 0
        Ship leftOut = new Barge(Compass.NORTH, new Position(5, 0)) {
            {
                positions.clear();
                positions.add(new Position(5, -1));
            }
        };
        assertFalse(leftOut.isInsideBoard());

        // Rightmost >= BOARD_SIZE
        Ship rightOut = new Barge(Compass.NORTH, new Position(5, 0)) {
            {
                positions.clear();
                positions.add(new Position(5, Game.BOARD_SIZE));
            }
        };
        assertFalse(rightOut.isInsideBoard());

        // Topmost < 0
        Ship topOut = new Barge(Compass.NORTH, new Position(0, 5)) {
            {
                positions.clear();
                positions.add(new Position(-1, 5));
            }
        };
        assertFalse(topOut.isInsideBoard());

        // Bottommost >= BOARD_SIZE
        Ship bottomOut = new Barge(Compass.NORTH, new Position(0, 5)) {
            {
                positions.clear();
                positions.add(new Position(Game.BOARD_SIZE, 5));
            }
        };
        assertFalse(bottomOut.isInsideBoard());
    }

    @Test
    @DisplayName("Test toString format")
    void testToString() {
        String result = ship.toString();
        assertNotNull(result);
        assertTrue(result.contains("barca") || result.contains("Barca"));
    }

    @Test
    @DisplayName("Test assertions failure branches when -ea is enabled")
    void testAssertions() {
        assertThrows(AssertionError.class, () -> Ship.buildShip(null, Compass.NORTH, new Position(1, 1)));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", null, new Position(1, 1)));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", Compass.NORTH, null));
        assertThrows(AssertionError.class, () -> ship.occupies(null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IShip) null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IPosition) null));
        assertThrows(AssertionError.class, () -> ship.shoot(null));
        assertThrows(AssertionError.class, () -> ship.shoot(new Position(-1, -1)));
    }

    @Test
    @DisplayName("Test getAdjacentPositions branches with multi-position ship (covering internal duplicate and self-containment checks)")
    void testGetAdjacentPositionsMultiPosition() {
        // Cria um navio com várias posições (ex: Fragata na horizontal ou vertical)
        Ship multiShip = new Frigate(Compass.NORTH, new Position(3, 3));
        List<IPosition> adjacents = multiShip.getAdjacentPositions();

        assertNotNull(adjacents);
        assertFalse(adjacents.isEmpty());

        // Garante que nenhuma das posições adjacentes pertence ao próprio navio
        // (testa o ramo false de !getPositions().contains(adj))
        for (IPosition adj : adjacents) {
            assertFalse(multiShip.getPositions().contains(adj), "Adjacent position should not be part of the ship.");
        }
    }

    @Test
    @DisplayName("Test assertion failures using Ship's own implementation (no subclass overrides)")
    void testAssertionsPureShip() {
        Ship s = customShip(new Position(5, 5), new Position(5, 6));

        assertThrows(AssertionError.class, () -> s.occupies(null));
        assertThrows(AssertionError.class, () -> s.tooCloseTo((IShip) null));
        assertThrows(AssertionError.class, () -> s.tooCloseTo((IPosition) null));
        assertThrows(AssertionError.class, () -> s.shoot(null));
        assertThrows(AssertionError.class, () -> s.shoot(new Position(-1, -1)));
    }

    @Test
    @DisplayName("Test occupies and tooCloseTo(IShip) using Ship's own implementation")
    void testOccupiesAndTooCloseToPureShip() {
        Ship s = customShip(new Position(5, 5), new Position(5, 6));

        assertTrue(s.occupies(new Position(5, 6)));
        assertFalse(s.occupies(new Position(5, 7)));

        assertTrue(s.tooCloseTo(customShip(new Position(6, 7))));
        assertFalse(s.tooCloseTo(customShip(new Position(9, 9))));
    }
}