# Ficha Laboratorial 3 — Tarefa 2A: Qodana

**Aluno:** 98455 — Carmen Gonzalez  
**Projeto:** Battleship2  
**Code Smell:** God Class  
**Ferramenta:** JetBrains Qodana for JVM Community  
**Ano letivo:** 2026/2027

## 1. Identificação do Code Smell

A **God Class**, descrita por Martin Fowler (próxima de *Large Class*), ocorre quando uma classe concentra demasiada lógica e responsabilidades, centralizando o comportamento do sistema e acedendo a dados de outras classes. Isto torna a classe difícil de compreender, manter, testar e reutilizar.

A principal técnica de refatorização utilizada foi **Extract Class**, que consiste em mover uma responsabilidade da classe para uma nova classe, preservando o comportamento original do programa. Foram também simplificados métodos com ciclos e estruturas condicionais desnecessárias.

## 2. Mapeamento entre o Code Smell e o Qodana

| Cheiro (Fowler) | Inspeção no IDE | ID da inspeção | Métrica / limiar | Limitações |
|---|---|---|---|---|
| God Class | Overly complex class | `ClassComplexity` | WMC — soma da complexidade ciclomática dos métodos da classe; `m_limit: 47` (por omissão: 80) | O livro exige ATFD > FEW **e** WMC ≥ VERY HIGH **e** TCC < 1/3; o Qodana só mede o WMC, não tem ATFD nem TCC, não permite combinar condições com AND e usa um limiar absoluto |

| Propriedade | Valor |
|---|---|
| Code Smell | God Class |
| Inspeção IntelliJ/Qodana | Overly complex class |
| ID da inspeção | `ClassComplexity` |
| Métrica | WMC (Weighted Methods per Class), calculada como a soma da complexidade ciclomática dos métodos |
| Limite configurado | 47 |
| Opção | `m_limit: 47` |
| Linter | `jetbrains/qodana-jvm-community:2026.2` |

O valor de 47 corresponde ao limiar "muito alto" de WMC para Java em *Object-Oriented Metrics in Practice*, que é usado na estratégia de deteção da God Class. O valor por omissão da inspeção no IDE é 80.

### Limitações da inspeção

A estratégia de deteção do livro combina três condições: a classe acede diretamente a dados de outras classes (ATFD > FEW), tem complexidade muito alta (WMC ≥ VERY HIGH) e tem baixa coesão (TCC < 1/3).

O Qodana for JVM só tem uma inspeção equivalente à segunda condição. Não existem inspeções para ATFD nem para TCC, e não é possível combinar várias inspeções com uma condição AND. Além disso, o limite é absoluto e não relativo à distribuição das métricas no sistema. Por isso, a configuração usada é uma aproximação da estratégia do livro.

## 3. Configuração do Qodana

Foram criados ficheiros individuais identificados com o número de aluno 98455.

| Ficheiro | Finalidade |
|---|---|
| `../qodana-98455.yaml` | Configuração principal do Qodana |
| `../.qodana/profiles/profile-98455.yaml` | Perfil da inspeção God Class |
| `../.github/workflows/qodana-smell-98455.yaml` | Workflow GitHub Actions |
| `README-98455.md` | Documentação e evidências |

### 3.1. Configuração principal

Ficheiro `../qodana-98455.yaml`:

```yaml
# 98455 - Carmen Gonzalez
version: "1.0"
linter: jetbrains/qodana-jvm-community:2026.2
projectJDK: "21"
profile:
  path: .qodana/profiles/profile-98455.yaml
failureConditions:
  severityThresholds:
    any: 0
```

Esta configuração usa o Qodana JVM Community 2026.2, define o JDK 21 (a versão do `pom.xml`) e aponta para o perfil personalizado de inspeções.

A opção `severityThresholds.any: 0` define tolerância zero: basta um problema para o job falhar.

### 3.2. Perfil da inspeção

