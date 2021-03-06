package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BinaryOperator;

import javax.swing.JPanel;

import game.entities.Enemy;
import game.entities.Entity;
import game.entities.GreedyEnemy;
import game.entities.Player;
import graphs.CheapestPath;
import graphs.GraphMatrix;
import graphs.Position;

public class Panel extends JPanel implements MouseListener, MouseMotionListener {
	private class GreedyCheapestPath implements Comparable<GreedyCheapestPath> {
		private final GreedyEnemy greedyEnemy;
		private final CheapestPath<Position, Integer> path;
		private final Integer weight;
		private final Integer value;
		private final Double specificValue;
		private final Boolean valid;

		GreedyCheapestPath(GreedyEnemy greedyEnemy, Player player) {
			this.greedyEnemy = greedyEnemy;
			this.path = grid.dijkstra(new Position(greedyEnemy.getGridX(), greedyEnemy.getGridY()),
					new Position(player.getGridX(), player.getGridY()));
			this.weight = this.path == null ? null : path.getPath().size();
			this.value = this.path == null ? null : path.getTotalCost();
			this.specificValue = this.path == null ? null : (double) value / (double) weight;
			this.valid = this.path == null ? false : true;
		}

		/**
		 * @return the greedyEnemy
		 */
		GreedyEnemy getGreedyEnemy() {
			return greedyEnemy;
		}

		/**
		 * @return the path
		 */
		CheapestPath<Position, Integer> getPath() {
			return path;
		}

		/**
		 * @return the weight
		 */
		Integer getWeight() {
			return weight;
		}

		/**
		 * @return the specificValue
		 */
		Double getSpecificValue() {
			return specificValue;
		}

		/**
		 * @return the valid
		 */
		Boolean getValid() {
			return valid;
		}

		@Override
		public int compareTo(GreedyCheapestPath o) {
			return Double.compare(this.getSpecificValue(), o.getSpecificValue());
		}
	}

	private static final long serialVersionUID = 1L;
	private static final Integer FORBIDDEN = -1;
	private static final Integer EMPTY = 0;
	private static final Integer VISITED = 1;
	private static int WIDTH = 500;
	private static int HEIGHT = 500;
	private Player player;
	private Map map;
	private List<Position> preview;
	private GraphMatrix<Integer, Integer> grid;
	private boolean running;
	private int lastMouseX;
	private int lastMouseY;
	private boolean inPlayer;
	private int moveCost;
	private int tileSize;

	private final int initialCost = 1;
	private final int minimumCost = 0;
	private final int maximumCost = Integer.MAX_VALUE;

	private List<Enemy> enemies = new ArrayList<Enemy>();
	private List<GreedyEnemy> greedyEnemies = new ArrayList<GreedyEnemy>();
	private List<Entity> allEnemies = new ArrayList<Entity>();

	private int sizeX;
	private int sizeY;
	final private int playerMoves = 5;
	final private Comparator<Integer> costComparator = new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			if (o1 < o2) {
				return -1;
			}

			if (o1 > o2) {
				return 1;
			}

