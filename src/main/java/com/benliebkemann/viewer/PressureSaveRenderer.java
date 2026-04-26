package com.benliebkemann.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import com.benliebkemann.simulation.Particle;
import com.benliebkemann.simulation.SimConstants;
import com.benliebkemann.simulation.SpatialHash;

public class PressureSaveRenderer extends Renderer {

	private String filePath;
	private String name;
	private int numFrames;

	private Camera camera;

	private int width;
	private int height;

	public SpatialHash hash;

	public PressureSaveRenderer(int width, int height, Camera camera, String filePath, String fileNames,
			Renderer next) {
		super(next);
		this.filePath = filePath;
		this.name = fileNames;
		this.camera = camera;
		this.numFrames = 0;
		this.width = width;
		this.height = height;
		this.hash = new SpatialHash(SimConstants.H);
	}

	@Override
	protected void render(List<Particle> particles) {

		// super.render(particles);

		BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = image.createGraphics();

		g2d.setBackground(new Color(128, 128, 128));

		g2d.clearRect(0, 0, width, height);

		hash.clear();
		for (Particle p : particles) {
			hash.insert(p);
		}

		List<Particle> particles2 = new ArrayList<Particle>();

		for (int y = 0; y < 200; y++) {
			for (int x = 0; x < 200; x++) {
				hash.getNeighbors(new Particle(0.001 * x, 0.001 * y), particles2);
				double pressure = 0.0;
				double density = 0.0;
				for (Particle p : particles2) {
					if (!p.isSolid) {
						pressure += p.pressure;
						density += p.density;
					}
				}
				int outColor = Math.clamp((int) (0 + pressure * 0.001), 0, 255);
				image.setRGB(x, y, new Color(outColor, outColor, outColor).getRGB());
			}
		}
		String finalName = String.format("%s%s%s%03d.png", filePath, File.separator, name, numFrames);
		numFrames++;

		File newFile = new File(finalName);

		try {
			boolean result = ImageIO.write(image, "png", newFile);
		} catch (IOException e) {
			System.err.println("Error saving image: " + e.getMessage());
		}

		g2d.dispose();
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
