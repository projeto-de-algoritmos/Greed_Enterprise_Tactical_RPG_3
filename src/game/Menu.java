package game;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Menu {
	private JFrame frame;
	private boolean started = false;

	public JFrame getFrame() {
		return frame;
	}

	public boolean isStarted() {
		return started;
	}

	public Menu() {

		frame = new JFrame();
		JPanel panel = new JPanel();
		JButton startButton = new JButton("Start");

		frame.setSize(500, 500);

		startButton.setActionCommand("Start");
		startButton.addActionListener(new EventoBotao());
		startButton.setPreferredSize(new Dimension(500, 500));

		panel.setSize(500, 500);
		panel.add(startButton);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Enterprise Tactical RPG");
		frame.add(panel);
		frame.pack();
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);

		frame.setVisible(true);
	}

	private class EventoBotao implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String comando = e.getActionCommand();
			if (comando.equals("Start")) {
				started = true;
			}
		}
	}

}
