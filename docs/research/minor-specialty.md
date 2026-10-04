# Minor, Specialty & Radioactive Metals — Ore → Product Research

Scope: minor/specialty/radioactive commodities with their own primary ores. Excludes ferrous/ferroalloy, base metals, precious/PGM, light/battery/tech, and rare earths (covered elsewhere). Tungsten and mercury are handled elsewhere and are omitted here.

Hazard flags for gameplay hooks:
- **RADIOACTIVE**: Uranium, Thorium (and monazite feed). Ore handling, dust, and tailings carry radiological hazard; intermediates (yellowcake, ThO2) are regulated nuclear materials.
- **TOXIC**: Arsenic (As2O3 is acutely toxic; roaster off-gas and flue dust are poisonous), and to a lesser degree antimony and selenium/thallium commonly co-occurring. HF (fluorine route) is corrosive/toxic.

## Summary Table

| Mineral | Formula | Target commodity | Key byproducts | Beneficiation | Intermediate | Extraction route | Final product |
|---|---|---|---|---|---|---|---|
| Stibnite | Sb2S3 | Antimony | Au, Ag (co-recovered) | Crushing, Grinding, FrothFlotation | Sb2O3 (roaster oxide / crude antimony oxide) | Roasting / volatilizing roast → Sb2O3; CarbothermicReduction with Na2CO3 flux (or liquation of rich ore) | Antimony metal (regulus); Sb2O3 flame retardant |
| Bismuthinite | Bi2S3 | Bismuth | Pb, Cu, W, Mo, Ag (host) | Crushing, Grinding, FrothFlotation | Bi2O3 (roaster oxide) or crude Bi bullion | Roasting → oxide, Smelting/CarbothermicReduction with coke; largely byproduct of Pb/Cu/W refining (electrolytic slimes, debismuthizing) | Refined bismuth metal |
| Arsenopyrite | FeAsS | Arsenic (mostly byproduct) | Au, Cu, Co (host metals) | Crushing, Grinding, FrothFlotation | As2O3 (white arsenic, condensed from flue gas) | Roasting in air → volatilized As2O3 captured in baghouse/flues; reduction of As2O3 with C → metal | As2O3 (arsenic trioxide); arsenic metal |
| Realgar / Orpiment | As4S4 / As2S3 | Arsenic | — | Crushing, Grinding, GravitySeparation | As2O3 | Roasting (2 As2S3 + 9 O2 → 2 As2O3 + 6 SO2) | As2O3 |
| Uraninite (pitchblende) | UO2 | Uranium **(RADIOACTIVE)** | Ra, V (carnotite), REE, Mo | Crushing, Grinding (or in-situ leach) | Uranyl sulfate / uranyl tricarbonate liquor; yellowcake U3O8 | Leaching (acid H2SO4 or alkaline Na2CO3) → SolventExtraction / IonExchange → Precipitation → Calcination | Yellowcake (U3O8); then conversion to UF6 |
| Coffinite | U(SiO4)·nH2O | Uranium **(RADIOACTIVE)** | V, Se, Mo | Crushing, Grinding | Pregnant leach solution → U3O8 | Leaching → IonExchange/SolventExtraction → Precipitation → Calcination | Yellowcake (U3O8) |
| Carnotite | K2(UO2)2(VO4)2·3H2O | Uranium + Vanadium **(RADIOACTIVE)** | V2O5 | Crushing, Grinding | Co-recovered U and V liquors | Roasting (salt roast), Leaching → SolventExtraction/IonExchange → Precipitation | U3O8 + V2O5 |
| Thorianite | ThO2 | Thorium **(RADIOACTIVE)** | U, REE | Crushing, Grinding, GravitySeparation | Th(SO4)/Th(NO3) liquor | Leaching (acid/alkaline digest) → SolventExtraction → Precipitation → Calcination | ThO2 (thorium oxide) |
| Monazite | (Ce,La,Th)PO4 | Thorium (byproduct of REE) **(RADIOACTIVE)** | REE (primary), U | Washing, GravitySeparation, magnetic | Mixed RE/Th sulfate or hydroxide cake | Acid (H2SO4) or alkaline (NaOH) digest → selective Precipitation / SolventExtraction separates Th from REE | ThO2 (cross-linked to rare-earth chain) |
| Borax | Na2B4O7·10H2O | Boron / borates | — | Washing (dissolution–recrystallization) | Borate liquor | Dissolution, Evaporation/recrystallization; acidify with H2SO4 → boric acid | Borax, boric acid (H3BO3) |
| Kernite | Na2B4O7·4H2O | Boron / borates | — | Crushing, Washing | Borate liquor | Dissolution → Evaporation/recrystallization | Borax / boric acid |
| Colemanite | Ca2B6O11·5H2O | Boron / borates | — | Crushing, Grinding, FrothFlotation | Borate liquor | Acid Leaching (H2SO4) → Precipitation/crystallization | Boric acid (H3BO3) |
| Fluorite (fluorspar) | CaF2 | Fluorine / HF | Pb, Zn, barite (gangue) | Crushing, Grinding, FrothFlotation (acid-grade ≥97% CaF2) | Acid-grade fluorspar (acidspar) | React with conc. H2SO4 at ~250 °C: CaF2 + H2SO4 → 2 HF + CaSO4 | Hydrogen fluoride / hydrofluoric acid **(TOXIC/corrosive)** |
| Native sulfur | S | Sulfur → sulfuric acid | — | FraschProcess (superheated steam melt-extraction) | Molten/elemental S | Combustion S → SO2; ContactProcess (SO2→SO3→H2SO4) | Elemental sulfur; sulfuric acid |
| Pyrite | FeS2 | Sulfur → sulfuric acid | Fe (cinder), Au, Cu | Crushing, Grinding, FrothFlotation | SO2 roaster gas | Roasting (4 FeS2 + 11 O2 → 2 Fe2O3 + 8 SO2) → ContactProcess | Sulfuric acid (+ iron oxide cinder) |
| Sour gas H2S | H2S | Sulfur (recovered) | — | — (gas sweetening) | Elemental S | ClausProcess (partial oxidation of H2S → S); tail gas → ContactProcess | Elemental sulfur; sulfuric acid |
| Barite (baryte) | BaSO4 | Barium | — | Crushing, Grinding, GravitySeparation, FrothFlotation | BaS (black ash) | Reduction roasting with coke → BaS (black ash), then leach/precipitate to BaCO3/BaCl2 | Barite (drilling mud) direct; barium chemicals |
| Celestine (celestite) | SrSO4 | Strontium | Ba (solid-solution) | Crushing, Grinding, GravitySeparation, FrothFlotation | SrS (black ash) or direct SrCO3 | Black-ash: reduction roast → SrS → carbonation; or direct conversion with Na2CO3 | Strontium carbonate (SrCO3) |

