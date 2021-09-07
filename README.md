# Enterprise Tactical RPG 3

**Número da Lista**: 25<br>
**Conteúdo da Disciplina**: Greed<br>

## Alunos

| Matrícula | Aluno |
| ---------- | -- |
| 15/0058462 |  Davi Antônio da Silva Santos |
| 18/0100840 |  Gabriel Azevedo Batalha |

## Sobre

Um jogo cujo objetivo é sobreviver o maior número de turnos. É uma
evolução do projeto de Grafos 1 e Grafos 2, agora permitindo alterar o
tamanho do mapa e com adição de inimigos *Greed*.

## Screenshots

![Menu](https://i.imgur.com/zl98ejb.png)
Menu


![Jogo em execução](https://i.imgur.com/W0z49qk.png)
Jogo em execução em mapa 20x20


![Jogo em execução](https://i.imgur.com/69KwnX7.png)
Jogo em execução em mapa 30x30



## Instalação 
**Linguagem**: Java<br>
**Framework**: Swing<br>

### Requisitos

- Java JRE 11 ou superior.
  - JDK 11 ou superior exigido para compilar ou desenvolver
- Computador com *mouse*.

## Uso 

Clone o repositório para compilar o projeto ou baixe somente o .jar
disponível nas [releases](https://github.com/projeto-de-algoritmos/Greed_Enterprise_Tactical_RPG_3/releases)

Para executar o programa, use
```
java -jar EnterpriseTacticalRPG3.jar
```
O jogo é controlado pelo *mouse*. Há a possibilidade de escolha do
tamanho do mapa até no máximo 30x30 e no mínimo 16x16 posições.

O jogador é um ponto azul na tela e deve fugir dos pontos vermelhos
inimigos. Os quadrados vermelhos são inimigos que seguem o jogador
independentemente, traçando o caminho de menor custo usando o algoritmo
de Dijkstra.

Já os círculos vermelhos são um exército que persegue o jogador usando
um algoritmo ganancioso. O exército ganancioso tem um número fixo de
casas que pode movimentar, e escolhe o inimigo que chega mais próximo
do jogador no menor número de casas.

As áreas são coloridas conforme os custos para atravessá-las. Regiões
verdes possuem o custo mais baixo, e quanto mais amarelo, mais alto o
custo. Regiões pretas são intransponíveis.

A partida termina quando o jogador é alcançado por qualquer um dos
inimigos ou quando não há movimentos válidos restantes.

## Desenvolvimento

Ao importar o projeto em sua IDE talvez seja necessário retirar o
.jar gerado do caminho do projeto. É possível que a IDE tente usar as
classes empacotadas no lugar das que estão definidas no código fonte.

## Outros

Os movimentos do jogador e dos inimigos agora são determinados pelo
algoritmo de Dijkstra implementado para traçar o caminho de menor custo
em uma matriz de elementos genéricos. Esse algoritmo também é usado
para traçar o caminho dos inimigos gananciosos. Também agora existem
casas com custos de travessia mais altos e obstáculos no mapa. Foram
realizadas otimizações no código anterior para se diminuir o alto uso
de CPU.