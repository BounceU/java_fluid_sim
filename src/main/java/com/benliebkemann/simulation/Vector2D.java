package com.benliebkemann.simulation;

public class Vector2D {

	public double x, y;

	public Vector2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double mag() {
		return Math.sqrt(this.x * this.x + this.y * this.y);
	}

	public void add(Vector2D other) {
		this.x += other.x;
		this.y += other.y;
	}

	public void sub(Vector2D other) {
		this.x -= other.x;
		this.y -= other.y;
	}

	public void scale(double value) {
		this.x *= value;
		this.y *= value;
	}

	public void set(Vector2D other) {
		this.x = other.x;
		this.y = other.y;
	}

}
