import numpy as np
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from pathlib import Path
import glob

DX = 1e-3  # 1 mm in meters

# --- Load data ---
csv_files = sorted(glob.glob("densityValues*.csv"))
frames = []
for f in csv_files:
    row = np.fromstring(open(f).read().strip().rstrip(','), sep=',', dtype=float)
    row[row < 0] = np.nan  # -1 sentinel → NaN
    frames.append(row)

frames = np.array(frames)  # shape: (N_frames, 200)
N_frames, N_cols = frames.shape
print(f"Loaded {N_frames} frames, {N_cols} columns each")

# Replace NaN with 0 for FFT (boundary cells)
data = np.nan_to_num(frames, nan=0.0)

# --- Spatial FFT per frame ---
# Subtract per-frame mean to remove DC / background level
detrended = data - data.mean(axis=1, keepdims=True)

# FFT along spatial axis, normalized by N so power ~ (amplitude/2)^2
fft_vals = np.fft.rfft(detrended, axis=1) / N_cols
power = np.abs(fft_vals) ** 2  # units: (particle count)^2

# Frequencies and wavelengths
freqs = np.fft.rfftfreq(N_cols, d=DX)  # cycles/meter
with np.errstate(divide='ignore'):
    wavelengths_m = np.where(freqs > 0, 1.0 / freqs, np.inf)
wavelengths_mm = wavelengths_m * 1e3  # convert to mm

# --- Average power spectrum across all frames ---
mean_power = power.mean(axis=0)

# --- Find dominant wavelengths (peaks in mean spectrum, skip DC) ---
from scipy.signal import find_peaks
# Search indices 1 onwards (skip DC at index 0)
peaks, props = find_peaks(mean_power[1:], prominence=mean_power[1:].max() * 0.05)
peaks += 1  # offset back

print("\nDominant wavelengths (by mean power across all frames):")
sorted_peaks = sorted(peaks, key=lambda i: mean_power[i], reverse=True)
for i in sorted_peaks[:8]:
    print(f"  λ = {wavelengths_mm[i]:.1f} mm  (freq = {freqs[i]*1e-3:.4f} cycles/mm, power = {mean_power[i]:.2f})")

# --- Plot 1: Mean power spectrum vs wavelength ---
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
fig.suptitle("Fluid Simulation — Spatial Wave Analysis", fontsize=14)

ax = axes[0, 0]
ax.plot(wavelengths_mm[1:], mean_power[1:], color='steelblue', linewidth=1.2)
for i in sorted_peaks[:5]:
    ax.axvline(wavelengths_mm[i], color='crimson', linestyle='--', alpha=0.7, linewidth=1)
    ax.text(wavelengths_mm[i] + 1, mean_power[i] * 1.2, f"{wavelengths_mm[i]:.0f} mm",
            fontsize=8, color='crimson', rotation=90, va='bottom')
ax.set_xlabel("Wavelength (mm)")
ax.set_ylabel("Power (particle count)²")
ax.set_title("Mean Power Spectrum (all frames)")
ax.set_xlim([0, N_cols * DX * 1e3])
ax.grid(True, alpha=0.3)

# --- Plot 2: Power spectrum evolution over time (heatmap) ---
ax = axes[0, 1]
# Only plot wavelengths in a useful range (skip DC and very short)
wl_idx = np.where((wavelengths_mm >= 5) & (wavelengths_mm <= 200))[0]
extent = [wavelengths_mm[wl_idx[-1]], wavelengths_mm[wl_idx[0]], 0, N_frames]
img = ax.imshow(
    np.log1p(power[:, wl_idx][:, ::-1]),  # flip so x-axis reads left-to-right (longer λ left)
    aspect='auto', extent=extent,
    cmap='inferno', origin='lower'
)
plt.colorbar(img, ax=ax, label="log(1 + power)")
ax.set_xlabel("Wavelength (mm)")
ax.set_ylabel("Frame")
ax.set_title("Power Spectrum Over Time")

# --- Plot 3: Raw density heatmap ---
ax = axes[1, 0]
x_mm = np.arange(N_cols) * DX * 1e3
extent_raw = [x_mm[0], x_mm[-1], 0, N_frames]
im = ax.imshow(data, aspect='auto', extent=extent_raw,
               cmap='viridis', origin='lower', vmin=0)
plt.colorbar(im, ax=ax, label="Particle count")
ax.set_xlabel("x (mm)")
ax.set_ylabel("Frame")
ax.set_title("Density Field Over Time")

# --- Plot 4: Peak power vs frame for top wavelengths ---
ax = axes[1, 1]
top_n = min(4, len(sorted_peaks))
colors = plt.cm.tab10(np.linspace(0, 1, top_n))
for color, i in zip(colors, sorted_peaks[:top_n]):
    ax.plot(power[:, i], label=f"λ={wavelengths_mm[i]:.0f} mm", color=color, linewidth=1)
ax.set_xlabel("Frame")
ax.set_ylabel("Power")
ax.set_title("Dominant Wavelength Power Over Time")
ax.legend(fontsize=8)
ax.grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig("wave_analysis.png", dpi=150)
print("\nSaved wave_analysis.png")
plt.show()
