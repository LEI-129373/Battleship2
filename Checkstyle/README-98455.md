# Tarefa 2B — Checkstyle (98455)

**Aluno:** 98455 — Carmen Gonzalez

## Regra escolhida

- **Categoria:** Nomes de constantes Java
- **Regra Checkstyle:** `ConstantName`
- **Ficheiro de configuração:** `checkstyle-98455.xml`
- **Workflow:** `.github/workflows/checkstyle-98455.yaml`

A regra aplica-se a campos `static final` e a campos de interfaces. Exige que os nomes das constantes estejam em maiúsculas, com palavras separadas por `_`, segundo o padrão `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`.

## Demonstração da violação

Foi criada a constante `maxShips` no ficheiro `src/main/java/battleship/Game.java`:

```java
public static final int maxShips = 11;
```

O Checkstyle detetou a violação na linha 71, porque o nome usava minúsculas em vez de maiúsculas com `_`.

- **Execução vermelha:** https://github.com/LEI-129373/Battleship2/actions/runs/42440175568
- **Execução verde após correção:** https://github.com/LEI-129373/Battleship2/actions/runs/42441913395
- **Pull Request:** https://github.com/LEI-129373/Battleship2/pull/19

A correção consistiu em alterar `maxShips` para `MAX_SHIPS`.

## Proteção da branch main

O check `Checkstyle ConstantName 98455` foi acrescentado à regra de proteção da branch `main`, que passou a exigir que este check passe antes de permitir o merge. No Pull Request com a violação, o merge ficou bloqueado enquanto o check estava vermelho.

## Limitações da regra

A regra `ConstantName` verifica apenas se os nomes das constantes respeitam o padrão definido, sem avaliar se são claros ou descritivos. Por exemplo, uma constante chamada `X` ou `VALOR_2` passa na verificação, apesar de o nome não explicar o que representa. Além disso, a regra só se aplica a campos já declarados como `static final`, por isso não deteta valores que deviam ser constantes e foram declarados como variáveis. Assim, o Checkstyle ajuda a garantir consistência na nomenclatura, mas não substitui a revisão humana do código.
