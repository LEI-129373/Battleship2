package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários (caixa-branca) da classe {@link ClassificacaoJogos}.
 */
@DisplayName("Testes da classe ClassificacaoJogos")
class ClassificacaoJogosTest {

    private ClassificacaoJogos classificacao;

    @BeforeEach
    void setUp() {
        classificacao = new ClassificacaoJogos();
    }

    // ------------------------------------------------------------------
    // registarVitoria
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("registarVitoria")
    class RegistarVitoria {

        @Test
        @DisplayName("Vencedor null lança IllegalArgumentException")
        void nullWinnerThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> classificacao.registarVitoria(null));
            assertEquals("Vencedor obrigatorio", ex.getMessage());
        }

        @Test
        @DisplayName("Vencedor vazio lança IllegalArgumentException")
        void emptyWinnerThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> classificacao.registarVitoria(""));
        }

        @Test
        @DisplayName("Vencedor só com espaços lança IllegalArgumentException")
        void blankWinnerThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> classificacao.registarVitoria("   "));
        }

        @Test
        @DisplayName("Um nome inválido não é registado na classificação")
        void invalidWinnerIsNotRegistered() {
            assertThrows(IllegalArgumentException.class,
                    () -> classificacao.registarVitoria(" "));
            assertTrue(classificacao.classificacao().isEmpty());
        }

        @Test
        @DisplayName("Vencedor válido fica registado com 1 vitória")
        void validWinnerIsRegistered() {
            classificacao.registarVitoria("Ana");

            List<ClassificacaoJogos.Resultado> tabela = classificacao.classificacao();
            assertEquals(1, tabela.size());
            assertEquals(new ClassificacaoJogos.Resultado("Ana", 1, 0), tabela.get(0));
        }
    }

    // ------------------------------------------------------------------
    // classificacao
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("classificacao")
    class Classificacao {

        @Test
        @DisplayName("Sem vitórias registadas devolve lista vazia")
        void emptyWhenNoWins() {
            assertTrue(classificacao.classificacao().isEmpty());
        }

        @Test
        @DisplayName("Várias vitórias do mesmo jogador são somadas numa só entrada")
        void winsOfSamePlayerAreCounted() {
            classificacao.registarVitoria("Rui");
            classificacao.registarVitoria("Rui");
            classificacao.registarVitoria("Rui");

            assertEquals(List.of(new ClassificacaoJogos.Resultado("Rui", 3, 0)),
                    classificacao.classificacao());
        }

        @Test
        @DisplayName("Ordena por vitórias (decrescente) e, em empate, por nome (alfabético)")
        void sortedByWinsThenByName() {
            classificacao.registarVitoria("Rui");
            classificacao.registarVitoria("Bruno");
            classificacao.registarVitoria("Ana");
            classificacao.registarVitoria("Rui");
            classificacao.registarVitoria("Bruno");
            classificacao.registarVitoria("Ana");
            classificacao.registarVitoria("Rui");

            List<ClassificacaoJogos.Resultado> esperado = List.of(
                    new ClassificacaoJogos.Resultado("Rui", 3, 0),
                    new ClassificacaoJogos.Resultado("Ana", 2, 0),
                    new ClassificacaoJogos.Resultado("Bruno", 2, 0));
            assertEquals(esperado, classificacao.classificacao());
        }

        @Test
        @DisplayName("As derrotas são sempre 0")
        void lossesAreAlwaysZero() {
            classificacao.registarVitoria("Ana");
            classificacao.registarVitoria("Rui");

            for (ClassificacaoJogos.Resultado r : classificacao.classificacao())
                assertEquals(0, r.derrotas());
        }

        @Test
        @DisplayName("A lista devolvida é imutável")
        void returnedListIsUnmodifiable() {
            classificacao.registarVitoria("Ana");
            List<ClassificacaoJogos.Resultado> tabela = classificacao.classificacao();

            assertThrows(UnsupportedOperationException.class,
                    () -> tabela.add(new ClassificacaoJogos.Resultado("Rui", 1, 0)));
        }

        @Test
        @DisplayName("Os acessores do record Resultado devolvem os valores do construtor")
        void resultadoAccessors() {
            ClassificacaoJogos.Resultado r = new ClassificacaoJogos.Resultado("Ana", 5, 2);

            assertEquals("Ana", r.jogador());
            assertEquals(5, r.vitorias());
            assertEquals(2, r.derrotas());
            assertEquals(new ClassificacaoJogos.Resultado("Ana", 5, 2), r);
            assertEquals(r.hashCode(), new ClassificacaoJogos.Resultado("Ana", 5, 2).hashCode());
            assertTrue(r.toString().contains("Ana"));
        }
    }

    // ------------------------------------------------------------------
    // apresentar
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("apresentar")
    class Apresentar {

        @Test
        @DisplayName("Sem resultados mostra o cabeçalho e a mensagem de lista vazia")
        void presentationWhenEmpty() {
            assertEquals("CLASSIFICACAO DE VITORIAS\nAinda nao existem resultados.\n",
                    classificacao.apresentar());
        }

        @Test
        @DisplayName("Com resultados mostra a classificação numerada e sem mensagem de lista vazia")
        void presentationWithResults() {
            classificacao.registarVitoria("Bruno");
            classificacao.registarVitoria("Rui");
            classificacao.registarVitoria("Rui");
            classificacao.registarVitoria("Ana");

            String esperado = "CLASSIFICACAO DE VITORIAS\n"
                    + "1. Rui - 2 vitorias\n"
                    + "2. Ana - 1 vitorias\n"
                    + "3. Bruno - 1 vitorias\n";
            String texto = classificacao.apresentar();

            assertEquals(esperado, texto);
            assertFalse(texto.contains("Ainda nao existem resultados."));
        }
    }
}