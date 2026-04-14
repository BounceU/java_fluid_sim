package com.benliebkemann.simulation;

import java.util.ArrayList;
import java.util.List;

public class Simulation implements Runnable {

	private List<Particle> particles;

	private SpatialHash spatialHash;

	double simulationTime;

	Vector2D temp1;
	Vector2D temp2;

	Vector2D pressureAcceleration;
	Vector2D viscosityAcceleration;
	Vector2D colorGradient;
	Vector2D pressureGradient;

	public Simulation() {
		particles = new ArrayList<>();
		temp1 = new Vector2D(0.0, 0.0);
		temp2 = new Vector2D(0.0, 0.0);
		pressureAcceleration = new Vector2D(0.0, 0.0);
		viscosityAcceleration = new Vector2D(0.0, 0.0);
		colorGradient = new Vector2D(0.0, 0.0);
		pressureGradient = new Vector2D(0.0, 0.0);
		spatialHash = new SpatialHash(SimConstants.H);
		simulationTime = 0.0;
		initParticles();
	}

	private void initParticles() {
		double spacing = SimConstants.H / 2.0;
		for (double y = 0.01; y < 0.06; y += spacing) {
			for (double x = 0.1; x < 0.3; x += spacing) {
				particles.add(new Particle(x, y));
			}
		}
	}

	@Override
	public void run() {

		simulationTime = 0.0;
		int frameCount = 0;

		while (true) {

			double maxVelocity = getMaximumParticleVelocity();
			double cflDt = 0.4 * (SimConstants.H / (SimConstants.SPEED_OF_SOUND + maxVelocity));

			double dt = Math.min(cflDt, 0.001);

			updatePhysics(dt);
			simulationTime += dt;

		}

	}

	private double getMaximumParticleVelocity() {
		double maxVelocity = 0.0;
		for (Particle p : particles) {
			double v = p.velocity.mag();
			if (v > maxVelocity)
				maxVelocity = v;
		}

		return maxVelocity;
	}

