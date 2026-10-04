# Rare Earth Elements (REEs) — Ores & Processing Chains

Scope: the lanthanides (La–Lu) plus scandium (Sc) and yttrium (Y). REEs are chemically
near-identical, so nature delivers them **mixed** and the entire industrial art is
*separating them element-by-element*. This document models the realistic chain for the
`fundamentals` mod and is the flagship example of a mixed concentrate that must be split.

Conventional grouping used below:
- **LREE (light)** — La, Ce, Pr, Nd, (Sm, Eu). Hosted mainly by **bastnäsite** and **monazite**.
- **HREE (heavy)** — Gd…Lu plus **Y**. Hosted by **xenotime** and, above all, **ion-adsorption clays**.
- **Th/U** — frequently ride along with phosphate ores (monazite especially); radioactive, must be removed and handled.

---

## Mineral / source summary

| Mineral / source | Formula | Target commodity | Key byproducts | Beneficiation | Intermediate | Extraction route | Final product |
|---|---|---|---|---|---|---|---|
| Bastnäsite | (Ce,La,Nd)CO₃F | Mixed LREE oxides (Ce, La, Nd, Pr) | Fluoride, barite/calcite gangue | Crushing → Grinding → FrothFlotation (± gravity/magnetic) to ~60% REO concentrate | Roast/calcine + acid or alkaline leach → mixed RE chloride/nitrate liquor | SolventExtraction cascade → individual REO → MoltenSaltElectrolysis | Individual LREE metals; NdPr → NdFeB magnet feed |
| Monazite | (Ce,La,Nd,Th)PO₄ | Mixed LREE oxides + Th | **Th** (radioactive), phosphate (→ fertilizer/phosphoric acid) | Crushing → Grinding → GravitySeparation + MagneticSeparation (it is paramagnetic & dense) → flotation cleanup | Hot-NaOH alkaline bake **or** H₂SO₄ bake to crack phosphate & liberate Th → dissolve → precipitate Th(OH)₄ out → mixed RE liquor | SolventExtraction cascade → individual REO → electrolysis / Ca reduction | Individual LREE metals; Th stored/sequestered |
| Xenotime | YPO₄ | HREE-enriched oxides (Y, Dy, Yb, Er, Gd) | Th/U (minor), phosphate | Crushing → Grinding → GravitySeparation + MagneticSeparation → flotation | Hot-NaOH or H₂SO₄ bake (more refractory than monazite) → dissolve → mixed HRE liquor | SolventExtraction (hardest separations) → individual HREO → metallothermic/electro reduction | Y, Dy, Tb, Er oxides/metals |
| Ion-adsorption clay (South China) | REE³⁺ adsorbed on kaolinite/halloysite/illite (no discrete mineral) | **HREE** incl. Dy, Tb, Y (plus some LREE) | Clay, Al | **No crush/flotation needed** — Washing only; REEs are ion-exchange-desorbed in place | In-situ / heap **IonExchange leach** with (NH₄)₂SO₄ or NaCl → RE sulfate/chloride liquor → Precipitation (oxalate/carbonate) | SolventExtraction cascade → individual HREO | Dy, Tb, Y oxides → heavy-REE magnet additives / phosphors |
| Loparite (brief) | (Na,Ca,Ce)(Ti,Nb,Ta)O₃ | Ti, Nb, Ta **primary**; LREE byproduct | Ti, Nb, Ta, Th | Crushing → Grinding → GravitySeparation + MagneticSeparation | Chlorination / acid crack → RE liquor | SolventExtraction | LREE oxides (byproduct of Ti/Nb/Ta) |
| Euxenite (brief) | (Y,Ca,Ce,U,Th)(Nb,Ta,Ti)₂O₆ | HREE + Nb/Ta/Ti | U, Th, Nb, Ta, Ti | Crushing → Grinding → GravitySeparation (dense, pegmatite) | Acid/alkaline crack | SolventExtraction | HREE oxides + Nb/Ta |

---

## Individual separation products (what the cascade yields)

