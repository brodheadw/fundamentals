# Industrial (Non-Metal / Bulk / Chemical-Feedstock) Minerals

Research for the **fundamentals** Minecraft mod — chemistry-driven materials & processing framework.
Scope: industrial minerals that are **not** primarily smelted to a metal. These feed non-metal
production chains (fertilizers, alkalis, chlorine, silicon, abrasives, ceramics, battery anodes).

Date: 2026-10-04. Facts verified via web search (sources at bottom).

> **Canonical stage vocabulary:** Crushing, Grinding, Washing, Screening, GravitySeparation,
> FrothFlotation, Dissolution, Crystallization, Evaporation, Calcination, Roasting,
> Leaching(AcidPurification), Precipitation, Electrolysis(ChlorAlkali), CarbothermicReduction,
> SiemensProcess, SolvayProcess, ContactProcess, Refining, Purification.

---

## Summary Table

| Mineral / Rock | Formula | Target product | Key byproducts | Beneficiation | Intermediate | Process route | Final product |
|---|---|---|---|---|---|---|---|
| Phosphate rock (fluorapatite) | Ca5(PO4)3F (unit cell Ca10(PO4)6F2) | P2O5 / phosphoric acid | Gypsum (phosphogypsum), fluosilicic acid H2SiF6, **REEs**, U | Crushing, Grinding, Washing, Screening, FrothFlotation | Wet-process phosphoric acid (WPA) | Rock + H2SO4 digestion → slurry → filter off CaSO4 | Phosphoric acid / MAP/DAP/TSP fertilizer |
| Potash — sylvite / sylvinite | KCl (sylvinite = KCl+NaCl) | KCl (muriate of potash) | NaCl (halite tailings), brine | Crushing, Grinding, FrothFlotation **or** Dissolution | Pregnant brine (hot-leach route) | Flotation (amine) **or** hot dissolution → cool Crystallization | KCl fertilizer (95–96% flotation) |
| Potash — carnallite | KCl·MgCl2·6H2O | KCl | MgCl2 brine, NaCl | Crushing, Dissolution | Saturated K brine | Hot Dissolution → Evaporation → cool Crystallization | KCl fertilizer |
| Halite (rock salt) | NaCl | Cl2, NaOH, (Na) | H2 (cathode), HCl downstream | Crushing, Dissolution (solution mining), brine purification | Purified ~26% brine | Electrolysis(ChlorAlkali) — membrane cell | Chlorine + caustic soda (+H2) |
| Trona | Na2CO3·NaHCO3·2H2O | Na2CO3 (soda ash) | CO2, H2O | Crushing, Screening | Crude soda ash | Calcination (decompose sesquicarbonate) → dissolve/recrystallize | Natural soda ash |
| (Synthetic soda ash) | from NaCl + CaCO3 | Na2CO3 | CaCl2, NH3 recycled | — | NaHCO3 precipitate | SolvayProcess (NH3 + brine + CO2) → Calcination | Soda ash |
| Gypsum | CaSO4·2H2O | Plaster / wallboard | Water vapor | Crushing, Grinding | Stucco (hemihydrate CaSO4·½H2O) | Calcination 150–170 °C (dehydration) | Plaster of Paris / gypsum board |
| Limestone / calcite | CaCO3 | Quicklime CaO; cement feed | CO2 | Crushing, Screening, Washing | — | Calcination ~900 °C → (slaking w/ water) | Quicklime → slaked lime Ca(OH)2 / clinker |
| Silica (quartz) | SiO2 | **Si (silicon)** | CO/CO2, SiO2 fume, HCl | Crushing, Washing, GravitySeparation | **MG-Si → trichlorosilane SiHCl3** | CarbothermicReduction → chlorination/distillation → SiemensProcess | Metallurgical Si → **polysilicon** |
| Graphite (natural flake) | C | Spherical/purified graphite | Silicate gangue, acid waste | Crushing, Grinding, Screening, FrothFlotation | Flake concentrate (~90–95% C) | Spheroidization → Leaching(AcidPurification) / thermal Purification | Battery-grade spherical graphite (>99.9% C) |
| Kaolinite (china clay) | Al2Si2O5(OH)4 | Refined kaolin | Quartz/mica rejects | Washing, GravitySeparation, FrothFlotation | Kaolin slurry | Classification, bleaching (acid), dewatering | Paper/ceramic filler |
| Feldspar | (K,Na,Ca)(Al,Si)4O8 | Ceramic/glass flux | Quartz, mica, iron minerals | Crushing, Grinding, FrothFlotation (HF or HF-free) | — | Flotation to remove quartz/mica/Fe | Ceramic & glass feldspar |
| Garnet | X3Al2(SiO4)3 (e.g. almandine Fe3Al2(SiO4)3) | Abrasive / waterjet media | Ilmenite, magnetite | Crushing, Screening, GravitySeparation, magnetic sep | — | Sizing / magnetic cleaning | Blasting & waterjet abrasive |
| Diamond | C (cubic) | Abrasive / gem | Kimberlite gangue | Crushing, GravitySeparation (DMS), grease/X-ray sort | — | Dense-media separation → optical sorting | Industrial abrasive / gemstone |