			return 0;
		}
	};
	final private BinaryOperator<Integer> costAdder = (Integer a, Integer b) -> a + b;

	private int enemyMoves = 3;
	private int greedyArmyMoveBudget = enemyMoves;

	int rounds = 0;

	public Panel(int size) {

		// Configura????es do Painel
		setFocusable(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));

		// Mouse Listeners
		addMouseListener(this);
		addMouseMotionListener(this);

		// Define os tamanhos
		sizeX = size;
		sizeY = size;
		tileSize = HEIGHT / size;

		// Inicializar Jogador
		int h = (int) (tileSize * 0.8); // 80% do tileSize
		int w = (int) (tileSize * 0.8); // 80% do tileSize
		int off = (tileSize - w) / 2; 	// Meio do tile
		player = new Player(playerMoves, 5, 5, tileSize, off, h, w, Color.BLUE);

		// Inicializar inimigos comuns
		h = (int) (tileSize * 0.6); // 60% do tileSize
		w = (int) (tileSize * 0.6); // 60% do tileSize
		off = (tileSize - w) / 2; 	// Meio do tile
		enemies.add(new Enemy(enemyMoves, 10, 10, tileSize, off, h, w, Color.RED));
		enemies.add(new Enemy(enemyMoves, 15, 15, tileSize, off, h, w, Color.RED));
		enemies.add(new Enemy(enemyMoves, 10, 15, tileSize, off, h, w, Color.RED));

		// Inicializar inimigos ambiciosos
		h = (int) (tileSize * 0.95); // 95% do tileSize
		w = (int) (tileSize * 0.95); // 95% do tileSize
		off = (tileSize - w) / 2;	 // Meio do tile
		greedyEnemies.add(new GreedyEnemy(enemyMoves, 9, 9, tileSize, off, h, w, Color.RED));
		greedyEnemies.add(new GreedyEnemy(enemyMoves, 14, 14, tileSize, off, h, w, Color.RED));
		greedyEnemies.add(new GreedyEnemy(enemyMoves, 9, 14, tileSize, off, h, w, Color.RED));

		// Lista de inimigos
		allEnemies.addAll(enemies);
		allEnemies.addAll(greedyEnemies);

		// Inicializa Grafo do Mapa
		grid = new GraphMatrix<Integer, Integer>(sizeX, sizeY, EMPTY, VISITED, FORBIDDEN, initialCost, minimumCost,
				maximumCost, costComparator, costAdder);
		
		// Inicializa o Preview do movimento do Jogador
		preview = new ArrayList<Position>();

		// Cria dicion??rio de custo/cores
		HashMap<Integer, Color> hash = new HashMap<Integer, Color>();
		hash.put(initialCost, Color.GREEN);
		hash.put(initialCost + 1, Color.YELLOW);
		hash.put(initialCost + 2, Color.ORANGE);

		// Inicializa Mapa
		map = new Map(grid, hash, WIDTH, HEIGHT, sizeX, sizeY);
		addRandomCosts((sizeX * sizeY) / 2, hash.size()+1);
		addRandomForbidden(sizeX);
		grid.setElementCost(player.getGridX(), player.getGridY(), initialCost);
		grid.setElementValue(player.getGridX(), player.getGridY(), EMPTY);

		// Inicia o Jogo
		start();
	}

	// Altera o custo de at?? <number> casas aleat??rias
	private void addRandomCosts(int number, int max) {
		for (int i = 0; i < number; i++) {
			int randomX = ThreadLocalRandom.current().nextInt(0, sizeX);
			int randomY = ThreadLocalRandom.current().nextInt(0, sizeY);
			int randomCost = ThreadLocalRandom.current().nextInt(initialCost, max);
			grid.setElementCost(randomX, randomY, randomCost);
		}
	}

	// Adiciona at?? <number> obst??culos intranspon??veis
	private void addRandomForbidden(int number) {
		List<Entity> entities = new ArrayList<Entity>();
		entities.addAll(allEnemies);
		entities.add(player);

		for (int i = 0; i < number; i++) {
			Boolean acceptable = true;
			int randomX = ThreadLocalRandom.current().nextInt(0, sizeX);
			int randomY = ThreadLocalRandom.current().nextInt(0, sizeY);
			
			// Verifica sobreposi????o com as entidades
			for (Entity entity : entities) {
				if (checkOverride(randomX, randomY, entity.getGridX(), entity.getGridY())) {
					acceptable = false;
				}
			}
			// Apenas acrescenta o obst??culo caso a casa esteja livre
			if (acceptable) {
				grid.setElementValue(randomX, randomY, FORBIDDEN);
			}
		}
	}

	private void start() {
		running = true;
	}

	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;

		// Desenha a Grade
		map.draw(g2d);

		// Desenha a Preview do movimento do Jogador
		drawPreview(g2d);

		// Desenha o Jogador
		player.draw(g2d);

		// Desenha inimigos
		for (Entity enemy : allEnemies) {
			enemy.draw(g2d);
		}
	}

	public void stop() {
		running = false;
	}

	@Override
	public void mouseMoved(MouseEvent m) {
		stopOnTKO();

		// Coordenadas atuais do mouse na grade
		int mx = coordToGrid(m.getX());
		int my = coordToGrid(m.getY());

		// Atualiza as coordenadas do Mouse
		if (lastMouseX != mx || lastMouseY != my) {
			lastMouseX = mx;
			lastMouseY = my;
			if (mx == player.getGridX() && my == player.getGridY())
				inPlayer = true;
			else {
				inPlayer = false;
				try {
					encontraCaminho();
				} catch (ArrayIndexOutOfBoundsException e) {

				}
			}
			repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent m) {
		stopOnTKO();

		// Move o Jogador
		if (moveCost <= player.getMoves() && !inPlayer && !isForbidden(m)) {
			player.setGridX((m.getX() - 1) / tileSize);
			player.setGridY((m.getY() - 1) / tileSize);
			inPlayer = true;

			encontraCaminhoInimigos();

			rounds++;
			if (rounds % 10 == 0 && enemyMoves <= 2 * playerMoves)
				enemyMoves++;
			for (Enemy enemy : enemies) {
				enemy.setMoves(enemyMoves);
			}

			for (Entity enemy : allEnemies) {
				if (enemy.getGridX().equals(player.getGridX()) && enemy.getGridY().equals(player.getGridY())) {
					stop();
				}
			}
			repaint();
		}
	}

	@Override
	public void mouseEntered(MouseEvent m) {
		mouseMoved(m);
	}

	@Override
	public void mouseExited(MouseEvent m) {
		stopOnTKO();
		inPlayer = true;
		lastMouseX = player.getGridX();
		lastMouseY = player.getGridY();
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent m) {
	}

	@Override
	public void mouseReleased(MouseEvent m) {
	}

	@Override
	public void mouseDragged(MouseEvent m) {
	}

	// Desenha a preview do movimento do Jogador
	private void drawPreview(Graphics2D g) {
		int x = -1;
		int y = -1;
		moveCost = 0;
		g.setColor(Color.RED);
		if (!preview.isEmpty() && !inPlayer) {
			for (Position e : preview) {
				if (moveCost > player.getMoves())
					break;
				if (x != -1 && y != -1) {
					if (grid.getElementCost(e) + moveCost <= player.getMoves())
						g.drawLine(gridToCoord(e.getPosX()) + tileSize / 2, gridToCoord(e.getPosY()) + tileSize / 2,
								gridToCoord(x) + tileSize / 2, gridToCoord(y) + tileSize / 2);
					moveCost += grid.getElementCost(e);
				}
				x = e.getPosX();
				y = e.getPosY();

			}
		}

	}

	private boolean isForbidden(MouseEvent m) {
		if (grid.getElementValue(coordToGrid(m.getX()), coordToGrid(m.getY())).equals(grid.getFORBIDDEN()))
			return true;
		else
			return false;
	}

	private List<Position> cheapestPathToList(CheapestPath<Position, Integer> cpt) {
		if (cpt == null) {
			return new ArrayList<Position>();
		}

		return cpt.getPath();
	}

	private void stopOnTKO() {
		grid.setVisitedToEmpty();
		// Ajuda a n??o entrar nos inimigos
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), FORBIDDEN);
		}

		List<Position> ps = grid.visitableNeighbours(player.getGridX(), player.getGridY());

		if (ps.isEmpty()) {
			stop();
		}

		// Reverter modifica????o
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), EMPTY);
		}
	}

	private void encontraCaminho() {
		grid.setVisitedToEmpty();

		// Ajuda a n??o entrar nos inimigos
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), FORBIDDEN);
		}

		CheapestPath<Position, Integer> cpt = grid.dijkstra(new Position(player.getGridX(), player.getGridY()),
				new Position(lastMouseX, lastMouseY));
		preview = cheapestPathToList(cpt);

		// Reverter modifica????o
		for (Entity e : allEnemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), EMPTY);
		}

		grid.setVisitedToEmpty();
	}

	private void encontraCaminhoInimigos() {
		grid.setVisitedToEmpty();

		// Caminho dos inimigos comuns
		encontraCaminhoInimigosComuns();

		// Caminho dos inimigos Greedy
		encontraCaminhoInimigosGreedy();
	}

	private void encontraCaminhoInimigosComuns() {
		for (Enemy enemy : enemies) {
			// Impedir inimigos de entrarem uns nos outros
			for (Enemy otherEnemy : enemies) {
				grid.setElementValue(otherEnemy.getGridX(), otherEnemy.getGridY(), FORBIDDEN);
			}

			grid.setElementValue(enemy.getGridX(), enemy.getGridY(), EMPTY);

			List<Position> caminho = cheapestPathToList(grid.dijkstra(new Position(enemy.getGridX(), enemy.getGridY()),
					new Position(player.getGridX(), player.getGridY())));

			// Caminho do inimigo
			if (!caminho.isEmpty()) {
				int cost = 0;
				Position r = new Position(enemy.getGridX(), enemy.getGridY());
				int enemyTileCost = grid.getElementCost(r);
				for (Position p : caminho) {
					cost += grid.getElementCost(p);
					if (cost > enemy.getMoves() + enemyTileCost)
						break;
					else if (p.getPosX().equals(player.getGridX()) && p.getPosY().equals(player.getGridY())) {
						r = p;
						break;
					} else
						r = p;
				}
				enemy.setGridX(r.getPosX());
				enemy.setGridY(r.getPosY());
			}

			// Reverter mudan??a
			for (Enemy otherEnemy : enemies) {
				grid.setElementValue(otherEnemy.getGridX(), otherEnemy.getGridY(), EMPTY);
			}

			grid.setVisitedToEmpty();
		}
	}

	/**
	 * Encontra caminho para os inimigos ambiciosos usando o algoritmo da mochila
	 * com itens divis??veis (fractional knapsack) O algoritmo considera como peso o
	 * n??mero de casas a mover, o valor ?? o custo do movimento (quanto mais alto o
	 * custo, mais perto do jogador, pois o caminho tra??ado ?? o mais curto), e o
	 * valor espec??fico ?? a divis??o entre o valor e o peso (?? mais valioso um
	 * movimento que chegue o mais perto do jogador no menor n??mero de casas)
	 */
	private void encontraCaminhoInimigosGreedy() {
		grid.setVisitedToEmpty();

		Integer maxWeight = greedyArmyMoveBudget;

		// used moves
		Integer currWeight = 0;

		List<GreedyCheapestPath> items = new ArrayList<GreedyCheapestPath>();
		for (GreedyEnemy enemy : greedyEnemies) {
			// Impedir inimigos de entrarem uns nos outros
			for (GreedyEnemy otherEnemy : greedyEnemies) {
				grid.setElementValue(otherEnemy.getGridX(), otherEnemy.getGridY(), FORBIDDEN);
			}
			grid.setElementValue(enemy.getGridX(), enemy.getGridY(), EMPTY);

			GreedyCheapestPath item = new GreedyCheapestPath(enemy, player);

			if (item.getValid()) {
				items.add(item);
			}

			// Reverter mudan??a
			for (GreedyEnemy otherEnemy : greedyEnemies) {
				grid.setElementValue(otherEnemy.getGridX(), otherEnemy.getGridY(), EMPTY);
			}
		}

		items.sort(null);

		for (GreedyCheapestPath item : items) {
			Integer lastPos;
			Integer x;
			Integer y;
			if (currWeight + item.getWeight() <= maxWeight) {
				lastPos = item.getPath().getPath().size() - 1;
				x = item.getPath().getPath().get(lastPos).getPosX();
				y = item.getPath().getPath().get(lastPos).getPosY();
				item.getGreedyEnemy().setGridX(x);
				item.getGreedyEnemy().setGridY(y);
				currWeight += item.getWeight();
			} else {
				Integer remainder = maxWeight - currWeight;
				lastPos = item.getPath().getPath().size() > remainder ? remainder : item.getPath().getPath().size();
				x = item.getPath().getPath().get(lastPos).getPosX();
				y = item.getPath().getPath().get(lastPos).getPosY();
				item.getGreedyEnemy().setGridX(x);
				item.getGreedyEnemy().setGridY(y);
				break;
			}
		}

		grid.setVisitedToEmpty();
	}

	private boolean checkOverride(int x1, int y1, int x2, int y2) {
		return (x1 == x2 && y1 == y2);
	}

	private int gridToCoord(int v) {
		return v * tileSize;
	}

	private int coordToGrid(int v) {
		return (v - 1) / tileSize;
	}

	public boolean getRunning() {
		return running;
	}

	public int getScore() {
		return rounds;
	}
}
