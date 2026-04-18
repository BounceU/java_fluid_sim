# fluidsim

A 2D Smoothed Particle Hydrodynamics (SPH) fluid simulation in Java, designed to study the Rayleigh-Taylor instability for my Fluid Mechanics class.

See [MATH.md](MATH.md) for a full derivation of the SPH equations and numerical methods used.

---

## Physics Overview

The simulation models a thin layer of fluid (water-like parameters) adhered to the underside of a solid ceiling. Gravity pulls the fluid downward while a kinematic adhesion force holds it to the surface. Small sinusoidal perturbations seed the Rayleigh-Taylor instability, which causes the interface to grow fingers at a characteristic wavelength determined by the balance of gravity and surface tension.

Key physical parameters (see `SimConstants.java`):

| Parameter | Value | Notes |
|---|---|---|
| Gravity | 0.5 m/s² | Reduced from 9.81 to slow dynamics and widen RT wavelength |
| Rest density | 998.2 kg/m³ | Water |
| Viscosity | 0.1 Pa·s | 100× water; stabilizes numerics |
| Surface tension | 0.0728 N/m | Physical water-air value |
| Speed of sound | 15.0 m/s | Artificially reduced for weakly-compressible SPH |
| Smoothing radius H | 1 mm | Kernel support radius |
| Particle spacing | H/2 = 0.5 mm | Initial lattice spacing |

The fastest-growing Rayleigh-Taylor wavelength at these parameters is:

$$\lambda_{RT} = 2\pi\sqrt{\frac{\sigma}{\rho g}} \approx 76 \text{ mm}$$

The 160 mm wide fluid domain fits approximately 2 RT fingers.

---

## Project Structure

```
src/main/java/com/benliebkemann/
├── App.java                        Entry point — UI setup, file picker, thread launch
└── simulation/
    ├── SimConstants.java           All tunable physical and numerical constants
    ├── Particle.java               Particle state (position, velocity, force, density, pressure)
    ├── Vector2D.java               Mutable 2D vector math
    ├── SpatialHash.java            O(1) neighbor lookup via spatial hashing
    └── Simulation.java             SPH physics loop (3-pass: density → forces → integration)
└── viewer/
    ├── Renderer.java               Abstract renderer base (chain-of-responsibility)
    ├── Camera.java                 World-to-screen coordinate transform
    ├── WindowRenderer.java         Live Swing window display
    └── SaveRenderer.java           Saves frames as PNG files
```

---

## Requirements

- JDK 21
- Use the Maven Wrapper committed in this repository (`./mvnw`)

---

## Quick Start

```bash
# 1. Verify Java version (must be 21.x)
java -version

# 2. Build and run tests
./mvnw -B -ntp clean verify

# 3. Run the simulation
./mvnw -B -ntp exec:java
```

On launch, a file picker dialog will appear. **Select a directory** where output PNG frames will be saved. The simulation window opens immediately and frames are written to that directory at 200 frames/second of simulated time.

---

## Running the Simulation

1. Run `./mvnw -B -ntp exec:java`
2. A dialog box asks for an output folder — pick any directory you want PNG frames written to
3. The simulation window opens showing:
   - **Red** — solid ceiling particles (pinned, never move)
   - **Blue** — fluid particles
4. Frames are saved as `outputImages000.png`, `outputImages001.png`, … in the chosen directory
5. Close the window to stop

### Assembling Frames into Video

Using ffmpeg:

```bash
ffmpeg -framerate 60 -i outputImages%03d.png -c:v libx264 -pix_fmt yuv420p output.mp4
```

---

## Tuning Parameters

All parameters live in `SimConstants.java`. The most useful ones to adjust:

**Gravity** — controls RT growth rate and finger wavelength.
```java
public static double GRAVITY = 0.5;   // m/s² — lower = slower dynamics, wider fingers
```
At physical gravity (9.81), λ_RT ≈ 17 mm (too small for the domain). At 0.5, λ_RT ≈ 76 mm.

**Adhesion strength** — how strongly the ceiling holds the fluid.
```java
static double SMOOTH_ADHESION = 5.8;  // higher = fluid clings more aggressively
```

**Artificial pressure epsilon** — Monaghan (2000) tensile instability correction.
```java
static double ARTIFICIAL_PRESSURE_EPS = 0.1;  // 0.01 (mild) to 0.3 (aggressive)
```
If particles explode or cluster, increase this. If cohesion feels too weak, decrease it.

**Surface tension** — controls finger wavelength and droplet size.
```java
static double SURFACE_TENSION = 0.0728;  // N/m — physical water-air value
```

**Viscosity** — damps oscillations; increase if simulation is noisy.
```java
static double VISCOSITY = 0.1;  // Pa·s
```

---

## Physics Summary

I use a 3-pass SPH loop every timestep:

1. Density and Pressure:
   1. Each particle sums kernel-weighted contributions from its neighbors (Poly6 kernel) to compute local density. Pressure is computed via the Tait equation of state.

2. Forces: 
   1. For each particle pair within the smoothing radius:
      1. Pressure: Symmetric SPH pressure acceleration using the Spiky kernel gradient, with (Monaghan 2000) artificial pressure to prevent tensile instability 
      2. Viscosity: (Muller 2003) viscosity Laplacian 
      3. Surface tension: (Morris 2000) Continuum Surface Force (CSF) method using the color field Laplacian and gradient 
      4. Adhesion: Color field gradient between fluid and solid particles, scaled by `SimConstants.SMOOTH_ADHESION`

3. Integration: Symplectic Euler integration. Solid particles have their velocity zeroed every step. Reflective x-boundaries prevent particles from leaving the domain, while particles that fall out of frame are removed.

`dt` is adaptive via the CFL condition: $\Delta t = 0.4 \cdot H / (c_s + v_\text{max})$, capped at 1 ms.

See [MATH.md](MATH.md) for full derivations.

---

## Build Commands

```bash
./mvnw -B -ntp test                  # Run unit tests only
./mvnw -B -ntp clean verify          # Full build + tests + coverage
./mvnw -B -ntp exec:java             # Run the app
./mvnw -B -ntp clean package && \
  java -jar target/fluidsim-1.0-SNAPSHOT.jar   # Run from jar
```

JaCoCo coverage report: `target/site/jacoco/index.html`

---

## Bibliography

1. Muller, M., Charypar, D., & Gross, M. (2003). *Particle-based fluid simulation for interactive applications.* Proceedings of the 2003 ACM SIGGRAPH/Eurographics Symposium on Computer Animation (SCA '03), pp. 154–159. [PDF](https://matthias-research.github.io/pages/publications/sca03.pdf)

2. Monaghan, J. J. (2000). *SPH without a tensile instability.* Journal of Computational Physics, 159(2), 290–311. [Article](https://www.sciencedirect.com/science/article/abs/pii/S0021999100964398)

3. Monaghan, J. J. (2012). *Smoothed Particle Hydrodynamics and Its Diverse Applications.*
Annual Review of Fluid Mechanics, 44, 323-346 [Article](https://www.annualreviews.org/content/journals/10.1146/annurev-fluid-120710-101220)

1. Morris, J. P. (2000). *Simulating surface tension with smoothed particle hydrodynamics.* International Journal for Numerical Methods in Fluids, 33(3), 333–353. [Article](https://onlinelibrary.wiley.com/doi/abs/10.1002/1097-0363(20000615)33%3A3%3C333%3A%3AAID-FLD11%3E3.0.CO%3B2-7)