Ficheiro `../.qodana/profiles/profile-98455.yaml`:

```yaml
# 98455 - Carmen Gonzalez
baseProfile: "empty"
name: "god-class-98455"
inspections:
  - inspection: ClassComplexity
    enabled: true
    severity: ERROR
    options:
      m_limit: 47
```

O perfil parte de um perfil vazio (`empty`) e ativa apenas a inspeção `ClassComplexity`, com um limite de 47.

**Nota:** o formato de perfil sugerido no enunciado (`profile:` → `base: name: empty`) foi ignorado pelo Qodana. Com esse formato, a ferramenta usou o perfil *Project Default* e reportou mais de 190 problemas de todo o tipo (Javadoc, imports não usados, etc.). O formato que funcionou foi o dos perfis oficiais da JetBrains (repositório `JetBrains/qodana-profiles`), com `baseProfile`, `name` e `inspections` no nível de topo do ficheiro.

### 3.3. Workflow GitHub Actions

Ficheiro `../.github/workflows/qodana-smell-98455.yaml`:

```yaml
# 98455 - Carmen Gonzalez - Qodana God Class
name: Qodana God Class 98455

on:
  pull_request:
  push:
  workflow_dispatch:

permissions:
  contents: read
  checks: write
  pull-requests: write

jobs:
  qodana:
    name: Qodana God Class 98455
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          ref: ${{ github.event.pull_request.head.sha || github.sha }}
          fetch-depth: 0

      - name: Run Qodana
        uses: JetBrains/qodana-action@v2026.2
        with:
          args: --config qodana-98455.yaml
          pr-mode: false
          use-annotations: true
          post-pr-comment: true
```

O workflow corre automaticamente em eventos `push` e `pull_request` e também pode ser iniciado manualmente através de `workflow_dispatch`. A opção `pr-mode: false` faz com que o projeto seja sempre analisado na íntegra, e não apenas os ficheiros alterados.

## 4. Quality Gate

O objetivo do quality gate é detetar automaticamente classes com características de God Class.

Para a inspeção escolhida, pretende-se ter **zero classes com WMC acima de 47**.

Quando a análise encontra pelo menos uma classe acima do limite, o GitHub Actions apresenta uma falha (*"The number of problems exceeds the failThreshold"*). Quando não encontra nenhuma, o workflow termina com sucesso.

## 5. Identificação e refatorização dos problemas

A análise do projeto completo identificou duas classes acima do limite:

| Ficheiro | Classe | WMC inicial |
|---|---|---:|
| `../src/main/java/battleship/Game.java` | `Game` | 67 |
| `../src/main/java/battleship/Ship.java` | `Ship` | 51 |

### Refatorizações realizadas

**Game.java:** foi aplicada a refatorização **Extract Class**. A lógica de desenho do tabuleiro (`printBoard`, `printShips`, `printBoardShots`, `printBoardFrame` e os marcadores) passou para a nova classe `BoardPrinter`, e `Game.printBoard` passou a delegar nessa classe. O método `randomEnemyFire` foi simplificado, substituindo dois ciclos duplicados por `Collections.shuffle`, e `repeatedShot` passou a usar `anyMatch`.

**Ship.java:** o `switch` de `buildShip` foi substituído por um mapa que associa cada tipo de navio ao respetivo construtor. Os ciclos de `getTopMostPos`, `getBottomMostPos`, `getLeftMostPos`, `getRightMostPos`, `stillFloating`, `occupies` e `tooCloseTo` foram substituídos por streams.

| Classe | WMC antes | WMC depois (aprox.) |
|---|---:|---:|
| `Game` | 67 | ~39 |
| `Ship` | 51 | ~31 |
| `BoardPrinter` (nova) | — | ~24 |

Estas alterações reduziram a complexidade das duas classes para valores abaixo do limite, mantendo o comportamento do jogo.

## 6. Evidências das execuções

