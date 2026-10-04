# Precious Metals & Platinum-Group Metals (PGMs) — Research

Research for the **fundamentals** Minecraft mod. Scope: gold, silver, platinum-group
metals, and mercury (precious-adjacent). Focus on the chemistry-driven
ore → concentrate → intermediate → metal/product chains, with extra depth on the
**refractory-gold** and **PGM-refining** chains as gameplay progression hooks.

Canonical stage vocabulary used throughout: Crushing, Grinding, Washing,
GravitySeparation, FrothFlotation, Roasting, PressureOxidation, BioOxidation,
Leaching(Cyanidation), SolventExtraction, IonExchange, CarbonAdsorption,
Precipitation(MerrillCrowe), Smelting, Retorting, Electrowinning, Electrorefining,
Refining, Cupellation.

## Catalog

| Mineral | Formula | Target commodity | Key byproducts | Beneficiation | Intermediate | Extraction route | Final product |
|---|---|---|---|---|---|---|---|
| Native gold | Au | Gold | Silver (in electrum-rich ores) | Gravity separation, froth flotation | Gold gravity concentrate / flotation concentrate | Cyanide leach (CIL/CIP) → carbon adsorption or Merrill-Crowe → smelt to doré → Wohlwill electrorefining | Fine gold (99.99%) |
| Electrum | (Au,Ag) | Gold + Silver | Silver (20–55% of alloy) | Gravity separation, froth flotation | Gold-silver concentrate | Cyanidation → Merrill-Crowe (favored, high Ag) → smelt to doré → parting / electrorefining | Fine gold + silver |
| Aurostibite | AuSb₂ | Gold | Antimony | Froth flotation | Sb-bearing gold concentrate | Roasting (drive off Sb as oxide) → cyanidation → carbon adsorption → doré → refining | Fine gold |
| Refractory gold in arsenopyrite | Au locked in FeAsS | Gold | Arsenic, iron, sulfur | Froth flotation | Refractory sulfide gold concentrate | **Pressure oxidation OR bio-oxidation OR roasting** (liberate locked gold) → cyanidation → carbon adsorption/CIL → doré → refining | Fine gold |
| Refractory gold in pyrite | Au locked in FeS₂ | Gold | Sulfur, iron (SO₂ / acid) | Froth flotation | Refractory pyritic gold concentrate | **Pressure oxidation / bio-oxidation / roasting** → cyanidation → CIL → doré → refining | Fine gold |
| Argentite / Acanthite | Ag₂S | Silver | Lead, zinc, copper (host ores) | Froth flotation | Silver / Pb-Ag-Zn-Cu concentrate | Cyanidation + Merrill-Crowe; OR as Pb-smelter byproduct → Parkes desilvering → cupellation → Moebius electrorefining | Fine silver (99.9%) |
| Native silver | Ag | Silver | Base metals | Gravity separation, froth flotation | Silver concentrate | Cyanidation → Merrill-Crowe → doré → cupellation / Moebius electrorefining | Fine silver |
| Sperrylite | PtAs₂ | Platinum (PGMs) | Pd, Rh, Ru, Ir, Os; Ni, Cu, Co; As, S | Gravity separation, froth flotation (with Ni-Cu sulfides) | Ni-Cu-PGM sulfide flotation concentrate | Smelt to PGM-rich matte → converting → base-metal leach → PGM residue → pressure-oxidation leach → solvent extraction / ion exchange / precipitation chain → refining | Refined Pt + co-PGMs |
| Cooperite | PtS | Platinum (PGMs) | Pd, Rh, Ru, Ir, Os; Ni, Cu | Gravity separation, froth flotation | PGM-bearing sulfide concentrate | Smelting → matte → base-metal removal → PGM refining (SX / IX / precipitation) | Refined Pt + co-PGMs |
| Braggite | (Pt,Pd,Ni)S | Platinum + Palladium | Rh, Ru, Ir, Os; Ni, Cu | Gravity separation, froth flotation | PGM-bearing sulfide concentrate | Smelting → matte → base-metal leach → PGM refining chain | Refined Pt/Pd + co-PGMs |
| Cinnabar | HgS | Mercury | Sulfur (SO₂) | Crushing, gravity separation, flotation | Mercury concentrate / lump ore | **Roasting / retorting** (500–600°C, volatilize Hg vapor) → condensation | Liquid mercury (further purified by distillation) |

## Notes per commodity

### Gold
Native gold and electrum are recovered first by **gravity separation** (coarse free
gold) and **froth flotation**. Free-milling ore then goes through
**cyanidation** (NaCN leach at pH 10–11), with the gold-cyanide complex recovered
either by **carbon adsorption** (CIL = carbon-in-leach, carbon added during the
leach; CIP = carbon-in-pulp, added after) or by the **Merrill-Crowe** zinc-dust
cementation route. Merrill-Crowe is strongly preferred for high-silver ores because
silver overloads carbon-stripping circuits. The precipitate/loaded carbon is smelted
to **doré** (impure Au-Ag bullion), then purified: the **Miller process**
(chlorine gas bubbled through molten doré, ~99.5%) followed by the **Wohlwill
process** (electrorefining in a chloroauric acid / HCl electrolyte) to reach
99.99–99.999% fine gold.

