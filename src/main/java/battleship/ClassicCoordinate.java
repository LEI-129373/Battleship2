package battleship;

/**
 * Coordenada na notação clássica (coluna em letra, linha em número).
 * A coluna é sempre normalizada para maiúscula.
 */
record ClassicCoordinate(char column, int row) {

    ClassicCoordinate {
        column = Character.toUpperCase(column);
    }

    IPosition toPosition() {
        return Position.createPosition(column, row);
    }
}