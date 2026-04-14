package com.benliebkemann.simulation;

public class SimConstants {

	// Physical constants

	/** Gravitational Constant (m/s^2) */
	public static double GRAVITY = 9.81;

	/** Rest density (kg/m^3) */
	static double REST_DENSITY = 998.2;

	/** Surface tension for color field gradient (N/m) */
	static double SURFACE_TENSION = 0.0728;

	/** Fluid Viscosity (Pa*s) */
	static double VISCOSITY = 0.001;

	/** Speed of sound (m/s) */
	static double SPEED_OF_SOUND = 50.0; // 1480.0;

	// Simulation constants

	/** Smoothing Radius (Kernel Size), (m) */
	static double H = 0.0171;

	/** Initial spacing of particles (m) */
	static double PARTICLE_SPACING = H / 2.0;

	/** Particle Mass (kg) */
	static double MASS = REST_DENSITY * Math.pow(PARTICLE_SPACING, 2); // 2D

	/** Tait constant */
	static double B = (REST_DENSITY * Math.pow(SPEED_OF_SOUND, 2)) / 7.0;

}
