
package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoPdfTest {

    @TempDir
    Path pastaTemporaria;

    @Test
    @DisplayName("Deve exportar um PDF de uma partida sem jogadas")
    void testExportarPartidaVazia() throws Exception {

        // Criar um jogo real com uma frota vazia
        Game jogo = new Game(new Fleet());

        // Definir o destino do PDF
        Path destino = pastaTemporaria.resolve("historico.pdf");

        // Executar o método que queremos testar
        HistoricoPdf.exportar(jogo, destino);

        // Verificar se o ficheiro foi criado
        assertTrue(Files.exists(destino));

        // Verificar se o ficheiro não está vazio
        assertTrue(Files.size(destino) > 0);
    }


    @Test
    @DisplayName("Deve exportar todos os tipos de resultados dos disparos")
    void testExportarDiferentesResultados() throws Exception {

        Game jogo = new Game(new Fleet());

        // Criar posições para os disparos
        List<IPosition> posicoes = List.of(
                new Position(0, 0),
                new Position(0, 1),
                new Position(0, 2),
                new Position(0, 3),
                new Position(0, 4),
                new Position(0, 5)
        );

        // Criar resultados diferentes
        List<IGame.ShotResult> resultados = List.of(
                new IGame.ShotResult(false, false, null, false),
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(true, false, null, true),
                new IGame.ShotResult(true, false, new Frigate(Compass.EAST, new Position(2, 2)), false),                new IGame.ShotResult(true, false, null, false)
        );

        // Criar uma jogada com os resultados
        Move jogada = new Move(1, posicoes, resultados);

        // Adicionar a jogada ao histórico
        jogo.getMyMoves().add(jogada);

        // Exportar para PDF
        Path destino = pastaTemporaria.resolve("resultados.pdf");
        HistoricoPdf.exportar(jogo, destino);

        // Verificar se o PDF foi criado
        assertTrue(Files.exists(destino));
        assertTrue(Files.size(destino) > 0);
    }


    @Test
    @DisplayName("Deve criar várias páginas quando o histórico é grande")
    void testExportarVariasPaginas() throws Exception {

        // Criar um jogo sem navios
        Game jogo = new Game(new Fleet());

        // Adicionar muitas jogadas ao histórico
        for (int i = 0; i < 30; i++) {

            List<IPosition> posicoes = new ArrayList<>();
            List<IGame.ShotResult> resultados = new ArrayList<>();

            for (int j = 0; j < 3; j++) {
                posicoes.add(new Position(j, i % 10));

                resultados.add(
                        new IGame.ShotResult(
                                true, false, null, false
                        )
                );
            }

            Move jogada = new Move(i + 1, posicoes, resultados);

            jogo.getMyMoves().add(jogada);
        }

        // Exportar o histórico
        Path destino = pastaTemporaria.resolve("historicoGrande.pdf");

        HistoricoPdf.exportar(jogo, destino);

        // Verificar que o ficheiro foi criado
        assertTrue(Files.exists(destino));

        // Abrir o PDF e verificar o número de páginas
        try (PDDocument documento = Loader.loadPDF(destino.toFile())) {

            assertTrue(
                    documento.getNumberOfPages() > 1,
                    "O PDF deveria conter mais de uma página"
            );
        }
    }


    @Test
    @DisplayName("Deve fechar corretamente quando ocorre um erro antes de criar a página")
    void testExportarJogoNull() {

        Path destino = pastaTemporaria.resolve("erro.pdf");

        assertThrows(NullPointerException.class, () -> {
            HistoricoPdf.exportar(null, destino);
        });
    }



}
