package com.benliebkemann.simulation;

import java.util.ArrayList;
import java.util.List;

import com.benliebkemann.viewer.Renderer;

public class Simulation implements Runnable {

	private List<Particle> particles;

	private SpatialHash spatialHash;

	private Renderer renderObserver;

	double simulationTime;

	Vector2D temp1;
	Vector2D temp2;

	Vector2D pressureAcceleration;
	Vector2D viscosityAcceleration;

	Vector2D fluidColorGradient;
	Vector2D solidColorGradient;

	Vector2D pressureGradient;

	private double wRef;

	public Simulation() {
		particles = new ArrayList<>();
		temp1 = new Vector2D(0.0, 0.0);
		temp2 = new Vector2D(0.0, 0.0);
		pressureAcceleration = new Vector2D(0.0, 0.0);
		viscosityAcceleration = new Vector2D(0.0, 0.0);
		fluidColorGradient = new Vector2D(0.0, 0.0);
		solidColorGradient = new Vector2D(0.0, 0.0);
		pressureGradient = new Vector2D(0.0, 0.0);
		spatialHash = new SpatialHash(SimConstants.H);
		simulationTime = 0.0;
		renderObserver = null;
		this.wRef = poly6Kernel(SimConstants.PARTICLE_SPACING, SimConstants.H);

		initParticles();
	}

	public void addRenderer(Renderer renderer) {
		this.renderObserver = renderer;
	}

	public void fireRender() {
		this.renderObserver.process(particles);
	}

	private void initParticles() {
		double spacing = SimConstants.PARTICLE_SPACING;

		for (double y = 0.008; y <= 0.010; y += spacing) {
			for (double x = 0.0; x <= 0.20; x += spacing) {
				Particle p = new Particle(x, y);
				p.isSolid = true;
				particles.add(p);
			}
		}

		for (double y = 0.010 + spacing / 2; y <= 0.010 + SimConstants.H * 6.0; y += spacing) {
			for (double x = 0.02; x <= 0.18; x += spacing) {
				Particle p = new Particle(x, y);
				double lambda = 0.03;
				p.velocity.y = 0.005 * Math.sin(2 * Math.PI * x / lambda);
				particles.add(p);
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

			System.out.println(particles.size() + "\t" + simulationTime);

			while (simulationTime > frameCount * 1.0 / 200) {
				System.out.println("Trying to render!");
				fireRender();
				frameCount++;

			}

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

		}

		// Pass 2, forces
		for (Particle p1 : particles) {
			pressureAcceleration.x = 0;
			pressureAcceleration.y = 0;
			viscosityAcceleration.x = 0;
			viscosityAcceleration.y = 0;
			fluidColorGradient.x = 0;
			fluidColorGradient.y = 0;
			solidColorGradient.x = 0;
			solidColorGradient.y = 0;
			double colorLaplacian = (SimConstants.MASS / p1.density) * poly6KernelLaplacian(0.0, SimConstants.H);

			for (Particle p2 : p1.neighbors) {
				if (p1 == p2)
					continue;

				temp1.set(p1.position);
				temp1.sub(p2.position);
				double dist = temp1.mag();

				if (dist < SimConstants.H) {
					// Pressure acceleration, a = -m_2 * (p_1/rho_1^2 + p_2/rho_2^2 + R_ij*f^4) *
					// gradW

					spikyKernelGradient(temp1, dist, SimConstants.H, pressureGradient);
					double pTerm = (p1.pressure / (p1.density * p1.density))
							+ (p2.pressure / (p2.density * p2.density));

					double Ri = (p1.pressure < 0) ? SimConstants.ARTIFICIAL_PRESSURE_EPS
							* Math.abs(p1.pressure) / (p1.density * p1.density) : 0.0;
					double Rj = (p2.pressure < 0) ? SimConstants.ARTIFICIAL_PRESSURE_EPS
							* Math.abs(p2.pressure) / (p2.density * p2.density) : 0.0;
					double fij = poly6Kernel(dist, SimConstants.H)
							/ wRef;
					double artificialPressure = (Ri + Rj) * Math.pow(fij, 4);

					pressureGradient.scale(-SimConstants.MASS * (pTerm + artificialPressure));
					pressureAcceleration.add(pressureGradient);

					// Viscosity acceleration, a = (mu * m_2 / (rho_1 * rho_2)) * lapW * (v_2 - v_1)
					temp2.set(p2.velocity);
					temp2.sub(p1.velocity);
					double viscosityLaplacian = viscosityKernelLaplacian(dist, SimConstants.H);
					temp2.scale(SimConstants.VISCOSITY * SimConstants.MASS * viscosityLaplacian
							/ (p1.density * p2.density));
					viscosityAcceleration.add(temp2);

					// Color gradient for surface tension and adhesion
					poly6KernelGradient(temp1, dist, SimConstants.H, temp2);
					temp2.scale(SimConstants.MASS / p2.density);

					if (p2.isSolid) {
						solidColorGradient.add(temp2);
					} else {
						fluidColorGradient.add(temp2);
						colorLaplacian += (SimConstants.MASS / p2.density) * poly6KernelLaplacian(dist, SimConstants.H);
					}
				}
			}

			// Gravity acceleration, a = g
			p1.force.x = 0.0;
			p1.force.y = SimConstants.GRAVITY;
			p1.force.add(pressureAcceleration);
			p1.force.add(viscosityAcceleration);

			// Surface Tension, a = (sigma / rho_1) * kappa * n
			double fluidGradientMagnitude = fluidColorGradient.mag();
			// (MASS/rho) * |grad_W| \approx PARTICLE_SPACING^2 * (24/pi/H^8) * r *
			// (H^2−r^2)^2
			// \approx 500 m⁻¹
			if (fluidGradientMagnitude > 50.0) {
				double kappa = -colorLaplacian / fluidGradientMagnitude;
				temp1.set(fluidColorGradient);
				temp1.scale((SimConstants.SURFACE_TENSION * kappa) / (p1.density * fluidGradientMagnitude));
				p1.force.add(temp1);
			}

			// Kinematic adhesion
			double solidGradientMagnitude = solidColorGradient.mag();
			if (solidGradientMagnitude > 0.01) {
				temp1.set(solidColorGradient);
				temp1.scale(SimConstants.SMOOTH_ADHESION);
				p1.force.add(temp1);
			}

		}

		// Pass 3
		for (Particle p1 : particles) {

			if (p1.isSolid) {
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

			if (p1.position.x < 0) {
				p1.position.x = 0;
				p1.velocity.x = Math.abs(p1.velocity.x);
			} else if (p1.position.x > 0.20) {
				p1.position.x = 0.20;
				p1.velocity.x = -Math.abs(p1.velocity.x);
			}

		}

		particles.removeIf(p -> !p.isSolid && p.position.y > 0.2);

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
		if (dist <= 1e-8 || dist >= h) {
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
		if (dist <= 1e-8 || dist >= h) {
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
