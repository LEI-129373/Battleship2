# Qodana — God Class (98455 - Carmen Gonzalez)

Quality gate no GitHub Actions para detetar instâncias do cheiro no código **God Class** no projeto BattleShip2, usando o Qodana Community for JVM.

## Ficheiros entregues

| Ficheiro | Função |
|---|---|
| `.github/workflows/qodana-smell-98455.yaml` | Workflow do GitHub Actions (corre em `push`, `pull_request` e `workflow_dispatch`) |
| `qodana-98455.yaml` | Configuração do linter: `qodana-jvm-community:2026.2`, `projectJDK: "21"`, perfil e quality gate (`failureConditions.severityThresholds.any: 0`) |
| `.qodana/profiles/profile-98455.yaml` | Perfil de inspeção: parte do perfil `empty` e ativa apenas `ClassComplexity` com `m_limit: 47` |

## Tabela de mapeamento (passo 4)

| Cheiro (Fowler) | Inspeção no IDE | ID da inspeção | Métrica / limiar | Limitações |
|---|---|---|---|---|
| God Class | Overly complex class | `ClassComplexity` | WMC (soma da complexidade ciclomática dos métodos da classe); opção `m_limit` = 47 (valor "muito alto" para WMC em Java). Por omissão, o IDE usa 80 | A estratégia do livro combina três condições com AND: **ATFD > FEW** (acesso a dados de outras classes), **WMC ≥ VERY HIGH** e **TCC < 1/3** (baixa coesão). O Qodana só mede o WMC, não tem inspeções para ATFD nem TCC e não permite combinar inspeções com AND. O limiar é absoluto e não relativo ao sistema |

## Resultados

### Análise do projeto completo (passo 8)

Com `m_limit: 47`, o quality gate falhou e identificou duas classes:

| Classe | Localização | WMC |
|---|---|---|
| `Game` | `src/main/java/battleship/Game.java#L9` | 67 |
| `Ship` | `src/main/java/battleship/Ship.java#L14` | 51 |

Execução: https://github.com/LEI-129373/Battleship2/actions/runs/42404969831

**Experiência com os limiares.** [Preencher com o resultado de, por exemplo, `m_limit: 31` e `m_limit: 70`, e de `any: 2` no `failureConditions`.]

### Refabricação

Para que o projeto passasse no gate, as duas classes foram refabricadas sem alterar o comportamento:

- **`Game` (67 → ~39):** *Extract Class*. A lógica de desenhar o tabuleiro passou para a nova classe `BoardPrinter` (~24), e `Game.printBoard` passou a delegar nela. O método `randomEnemyFire` foi simplificado (`Collections.shuffle` em vez de dois ciclos duplicados) e `repeatedShot` passou a usar `anyMatch`.
- **`Ship` (51 → ~31):** o `switch` de `buildShip` foi substituído por um mapa de construtores, e os ciclos de `getTopMostPos`, `getBottomMostPos`, `getLeftMostPos`, `getRightMostPos`, `stillFloating`, `occupies` e `tooCloseTo` foram substituídos por streams.

### Provocar o cheiro (passo 9)

No ramo `God-Class-98455`, as versões originais de `Game` e `Ship` foram repostas deliberadamente e foi aberto um pull request para o `main`.

- Pull request: https://github.com/LEI-129373/Battleship2/pull/15
- Execução que **falha** (gate aponta `Game` = 67 e `Ship` = 51): https://github.com/LEI-129373/Battleship2/actions/runs/42404969831
- Execução que **passa**, depois de repor as classes refabricadas: https://github.com/LEI-129373/Battleship2/actions/runs/42409932535

### Bloqueio do merge (passo 10)

O ramo `main` tem uma regra de proteção que exige o check **Qodana God Class 98455** antes do merge. No PR #15, com o gate vermelho, o botão *Merge pull request* ficou bloqueado. [Inserir print]

Notas sobre outros checks que aparecem no PR e não pertencem a este quality gate:

- **Java CI with Maven / build** falha em todos os PRs para o `main` porque o workflow `maven.yml` usa o JDK 17 e o `pom.xml` exige Java 21.
- **Qodana Long Method 129373** está marcado como obrigatório, mas o respetivo workflow ainda não existe no `main`, por isso fica em estado *Expected* e também impede o merge.

## Reflexão

O Qodana detetou duas classes com complexidade acima do limiar: `Game` (WMC 67) e `Ship` (WMC 51). `Game` é um verdadeiro positivo, porque acumulava a lógica do jogo e a apresentação do tabuleiro, e a extração de `BoardPrinter` resolveu-o. `Ship` aproxima-se de um falso positivo: a complexidade vinha de muitos métodos simples com ciclos e de um `switch`, e não de uma classe pouco coesa a manipular dados de outras, o que o critério completo do livro (ATFD e TCC) provavelmente não sinalizaria. Pelo mesmo motivo podem existir falsos negativos: classes com WMC moderado mas com muito acesso a dados alheios e baixa coesão não são detetadas. O resultado é muito sensível ao limiar, porque com o valor por omissão do IDE (80) nenhuma classe seria apontada. Por isso, a deteção com uma única métrica absoluta é apenas uma aproximação da estratégia de deteção do livro.

## Nota sobre a configuração

O formato de perfil YAML sugerido no enunciado (`profile:` → `base: name: empty`) foi ignorado pelo Qodana, que caiu no perfil *Project Default* e reportou mais de 190 problemas de todo o tipo. O formato que funcionou foi o dos perfis oficiais da JetBrains (repositório `JetBrains/qodana-profiles`), com `baseProfile: "empty"`, `name` e `inspections` no nível de topo do ficheiro.