## Per-commodity notes

**Antimony (stibnite Sb2S3).** Rich ore can be liquated (melt-separated). Sulfide concentrate is given a volatilizing/oxidizing roast to Sb2O3 (volatile ~900–950 °C in low-O2 gas; high O2 forms non-volatile SbO2 and hinders fuming), then carbothermically reduced with soda-ash flux to metal. Sulfur-fixation roasting with ZnO/C avoids SO2 emission. Antimony frequently co-occurs with gold.

**Bismuth (bismuthinite Bi2S3).** Overwhelmingly a byproduct of lead, copper, tin, and tungsten refining (e.g., debismuthizing of lead, treatment of anode slimes). Where processed as primary sulfide, roast to oxide then coke-reduce; alkaline/molten-salt smelting with Fe2O3 sulfur-fixing agent gives ~96% Bi recovery and co-recovers W/Mo to slag.

**Arsenic (arsenopyrite FeAsS; realgar As4S4; orpiment As2S3).** Almost entirely a byproduct — recovered as volatilized As2O3 ("white arsenic") from flue dust of gold, copper, and lead smelters/roasters. **TOXIC**: As2O3 is a potent poison; off-gas handling, baghouse dust, and stabilization of arsenic residues are the key hazards. Metal made by carbon reduction of the oxide.

**Uranium (uraninite/pitchblende UO2, coffinite, carnotite).** **RADIOACTIVE.** Mill or in-situ leach: acid (H2SO4, forming uranyl sulfate) or alkaline (Na2CO3, forming uranyl tricarbonate) leach; purify/concentrate by ion exchange or solvent extraction; precipitate and calcine to yellowcake (U3O8). Carnotite is a dual U–V ore (co-produces V2O5). Yellowcake is later converted to UF6 for enrichment. Radon/radium and tailings are gameplay hazard hooks.

**Thorium (thorianite ThO2; from monazite).** **RADIOACTIVE.** Thorianite is a minor direct source; the real supply is a byproduct of monazite (a RE phosphate, ~3–5% ThO2). Monazite is cracked by concentrated H2SO4 or NaOH, then Th is separated from the rare earths by selective precipitation/solvent extraction. **Cross-link: this chain is shared with the rare-earth catalog — Th is the radioactive co-product of REE recovery.**

**Boron (borax, kernite, colemanite).** Continental evaporite minerals. Sodium borates (borax, kernite) are purified by dissolution–recrystallization; calcium borate (colemanite) is acid-leached. Boric acid (H3BO3) is made by acidifying borate liquor with H2SO4. Brine operations use solar evaporation ponds.

**Fluorine (fluorite/fluorspar CaF2).** Acid-grade fluorspar (≥97% CaF2, ~60–65% of global output) reacts with hot concentrated H2SO4 to liberate HF (byproduct gypsum/anhydrite). HF is the feedstock for all fluorochemicals. **TOXIC/corrosive** intermediate.

**Sulfur (native, Frasch; pyrite FeS2; recovered Claus).** Native sulfur in salt domes is won by the Frasch hot-water process. Pyrite is roasted to SO2 (leaving iron-oxide cinder). Modern dominant source is recovered sulfur from sour natural gas/refinery H2S via the Claus process. All feed into the Contact Process (SO2 → SO3 → H2SO4).

**Barium (barite BaSO4) & Strontium (celestine SrSO4).** Isostructural members of the barite group (complete BaSO4–SrSO4 solid solution). Barite is mostly used directly (drilling mud, filler) via gravity/flotation beneficiation; barium chemicals go through reduction-roast "black ash" (BaS). Celestine → strontium carbonate by black-ash reduction roast (to SrS, ~1100–1300 °C) or direct soda-ash conversion.