	private void updatePhysics(double dt) {

		double[] zeros = { 0.0, 0.0 };

		spatialHash.clear();
		for (Particle p : particles) {
			spatialHash.insert(p);
		}

		// Pass 1, density/pressure

		for (Particle p1 : particles) {
			p1.density = SimConstants.MASS * poly6Kernel(0.0, SimConstants.H);

			spatialHash.getNeighbors(p1, p1.neighbors);

			for (Particle p2 : p1.neighbors) {
				if (p1 == p2)
					continue;

				temp1.set(p1.position);
				temp1.sub(p2.position);
				double dist = temp1.mag();

				if (dist < SimConstants.H) {
					p1.density += SimConstants.MASS * poly6Kernel(dist, SimConstants.H);
				}
			}

			double densityRatio = p1.density / SimConstants.REST_DENSITY;
			p1.pressure = SimConstants.B * (Math.pow(densityRatio, 7.0) - 1.0);

			if (p1.pressure < 0.0)
				p1.pressure = 0.0;

		}

		// Pass 2, forces
		for (Particle p1 : particles) {
			pressureAcceleration.x = 0;
			pressureAcceleration.y = 0;
			viscosityAcceleration.x = 0;
			viscosityAcceleration.y = 0;
			colorGradient.x = 0;
			colorGradient.y = 0;
			double colorLaplacian = 0.0;

			for (Particle p2 : p1.neighbors) {
				if (p1 == p2)
					continue;

				temp1.set(p1.position);
				temp1.sub(p2.position);
				double dist = temp1.mag();

				if (dist < SimConstants.H) {
					// Pressure acceleration, a = -m_2 * (p_1/rho_1^2 + p_2/rho_2^2) * gradW
					spikyKernelGradient(temp1, dist, SimConstants.H, pressureGradient);
					double pTerm = (p1.pressure / (p1.density * p1.density))
							+ (p2.pressure / (p2.density * p2.density));
					pressureGradient.scale(-SimConstants.MASS * pTerm);
					pressureAcceleration.add(pressureGradient);

					// Viscosity acceleration, a = (mu * m_2 / (rho_1 * rho_2)) * lapW * (v_2 - v_1)
					temp2.set(p2.velocity);
					temp2.sub(p1.velocity);
					double viscosityLaplacian = viscosityKernelLaplacian(dist, SimConstants.H);
					temp2.scale(SimConstants.VISCOSITY * SimConstants.MASS * viscosityLaplacian
							/ (p1.density * p2.density));
					viscosityAcceleration.add(temp2);

					poly6KernelGradient(temp1, dist, SimConstants.H, temp2);
					temp2.scale(SimConstants.MASS / p2.density);
					colorGradient.add(temp2);
					colorLaplacian += (SimConstants.MASS / p2.density) * poly6KernelLaplacian(dist, SimConstants.H);
				}
			}

			// Gravity acceleration, a = g
			p1.force.x = 0.0;
			p1.force.y = SimConstants.GRAVITY;
			p1.force.add(pressureAcceleration);
			p1.force.add(viscosityAcceleration);

			// Surface Tension, a = (sigma / rho_1) * kappa * n
			double gradientMagnitude = colorGradient.mag();
			if (gradientMagnitude > 0.01) {
				temp1.set(colorGradient);
				temp1.scale(-1.0 / gradientMagnitude);
				temp1.scale((SimConstants.SURFACE_TENSION * colorLaplacian) / p1.density);
				p1.force.add(temp1);
			}
		}

		// Pass 3
		for (Particle p1 : particles) {

			if (p1.position.y <= 0.015) {
				p1.velocity.x = 0;
				p1.velocity.y = 0;
				continue;
			}

			temp1.set(p1.force);
			temp1.scale(dt);
			p1.velocity.add(temp1);

			temp2.set(p1.velocity);
			temp2.scale(dt);
			p1.position.add(temp2);
		}

	}

	// https://matthias-research.github.io/pages/publications/sca03.pdf

	private double poly6Kernel(double rMag, double h) {

		if (rMag < 0 || rMag > h) {
			return 0.0;
		}

		double h2 = h * h;
		double r2 = rMag * rMag;
		double coefficient = 4.0 / (Math.PI * Math.pow(h, 8)); // 2D

		return coefficient * Math.pow(h2 - r2, 3);
	}

	private void poly6KernelGradient(Vector2D r, double dist, double h, Vector2D target) {
		if (dist <= 0.0001 || dist >= h) {
			target.x = 0;
			target.y = 0;
			return;
		}

		target.set(r);
		double h2 = h * h;
		double r2 = dist * dist;
		double coefficient = -(24.0 / (Math.PI * Math.pow(h, 8))) * Math.pow(h2 - r2, 2); // 2D
		target.scale(coefficient);
	}

	private double poly6KernelLaplacian(double rMag, double h) {
		if (rMag < 0 || rMag >= h) {
			return 0.0;
		}

		double h2 = h * h;
		double r2 = rMag * rMag;
		double coefficient = -(48.0 / (Math.PI * Math.pow(h, 8))); // 2D

		return coefficient * (h2 - r2) * (h2 - 3.0 * r2);

	}

	private void spikyKernelGradient(Vector2D r, double dist, double h, Vector2D target) {
		if (dist <= 0.0001 || dist >= h) {
			target.x = 0;
			target.y = 0;
			return;
		}

		double coefficient = -(30.0 / (Math.PI * Math.pow(h, 5))) * Math.pow(h - dist, 2) / dist; // 2D

		target.set(r);
		target.scale(coefficient);

	}

	private double viscosityKernelLaplacian(double rMag, double h) {
		if (rMag < 0 || rMag >= h) {
			return 0.0;
		}
		double coefficient = 20.0 / (Math.PI * Math.pow(h, 5)); // 2D
		return coefficient * (h - rMag);
	}

}
