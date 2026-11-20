package battleship;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ShotParser {
    static @NotNull List<IPosition> readShots(String input) {
        List<IPosition> shots = new ArrayList<>();

        Scanner inputScanner = new Scanner(input);
        while (shots.size() < Game.NUMBER_SHOTS && inputScanner.hasNext()) {
            // Lê a próxima parte e constrói uma posição
            String token = inputScanner.next();

            addShotFromToken(token, inputScanner, shots);
        }
        return shots;
    }

    private static void addShotFromToken(String token, Scanner inputScanner, List<IPosition> shots) {
        if (token.matches("[A-Za-z]")) {
            // Caso seja somente uma coluna ("A", "B", etc.), esperar o próximo número
            if (inputScanner.hasNextInt()) {
                int row = inputScanner.nextInt();
                shots.add(new Position(token.toUpperCase().charAt(0), row));
            } else {
                throw new IllegalArgumentException("Posição incompleta! A coluna '" + token + "' não é seguida por uma linha.");
            }
        } else {
            // Caso o token já contenha a coluna e a linha juntas (ex.: "A3")
            Scanner singleScanner = new Scanner(token);
            shots.add(Tasks.readClassicPosition(singleScanner));
        }
    }

    static void validateShotCount(List<IPosition> shots) {
        if (shots.size() != Game.NUMBER_SHOTS) {
            throw new IllegalArgumentException("Você deve inserir exatamente " + Game.NUMBER_SHOTS + " posições!");
        }
    }
}
