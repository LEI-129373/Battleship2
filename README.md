# ⚓ Battleship 2.0

![Java CI with Maven](https://github.com/LEI-129373/Battleship2/actions/workflows/maven.yml/badge.svg)
![Java Version](https://img.shields.io/badge/Java-17%2B-blue)
![License](https://img.shields.io/badge/license-MIT-green)

> A modern take on the classic naval warfare game, designed for the XVII century setting with updated software engineering patterns.
# 📼 Vídeo
https://youtu.be/-Jo4VVeibHw

## 👥 Equipa
**Grupo Tp03_LEI-PL-4**

**Nickname:** Tp03_LEI-PL-4  
**Curso:** Licenciatura em Engenharia Informática em Pós-Laboral

| Nome | Nº de Aluno |
|:---:|:---:|
| Aloísio Ferreira | 129373 |
| Carmen González | 98455 |
| Carolina Andrade | 124173 |
| Cíntia Brandão | 123282 |
---

## ⚙️ Integração contínua (GitHub Actions)

O projeto usa o workflow **Java CI with Maven**, definido em [`.github/workflows/maven.yml`](.github/workflows/maven.yml), para executar automaticamente a bateria de testes unitários JUnit.

**Quando é executado**
- Em cada `push` para **qualquer ramo** (incluindo os ramos de trabalho `UnitTests_XXXXXX` e `Refactoring_XXXXXX`);
- Em cada `pull request` dirigido ao ramo `main`.

**O que faz**
1. Cria uma máquina virtual `ubuntu-latest`;
2. Faz checkout do código do repositório;
3. Instala o JDK 17 (distribuição Temurin), com cache das dependências Maven;
4. Executa `mvn -B package`, que compila o projeto e corre todos os testes em `src/test/java` antes de gerar o `.jar`.

**Resultado**
- ✅ Se todos os testes passarem, o workflow termina com sucesso;
- ❌ Se algum teste falhar, o build é interrompido e o workflow falha, sinalizando que a alteração introduziu efeitos colaterais.

O estado da última execução aparece no badge no topo deste ficheiro, e o histórico completo está no separador [Actions](../../actions) do repositório.
