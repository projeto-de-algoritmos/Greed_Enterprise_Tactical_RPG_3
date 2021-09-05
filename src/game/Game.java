package game;

import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

public class Game {

	private boolean running;

	private JFrame frame;
	private Panel panel;
	private int score;

	public Game(int size) {
		this.setRunning(true);
		frame = new JFrame();
		panel = new Panel(size);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Enterprise Tactical RPG");
		frame.pack();
		frame.setSize(501, 531);
		frame.setLocationRelativeTo(null);
		frame.add(panel);
		frame.setVisible(true);
		frame.setResizable(false);

		score = 0;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void update() {
		try {
			TimeUnit.MILLISECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!panel.getRunning()) {
			setRunning(false);
			score = panel.getScore();
		}
	}

	public JFrame getFrame() {
		return frame;
	}

	public int getScore() {
		return score;
	}
}
