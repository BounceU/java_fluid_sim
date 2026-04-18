
## 1. Smoothed Particle Hydrodynamics (SPH)

SPH approximates a continuous field $A(\mathbf{x})$ by a weighted sum over discrete particles:

$$A(\mathbf{x}) \approx \sum_j \frac{m_j}{\rho_j} A(\mathbf{x}_j) \, W(|\mathbf{x} - \mathbf{x}_j|, h)$$

where $m_j$ and $\rho_j$ are the mass and density of particle $j$, and $W(r, h)$ is a smoothing kernel with support radius $h$. The kernel must satisfy:

$$\int W(r, h) \, dA = 1, \qquad \lim_{h \to 0} W(r, h) = \delta(\mathbf{r})$$

Spatial derivatives follow directly from the kernel:

$$\nabla A(\mathbf{x}) \approx \sum_j \frac{m_j}{\rho_j} A(\mathbf{x}_j) \, \nabla W(|\mathbf{x} - \mathbf{x}_j|, h)$$

(Muller)

---

## 2. Smoothing Kernels

Three kernels are used, each chosen for specific properties. All are zero for $r \geq h$.

### 2.1 Poly6 Kernel (density, color field)

Used for density estimation and surface tension because it is smooth and has continuous derivatives everywhere including $r = 0$:

$$W_{\text{poly6}}(r, h) = \frac{4}{\pi h^8}(h^2 - r^2)^3, \quad 0 \leq r \leq h$$

> Implementation: `Simulation.java:271-282`.

**Gradient:**

$$\nabla W_{\text{poly6}}(\mathbf{r}, h) = -\frac{24}{\pi h^8}(h^2 - r^2)^2 \, \mathbf{r}$$

where $\mathbf{r} = \mathbf{x}_i - \mathbf{x}_j$ and $r = |\mathbf{r}|$.

> Implementation: `Simulation.java:284-296`.

**Laplacian:**

$$\nabla^2 W_{\text{poly6}}(r, h) = -\frac{48}{\pi h^8}(h^2 - r^2)(h^2 - 3r^2)$$

(Muller)

> Implementation: `Simulation.java:298-309`.

### 2.2 Spiky Kernel (pressure gradient)

The Poly6 gradient vanishes as $r \to 0$, which causes particles at the same position to feel no repulsive force — the source of particle clustering. The Spiky kernel has a non-zero gradient at $r = 0$, providing guaranteed repulsion:

$$\nabla W_{\text{spiky}}(\mathbf{r}, h) = -\frac{30}{\pi h^5} \frac{(h-r)^2}{r} \, \mathbf{r}$$

(Muller)

> Implementation: `Simulation.java:311-323`.

### 2.3 Viscosity Kernel (viscous Laplacian)

Chosen so that its Laplacian is strictly positive, guaranteeing that viscosity is always dissipative:

$$\nabla^2 W_{\text{visc}}(r, h) = \frac{20}{\pi h^5}(h - r)$$

(Muller)

> Implementation: `Simulation.java:325-331`.

---

## 3. Density Estimation

Each particle's density is computed by summing kernel-weighted mass contributions from all neighbors within $h$, including itself:

