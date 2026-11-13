package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de TabuleiroGrafico com o objetivo de cobrir 100% dos branches.
 *
 * Branches que o teste anterior deixava por cobrir:
 *  - case '#' do switch (um navio que NAO e sobrescrito por um tiro);
 *  - resultado.repeated() == true (tiro repetido ignorado);
 *  - combinacoes completas de (!valid || repeated).
 *
 * Nota: requer ambiente grafico (nao headless), por exemplo com xvfb em CI.
 */
@DisplayName("Testes da classe TabuleiroGrafico")
class TabuleiroGraficoCoberturaTest {

    @AfterEach
    void fecharJanelas() {
        for (Window w : Window.getWindows()) w.dispose();
    }

    @Test
    @DisplayName("Construtor privado (cobertura da classe utilitaria)")
    void construtorPrivado() throws Exception {
        Constructor<TabuleiroGrafico> c = TabuleiroGrafico.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(c.getModifiers()));
        c.setAccessible(true);
        assertNotNull(c.newInstance());
    }

    @Test
    @DisplayName("Tabuleiro proprio: '#' mantido, 'X' sobrescreve, posicao fora ignorada, '.' por omissao")
    void tabuleiroProprio() throws Exception {
        IPosition dentro1 = pos(1, 1, true);    // fica '#'
        IPosition fora = pos(-1, -1, false);    // ignorada (isInside false)
        IPosition dentro2 = pos(2, 2, true);    // '#' depois sobrescrito por 'X'

        IShip navio = stub(IShip.class, Map.of("getPositions", List.of(dentro1, fora, dentro2)));
        IFleet frota = stub(IFleet.class, Map.of("getShips", List.of(navio)));

        IGame.ShotResult acerto = new IGame.ShotResult(true, false, navio, false);
        IMove jogada = stub(IMove.class, Map.of(
                "getShots", List.of(dentro2),
                "getShotResults", List.of(acerto)));

        IGame jogo = stub(IGame.class, Map.of(
                "getMyFleet", frota,
                "getAlienFleet", frota,
                "getAlienMoves", List.of(jogada),
                "getMyMoves", List.of()));

        TabuleiroGrafico.mostrar(jogo, true);
        esperarEDT();
        assertTrue(existeJanela("O meu tabuleiro"));
    }

    @Test
    @DisplayName("Tabuleiro adversario: agua, fora, invalido, repetido, acerto e tiro sem resultado")
    void tabuleiroAdversario() throws Exception {
        IShip navio = stub(IShip.class, Map.of());
        IFleet frota = stub(IFleet.class, Map.of("getShips", List.of()));

        IPosition agua = pos(0, 0, true);
        IPosition fora = pos(-1, -1, false);
        IPosition invalido = pos(0, 2, true);
        IPosition repetido = pos(0, 3, true);
        IPosition acerto = pos(0, 4, true);
        IPosition semResultado = pos(0, 5, true);

        List<IPosition> tiros = List.of(agua, fora, invalido, repetido, acerto, semResultado);
        List<IGame.ShotResult> resultados = List.of(
                new IGame.ShotResult(true, false, null, false),   // 'o'
                new IGame.ShotResult(true, false, null, false),   // (consumido pelo 'fora')
                new IGame.ShotResult(false, false, null, false),  // !valid
                new IGame.ShotResult(true, true, null, false),    // valid mas repeated
                new IGame.ShotResult(true, false, navio, false)   // 'X'
        );

        IMove jogada = stub(IMove.class, Map.of("getShots", tiros, "getShotResults", resultados));

        IGame jogo = stub(IGame.class, Map.of(
                "getMyFleet", frota,
                "getAlienFleet", frota,
                "getMyMoves", List.of(jogada),
                "getAlienMoves", List.of()));

        TabuleiroGrafico.mostrar(jogo, false);
        esperarEDT();
        assertTrue(existeJanela("Tabuleiro adversario"));
    }

    // ---------- auxiliares ----------

    private static void esperarEDT() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }

    private static boolean existeJanela(String titulo) {
        for (Window w : Window.getWindows()) {
            if (w instanceof Frame f && titulo.equals(f.getTitle())) return true;
        }
        return false;
    }

    private static IPosition pos(int row, int col, boolean inside) {
        return stub(IPosition.class, Map.of("getRow", row, "getColumn", col, "isInside", inside));
    }

    /** Cria um stub dinamico: devolve os valores dados e valores por omissao para o resto. */
    private static <T> T stub(Class<T> tipo, Map<String, Object> valores) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (p, m, a) -> {
            if (valores.containsKey(m.getName())) return valores.get(m.getName());
            Class<?> r = m.getReturnType();
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            if (r == char.class) return (char) 0;
            return null;
        }));
    }
}