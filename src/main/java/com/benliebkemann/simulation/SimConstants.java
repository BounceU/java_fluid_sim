package com.benliebkemann.simulation;

public class SimConstants {

	// Physical constants

	/** Gravitational Constant (m/s^2) */
	public static double GRAVITY = 0.5;// 9.81;

	/** Rest density (kg/m^3) */
	static double REST_DENSITY = 998.2;

	/** Surface tension for color field gradient (N/m) */
	static double SURFACE_TENSION = 0.0728;

	/** Work of adhesion for the fluid-solid boundary (N/m) */
	static double WORK_OF_ADHESION = 0.14; // water-glass

	/** Fluid Viscosity (Pa*s) */
	static double VISCOSITY = 0.1; // 0.05;// 0.001;

	/** Speed of sound (m/s) */
	static double SPEED_OF_SOUND = 15.0;// 50.0; // 1480.0;

	// Simulation constants

	/** Smoothing Radius (Kernel Size), (m) */
	static double H = 0.001;

	/** Initial spacing of particles (m) */
	static double PARTICLE_SPACING = H / 2.0;

	/** Particle Mass (kg) */
	static double MASS = REST_DENSITY * Math.pow(PARTICLE_SPACING, 2); // 2D

	/** Tait constant */
	static double B = (REST_DENSITY * Math.pow(SPEED_OF_SOUND, 2)) / 7.0;

	static double SMOOTH_ADHESION = 5.8;// 0.3;

	/**
	 * Monaghan (2000) artificial pressure epsilon.
	 * Only activates when pressure is negative (tensile).
	 * Tune between 0.01 (mild) and 0.3 (aggressive). Start at 0.1.
	 */
	static double ARTIFICIAL_PRESSURE_EPS = 0.1;
}
