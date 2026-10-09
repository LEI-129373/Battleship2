package battleship;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Exporta as jogadas de uma partida para PDF. */
public final class HistoricoPdf {
    private HistoricoPdf() {}
    public static void exportar(IGame jogo, Path destino) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage pagina = null;
            PDPageContentStream escrita = null;
            float y = 0;
            try {
                for (String linha : linhas(jogo)) {
                    if (pagina == null || y < 55) {
                        if (escrita != null) escrita.close();
                        pagina = new PDPage(PDRectangle.A4);
                        documento.addPage(pagina);
                        escrita = new PDPageContentStream(documento, pagina);
                        y = 790;
                    }
                    escrita.beginText();
                    escrita.setFont(fonte, 11);
                    escrita.newLineAtOffset(45, y);
                    escrita.showText(linha);
                    escrita.endText();
                    y -= 18;
                }
            } finally { if (escrita != null) escrita.close(); }
            documento.save(destino.toFile());
        }
    }
    private static List<String> linhas(IGame jogo) {
        java.util.ArrayList<String> texto = new java.util.ArrayList<>();
        texto.add("Batalha Naval - Historico de jogadas");
        acrescentar(texto, "Jogador", jogo.getMyMoves());
        acrescentar(texto, "Adversario", jogo.getAlienMoves());
        texto.add("Acertos registados: " + jogo.getHits());
        return texto;
    }
    private static void acrescentar(List<String> texto, String lado, List<IMove> jogadas) {
        texto.add("Jogadas: " + lado);
        for (IMove jogada : jogadas) {
            List<IGame.ShotResult> resultados = jogada.getShotResults();
            for (int i = 0; i < jogada.getShots().size(); i++) {
                IPosition p = jogada.getShots().get(i);
                String resultado = "sem resultado";
                if (i < resultados.size()) {
                    IGame.ShotResult r = resultados.get(i);
                    resultado = !r.valid() ? "invalido" : r.repeated() ? "repetido" : r.sunk() ? "afundou" : r.ship() != null ? "acerto" : "agua";
                }
                texto.add("Jogada " + jogada.getNumber() + ": " + p.getClassicRow() + p.getClassicColumn() + " - " + resultado);
            }
        }
    }
}