$$\rho_i = \sum_j m_j \, W_{\text{poly6}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

The self-contribution ($j = i$, $r = 0$) is always included:

$$\rho_i^{\text{self}} = m \cdot W_{\text{poly6}}(0, h) = m \cdot \frac{4}{\pi h^8} h^6 = \frac{4m}{\pi h^2}$$

(Muller)

> Implementation: `Simulation.java:127-148`.

---

## 4. Equation of State — Cole's Equation

Literature on SPH simulations refers to this as Tait's Equation of State but as pointed out by (Monaghan 2012), it was actually introduced by R. H. Cole based on Tait's equations ([discussion](https://forums.dual.sphysics.org/discussion/1804/theory-tait-equation-of-state))

Pressure is derived from density using the weakly-compressible Tait equation of state, which approximates a nearly incompressible fluid:

$$p_i = B\left[\left(\frac{\rho_i}{\rho_0}\right)^7 - 1\right]$$

where the stiffness coefficient is:

$$B = \frac{\rho_0 c_s^2}{7}$$

$c_s$ is the peed of sound. Using $c_s = 15$ m/s rather than the actual 1480 m/s for water keeps the timestep large enough for a fast simulation while still hopefully providing enough stiffness to approximate incompressibility.

At rest, $\rho_i = \rho_0$ and $p_i = 0$. Compression ($\rho > \rho_0$) gives positive pressure. Tension ($\rho < \rho_0$) gives negative pressure.

---

## 5. Pressure Force

The momentum equation for an inviscid fluid is:

$$\frac{D\mathbf{v}}{Dt} = -\frac{\nabla p}{\rho} + \mathbf{g}$$

The standard SPH discretization of $-\nabla p / \rho$ is not symmetric and does not conserve momentum, but we can use

$$\left(\frac{D\mathbf{v}_i}{Dt}\right)_p = -\sum_j m_j \left(\frac{p_i}{\rho_i^2} + \frac{p_j}{\rho_j^2}\right) \nabla_i W_{\text{spiky}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

(Monaghan 2000)

> Implementation: `Simulation.java:174-188`.

### 5.1 Monaghan Artificial Pressure to fix the Tensile Instability

When $p_i < 0$, the symmetric pressure formulation generates an attractive force between neighboring particles. This causes particles to cluster uncontrollably and explode. Clamping pressure to zero removes the instability but also removes cohesion that needs to be modeled.

To fix this, we add a small per-pair repulsive correction that activates only under tension:

$$\left(\frac{D\mathbf{v}_i}{Dt}\right)_p = -\sum_j m_j \left(\frac{p_i}{\rho_i^2} + \frac{p_j}{\rho_j^2} + R_{ij} f_{ij}^n\right) \nabla_i W_{\text{spiky}}$$

where:

$$R_i = \begin{cases} \varepsilon \left|\dfrac{p_i}{\rho_i^2}\right| & p_i < 0 \\ 0 & p_i \geq 0 \end{cases}, \qquad R_{ij} = R_i + R_j$$

$$f_{ij} = \frac{W_{\text{poly6}}(|\mathbf{x}_i - \mathbf{x}_j|, h)}{W_{\text{poly6}}(\Delta p, h)}, \qquad n = 4$$

(Monaghan 2000)

$\Delta p$ is the initial particle spacing. $f_{ij} \in [0, 1]$ for $r \geq \Delta p$, and $f_{ij}^4$ concentrates the correction at close range, where tensile instability is most severe. The parameter $\varepsilon \in [0.01, 0.3]$ controls correction strength.

> Implementation: `Simulation.java:178-184`.

---

## 6. Viscosity Force

Viscous stress in a Newtonian fluid is:

$$\left(\frac{D\mathbf{v}}{Dt}\right)_\mu = \frac{\mu}{\rho} \nabla^2 \mathbf{v}$$

The SPH approximation using the viscosity Laplacian kernel:

$$\left(\frac{D\mathbf{v}_i}{Dt}\right)_\mu =\mu \sum_j m_j \frac{\mathbf{v}_j - \mathbf{v}_i }{\rho_i \rho_j}  \, \nabla^2 W_{\text{visc}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

(Muller)

The $(v_j - v_i)$ form guarantees that viscosity always drives particles toward the same velocity, and $\nabla^2 W_{\text{visc}} > 0$ everywhere ensures the force is always dissipative.

The simulation uses $\mu = 0.1$ Pa·s  to damp high-frequency oscillations that arise from the weakly-compressible pressure model.

> Implementation: `Simulation.java:190–195`.

---

## 7. Surface Tension — Continuum Surface Force (CSF)

I'm modelling surface tension using the color fields.

(Morris)

### 7.1 Color Field

We make a scalar color field $c$ that is 1 inside the fluid and 0 outside. In SPH, approximated as:

$$c_i = \sum_j \frac{m_j}{\rho_j} W_{\text{poly6}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

Its gradient $\mathbf{n} = \nabla c$ is the surface normal (nonzero only near the interface) and its divergence gives the mean curvature:

$$\kappa = -\nabla \cdot \hat{\mathbf{n}} = -\frac{\nabla^2 c}{|\mathbf{n}|}$$

### 7.2 Surface Tension Acceleration

The CSF formulation converts surface tension into a volumetric body force:

$$\left(\frac{D\mathbf{v}_i}{Dt}\right)_\sigma = \frac{\sigma}{\rho_i} \kappa_i \hat{\mathbf{n}}_i = \frac{-\sigma \, \nabla^2 c_i}{\rho_i |\mathbf{n}_i|^2} \mathbf{n}_i$$

So in SPH:

$$\mathbf{n}_i = \sum_{j \in \text{fluid}} \frac{m_j}{\rho_j} \nabla W_{\text{poly6}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

$$\nabla^2 c_i = \sum_{j \in \text{fluid}} \frac{m_j}{\rho_j} \nabla^2 W_{\text{poly6}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

The force is applied only when $|\mathbf{n}_i| > n_{\min}$ (threshold $\approx 50\ \text{m}^{-1}$) to avoid singularities in interior particles where $\mathbf{n} \approx \mathbf{0}$ due to symmetric neighborhoods.

The Poly6 Laplacian changes sign at $r = h/\sqrt{3} \approx 0.577h$, so near neighbors contribute negatively and far neighbors positively. The self-contribution at $r=0$ dominates:

$$\nabla^2 W_{\text{poly6}}(0, h) = -\frac{12}{\pi h^4}$$

This gives a large-magnitude curvature estimate that is relatively insensitive to the exact interface geometry, which is a known limitation of the SPH color-field method.

> Implementation: `Simulation.java:198-206,221-226`. (Note, only fluid-to-fluid interactions enter $\mathbf{n}_i$ and $\nabla^2 c_i$.  Solid neighbors are separated into the adhesion term)

---

## 8. Solid Adhesion

The adhesion of fluid to the solid ceiling is modeled by computing a separate color field gradient using only the solid particle contributions:

$$\mathbf{s}_i = \sum_{j \in \text{solid}} \frac{m_j}{\rho_j} \nabla W_{\text{poly6}}(|\mathbf{x}_i - \mathbf{x}_j|, h)$$

The resulting force on a fluid particle is:

$$\left(\frac{D\mathbf{v}_i}{Dt}\right)_{\text{adh}} = \alpha \, \mathbf{s}_i, \quad |\mathbf{s}_i| > s_{\min}$$

where $\alpha$ = `SMOOTH_ADHESION` = 5.8. Since $\nabla W_{\text{poly6}}$ at a fluid particle below the solid points **upward** (toward the region of higher solid density), $\mathbf{s}_i$ points toward the ceiling and the adhesion force opposes gravity.

The effective adhesion range is limited to $r < h = 1$ mm by the kernel support. Fluid particles further than 1 mm from the solid surface receive zero adhesion — this is the primary constraint in the simulation currently on how thick a hanging fluid layer can be. (I'm working on it).

> Implementation: `Simulation.java:198-206,229-234`. Solid particles are identified by the `isSolid` variable in the `Particle` class.

---

## 9. Gravity

$$\left(\frac{D\mathbf{v}_i}{Dt}\right)_g = g \hat{\mathbf{y}}$$

where $\hat{\mathbf{y}}$ is the downward direction (positive y in screen coordinates). $g = 0.5$ m/s² (I reduced it from 9.81 to slow the dynamics and widen the RT finger wavelength).

> Implementation: `Simulation.java:211–212`.

---

## 10. Rayleigh-Taylor Instability Analysis

### 10.1 Physical Setup

The Rayleigh-Taylor (RT) instability occurs at the interface between a denser fluid above and a lighter fluid (or vacuum, since simulating air particles would take forever) below. In my simulation, water-density fluid hangs from a solid ceiling against gravity, with vacuum below. The interface at the bottom of the fluid layer is RT-unstable and the fluid falls, though a thin film of fluid stays attached to the solid because of the adhesion term.

## 11. Time Integration

### 11.1 Symplectic Euler

Integration uses the symplectic Euler scheme:

$$\mathbf{v}_i^{n+1} = \mathbf{v}_i^n + \Delta t \, \mathbf{a}_i^n$$

$$\mathbf{x}_i^{n+1} = \mathbf{x}_i^n + \Delta t \, \mathbf{v}_i^{n+1}$$

which is first-order accurate.

> Implementation: `Simulation.java:239-261`

### 11.2 CFL Timestep

The timestep is adaptive, controlled by the Courant-Friedrichs-Lewy (CFL) condition for acoustic waves:

$$\Delta t = \min\left(0.4 \cdot \frac{h}{c_s + v_{\max}},\ \Delta t_{\max}\right)$$

where $v_{\max}$ is the maximum particle speed, $c_s = 15$ m/s is the speed of sound, and $\Delta t_{\max} = 1$ ms. The coefficient 0.4 is conservative; values up to 0.5 are typically stable for SPH.

At rest: $\Delta t \approx 0.4 \times 10^{-3} / 15 \approx 2.7 \times 10^{-5}$ s. The rendering rate is 200 frames per simulated second, so approximately 185 physics steps are taken per rendered frame.

> Implementation: `Simulation.java:86-89`.

---

## 12. Spatial Hashing

Searching for all particles within distance $h$ of a query point naively costs $O(N^2)$ per step, so I use a spatial hash grid with cell size $h$: each particle is inserted into the cell containing its position before the first pass, and neighborhood searches only check the 9 cells immediately surrounding the particle. Since cells have width $h$, all particles within distance $h$ are guaranteed to appear in those 9 cells.

Cells are keyed by a 64-bit integer formed by bit-packing the 32-bit grid column and row:

```
key = (col << 32) | (row & 0xFFFFFFFFL)
```

This avoids hashing collisions for typical simulation domains while allowing negative grid coordinates (row values are masked to the lower 32 bits as unsigned).

> Implementation: `SpatialHash.java`.

---

## 13. Summary of Forces

The total acceleration on fluid particle $i$ each timestep is:

$$\frac{D\mathbf{v}_i}{Dt} = \underbrace{-\sum_j m_j\!\left(\frac{p_i}{\rho_i^2} + \frac{p_j}{\rho_j^2} + R_{ij}f_{ij}^4\right)\!\nabla W_\text{spiky}}_{\text{pressure + artificial pressure}} + \underbrace{\sum_j \frac{\mu\, m_j}{\rho_i\rho_j}(\mathbf{v}_j - \mathbf{v}_i)\nabla^2 W_\text{visc}}_{\text{viscosity}} + \underbrace{\frac{-\sigma\,\nabla^2 c_i}{\rho_i|\mathbf{n}_i|^2}\mathbf{n}_i}_{\text{surface tension}} + \underbrace{\alpha\,\mathbf{s}_i}_{\text{adhesion}} + \underbrace{g\hat{\mathbf{y}}}_{\text{gravity}}$$

Solid particles receive the same force computation but their velocity is zeroed at the end of every step, making them a rigid kinematic boundary.

---

## Bibliography

1. Muller, M., Charypar, D., & Gross, M. (2003). *Particle-based fluid simulation for interactive applications.* Proceedings of the 2003 ACM SIGGRAPH/Eurographics Symposium on Computer Animation (SCA '03), pp. 154–159.

2. Monaghan, J. J. (2000). *SPH without a tensile instability.* Journal of Computational Physics, 159(2), 290–311.

3. Monaghan, J. J. (2012). *Smoothed Particle Hydrodynamics and Its Diverse Applications.*
Annual Review of Fluid Mechanics, 44, 323-346

1. Morris, J. P. (2000). *Simulating surface tension with smoothed particle hydrodynamics.* International Journal for Numerical Methods in Fluids, 33(3), 333–353.
