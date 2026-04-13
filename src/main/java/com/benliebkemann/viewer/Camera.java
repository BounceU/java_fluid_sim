package com.benliebkemann.viewer;

public class Camera {

	public double x = 0;
	public double y = 0;
	public double scale = 1.0;

	public int toScreenX(double worldX) {
		return (int) ((worldX - x) * scale);
	}

	public int toScreenY(double worldY) {
		return (int) ((worldY - y) * scale);
	}

	public double toWorldX(int screenX) {
		return (screenX / scale) + x;
	}

	public double toWorldY(int screenY) {
		return (screenY / scale) + y;
	}

	public int toScreenSize(double worldSize) {
		return (int) (worldSize * scale);
	}

}
