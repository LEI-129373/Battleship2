package battleship;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Guarda partidas e os respetivos disparos em SQLite. */
public final class HistoricoSqlite {
    private final String url;
    public record Partida(long id, String jogador, String adversario, String vencedor, String data) {}
    public HistoricoSqlite(Path ficheiro) throws SQLException {
        url = "jdbc:sqlite:" + ficheiro.toAbsolutePath();
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS partidas (id INTEGER PRIMARY KEY AUTOINCREMENT, jogador TEXT NOT NULL, adversario TEXT NOT NULL, vencedor TEXT, data TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS disparos (id INTEGER PRIMARY KEY AUTOINCREMENT, partida_id INTEGER NOT NULL, lado TEXT NOT NULL, jogada INTEGER NOT NULL, linha INTEGER NOT NULL, coluna INTEGER NOT NULL, resultado TEXT NOT NULL, FOREIGN KEY(partida_id) REFERENCES partidas(id))");
        }
    }
    public long guardar(IGame jogo, String jogador, String adversario, String vencedor) throws SQLException {
        try (Connection c = DriverManager.getConnection(url)) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO partidas(jogador,adversario,vencedor,data) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, jogador); ps.setString(2, adversario); ps.setString(3, vencedor);
                    ps.setString(4, LocalDateTime.now().toString()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Sem identificador"); id = keys.getLong(1); }
                }
                guardarDisparos(c, id, "jogador", jogo.getMyMoves());
                guardarDisparos(c, id, "adversario", jogo.getAlienMoves());
                c.commit(); return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }
    private static void guardarDisparos(Connection c, long id, String lado, List<IMove> jogadas) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO disparos(partida_id,lado,jogada,linha,coluna,resultado) VALUES(?,?,?,?,?,?)")) {
            for (IMove jogada : jogadas) for (int i = 0; i < jogada.getShots().size(); i++) {
                IPosition p = jogada.getShots().get(i);
                String resultado = "desconhecido";
                if (i < jogada.getShotResults().size()) {
                    IGame.ShotResult r = jogada.getShotResults().get(i);
                    resultado = !r.valid() ? "invalido" : r.repeated() ? "repetido" : r.sunk() ? "afundado" : r.ship() != null ? "acerto" : "agua";
                }
                ps.setLong(1,id); ps.setString(2,lado); ps.setInt(3,jogada.getNumber());
                ps.setInt(4,p.getRow()); ps.setInt(5,p.getColumn()); ps.setString(6,resultado); ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    public List<Partida> listar() throws SQLException {
        List<Partida> partidas = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT id,jogador,adversario,vencedor,data FROM partidas ORDER BY id DESC")) {
            while (rs.next()) partidas.add(new Partida(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)));
        }
        return partidas;
    }
    public List<String> disparos(long partidaId) throws SQLException {
        List<String> resultado = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url); PreparedStatement ps = c.prepareStatement("SELECT lado,jogada,linha,coluna,resultado FROM disparos WHERE partida_id=? ORDER BY id")) {
            ps.setLong(1, partidaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultado.add(rs.getString(1) + " jogada " + rs.getInt(2) + " (" + rs.getInt(3) + "," + rs.getInt(4) + "): " + rs.getString(5));
            }
        }
        return resultado;
    }
}