| REE | Oxide intermediate | Final product | Primary end use |
|---|---|---|---|
| Neodymium (Nd) | Nd₂O₃ | Nd metal / NdFeB alloy | High-strength permanent magnets (EV motors, wind turbines, HDDs) |
| Praseodymium (Pr) | Pr₆O₁₁ | Pr metal / "NdPr" (didymium) | Magnets — usually co-produced with Nd as NdPr |
| Dysprosium (Dy) | Dy₂O₃ | Dy metal | Magnet additive — keeps NdFeB coercive at high temperature |
| Terbium (Tb) | Tb₄O₇ | Tb metal | Magnet additive + green phosphor |
| Samarium (Sm) | Sm₂O₃ | Sm metal / SmCo alloy | SmCo magnets (very high-temp, aerospace/defence) |
| Lanthanum (La) | La₂O₃ | La metal | FCC petroleum-cracking catalysts; NiMH battery alloys |
| Cerium (Ce) | CeO₂ | Ce metal/oxide | Glass polishing, auto catalysts, UV glass |
| Europium (Eu) | Eu₂O₃ | Eu metal | Red/blue phosphors (displays, lighting) |
| Yttrium (Y) | Y₂O₃ | Y metal | Phosphor host lattice, YAG, ceramics |

---

## The pipeline (prose)

**1. Mining → physical concentration.** Hard-rock ores (bastnäsite, monazite, xenotime,
loparite, euxenite) are **Crushed** and **Ground**, then upgraded by physical means that
exploit the minerals' high density, paramagnetism, and surface chemistry:
**GravitySeparation** and **MagneticSeparation** (monazite and xenotime are dense and
paramagnetic), and **FrothFlotation** (the workhorse for bastnäsite, e.g. Mountain Pass,
which floats a ~60% REO concentrate). The output of this whole stage is a **mixed REE
concentrate** — it contains *all* the rare earths present, not a single element.

Ion-adsorption clays are the exception: the REEs are not in a mineral but are loosely
**adsorbed as ions on clay surfaces**, so there is nothing to crush or float. A simple
**IonExchange leach** (ammonium sulfate / salt solution) displaces the REE³⁺ ions straight
into solution. This is why South China clays are the cheap, dominant source of the **heavy**
REEs (Dy, Tb, Y) that magnets need.

**2. Cracking the concentrate (chemical liberation).** The mineral lattice must be broken
to release the REEs into an acid-soluble form.
- **Bastnäsite** (a fluorocarbonate) is **Roasted / Calcined** to drive off CO₂ and oxidize
  Ce, then **Leached** with acid (or an alkaline route) to a mixed chloride/nitrate liquor.
- **Monazite / xenotime** (phosphates) are **cracked** by either **hot concentrated NaOH
  (AlkalineBake, ~120–175 °C)** or **H₂SO₄ bake (AcidBake, ~200–300 °C)**. This destroys the
  phosphate matrix (recovered as trisodium phosphate / phosphoric acid) and liberates both
  the REEs **and thorium**.

**3. Thorium / radioactivity handling.** Monazite (and euxenite/loparite) carry **Th**
(and some U) — the minor/radioactive group. After cracking and dissolution, **Th(OH)₄ is
selectively precipitated** (by controlled pH with NaOH/NH₄OH) and sequestered before the
REEs move on. This is a mandatory, gameplay-relevant hazard step: monazite is a Th source
as well as a REE source, and the Th tailings are the environmental/regulatory headache that
historically shut down monazite mining in many places.

**4. THE KEY STEP — solvent-extraction cascade.** Because neighboring REEs differ only
subtly in ion radius and have nearly identical chemistry, separating them is the whole
challenge. The mixed liquor is fed through **many stages (dozens to >100 mixer-settlers) of
SolventExtraction** (organophosphorus extractants such as D2EHPA/PC88A/Cyanex, often with
HNO₃ or HCl), typically splitting first into groups (La/Ce/Pr/Nd … Sm–Eu–Gd … heavies),
then cascading again and again until each element is isolated. **IonExchange** polishing is
used for the highest purities and the hardest heavies. **The adjacent-heavy separations
(especially Dy and Tb) are the most difficult and the whole economic point of the chain.**

