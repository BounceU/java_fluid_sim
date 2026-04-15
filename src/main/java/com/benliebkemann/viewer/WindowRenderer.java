package com.benliebkemann.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

import com.benliebkemann.simulation.Particle;

public class WindowRenderer extends Renderer {

	private Camera camera;

	private int width;
	private int height;

	public ImagePanel panel;

	public WindowRenderer(int width, int height, Camera camera, Renderer next) {
		super(next);
		this.camera = camera;
		this.width = width;
		this.height = height;
		this.panel = new ImagePanel();
	}

	@Override
	protected void render(List<Particle> particles) {
		// super.render(particles);

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = image.createGraphics();

		g2d.setBackground(Color.black);

		g2d.setColor(Color.red);
		g2d.fillRect(camera.toScreenX(0), camera.toScreenY(0), camera.toScreenX(0.2), camera.toScreenY(0.0105));

		g2d.setColor(Color.blue.brighter());

		for (Particle p : particles) {
			g2d.fillOval(camera.toScreenX(p.position.x) - 1, camera.toScreenY(p.position.y) - 1, 2, 2);
		}

		panel.image = image;
		panel.repaint();
	}

	public class ImagePanel extends JPanel {

		BufferedImage image;

		public ImagePanel() {
			super();
			this.image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			System.out.println("Painting!");
		}
	}

}
