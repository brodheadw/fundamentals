# Base (Non-Ferrous Industrial) Metals — Ore & Processing Catalog

Research for the **fundamentals** Minecraft mod. Scope: copper, aluminium, lead, zinc, tin.
Nickel laterite is handled by another group and is intentionally omitted.

All mineral formulas verified against mineralogical references (Wikipedia/Mindat) and
processing routes against industry/process-metallurgy sources (see end of file).

## Ore & Mineral Table

| Mineral | Formula | Target commodity | Key byproducts | Beneficiation | Intermediate | Extraction route | Final product |
|---|---|---|---|---|---|---|---|
| Chalcopyrite | CuFeS₂ | Copper | Au, Ag, Mo, Se, Te (Fe, S to slag/acid) | Crushing, Grinding, Froth flotation | Copper concentrate (~25–30% Cu) | Smelting → matte → converting → blister → fire + electrorefining | Cathode copper (99.99%) |
| Bornite | Cu₅FeS₄ | Copper | Au, Ag, Mo, Se, Te | Crushing, Grinding, Froth flotation | Copper concentrate | Pyrometallurgical (smelt/convert/refine) | Cathode copper |
| Chalcocite | Cu₂S | Copper | Ag, (Au) | Crushing, Grinding, Froth flotation | Copper concentrate | Pyro; or leach + SX-EW for secondary/supergene ore | Cathode copper |
| Covellite | CuS | Copper | Ag | Crushing, Grinding, Froth flotation | Copper concentrate | Pyro; or leach + SX-EW | Cathode copper |
| Malachite | Cu₂CO₃(OH)₂ | Copper | — (oxide/supergene, low precious metal) | Crushing (ROM heap) | PLS (pregnant leach solution) | Heap/vat acid leach → SX → EW | Cathode copper (SX-EW) |
| Azurite | Cu₃(CO₃)₂(OH)₂ | Copper | — | Crushing | PLS | Heap/vat acid leach → SX → EW | Cathode copper (SX-EW) |
| Cuprite | Cu₂O | Copper | — | Crushing | PLS | Acid leach → SX → EW | Cathode copper (SX-EW) |
| Native copper | Cu | Copper | Ag | Crushing, Grinding, Gravity separation, Flotation | — (feed directly to melt/refine) | Melt → fire/electrorefine | Cathode / refined copper |
| Bauxite (rock) | gibbsite Al(OH)₃, boehmite γ-AlO(OH), diaspore α-AlO(OH) | Aluminium | Ga (trace), red mud (Fe₂O₃, TiO₂ residue) | Crushing, Washing, Grinding | Alumina (Al₂O₃) | Bayer process → Hall-Héroult electrolysis | Primary aluminium metal |
| Galena | PbS | Lead | **Ag** (major), Au, Sb, Bi, As, Cu | Crushing, Grinding, Froth flotation | Lead concentrate → sinter (PbO) | Sinter/roast → blast furnace (or ISP) smelt → refine | Refined lead (Ag recovered in refining) |
| Sphalerite | (Zn,Fe)S (ZnS) | Zinc | **Cd, In, Ge**, Ga; S→acid; (Pb, Ag in mixed ore) | Crushing, Grinding, Froth flotation | Zinc concentrate → calcine (ZnO) | RLE: roast → leach → EW (dominant); or pyro/ISP | Zinc (SHG cathode / ISP slab) |
| Smithsonite | ZnCO₃ | Zinc | — (Cd minor) | Crushing, Grinding, (flotation/DMS) | Calcine / leach solution | Calcination → leach → EW; or pyro | Zinc metal / zinc oxide |
| Hemimorphite | Zn₄Si₂O₇(OH)₂·H₂O | Zinc | — | Crushing, Grinding | Leach solution (ZnSO₄) | Acid leach → purification → EW | Zinc metal |
| Cassiterite | SnO₂ | Tin | Nb, Ta, W (in hard-rock gangue), (Ag minor) | Crushing, Grinding, Gravity separation, Magnetic separation | Tin concentrate (SnO₂ ~70%+) | Carbothermic reduction (two-stage smelt) → refine | Refined tin metal |

## Per-commodity processing notes

### Copper
Two parallel routes, chosen by mineralogy:
- **Sulfides (chalcopyrite, bornite, chalcocite, covellite) — pyrometallurgical.**
  Froth flotation upgrades ore to a ~25–30% Cu concentrate. The concentrate is **smelted**
  (flash/bath furnace) to a copper–iron **matte** (45–74% Cu); **converting** (Peirce–Smith
  / continuous) oxidizes remaining Fe and S to yield **blister copper** (98.5–99.5% Cu);
  **fire refining** gives anode copper; **electrorefining** deposits 99.99% cathode.
  Precious/rare byproducts (Au, Ag, Se, Te) partition to matte → copper → **anode slimes**
  during electrorefining and are recovered there; Mo is recovered as molybdenite from the
  flotation circuit; S is captured as H₂SO₄.
