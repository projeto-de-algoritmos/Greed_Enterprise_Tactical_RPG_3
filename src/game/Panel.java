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
import game.entities.Player;
import graphs.CheapestPath;
import graphs.GraphMatrix;
import graphs.Position;

public class Panel extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private static final Integer FORBIDDEN = -1;
	private static final Integer EMPTY = 0;
	private static final Integer VISITED = 1;
	private static int WIDTH = 500;
	private static int HEIGHT = 500;
	private Entity player;
	private Map map;
	private List<Position> preview;
	private GraphMatrix<Integer, Integer> grid;
	private boolean running;
	private int lastMouseX;
	private int lastMouseY;
	private boolean inPlayer;
	private int moveCost;

	private final int initialCost = 1;
	private final int minimumCost = 0;
	private final int maximumCost = Integer.MAX_VALUE;

	private List<Enemy> enemies = new ArrayList<Enemy>();

	final private int sizeX = 20;
	final private int sizeY = 20;
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

	int rounds = 0;

	public Panel() {
		setFocusable(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		addMouseListener(this);
		addMouseMotionListener(this);

		player = new Player(playerMoves, 5, 5, 25, 6, 14, 14, Color.BLUE);
		enemies.add(new Enemy(enemyMoves, 10, 10, 25, 8, 10, 10, Color.RED));
		enemies.add(new Enemy(enemyMoves, 15, 15, 25, 8, 10, 10, Color.RED));
		enemies.add(new Enemy(enemyMoves, 10, 15, 25, 8, 10, 10, Color.RED));
		grid = new GraphMatrix<Integer, Integer>(sizeX, sizeY, EMPTY, VISITED, FORBIDDEN, initialCost, minimumCost,
				maximumCost, costComparator, costAdder);
		preview = new ArrayList<Position>();

		// Dicionário de custo/cores
		HashMap<Integer, Color> hash = new HashMap<Integer, Color>();
		hash.put(initialCost, Color.GREEN);
		hash.put(initialCost + 1, Color.YELLOW);
		hash.put(initialCost + 2, Color.ORANGE);

		map = new Map(grid, hash, WIDTH, HEIGHT, 20, 20);
		addRandomCosts(100, hash.size());
		addRandomForbidden(20);

		grid.setElementCost(player.getGridX(), player.getGridY(), initialCost);
		grid.setElementValue(player.getGridX(), player.getGridY(), EMPTY);

		start();
	}

	// Altera o custo de até <number> casas aleatórias(Temporário)
	private void addRandomCosts(int number, int max) {
		for (int i = 1; i <= number; i++) {
			int randomX = ThreadLocalRandom.current().nextInt(0, 20);
			int randomY = ThreadLocalRandom.current().nextInt(0, 20);
			int randomCost = ThreadLocalRandom.current().nextInt(2, 4);
			grid.setElementCost(randomX, randomY, randomCost);
		}
	}

	// Adiciona até <number> obstáculos intransponíveis(Temporário)
	private void addRandomForbidden(int number) {
		for (int i = 1; i <= number; i++) {
			int randomX = ThreadLocalRandom.current().nextInt(0, 20);
			int randomY = ThreadLocalRandom.current().nextInt(0, 20);
			if (checkOverride(randomX, randomY, player.getGridX(), player.getGridY()))
				continue;
			for(Enemy enemy : enemies)
				if (checkOverride(randomX, randomY, enemy.getGridX(), enemy.getGridY()))
						continue;
			grid.setElementValue(randomX, randomY, FORBIDDEN);
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

		// Desenha as linhas de caminho
		drawPreview(g2d);

		// Desenha o Jogador
		player.draw(g2d);

		// Desenha inimigos
		for (Enemy enemy : enemies) {
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
			player.setGridX((m.getX() - 1) / 25);
			player.setGridY((m.getY() - 1) / 25);
			inPlayer = true;

			encontraCaminhoInimigos();

			rounds++;
			if (rounds % 10 == 0 && enemyMoves <= 2 * playerMoves)
				enemyMoves++;
			for (Enemy enemy : enemies) {
				enemy.setMoves(enemyMoves);
			}

			for (Enemy enemy : enemies) {
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

	private void drawPreview(Graphics2D g) {
		int x = -1;
		int y = -1;
		moveCost = 0;
		g.setColor(Color.RED);
		if (!preview.isEmpty() && !inPlayer) {
			for (Position e : preview) {
				if (moveCost > player.getMoves())
					break;
				if (x != -1 && y != -1){
					if(grid.getElementCost(e)+ moveCost <= player.getMoves())
						g.drawLine(gridToCoord(e.getPosX()) + 12, gridToCoord(e.getPosY()) + 12, gridToCoord(x) + 12,
								gridToCoord(y) + 12);
					moveCost+= grid.getElementCost(e);
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
		// Ajuda a não entrar nos inimigos
		for (Enemy e : enemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), FORBIDDEN);
		}
		
		List<Position> ps = grid.visitableNeighbours(player.getGridX(), player.getGridY());
		
		if (ps.isEmpty()) {
			stop();
		}
		
		// Reverter modificação
		for (Enemy e : enemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), EMPTY);
		}
	}

	private void encontraCaminho() {
		grid.setVisitedToEmpty();

		// Ajuda a não entrar nos inimigos
		for (Enemy e : enemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), FORBIDDEN);
		}

		CheapestPath<Position, Integer> cpt = grid.dijkstra(new Position(player.getGridX(), player.getGridY()),
				new Position(lastMouseX, lastMouseY));
		preview = cheapestPathToList(cpt);

		// Reverter modificação
		for (Enemy e : enemies) {
			grid.setElementValue(e.getGridX(), e.getGridY(), EMPTY);
		}

		grid.setVisitedToEmpty();
	}

	private void encontraCaminhoInimigos() {
		grid.setVisitedToEmpty();
		
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
				for(Position p : caminho) {
					cost += grid.getElementCost(p);
					if(cost > enemy.getMoves()+enemyTileCost)
						break;
					else if (p.getPosX() == player.getGridX() && p.getPosY() == player.getGridY()) {
						r = p;
						break;
					}
					else
						r = p;
				}
				enemy.setGridX(r.getPosX());
				enemy.setGridY(r.getPosY());
			}

			// Reverter mudança
			for (Enemy otherEnemy : enemies) {
				grid.setElementValue(otherEnemy.getGridX(), otherEnemy.getGridY(), EMPTY);
			}

			grid.setVisitedToEmpty();
		}
	}

	private boolean checkOverride(int x1, int y1, int x2, int y2) {
		return (x1 == x2 && y1 == y2);
	}

	private int gridToCoord(int v) {
		return v * 25;
	}

	private int coordToGrid(int v) {
		return (v - 1) / 25;
	}

	public boolean getRunning() {
		return running;
	}

	public int getScore() {
		return rounds;
	}
}
