# Oportunidades de Refabricação

Oportunidades de refabricação identificadas pelo grupo com o MetricsTree, o Qodana, o SonarQube for IDE e revisão direta do código. Os nomes dos cheiros e das refabricações seguem os catálogos de Martin Fowler.

Cada membro trabalha num conjunto exclusivo de ficheiros, para evitar conflitos na fusão dos ramos `Refactoring_<nº>`.

| Local | Cheiro no código | Refabricação | Nº de aluno |
|---|---|---|---|
| `Game::randomEnemyFire` | Long Method | Extract Method | 123282 |
| `Game::randomEnemyFire` | Feature Envy | Move Method | 123282 |
| `Game` | Large Class | Extract Class | 123282 |
| `BoardPrinter::printBoard` | Long Parameter List | Introduce Parameter Object | 123282 |
| `Game::jsonShots` | Duplicated Code | Extract Method / Move Method | 123282 |
| `Game::fireShots` | Comments | Extract Method + Rename Method | 123282 |
| `Game::readEnemyFire` | Primitive Obsession | Replace Data Value with Object | 123282 |
| `Tasks::menu` (caso `GUARDAR`) | Long Method | Extract Method | 98455 |
| `HistoricoPdf::acrescentar` | Feature Envy | Extract Method + Move Method | 98455 |
| `Tasks` (leitura da frota extraída para `LeitorFrota`) | Large Class | Extract Class | 98455 |
| `HistoricoSqlite::guardar` | Long Parameter List | Introduce Parameter Object | 98455 |
| `Tasks::menu` (caso `GRAFICO`) e `Tasks::guardarPartida` | Duplicated Code | Replace Magic Number with Symbolic Constant | 98455 |
| `Tasks::menu` (casos `RAJADA` e `SIMULA`) | Duplicated Code | Extract Method | 98455 |
| `ClassificacaoJogos.Resultado` (componente `derrotas`) | Speculative Generality | Remove Parameter | 98455 |
| `Move::calculateFireSummary` | Long Method | Extract Method | 129373 |
| `TabuleiroGrafico::construir` | Feature Envy | Move Method | 129373 |
| `Ship::Ship` (construtor) | Long Parameter List | Remove Parameter | 129373 |
| `Move.FireSummary` | Data Class | Encapsulate Field | 129373 |
| `Position::Position` (construtores) | Data Clumps | Replace Constructor with Factory Method | 129373 |

## Distribuição de ficheiros

| Membro | Ficheiros exclusivos |
|---|---|
| 123282 | `Game.java`, `BoardPrinter.java` |
| 98455 | `Tasks.java`, `HistoricoSqlite.java`, `HistoricoPdf.java`, `ClassificacaoJogos.java` |
| 129373 | `Move.java`, `Ship.java`, `TabuleiroGrafico.java`, `Position.java` |