**5. To oxide, then to metal.** Each separated RE stream is **Precipitated** (as oxalate or
carbonate) and **Calcined to its oxide** (Nd₂O₃, Dy₂O₃, Eu₂O₃, etc.). Metal is then won by:
- **MoltenSaltElectrolysis** — the industrial route for the light metals and Nd: the oxide
  is dissolved in a molten **NdF₃–LiF** fluoride bath (~1050 °C) and electrolyzed; iron
  cathodes can even produce the **Nd–Fe alloy directly**.
- **MetallothermicReduction (Ca)** — calciothermic reduction of the fluoride/chloride for
  the heavy and high-melting metals (Dy, Tb, Y, Sm) where electrolysis is impractical.

**6. Alloying.** The finished metals go to **Alloying / Refining**:
- **NdFeB** (Nd + Pr + Fe + B, with Dy/Tb added for high-temperature coercivity) — the
  dominant high-energy permanent magnet.
- **SmCo** (Sm + Co) — magnets for very high temperature / aerospace & defence.

### Which REEs matter for what
- **Nd, Pr, Dy, Tb → permanent magnets** (NdFeB; Dy/Tb hold strength when hot). The flagship demand driver.
- **Sm → SmCo magnets** (high-temperature niche).
- **Eu, Y, Tb → phosphors** (red/green/blue in displays & lighting).
- **La → catalysts (FCC petroleum cracking) and NiMH battery alloys.**
- **Ce → glass polishing, auto-exhaust catalysts, UV-absorbing glass.**

**Takeaway for the mod:** every REE ore yields a *mixed* concentrate; crushing/flotation only
gets you a lump of "rare earths." The value and the difficulty live in the **solvent-extraction
cascade** that teases apart chemically near-identical neighbors — hardest for the heavy Dy/Tb —
and monazite additionally forces the player to deal with **radioactive thorium**.

---

## Sources
- [Britannica — Rare-earth element: minerals & ores](https://www.britannica.com/science/rare-earth-element/Minerals-and-ores) and [processing of ores](https://www.britannica.com/science/rare-earth-element/Processing-ores)
- [ScienceDirect — Bastnasite overview](https://www.sciencedirect.com/topics/earth-and-planetary-sciences/bastnasite)
- [MDPI — Selective leaching of Bayan Obo RE concentrate (bastnäsite:monazite 9:1–1:1)](https://www.mdpi.com/2075-4701/15/4/431)
- [ScienceDirect — Sulfuric acid baking/leaching of monazite (REE, Th, phosphate; 200–800 °C)](https://www.sciencedirect.com/science/article/abs/pii/S0304386X18301658)
- [ResearchGate — Review of cracking, baking and leaching of REE concentrates](https://www.researchgate.net/publication/319031798_A_review_on_the_cracking_baking_and_leaching_processes_of_rare_earth_element_concentrates)
- [ScienceDirect — Review of REE adsorption/desorption on clays (ion-adsorption deposits)](https://www.sciencedirect.com/science/article/pii/S0169136823001610)
- [Rare Earth Exchanges — Ion-adsorption clay & heavy rare earths](https://rareearthexchanges.com/ion-adsorption-clay-heavy-rare-earths/)
- [ScienceDirect — Critical review of solvent extraction of rare earths](https://www.sciencedirect.com/science/article/pii/S0892687513003452)
- [Nature Communications Materials — magnesiothermic/molten-salt reduction of Nd₂O₃ (NdF₃–LiF, ~1050 °C, Nd–Fe alloy)](https://www.nature.com/articles/s43246-025-01041-5)
- [ScienceDirect — Loparite (Ce,Na,Sr,Ca)(Ti,Nb,Ta,Fe³⁺)O₃](https://www.sciencedirect.com/science/article/abs/pii/S0925838896028241) and [Wikipedia — Euxenite (Y,Ca,Ce,U,Th)(Nb,Ta,Ti)₂O₆](https://en.wikipedia.org/wiki/Euxenite)
- [Lynas — Summary of rare earths (end uses)](https://lynasrareearths.com/summary-of-rare-earths/)
