package battleship;

import java.util.Map;
import java.util.function.BiFunction;

public class ShipFactory {
    /**
     * The constant GALEAO.
     */
    static final String GALEAO = "galeao";
    /**
     * The constant FRAGATA.
     */
    static final String FRAGATA = "fragata";
    /**
     * The constant NAU.
     */
    static final String NAU = "nau";
    /**
     * The constant CARAVELA.
     */
    static final String CARAVELA = "caravela";
    /**
     * The constant BARCA.
     */
    static final String BARCA = "barca";
    /**
     * Associa cada tipo de navio ao respetivo construtor (substitui o switch).
     */
    static final Map<String, BiFunction<Compass, IPosition, Ship>> SHIP_CONSTRUCTORS = Map.of(
            BARCA, Barge::new,
            CARAVELA, Caravel::new,
            NAU, Carrack::new,
            FRAGATA, Frigate::new,
            GALEAO, Galleon::new);

    /**
     * Create a new ship
     *
     * @param shipKind the ship kind
     * @param bearing  the bearing
     * @param pos      the pos
     * @return s ship
     */
    static Ship buildShip(String shipKind, Compass bearing, Position pos) {
        assert shipKind != null;
        assert bearing != null;
        assert pos != null;

        BiFunction<Compass, IPosition, Ship> constructor = SHIP_CONSTRUCTORS.get(shipKind);
        return constructor == null ? null : constructor.apply(bearing, pos);
    }
}