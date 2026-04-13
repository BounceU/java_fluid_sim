package com.benliebkemann.simulation;

public class SimConstants {

	/** Gravitational Constant */
	public static double GRAVITY = 9.81;

	/** Smoothing Radius (Kernel Size) */
	public static double H = 0.04;

	/** Particle Mass */
	static double MASS = 0.02;

	/** Rest density */
	static double REST_DENSITY = 1000.0;

	/** Stiffness for Tait's Equation */
	static double GAS_CONSTANT = 2000.0;

	/** Fluid Viscosity */
	static double VISCOSITY = 250.0;

	/** Surface tension for color field gradient */
	static double SURFACE_TENSION = 0.0728;

	/** Time step */
	static double DT = 0.0008;

}
