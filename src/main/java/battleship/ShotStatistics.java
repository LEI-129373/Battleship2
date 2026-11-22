package battleship;

public class ShotStatistics {
    Integer countInvalidShots;
    Integer countRepeatedShots;
    Integer countHits;
    Integer countSinks;

    public ShotStatistics() {
    }


    public int getRepeatedShots() {
        return this.countRepeatedShots;
    }


    public int getInvalidShots() {
        return this.countInvalidShots;
    }

    public int getHits() {
        return this.countHits;
    }

    public int getSunkShips() {
        return this.countSinks;
    }
}