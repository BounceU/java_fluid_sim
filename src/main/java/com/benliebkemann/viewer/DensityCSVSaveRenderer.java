package com.benliebkemann.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.benliebkemann.simulation.Particle;
import com.benliebkemann.simulation.SimConstants;
import com.benliebkemann.simulation.SpatialHash;

public class DensityCSVSaveRenderer extends Renderer {

	private String filePath;
	private String name;
	private int numFrames;

	private Camera camera;

	private int width;
	private int height;

	public SpatialHash hash;

	public DensityCSVSaveRenderer(int width, int height, Camera camera, String filePath, String fileNames,
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
				int outColor = Math.clamp((int) (0 + density * 0.005), 0, 255);
				image.setRGB(x, y, new Color(outColor, outColor, outColor).getRGB());
			}
		}
		String finalName = String.format("%s%s%s%03d.csv", filePath, File.separator, name, numFrames);
		numFrames++;

		File newFile = new File(finalName);

		int[] nums = new int[200];

		String output = "";

		for (int j = 0; j < 200; j++) {
			nums[j] = 0;
			int numZero = 0;
			for (int k = 9; k < 200; k++) {
				if (new Color(image.getRGB(j, k)).getRed() == 0) {
					numZero++;
				} else {
					numZero = 0;
				}
				if (numZero >= 5) {
					nums[j] -= (numZero - 1);
					break;
				} else {
					nums[j] += 1;
				}
			}
		}

		for (int j = 0; j < 200; j++) {
			output = output + nums[j] + ",";
		}

		try {
			Files.writeString(newFile.toPath(), output);
		} catch (IOException e) {
			e.printStackTrace();
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
