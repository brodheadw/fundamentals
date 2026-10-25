#!/bin/zsh
# build_ore_data runs last: it owns the mining-tool tags the other generators' blocks go into.
set -e
cd "$(dirname "$0")"
for script in paint_minerals paint_materials paint_elements paint_ironworking paint_oxidation paint_plastics paint_power \
        paint_separation paint_thermometers paint_titanium paint_uses paint_compute build_material_data build_separation_data build_uses_data \
        build_heat_data build_cold_data build_oxidation_data build_compute_data build_chemica_compat build_create_nuclear_compat build_arms_data build_book build_ponder build_advancements build_catalog build_byproducts build_ore_data \
        showcase/build_showcase; do
    python3 $script.py > /dev/null
done
