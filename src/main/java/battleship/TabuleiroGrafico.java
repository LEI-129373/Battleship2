package battleship;

import javax.swing.*;
import java.awt.*;

/** Janela Swing que apresenta o tabuleiro e os disparos conhecidos. */
public final class TabuleiroGrafico {
    private TabuleiroGrafico() {}
    public static void mostrar(IGame jogo, boolean tabuleiroProprio) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame(tabuleiroProprio ? "O meu tabuleiro" : "Tabuleiro adversario");
            JPanel grelha = new JPanel(new GridLayout(Game.BOARD_SIZE + 1, Game.BOARD_SIZE + 1, 2, 2));
            char[][] mapa = Game.construir(jogo, tabuleiroProprio);
            grelha.add(new JLabel("", SwingConstants.CENTER));
            for (int c = 0; c < Game.BOARD_SIZE; c++) grelha.add(new JLabel(Integer.toString(c + 1), SwingConstants.CENTER));
            for (int r = 0; r < Game.BOARD_SIZE; r++) {
                grelha.add(new JLabel(Character.toString((char)('A' + r)), SwingConstants.CENTER));
                for (int c = 0; c < Game.BOARD_SIZE; c++) {
                    JLabel celula = new JLabel(Character.toString(mapa[r][c]), SwingConstants.CENTER);
                    celula.setOpaque(true);
                    celula.setBackground(switch (mapa[r][c]) {
                        case 'X' -> new Color(240, 100, 100);
                        case 'o' -> new Color(160, 205, 245);
                        case '#' -> new Color(140, 140, 140);
                        default -> Color.WHITE;
                    });
                    grelha.add(celula);
                }
            }
            janela.add(grelha); janela.setSize(550, 550);
            janela.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            janela.setLocationRelativeTo(null); janela.setVisible(true);
        });
    }
}