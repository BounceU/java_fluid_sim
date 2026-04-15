package com.benliebkemann.viewer;

import java.util.List;

import com.benliebkemann.simulation.Particle;

public abstract class Renderer {

	Renderer next;

	public Renderer(Renderer next) {
		this.next = next;
	}

	public void process(List<Particle> particles) {
		render(particles);
		passThrough(particles);
	}

	protected void render(List<Particle> particles) {

	}

	protected void passThrough(List<Particle> particles) {
		if (this.next != null)
			next.process(particles);
	}

}
