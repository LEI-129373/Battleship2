# Oportunidades de Refabricação (`RefactoringOpportunities.md`)

Este documento apresenta os 19 *code smells* identificados no projeto **Battleship**, distribuídos pelos 3 elementos da equipa, sem sobreposição de classes/ficheiros entre os membros, excluindo interfaces iniciadas por `I` e utilizando a nomenclatura de refabricaçōes de Martin Fowler.

| local | nome do cheiro no código | nome da refabricação | número d@ alun@ |
| :--- | :--- | :--- | :--- |
| `Game::randomEnemyFire` | Long Method | Extract Method | 123282 |
| `Game::randomEnemyFire` | Feature Envy | Move Method | 123282 |
| `Game` | Large Class | Extract Class | 123282 |
| `BoardPrinter::printBoard` | Long Parameter List | Introduce Parameter Object | 123282 |
| `Game::jsonShots` | Duplicated Code | Extract Method | 123282 |
| `Game::fireShots` | Comments | Remove Dead Code | 123282 |
| `Game::readEnemyFire` | Primitive Obsession | Replace Data Value with Object | 123282 |
| `Tasks::menu` | Long Method | Extract Method | 98455 |
| `HistoricoPdf::acrescentar` | Feature Envy | Move Method | 98455 |
| `Tasks` | Large Class | Extract Class | 98455 |
| `HistoricoSqlite::guardar` | Long Parameter List | Introduce Parameter Object | 98455 |
| `Tasks::menu` | Switch Statements | Replace Conditional with Polymorphism | 98455 |
| `Tasks::menu` (casos `RAJADA` e `SIMULA`) | Duplicated Code | Extract Method | 98455 |
| `Tasks` (`menu`, `menuHelp`, constantes) | Shotgun Surgery | Move Method | 98455 |
| `Move::calculateFireSummary` | Long Method | Extract Method | 129373 |
| `TabuleiroGrafico::construir` | Feature Envy | Move Method | 129373 |
| `Ship::Ship` (construtor) | Long Parameter List | Remove Parameter | 129373 |
| `Move.FireSummary` | Data Class | Encapsulate Field | 129373 |
| `Position::Position` (construtores) | Data Clumps | Replace Constructor with Factory Method | 129373 |