---

## Per-Commodity Notes

### Phosphate rock (fluorapatite) — **cross-link hub**
Basic ore mineral is fluorapatite, **Ca5(PO4)3F** (crystallographic unit cell Ca10(PO4)6F2),
with a theoretical F:P2O5 weight ratio ~0.089. The dominant route is **wet-process phosphoric acid
(WPA)**: finely ground rock is digested with **sulfuric acid**, giving a slurry of phosphoric acid +
insoluble **calcium sulfate (phosphogypsum)** which is filtered off.
- **Fluorine byproduct:** evolves as SiF4 (and HF on concentration), recovered as **fluosilicic acid
  (H2SiF6)** — a feed for fluorine chemistry.
- **REE byproduct:** phosphate ores carry rare-earth elements; WPA liquor contains recoverable REEs
  (plus U, Cd impurities). This makes phosphate a secondary REE source.
- The sulfuric acid consumed links phosphate to the **ContactProcess** (H2SO4 from S/pyrite elsewhere).

### Potash (sylvite / sylvinite / carnallite)
Sylvite = **KCl** (63% K2O); sylvinite = KCl + NaCl; carnallite = KCl·MgCl2·6H2O (17% K2O).
Four beneficiation families: **froth flotation** (amine collector, ~95–96% KCl concentrate),
heavy-media separation, electrostatic separation, and **dissolution–crystallization (hot leach)**
exploiting KCl's steeper temperature-solubility vs NaCl (KCl crystallizes on cooling). Carnallite
needs energy-intensive dissolution + evaporation + cooling crystallization; MgCl2 brine is a byproduct.

### Halite → chlor-alkali — **tech-enabling chain**
Rock salt / solution-mined **NaCl** brine (purified to ~26% NaCl) is electrolyzed in a
**membrane cell**: a cation-exchange membrane passes only Na+. Products:
- **Cl2** at the anode,
- **NaOH** (caustic soda) in the catholyte,
- **H2** at the cathode.
This is the backbone of inorganic chemistry: **Cl2 feeds PVC/HCl/chlorinated routes (incl. the
silicon TCS step below), NaOH feeds alumina/pulp/soap**, and HCl loops back. Molten NaCl electrolysis
(Downs cell) instead yields **metallic Na + Cl2**.

### Trona / Solvay → soda ash
Natural route: **trona** (Na2CO3·NaHCO3·2H2O, ~70% Na2CO3) is **calcined** to drive off CO2 + H2O,
then dissolved/recrystallized to natural soda ash (~30% of world supply).
Synthetic route: **SolvayProcess** (~70% of supply) — ammoniated brine + CO2 (from **limestone
calcination**) precipitates **NaHCO3**, which is calcined to **Na2CO3**; NH3 is recycled, CaCl2 is the
waste byproduct. Links soda ash to both **salt** and **limestone**.

### Gypsum → plaster
**CaSO4·2H2O** calcined at 150–170 °C loses 1½ waters to give **hemihydrate (CaSO4·½H2O, "stucco"/
plaster of Paris)**; harder calcination gives anhydrite. β-hemihydrate (atmospheric) for board;
α-hemihydrate (autoclave, denser) for high-strength plasters. Note **phosphogypsum** from phosphate
processing is a synthetic gypsum source.

### Limestone / calcite → lime & cement
**CaCO3** calcined ~900 °C releases CO2 → **quicklime CaO**. Slaking with water gives
**slaked/hydrated lime Ca(OH)2** (exothermic). With clay in a rotary kiln, CaO fuses to **clinker**
→ ground to Portland cement. Calcination CO2 is a feed for the Solvay process.

