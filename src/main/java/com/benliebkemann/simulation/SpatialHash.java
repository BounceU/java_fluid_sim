package com.benliebkemann.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpatialHash {

	/** The size of each cell */
	private final double cellSize;

	/** The hash map of cells based on location */
	private final Map<Long, List<Particle>> grid;

	public SpatialHash(double cellSize) {
		this.cellSize = cellSize;
		this.grid = new HashMap<>();
	}

	/**
	 * Clear the hash, should be called at the beginning of each simulator timestep
	 */
	public void clear() {
		for (List<Particle> cell : grid.values()) {
			cell.clear();
		}
	}

	/**
	 * Insert particle into the hash map
	 * 
	 * @param p the particle to insert
	 */
	public void insert(Particle p) {
		long key = getKey(p.position.x, p.position.y);
		grid.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
	}

	/**
	 * Get a list of all the particles that could be interacted with according to
	 * cell size
	 * 
	 * @param p the particle to get the neighbors of
	 * @return the list of neighboring particles, includes p
	 */
	public List<Particle> getNeighbors(Particle p, List<Particle> results) {
		results.clear();

		int pCol = (int) Math.floor(p.position.x / cellSize);
		int pRow = (int) Math.floor(p.position.y / cellSize);

		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				long key = getHashKey(pCol + dx, pRow + dy);
				List<Particle> cell = grid.get(key);
				if (cell != null) {
					results.addAll(cell);
				}
			}
		}
		return results;

	}

	/**
	 * Convert particle x and y values into a key for that particle list in the hash
	 * map
	 * 
	 * @param x coordinate of particle
	 * @param y coordinate of particle
	 * @return key hash for map of particles
	 */
	private long getKey(double x, double y) {
		int col = (int) Math.floor(x / cellSize);
		int row = (int) Math.floor(y / cellSize);
		return getHashKey(col, row);
	}

	/**
	 * Convert 32-bit integer spatial column and row values into a single 64-bit
	 * long hash
	 * 
	 * @param col Spatial column
	 * @param row Spatial row
	 * @return 64-bit long hash value
	 */
	private long getHashKey(int col, int row) {
		return ((long) col << 32) | (row & 0xFFFFFFFFL);
	}

}