### 6.1. Execução vermelha — Failure

No ramo `God-Class-98455`, as versões originais de `Game` e `Ship` foram repostas deliberadamente para provocar o cheiro. O quality gate falhou e apontou as duas classes (`Game` com WMC 67 e `Ship` com WMC 51).

**[Execução vermelha — GitHub Actions](https://github.com/LEI-129373/Battleship2/actions/runs/42414133460)**

### 6.2. Execução verde — Success

Depois de repor as classes refatorizadas no mesmo ramo, o workflow terminou com sucesso.

**[Execução verde — GitHub Actions](https://github.com/LEI-129373/Battleship2/actions/runs/42414282346)**

### 6.3. Comparação

| Estado | Resultado | Evidência |
|---|---|---|
| Com o cheiro provocado | Failure | [Execução vermelha](https://github.com/LEI-129373/Battleship2/actions/runs/42414133460) |
| Após a correção | Success | [Execução verde](https://github.com/LEI-129373/Battleship2/actions/runs/42414282346) |

## 7. Proteção da branch main

Foi configurada uma regra de proteção para a branch `main` que exige que os status checks obrigatórios passem antes de permitir o merge de um Pull Request.

O check associado a esta tarefa é **Qodana God Class 98455**.

### 7.1. Demonstração com um Pull Request

Para testar a proteção, foi criada a branch `God-Class-98455`, onde o cheiro foi introduzido deliberadamente, e foi aberto o seguinte Pull Request para o `main`:

**[Pull Request #15 — God Class 98455](https://github.com/LEI-129373/Battleship2/pull/15)**

O Qodana identificou a violação da inspeção `ClassComplexity` e o quality gate ficou no estado **Failure**. A regra de proteção da branch `main` impediu o merge enquanto o check obrigatório estava vermelho.

### 7.2. Correção da violação

Depois da demonstração do erro, as classes refatorizadas foram repostas na branch, e uma nova execução do workflow confirmou que o quality gate voltou a passar.

Desta forma, foi demonstrado o processo de deteção, correção e reavaliação de uma violação do Code Smell God Class.

O Pull Request de teste não precisa de ser integrado na branch `main`, porque o seu objetivo é demonstrar o funcionamento do quality gate e da regra de proteção.

## 8. Reflexão sobre os resultados

O Qodana detetou duas classes acima do limiar: `Game` (WMC 67) e `Ship` (WMC 51). A primeira é um verdadeiro positivo, porque acumulava a lógica do jogo e o desenho do tabuleiro. A segunda aproxima-se de um falso positivo, já que a complexidade vinha de muitos métodos simples e não de baixa coesão ou de acesso a dados de outras classes. Pelo mesmo motivo podem existir falsos negativos, como classes com WMC moderado mas muito acopladas e pouco coesas, que esta inspeção não apanha. O resultado é muito sensível ao limiar: com o valor por omissão do IDE (80), nenhuma classe seria apontada, e com 47 são apontadas as duas. Comparada com a estratégia de *Object-Oriented Metrics in Practice*, esta configuração é apenas uma aproximação, porque usa uma única métrica com um limite absoluto em vez de combinar três métricas com limiares relativos.

## 9. Conclusão

Esta tarefa permitiu aplicar o Qodana à identificação automática do Code Smell **God Class** no projeto Battleship2.

Foi estabelecida a correspondência entre o Code Smell de Martin Fowler e a inspeção `ClassComplexity`, com um limite de 47 baseado nos valores do livro.

A refatorização **Extract Class** e a simplificação de métodos reduziram a complexidade das classes identificadas para valores abaixo do limite.

A integração com o GitHub Actions permitiu demonstrar o funcionamento do quality gate através de uma execução com falha e de outra com sucesso.

Por fim, a proteção da branch `main` e o Pull Request #15 permitiram testar o bloqueio de alterações que não satisfazem o quality gate.
