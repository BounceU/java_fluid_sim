package com.benliebkemann.simulation;

import org.ejml.simple.SimpleMatrix;

public class Particle {

	SimpleMatrix position, velocity, force;

	double density, pressure;

	public Particle() {
		double[] zeros = { 0.0, 0.0 };
		this.position = new SimpleMatrix(zeros);
		this.velocity = new SimpleMatrix(zeros);
		this.force = new SimpleMatrix(zeros);

		this.density = 0.0;
		this.pressure = 0.0;
	}

}
