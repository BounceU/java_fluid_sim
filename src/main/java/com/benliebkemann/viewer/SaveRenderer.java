package com.benliebkemann.viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.benliebkemann.simulation.Particle;

public class SaveRenderer extends Renderer {

	private String filePath;
	private String name;
	private int numFrames;

	private Camera camera;

	private int width;
	private int height;

	public SaveRenderer(int width, int height, Camera camera, String filePath, String fileNames, Renderer next) {
		super(next);
		this.filePath = filePath;
		this.name = fileNames;
		this.camera = camera;
		this.numFrames = 0;
		this.width = width;
		this.height = height;
	}

	@Override
	protected void render(List<Particle> particles) {
		super.render(particles);

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = image.createGraphics();

		g2d.setBackground(Color.black);

		g2d.setColor(Color.red);
		g2d.fillRect(camera.toScreenX(0), camera.toScreenY(0), camera.toScreenX(0.2), camera.toScreenY(0.0105));

		g2d.setColor(Color.blue.brighter());

		for (Particle p : particles) {
			g2d.fillOval(camera.toScreenX(p.position.x) - 1, camera.toScreenY(p.position.y) - 1, 2, 2);
		}

		String finalName = filePath + File.separator + name + numFrames + ".png";
		numFrames++;

		File newFile = new File(finalName);

		try {
			boolean result = ImageIO.write(image, "png", newFile);
		} catch (IOException e) {
			System.err.println("Error saving image: " + e.getMessage());
		}

	}

}