### Silica (quartz) → silicon — **tech-enabling chain (the Si backbone)**
1. **CarbothermicReduction:** quartz **SiO2** + carbon (coke/coal/charcoal/woodchips) in a submerged
   arc furnace at 1500–2000 °C → **metallurgical-grade silicon (MG-Si, 98–99%)** + CO.
2. **Chlorination:** MG-Si + HCl over Cu catalyst (fluidized bed) → **trichlorosilane (TCS, SiHCl3)**,
   purified by multiple **distillations** (the HCl ties this to the chlor-alkali chain).
3. **SiemensProcess:** purified TCS + H2, CVD onto heated Si seed rods → **ultra-pure polysilicon**
   (solar/semiconductor grade); byproduct HCl is recycled.
Energy-intensive throughout; the enabling path for solar PV and semiconductors.

### Graphite (natural flake) → battery anode
Flake ore: Crushing → Grinding → Screening → **froth flotation** → concentrate ~90–95% C.
For anodes: **spheroidization** (round the flakes) then **chemical purification** — acid–alkali–acid
(H2SO4/H2O2 → NaOH → HCl) and/or **thermal** treatment — to reach **>99.9% (ideally >99.98%) C**.
Purified spherical graphite boosts anode capacity (~350 vs ~280 mAh/g).

### Clays, feldspar, garnet, diamond (brief)
- **Kaolinite** Al2Si2O5(OH)4: wet-processed (washing, hydrocyclone classification, flotation,
  acid bleaching) for paper/ceramics filler.
- **Feldspar** (K,Na,Ca)(Al,Si)4O8: flotation removes quartz, mica and iron minerals → glass/ceramic flux.
- **Garnet** (almandine Fe3Al2(SiO4)3): crushed, sized, magnetically cleaned → blasting/waterjet abrasive.
- **Diamond** C: dense-media separation + X-ray/grease sorting → industrial abrasive or gem.

---

## Sources
- Phosphate / WPA / F / REE: [911 Metallurgist](https://www.911metallurgist.com/blog/how-to-remove-phosphate-rock-fluorine/), [IntechOpen — Utilization of Apatite Ores](https://www.intechopen.com/chapters/49965), [NeoNickel](https://www.neonickel.com/neonickel-news/phosphoric-acid-production)
- Potash: [911 Metallurgist — potash flotation](https://www.911metallurgist.com/blog/carnallite-sylvinite-ore-potash-flotation/), [BC Insight — overview of potash ore processing](https://www.bcinsight.crugroup.com/2024/03/31/an-overview-of-potash-ore-processing/)
- Chlor-alkali: [Lenntech — membrane cell](https://www.lenntech.com/applications/membrane-cell-process-for-chlor-alkali-production.htm), [Chandra Asri — chlor-alkali](https://chandra-asri.com/en/blog/what-is-chlor-alkali)
- Soda ash / Solvay / trona: [Wikipedia — Solvay process](https://en.wikipedia.org/wiki/Solvay_process), [Unseel — Solvay](https://unseel.com/chemistry/solvay-process), [METU thesis — Beypazari trona](https://etd.lib.metu.edu.tr/upload/663526/index.pdf)
- Silicon / Siemens: [PV Manufacturing — polysilicon production](https://pv-manufacturing.org/silicon-production/polysilicon-production/), [ScienceDirect — Siemens Process](https://www.sciencedirect.com/topics/engineering/siemens-process), [Elkem — quartz to silicon](https://magazine.elkem.com/material-science-insights/from-quartz-to-silicon-to-silicones/)
- Graphite: [ScienceDirect — chemical purification for Li-ion anodes](https://www.sciencedirect.com/science/article/abs/pii/S235249282032448X), [MDPI — flotation limitation](https://doi.org/10.3390/min14121187)
- Gypsum: [ScienceDirect — calcium sulfate hemihydrate](https://www.sciencedirect.com/topics/chemistry/calcium-sulfate-hemihydrate)
- Lime / cement: [Lhoist — quicklime](https://www.lhoist.com/en-US/products-and-services/material/quicklime), [ScienceDirect — lime](https://www.sciencedirect.com/topics/chemical-engineering/lime)
- Clays / feldspar / garnet: [IMA Europe — kaolin](https://ima-europe.eu/about-industrial-minerals/kaolin/), [ScienceDirect — feldspar beneficiation](https://www.sciencedirect.com/science/chapter/bookseries/abs/pii/S0167452800800873), [Minerals Education Coalition — kaolinite](https://mineralseducationcoalition.org/minerals-database/kaolinite/)