**Refractory gold (gameplay depth).** When gold is finely disseminated and locked
inside sulfides — chiefly **arsenopyrite (FeAsS)** and **pyrite (FeS₂)** — direct
cyanidation fails (cyanide/oxygen can't reach the gold). The concentrate must first
be **oxidized** to destroy the sulfide lattice via one of three competing
pretreatments, a natural tech-tree branch:
- **Roasting** — oldest, drives off S as SO₂ and As as As₂O₃ (toxic off-gas).
- **Pressure oxidation (POX)** — autoclave, 1–3 h, up to ~99.5% subsequent recovery.
- **Bio-oxidation (BIOX)** — bacterial, cheap/low-energy but slow (days vs hours),
  ~2% lower recovery than POX.
Only after oxidation does the standard cyanidation → adsorption → doré → refining
chain apply. **Aurostibite (AuSb₂)** is a separate antimony-bearing branch where
roasting removes antimony before leaching.

### Silver
Silver is overwhelmingly **byproduct-driven**. Primary silver minerals exist
(**argentite/acanthite Ag₂S**, **native silver**), but most mined silver is recovered
as a byproduct when **lead, zinc, and copper** ores are smelted/refined. Two main
routes:
- **Primary / cyanide circuits:** flotation → cyanidation → **Merrill-Crowe**
  (ideal, since Ag tolerance is high) → doré → **cupellation** and/or **Moebius
  electrorefining** (silver-nitrate electrolyte; the silver analogue of Wohlwill).
- **Lead-smelter byproduct (Parkes process):** molten argentiferous lead is dosed
  with **zinc**, in which silver is far more soluble; the Zn-Ag crust floats and is
  skimmed, zinc is distilled off, and the Ag-Pb alloy is **cupellated** (air
  oxidizes Pb to litharge, leaving silver) then electrorefined (Moebius).

### Platinum-group metals (PGMs)
PGMs are almost entirely **byproduct/co-product driven** and never recovered alone.
Two dominant deposit styles:
- **Bushveld Complex (South Africa): Merensky Reef & UG2 reef** — ~90% of world PGM
  reserves. Minerals: **sperrylite (PtAs₂)**, **cooperite (PtS)**,
  **braggite ((Pt,Pd,Ni)S)**, plus laurite (Ru) and PGE alloys.
- **Norilsk (Russia)** and Sudbury / Stillwater — PGMs (especially **palladium**)
  as a byproduct of **nickel-copper sulfide** ore; Pd hosted in pentlandite and PGM
  minerals.

**PGM refining chain (gameplay depth).** This is the deepest chain in scope:
1. **Grinding + gravity + froth flotation** (xanthate/dithiophosphate collectors,
   pH 7.5–9) to make a **Ni-Cu-PGM sulfide flotation concentrate**.
2. **Smelting** in flash/electric furnaces → **PGM-rich matte** (separates sulfides
   from silicate gangue).
3. **Converting** + **base-metal leach** to strip Ni, Cu, Co, Fe → a PGM-rich
   residue.
4. **Pressure-oxidation / dissolution-precipitation leach**, then a long
   **solvent-extraction + ion-exchange + selective precipitation** cascade to
   separate the six PGMs (Pt, Pd, Rh, Ru, Ir, Os) individually.
5. **Refining** to each pure metal.
Cross-link: the Ni, Cu, and Co stripped in step 3 feed the base-metals catalog.

### Mercury
**Cinnabar (HgS)** is the near-exclusive mercury ore. Processing is simple and
distinctive: after crushing and optional gravity/flotation concentration, the ore is
**roasted / retorted** at 500–600°C. HgS + O₂ → Hg vapor + SO₂; the mercury vapor is
**condensed** on cooling and collected as liquid metal (90–95% recovery). Rotary
kilns or multiple-hearth furnaces for large tonnage; externally heated **retorts**
for fine ore or small operations. Collected mercury is further purified by
distillation.

## Byproduct cross-links (summary)
- **Silver** ⟶ byproduct of **lead / zinc / copper** smelting (Parkes); also co-metal
  in **electrum** (gold ores).
- **Gold** ⟶ carries **silver** (electrum, doré); refractory gold ⟶ **arsenic**
  (arsenopyrite) and **sulfur/iron** (pyrite) in off-gas/acid streams; aurostibite ⟶
  **antimony**.
- **PGMs** ⟶ co-products **Pt-Pd-Rh-Ru-Ir-Os** together; byproduct of **nickel-copper
  (-cobalt)** sulfide smelting (Norilsk/Sudbury); sperrylite also carries **arsenic**.
- **Mercury** ⟶ emits **sulfur dioxide**; can co-occur with stibnite (**antimony**).
