package battleship;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Janela Swing que apresenta o tabuleiro e os disparos conhecidos. */
public final class TabuleiroGrafico {
    private TabuleiroGrafico() {}
    public static void mostrar(IGame jogo, boolean tabuleiroProprio) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame(tabuleiroProprio ? "O meu tabuleiro" : "Tabuleiro adversario");
            JPanel grelha = new JPanel(new GridLayout(Game.BOARD_SIZE + 1, Game.BOARD_SIZE + 1, 2, 2));
            char[][] mapa = construir(jogo, tabuleiroProprio);
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
    private static char[][] construir(IGame jogo, boolean proprio) {
        char[][] mapa = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];
        for (char[] linha : mapa) java.util.Arrays.fill(linha, '.');
        IFleet frota = proprio ? jogo.getMyFleet() : jogo.getAlienFleet();
        if (proprio) for (IShip navio : frota.getShips()) for (IPosition p : navio.getPositions())
            if (p.isInside()) mapa[p.getRow()][p.getColumn()] = '#';
        List<IMove> jogadas = proprio ? jogo.getAlienMoves() : jogo.getMyMoves();
        for (IMove jogada : jogadas) for (int i = 0; i < jogada.getShots().size(); i++) {
            IPosition p = jogada.getShots().get(i);
            if (!p.isInside()) continue;
            if (i >= jogada.getShotResults().size()) continue;
            IGame.ShotResult resultado = jogada.getShotResults().get(i);
            if (!resultado.valid() || resultado.repeated()) continue;
            mapa[p.getRow()][p.getColumn()] = resultado.ship() == null ? 'o' : 'X';
        }
        return mapa;
    }
}
