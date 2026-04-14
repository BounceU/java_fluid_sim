package com.benliebkemann.simulation;

import java.util.ArrayList;
import java.util.List;

public class Particle {

	Vector2D position, velocity, force;

	public List<Particle> neighbors = new ArrayList<>();

	double density, pressure;

	public Particle(double x, double y) {
		this.position = new Vector2D(x, y);
		this.velocity = new Vector2D(0.0, 0.0);
		this.force = new Vector2D(0.0, 0.0);

		this.density = 0.0;
		this.pressure = 0.0;
	}

}