- **Oxides / supergene (malachite, azurite, cuprite, secondary chalcocite) — hydrometallurgical.**
  Crushed ore is **heap-leached** with dilute sulfuric acid → **pregnant leach solution (PLS)**.
  **Solvent extraction (SX)** selectively upgrades and purifies copper into an organic then
  strips it to a clean electrolyte; **electrowinning (EW)** plates 99.99% cathode. SX-EW is
  ~20% of world copper and recovers little/no precious metal.
- **Native copper** needs only physical concentration then melting/refining.

### Aluminium
Bauxite is a **rock**, not a single mineral — a mix of **gibbsite Al(OH)₃**, **boehmite γ-AlO(OH)**,
and **diaspore α-AlO(OH)** (plus iron oxides, silica, TiO₂). Two fixed stages:
1. **Bayer process** — digest bauxite in hot caustic NaOH under pressure (≈140–240 °C;
   gibbsite dissolves easily, boehmite/diaspore need higher T). Aluminate liquor is clarified
   (insoluble **red mud** residue), precipitated and **calcined** to smelter-grade **alumina (Al₂O₃)**.
2. **Hall-Héroult** — alumina is dissolved in molten cryolite and **electrolysed** at ~960 °C
   with carbon anodes to produce molten primary aluminium. Extremely energy-intensive.

### Lead
**Galena (PbS)** is essentially the sole primary ore (86.6% Pb). Flotation → lead concentrate;
**sinter-roast** converts PbS to **PbO sinter** (SO₂ → acid); the sinter is **smelted** with coke
in a **blast furnace** (or the **Imperial Smelting Process / ISP**, which co-produces zinc) to
bullion. **Silver is the major byproduct** — often the economic driver — recovered during lead
refining (Parkes desilvering); Au, Sb, Bi, As, Cu also removed and recovered.

### Zinc
**Sphalerite (Zn,Fe)S** supplies ~95% of primary zinc. Dominant route is **RLE
(roast–leach–electrowin)**, ~85%+ of world output: **roast** sulfide concentrate to **calcine
(ZnO)** (SO₂ → H₂SO₄), **leach** the calcine in spent sulfuric electrolyte, purify (cement out
Cu/Cd/Co), then **electrowin** zinc onto aluminium cathodes → special-high-grade zinc.
Pyrometallurgical **ISP** (shared with lead) is the minority route and in decline.
Oxidized ores — **smithsonite (ZnCO₃)** and **hemimorphite (Zn₄Si₂O₇(OH)₂·H₂O)** — are
calcined/leached into the hydromet circuit. **Byproducts: Cd, In, Ge, Ga** recovered from
purification residues and leach solution; Pb/Ag report to residues in mixed ores.

### Tin
**Cassiterite (SnO₂)** is the only significant ore (78.8% Sn). Because it is dense (SG ~7) and
chemically inert, concentration is **gravity-based** (spirals, shaking tables, jigs, Falcon/Knelson)
with **magnetic separation** to reject iron/tungsten minerals; placer ores need almost no grinding.
The ~70%+ SnO₂ concentrate is reduced by **carbothermic smelting** (two-stage reduction with
coal/coke in reverberatory or electric furnaces), then refined (thermal/electrolytic).
Nb/Ta/W can report with hard-rock concentrates as byproducts.

## Notable byproduct cross-links to other ore groups
- **Copper sulfides** are the principal industrial source of **Se, Te, and much of the Re/Mo** —
  recovered from anode slimes and the flotation circuit; also significant **Au/Ag**.
- **Galena** is a leading source of **silver** (precious-metals group).
- **Sphalerite** is *the* dominant source of **cadmium, indium, and germanium** (minor/tech-metals group),
  plus gallium.
- **Lead and zinc** are metallurgically coupled through the **Imperial Smelting Process** and
  frequently co-mined (Pb–Zn–Ag deposits).
- **Bauxite/Bayer** red mud carries **Fe, Ti, and REE/Ga** residues (potential secondary sources).

## Sources
- Chalcopyrite / copper sulfides — https://en.wikipedia.org/wiki/Chalcopyrite , https://en.wikipedia.org/wiki/Covellite
- Copper oxide minerals — https://en.wikipedia.org/wiki/Cuprite , https://en.wikipedia.org/wiki/Malachite , https://en.wikipedia.org/wiki/Azurite
- Copper smelting/refining — https://www.epa.gov/sites/default/files/2020-11/documents/c12s03.pdf
- Copper SX-EW — https://en.wikipedia.org/wiki/Solvent_extraction_and_electrowinning
- Bauxite / Bayer / Hall-Héroult — https://geology.com/minerals/bauxite.shtml , https://en.wikipedia.org/wiki/Bayer_process
- Lead smelting / ISP — https://en.wikipedia.org/wiki/Lead_smelting , https://www.britannica.com/technology/lead-processing
- Zinc minerals & RLE — https://en.wikipedia.org/wiki/Sphalerite , https://en.wikipedia.org/wiki/Zinc_smelting
- Cassiterite / tin — https://en.wikipedia.org/wiki/Tin(IV)_oxide , https://pmc.ncbi.nlm.nih.gov/articles/PMC11243683/
