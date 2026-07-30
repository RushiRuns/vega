# Tonal Pill Design Language & Implementation Guide (Vega App)

This document provides the complete design specification and reference implementation for the **Tonal Pill Design Language** adapted for Jetpack Compose on Android (Material 3).

---

## 1. Core Principles

- **Dark-First, Tonal Elevation**: Deep black/near-black backgrounds (`#0B0B0D`), elevation established strictly via surface lightness (`#1C1C1E` $\to$ `#242426`), light theme as direct inversion.
- **Pill Geometry**: Stadium pill shapes (`999.dp`) applied across all interactive elements (navigation bar, badges, buttons, chips, date cells, search input).
- **One Hero Element**: Exactly one photographic/gradient hero card per screen; surrounding interface elements remain muted and grayscale.
- **Semantic Colors**: Vibrant colors (`AccentBlue`, `AccentGreen`, `AccentAmber`, `AccentRed`, `AccentOrange`, `AccentViolet`) reserved for status badges, active states, and hero gradients—never neutral chrome.
- **Typographic Structure**: Weight contrast (Bold headline $\to$ SemiBold title $\to$ Regular body) creates clear hierarchy without excessive size scaling.

---

## 2. Token Architecture

### 2.1 Colors (`com.vega.ui.theme.Color.kt`)
- `BackgroundDark`: `#0B0B0D`
- `SurfaceDark`: `#1C1C1E`
- `SurfaceElevatedDark`: `#242426`
- `SurfaceVariantDark`: `#2C2C2E`
- `OutlineDark`: `#3A3A3C`
- `AccentBlue`: `#0A84FF`
- `AccentGreen`: `#34C759`
- `AccentAmber`: `#FFC542`
- `AccentRed`: `#FF3B30`
- `AccentOrange`: `#FF9500`
- `AccentViolet`: `#8B5CF6`

### 2.2 Shapes (`com.vega.ui.theme.Shape.kt`)
- `HeroCardShape`: `28.dp`
- `SectionCardShape`: `20.dp`
- `ListItemShape`: `16.dp`
- `ChipShape`: `12.dp`
- `PillShape`: `999.dp`
- `IconCircleShape`: `CircleShape`

---

## 3. Component Reference

1. **StatusPill**: Stadium badge with 15% opacity accent fill and full-strength text/icon.
2. **HeroDayCard**: 210dp height hero section with multi-stop gradient brush, top pill badges, and bold date hierarchy.
3. **FloatingPillNavBar**: 60dp inset floating stadium navigation container with active tab highlight and adjacent FAB.
4. **StatProgressCard**: Card featuring headline, amber percentage hero stat, and 10-pip segmented momentum bar.
5. **ChecklistItem**: Interactive circular checkbox with amber ring (pending) $\to$ green check (done) bounce state.

---

## 4. Key Metrics & Grid

- Screen Side Margin: `20.dp`
- Card Internal Padding: `16.dp` $\text{–}$ `20.dp`
- List Item Spacing: `10.dp` $\text{–}$ `12.dp`
- Section Separation: `24.dp` $\text{–}$ `28.dp`
- Pill Minimum Touch Height: `48.dp`
