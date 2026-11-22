
package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoSqliteTest {

    @TempDir
    Path pastaTemporaria;

    @Test
    @DisplayName("Deve criar a base de dados e guardar uma partida")
    void testGuardarPartida() throws Exception {

        // Criar o caminho para a base de dados temporária
        Path destino = pastaTemporaria.resolve("historico.db");

        // Criar a base de dados SQLite
        HistoricoSqlite historico = new HistoricoSqlite(destino);

        // Verificar se o ficheiro foi criado
        assertTrue(Files.exists(destino));

        // Criar um jogo sem jogadas
        Game jogo = new Game(new Fleet());

        // Guardar a partida
        long id = historico.guardar(
                jogo,
                new HistoricoSqlite.Participantes("Jogador1", "Computador", "Jogador1"));

        // Verificar que foi atribuído um ID válido
        assertTrue(id > 0);

        // Consultar as partidas guardadas
        List<HistoricoSqlite.Partida> partidas = historico.listar();

        // Verificar que existe uma partida
        assertEquals(1, partidas.size());

        // Verificar os dados guardados
        HistoricoSqlite.Partida partida = partidas.get(0);

        assertEquals(id, partida.id());
        assertEquals("Jogador1", partida.jogador());
        assertEquals("Computador", partida.adversario());
        assertEquals("Jogador1", partida.vencedor());

        // Verificar que a data foi guardada
        assertNotNull(partida.data());

        // Como o jogo não tem jogadas, não existem disparos
        assertTrue(historico.disparos(id).isEmpty());
    }


    @Test
    @DisplayName("Deve guardar corretamente todos os resultados dos disparos")
    void testGuardarDiferentesDisparos() throws Exception {

        Path destino = pastaTemporaria.resolve("disparos.db");
        HistoricoSqlite historico = new HistoricoSqlite(destino);

        Game jogo = new Game(new Fleet());

        List<IPosition> posicoes = List.of(
                new Position(0, 0),
                new Position(0, 1),
                new Position(0, 2),
                new Position(0, 3),
                new Position(0, 4),
                new Position(0, 5)
        );

        List<IGame.ShotResult> resultados = List.of(
                new IGame.ShotResult(false, false, null, false),
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(true, false, null, true),
                new IGame.ShotResult(
                        true, false,
                        new Frigate(Compass.EAST, new Position(2, 2)),
                        false
                ),
                new IGame.ShotResult(true, false, null, false)
        );

        Move jogada = new Move(1, posicoes, resultados);

        jogo.getMyMoves().add(jogada);

        long id = historico.guardar(
                jogo,
                new HistoricoSqlite.Participantes("Jogador1", "Computador", "Jogador1"));

        List<String> disparos = historico.disparos(id);

        assertEquals(6, disparos.size());

        assertEquals("jogador jogada 1 (0,0): invalido", disparos.get(0));
        assertEquals("jogador jogada 1 (0,1): repetido", disparos.get(1));
        assertEquals("jogador jogada 1 (0,2): afundado", disparos.get(2));
        assertEquals("jogador jogada 1 (0,3): acerto", disparos.get(3));
        assertEquals("jogador jogada 1 (0,4): agua", disparos.get(4));
        assertEquals("jogador jogada 1 (0,5): desconhecido", disparos.get(5));
    }


    @Test
    @DisplayName("Deve fazer rollback quando ocorre um erro SQL")
    void testRollbackErroSQL() throws Exception {

        Path destino = pastaTemporaria.resolve("rollback.db");
        HistoricoSqlite historico = new HistoricoSqlite(destino);

        String url = "jdbc:sqlite:" + destino.toAbsolutePath();

        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement()) {
            s.executeUpdate("DROP TABLE disparos");
        }

        Game jogo = new Game(new Fleet());

        assertThrows(SQLException.class, () ->
                historico.guardar(
                        jogo,
                        new HistoricoSqlite.Participantes("Jogador1", "Computador", "Jogador1"))
        );

        assertTrue(historico.listar().isEmpty());
    }




    @Test
    @DisplayName("Deve guardar com trigger sem inserir uma partida real")
    void testGuardarSemIdentificador() throws Exception {

        // Criar base de dados temporária
        Path destino = pastaTemporaria.resolve("sem_id.db");

        HistoricoSqlite historico = new HistoricoSqlite(destino);

        String url = "jdbc:sqlite:" + destino.toAbsolutePath();

        // Substituir a tabela partidas por uma VIEW
        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement()) {

            s.executeUpdate("DROP TABLE partidas");

            s.executeUpdate("""
            CREATE VIEW partidas AS
            SELECT
                CAST(NULL AS INTEGER) AS id,
                CAST(NULL AS TEXT) AS jogador,
                CAST(NULL AS TEXT) AS adversario,
                CAST(NULL AS TEXT) AS vencedor,
                CAST(NULL AS TEXT) AS data
            WHERE 0
            """);

            // Criar trigger que aceita INSERT
            // sem inserir uma partida real
            s.executeUpdate("""
            CREATE TRIGGER inserir_partida
            INSTEAD OF INSERT ON partidas
            BEGIN
                SELECT 1;
            END;
            """);
        }

        // Criar jogo
        Game jogo = new Game(new Fleet());

        // Tentar guardar partida
        long id = historico.guardar(
                jogo,
                new HistoricoSqlite.Participantes("Jogador1", "Computador", "Jogador1"));

        // Mostrar o identificador devolvido
        System.out.println("ID devolvido: " + id);

        // Verificar que nenhuma partida foi inserida
        assertTrue(
                historico.listar().isEmpty(),
                "A VIEW não deveria conter partidas"
        );

        // Verificar o comportamento observado do driver
        assertEquals(
                0,
                id,
                "Esperava-se ID 0 quando o trigger não insere uma partida"
        );
    }




}
