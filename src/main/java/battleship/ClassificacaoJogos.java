package battleship;

import org.apache.commons.collections4.CollectionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Classificacao em memoria; pode receber dados carregados da base de dados. */
public final class ClassificacaoJogos {
    public record Resultado(String jogador, int vitorias, int derrotas) {}
    private final List<String> vencedores = new ArrayList<>();
    public void registarVitoria(String vencedor) {
        if (vencedor == null || vencedor.isBlank()) throw new IllegalArgumentException("Vencedor obrigatorio");
        vencedores.add(vencedor);
    }
    public List<Resultado> classificacao() {
        List<Resultado> tabela = new ArrayList<>();
        for (String nome : vencedores.stream().distinct().toList()) {
            int total = CollectionUtils.cardinality(nome, vencedores);
            tabela.add(new Resultado(nome, total, 0));
        }
        tabela.sort(Comparator.comparingInt(Resultado::vitorias).reversed().thenComparing(Resultado::jogador));
        return List.copyOf(tabela);
    }
    public String apresentar() {
        StringBuilder texto = new StringBuilder("CLASSIFICACAO DE VITORIAS\n");
        int lugar = 1;
        for (Resultado r : classificacao()) texto.append(lugar++).append(". ").append(r.jogador()).append(" - ").append(r.vitorias()).append(" vitorias\n");
        if (vencedores.isEmpty()) texto.append("Ainda nao existem resultados.\n");
        return texto.toString();
    }
}
