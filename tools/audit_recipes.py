"""Audits Will's recipes against the whole Wildspell pack: overlaps, dupe loops and shaped-pattern symmetry.

Loads every recipe and item/fluid tag from vanilla, NeoForge and the pack's mod jars, with Will's mods read
from their source trees instead of their jars, then reports:
  - overlaps: a grid (or machine input) that one of Will's recipes and another recipe both match;
  - dupe loops: conversions that come back to the item they started from with more of it;
  - asymmetric shaped patterns in Will's mods.
Exits 1 when any overlap or dupe loop involves Will's recipes.

    python3 tools/audit_recipes.py [--fundamentals DIR] [--magic DIR] [--mobs DIR] [--principles DIR]
"""

import argparse
import itertools
import json
import re
import sys
import zipfile
from collections import defaultdict
from fractions import Fraction
from io import BytesIO
from pathlib import Path

HOME = Path.home()
PROFILE = HOME / "Library/Application Support/ModrinthApp/profiles/Wildspell/mods"
VANILLA = HOME / ".gradle/caches/fabric-loom/1.21.1/minecraft-merged.jar"
NEOFORGE = sorted((HOME / "Library/Application Support/ModrinthApp/meta/libraries/net/neoforged/neoforge").glob("21.1.*/neoforge-*-universal.jar"))
WILL = {"fundamentals": "fundamentals", "wildspellmagic": "wildspell/wildspell-magic", "wildspellmobs": "wildspell/wildspell-mobs",
        "fundamentalmagic": "wildspell/fundamental-magic"}
JAR_PREFIX = {"fundamentals": "fundamentals-", "wildspellmagic": "wildspellmagic-", "wildspellmobs": "wildspellmobs-",
              "fundamentalmagic": "fundamentalmagic-"}

SHAPED = {"minecraft:crafting_shaped", "wildspellmobs:soulseeker"}
SHAPELESS = {"minecraft:crafting_shapeless"}
# Basin machines take whatever is in the basin; Create tries the recipes with the most item ingredients first.
BASIN = {"create:mixing", "create:compacting"}
# Recipes picked by the player (the stonecutter lists every result) or matched by code, not by their inputs.
NOT_COMPARED = {"minecraft:stonecutting", "minecraft:smithing_transform", "minecraft:smithing_trim"}
ALIASES = {"minecraft:crafting_shaped": "crafting", "minecraft:crafting_shapeless": "crafting", "wildspellmobs:soulseeker": "crafting"}


class Source:
    """One datapack root: a jar (with its jar-in-jar mods) or a source tree."""

    def __init__(self, name, will, files, mod_ids):
        self.name, self.will, self.files, self.mod_ids = name, will, files, mod_ids


def jar_source(name, path, will=False):
    files, mod_ids = {}, set()

    def walk(zf):
        for entry in zf.namelist():
            if entry.startswith("data/") and entry.endswith(".json") and ("/tags/" in entry or "/recipe" in entry):
                files.setdefault(entry, zf.read(entry))
            elif entry in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                mod_ids.update(re.findall(r'modId\s*=\s*"([^"]+)"', zf.read(entry).decode("utf-8", "replace")))
            elif entry.startswith("META-INF/jarjar/") and entry.endswith(".jar"):
                walk(zipfile.ZipFile(BytesIO(zf.read(entry))))

    walk(zipfile.ZipFile(path))
    return Source(name, will, files, mod_ids)


def tree_source(name, root):
    resources = root / "src/main/resources"
    files = {str(p.relative_to(resources)): p.read_bytes() for p in (resources / "data").rglob("*.json")
             if "/tags/" in str(p) or "/recipe" in str(p)}
    return Source(name, True, files, {name})


def load_sources(args):
    sources = [jar_source("minecraft", VANILLA), jar_source("neoforge", NEOFORGE[-1])]
    sources[0].mod_ids.add("minecraft")
    sources[1].mod_ids.add("neoforge")
    for jar in sorted(PROFILE.glob("*.jar")):
        if not any(jar.name.startswith(prefix) for prefix in JAR_PREFIX.values()):
            sources.append(jar_source(jar.stem, jar))
    for mod, default in WILL.items():
        sources.append(tree_source(mod, Path(getattr(args, mod) or HOME / "Github" / default)))
    return sources


def parse(raw):
    try:
        return json.loads(raw)
    except ValueError:
        return None


class Pack:
    def __init__(self, sources):
        self.sources = sources
        self.mods = set().union(*(s.mod_ids for s in sources))
        self.item_tags = self._tags(("tags/item/", "tags/items/"))
        self.fluid_tags = self._tags(("tags/fluid/", "tags/fluids/"))
        self._resolved = {}
        self.recipes = self._recipes()

    def _tags(self, folders):
        tags = {}
        for source in self.sources:
            for path, raw in source.files.items():
                for folder in folders:
                    if f"/{folder}" in path:
                        ns = path.split("/")[1]
                        tag_id = f"{ns}:{path.split('/' + folder, 1)[1][:-5]}"
                        data = parse(raw) or {}
                        entry = tags.setdefault(tag_id, {"values": [], "remove": []})
                        if data.get("replace"):
                            entry["values"] = []
                        entry["values"] += data.get("values", [])
                        entry["remove"] += data.get("remove", [])
        return tags

    def exists(self, item_id):
        return item_id.split(":")[0] in self.mods

    def tag(self, tag_id, fluid=False, stack=()):
        key = (tag_id, fluid)
        if key in self._resolved:
            return self._resolved[key]
        tags = self.fluid_tags if fluid else self.item_tags
        out = set()
        if tag_id in tags and tag_id not in stack:
            for value in tags[tag_id]["values"]:
                ref = value["id"] if isinstance(value, dict) else value
                if ref.startswith("#"):
                    out |= self.tag(ref[1:], fluid, stack + (tag_id,))
                elif self.exists(ref):
                    out.add(ref)
            for value in tags[tag_id]["remove"]:
                ref = value["id"] if isinstance(value, dict) else value
                out -= self.tag(ref[1:], fluid, stack + (tag_id,)) if ref.startswith("#") else {ref}
        self._resolved[key] = frozenset(out)
        return self._resolved[key]

    def condition(self, cond):
        kind = cond.get("type", "")
        if kind == "neoforge:mod_loaded":
            return cond["modid"] in self.mods
        if kind == "neoforge:not":
            return not self.condition(cond["value"])
        if kind == "neoforge:and":
            return all(self.condition(c) for c in cond["values"])
        if kind == "neoforge:or":
            return any(self.condition(c) for c in cond["values"])
        if kind == "neoforge:false":
            return False
        if kind == "neoforge:item_exists":
            return self.exists(cond["item"])
        if kind == "neoforge:tag_empty":
            return not self.tag(cond["tag"])
        return True

    def _recipes(self):
        recipes = {}
        for source in self.sources:
            for path, raw in source.files.items():
                parts = path.split("/")
                if len(parts) < 4 or parts[2] not in ("recipe", "recipes"):
                    continue
                recipe_id = f"{parts[1]}:{'/'.join(parts[3:])[:-5]}"
                data = parse(raw)
                if not isinstance(data, dict):
                    continue
                if not all(self.condition(c) for c in data.get("neoforge:conditions", [])) or "type" not in data:
                    recipes.pop(recipe_id, None)
                    continue
                recipes[recipe_id] = Recipe(recipe_id, source, data)
        return recipes

    def ingredient(self, value):
        """The item ids an ingredient accepts, or None when it isn't an item ingredient."""
        if isinstance(value, list):
            out = set()
            for child in value:
                items = self.ingredient(child)
                out |= items or set()
            return frozenset(out)
        if not isinstance(value, dict):
            return frozenset({value}) if isinstance(value, str) and ":" in value else None
        kind = value.get("type", "")
        if "fluid" in value or "fluid_tag" in value or kind in ("neoforge:single", "neoforge:tag", "fluid_stack", "fluid_tag") and "amount" in value:
            return None
        if "item" in value:
            return frozenset({value["item"]}) if self.exists(value["item"]) else frozenset()
        if "tag" in value:
            return self.tag(value["tag"])
        if kind == "neoforge:compound":
            return self.ingredient(value.get("ingredients", value.get("children", [])))
        if kind == "neoforge:difference":
            return self.ingredient(value["base"]) - self.ingredient(value["subtracted"])
        if kind == "neoforge:intersection":
            sets = [self.ingredient(c) for c in value.get("children", value.get("ingredients", []))]
            return frozenset.intersection(*sets)
        if kind in ("neoforge:components", "neoforge:block_tag"):
            items = value.get("items")
            if isinstance(items, str):
                items = self.tag(items[1:]) if items.startswith("#") else frozenset({items})
            else:
                items = self.ingredient(items) if items else frozenset()
            if "components" in value:
                # Only stacks carrying these components: X|{...} meets a plain X but not X with other components.
                stamp = json.dumps(value["components"], sort_keys=True)
                return frozenset(f"{i}|{stamp}" for i in items)
            return items
        return frozenset()

    def fluid(self, value):
        if not isinstance(value, dict):
            return None
        if "fluid" in value:
            return frozenset({value["fluid"]})
        if "fluid_tag" in value:
            return self.tag(value["fluid_tag"], fluid=True)
        if value.get("type") == "neoforge:tag" and "amount" in value:
            return self.tag(value["tag"], fluid=True)
        return None


class Recipe:
    def __init__(self, recipe_id, source, data):
        self.id, self.source, self.data, self.type = recipe_id, source, data, data["type"]
        self.will = source.will

    def __repr__(self):
        return self.id


def trimmed(pattern):
    rows = [r for r in pattern]
    width = max(len(r) for r in rows)
    rows = [r.ljust(width) for r in rows]
    while rows and not rows[0].strip():
        rows.pop(0)
    while rows and not rows[-1].strip():
        rows.pop()
    cols = [i for i in range(width) if any(r[i] != " " for r in rows)]
    if not cols:
        return []
    return [r[cols[0]:cols[-1] + 1] for r in rows]


class Model:
    """Each recipe as the inputs that make a machine (or the grid) pick it."""

    def __init__(self, pack):
        self.pack = pack
        self.grid, self.machine, self.auto, self.unparsed = {}, {}, {}, defaultdict(list)
        for recipe in pack.recipes.values():
            self.add(recipe)

    def shaped(self, recipe):
        data = recipe.data
        keys = {k: self.pack.ingredient(v) for k, v in data.get("key", {}).items()}
        rows = trimmed(data["pattern"])
        return [[None if c == " " else keys.get(c, frozenset()) for c in row] for row in rows]

    def add(self, recipe):
        data, kind, pack = recipe.data, recipe.type, self.pack
        if kind in NOT_COMPARED:
            return
        if kind == "fundamentals:magnet_rebuild":
            self.grid[recipe.id] = ("shapeless", [pack.tag("fundamentals:magnet_machines"), magnet_items(pack)])
            return
        if "pattern" in data and "key" in data:
            grid = self.shaped(recipe)
            if kind == "create:mechanical_crafting":
                self.machine[recipe.id] = (kind, ("shaped", grid, data.get("accept_mirrored", True)))
            else:
                self.grid[recipe.id] = ("shaped", grid)
                self.auto_basin(recipe.id, cells(grid), square=len(grid) == len(grid[0]))
            return
        if kind in SHAPELESS or (kind.endswith("shapeless") and "ingredients" in data):
            ings = [pack.ingredient(i) for i in data["ingredients"]]
            self.grid[recipe.id] = ("shapeless", ings)
            self.auto_basin(recipe.id, ings, square=True, shapeless=True)
            return
        if "crafting_special" in kind or kind.startswith("minecraft:crafting_"):
            return
        items, fluids = [], []
        if kind == "create:sequenced_assembly":
            # The deployer (or press, spout...) tries sequenced assembly first: its first step on the base item
            # competes with every recipe of that step's type.
            first = (data.get("sequence") or [{}])[0]
            items = [pack.ingredient(data["ingredient"])]
            for ing in first.get("ingredients", [])[1:]:
                f = pack.fluid(ing)
                (fluids.append(f) if f is not None else items.append(pack.ingredient(ing)))
            self.machine[recipe.id] = (first.get("type"), ("inputs", items, fluids, {}))
            return
        if kind in ("fundamentals:separation", "fundamentals:magnetic"):
            self.machine[recipe.id] = (kind, ("inputs", [], [frozenset({data["liquor"]})], {}))
            return
        raw = data.get("ingredients")
        if raw is None and "ingredient" in data:
            raw = [data["ingredient"]]
        if raw is None:
            self.unparsed[kind].append(recipe.id)
            return
        for ing in raw:
            f = pack.fluid(ing)
            if f is not None:
                fluids.append(f)
            else:
                items.append(pack.ingredient(ing))
        context = {k: data[k] for k in ("machines", "allowed_vat_types") if k in data}
        self.machine[recipe.id] = (kind, ("inputs", items, fluids, context))

    def auto_basin(self, rid, ings, square, shapeless=False):
        """Create's mixer takes shapeless crafting recipes, its press square grids of one ingredient."""
        if len(ings) in (4, 9) and square and all(i == ings[0] for i in ings):
            self.auto[rid] = ("create:compacting", ("inputs", ings, [], {}))
        elif shapeless and len(ings) > 1:
            self.auto[rid] = ("create:mixing", ("inputs", ings, [], {}))


def magnet_items(pack):
    items = {i for i in pack.tag("c:magnets")} | {"tfmg:magnet"}
    items |= {r.data["results"][0]["id"] for r in pack.recipes.values()
              if r.type == "tfmg:polarizing" and r.will and r.data.get("results")}
    return frozenset(items)


_stamped = {}


def meets(a, b):
    """Whether one stack can satisfy both ingredients."""
    if not a.isdisjoint(b):
        return True
    for x, y in ((a, b), (b, a)):
        if x not in _stamped:
            _stamped[x] = frozenset(i.split("|")[0] for i in x if "|" in i)
        if not _stamped[x].isdisjoint(y):
            return True
    return False


def injects(small, large):
    """Whether every set in `small` pairs with a distinct set in `large` that one stack satisfies."""
    match = {}

    def augment(i, seen):
        for j, r in enumerate(large):
            if j not in seen and meets(small[i], r):
                seen.add(j)
                if j not in match or augment(match[j], seen):
                    match[j] = i
                    return True
        return False

    return len(small) <= len(large) and all(augment(i, set()) for i in range(len(small)))


def perfect_matching(left, right):
    return len(left) == len(right) and injects(left, right)


def cells(grid):
    return [c for row in grid for c in row if c is not None]


def grids_overlap(a, b, mirrored=True):
    if len(a) != len(b) or len(a[0]) != len(b[0]):
        return False

    def same(x, y):
        for ra, rb in zip(x, y):
            for ca, cb in zip(ra, rb):
                if (ca is None) != (cb is None) or (ca is not None and not meets(ca, cb)):
                    return False
        return True

    return same(a, b) or (mirrored and same(a, [row[::-1] for row in b]))


def grid_pair(a, b):
    if a[0] == "shaped" and b[0] == "shaped":
        return grids_overlap(a[1], b[1])
    left = cells(a[1]) if a[0] == "shaped" else a[1]
    right = cells(b[1]) if b[0] == "shaped" else b[1]
    return perfect_matching(left, right)


def machine_pair(kind, a, b):
    """Whether the inputs one recipe is fed can start the other in the same machine."""
    if a[0] == "shaped" or b[0] == "shaped":
        return a[0] == b[0] and grids_overlap(a[1], b[1], a[2] or b[2])
    _, items_a, fluids_a, ctx_a = a
    _, items_b, fluids_b, ctx_b = b
    if kind == "tfmg:vat_machine_recipe":
        # The vat takes the first recipe whose machines match exactly and whose ingredients are all present,
        # whatever else is in it, and only checks the heat once it has chosen.
        if ctx_a.get("machines", []) != ctx_b.get("machines", []):
            return False
        # A recipe that leaves the vat types out (or spells the key differently) takes every vat.
        vats_a, vats_b = ctx_a.get("allowed_vat_types"), ctx_b.get("allowed_vat_types")
        if vats_a is not None and vats_b is not None and not set(vats_a) & set(vats_b):
            return False
        return any(injects(xi, yi) and injects(xf, yf)
                   for (xi, xf), (yi, yf) in (((items_a, fluids_a), (items_b, fluids_b)), ((items_b, fluids_b), (items_a, fluids_a))))
    if kind in BASIN:
        # The basin holds anything and any heat runs an unheated recipe; recipes with more item ingredients are
        # tried first, so only a tie collides.
        if not perfect_matching(items_a, items_b):
            return False
        small, large = sorted((fluids_a, fluids_b), key=len)
        return injects(small, large)
    return perfect_matching(items_a, items_b) and perfect_matching(fluids_a, fluids_b)


def same_results(pack, a, b):
    return sorted(map(str, results(pack, pack.recipes[a], chances=True))) == sorted(map(str, results(pack, pack.recipes[b], chances=True)))


def overlaps(pack, model):
    found = []
    grid = list(model.grid.items())
    by_count = defaultdict(list)
    for rid, g in grid:
        by_count[len(cells(g[1]) if g[0] == "shaped" else g[1])].append((rid, g))
    for rid, g in grid:
        if not pack.recipes[rid].will:
            continue
        for other, h in by_count[len(cells(g[1]) if g[0] == "shaped" else g[1])]:
            if other == rid or (pack.recipes[other].will and other < rid):
                continue
            if grid_pair(g, h):
                found.append(("crafting", rid, other))
    by_type = defaultdict(list)
    for rid, (kind, m) in model.machine.items():
        by_type[kind].append((rid, m, False))
    for rid, (kind, m) in model.auto.items():
        by_type[kind].append((rid, m, True))
    for kind, entries in by_type.items():
        for (rid, m, auto_a), (other, n, auto_b) in itertools.combinations(entries, 2):
            if auto_a and auto_b or not (pack.recipes[rid].will or pack.recipes[other].will):
                continue
            if machine_pair(kind, m, n):
                found.append((kind + (" (auto from crafting)" if auto_a or auto_b else ""), rid, other))
    return found


def results(pack, recipe, chances=False):
    """(id, count) of every guaranteed result (with chances: every result, with its chance and components); fluids as
    fluid:<id> in mB."""
    data, out = recipe.data, []
    raw = data.get("results") or ([data["result"]] if "result" in data else [])
    if isinstance(raw, dict):
        raw = [raw]
    for r in raw:
        if isinstance(r, str):
            out.append((r, Fraction(1)))
            continue
        if not isinstance(r, dict) or (r.get("chance", 1) < 1 and not chances):
            continue
        rid = r.get("id") or r.get("item") or r.get("fluid")
        if rid is None:
            continue
        if chances:
            rid += json.dumps([r.get("chance", 1), r.get("components")], sort_keys=True)
        if "amount" in r:
            out.append(("fluid:" + rid, Fraction(r["amount"])))
        else:
            out.append((rid, Fraction(r.get("count", 1))))
    if "byproduct" in data and isinstance(data["byproduct"], dict):
        out.append((data["byproduct"]["id"], Fraction(data["byproduct"].get("count", 1))))
    return out


def conversions(pack, model):
    """Edges item -> item with a ratio, from recipes whose inputs are all one ingredient (or one fluid)."""
    edges = defaultdict(list)
    for recipe in pack.recipes.values():
        if recipe.type in ("create:sequenced_assembly", "fundamentals:magnet_rebuild"):
            continue
        data = recipe.data
        if recipe.type == "minecraft:stonecutting":
            inputs, fluid_in = [pack.ingredient(data["ingredient"])], []
        elif recipe.id in model.grid:
            g = model.grid[recipe.id]
            inputs, fluid_in = (cells(g[1]) if g[0] == "shaped" else g[1]), []
        elif recipe.id in model.machine and model.machine[recipe.id][1][0] == "inputs":
            _, inputs, fluid_in, _ = model.machine[recipe.id][1]
        elif recipe.id in model.machine:
            inputs, fluid_in = cells(model.machine[recipe.id][1][1]), []
        else:
            continue
        if inputs and not fluid_in and all(i == inputs[0] for i in inputs):
            sources, need = inputs[0], Fraction(len(inputs))
        elif fluid_in and not inputs and len(fluid_in) == 1:
            amount = next((i.get("amount") for i in data.get("ingredients", []) if isinstance(i, dict) and "amount" in i), None)
            if not amount:
                continue
            sources, need = frozenset("fluid:" + f for f in fluid_in[0]), Fraction(amount)
        else:
            continue
        if len(sources) > 64:
            continue
        for src in sources:
            for out, count in results(pack, recipe):
                edges[src].append((out, count / need, recipe))
    return edges


def dupe_loops(pack, model, max_len=3):
    edges = conversions(pack, model)
    loops, seen = [], set()
    for start in list(edges):
        stack = [(start, Fraction(1), [])]
        while stack:
            node, ratio, path = stack.pop()
            for nxt, r, recipe in edges.get(node, ()):
                step = path + [(node, nxt, r, recipe)]
                if nxt == start:
                    total = ratio * r
                    if total > 1 and any(s[3].will for s in step):
                        key = frozenset(s[3].id for s in step)
                        if key not in seen:
                            seen.add(key)
                            loops.append((total, step))
                elif len(step) < max_len and nxt not in {s[0] for s in step}:
                    stack.append((nxt, ratio * r, step))
    return loops


def symmetry(pack):
    rows = []
    for recipe in sorted(pack.recipes.values(), key=lambda r: r.id):
        if not recipe.will or "pattern" not in recipe.data:
            continue
        pattern = trimmed(recipe.data["pattern"])
        lr = all(row == row[::-1] for row in pattern)
        tb = pattern == pattern[::-1]
        rows.append((recipe, pattern, lr, tb))
    return rows


def describe(pack, rid):
    r = pack.recipes[rid]
    data = r.data
    if "pattern" in data:
        keys = {k: (v.get("item") or "#" + v.get("tag", "?")) if isinstance(v, dict) else str(v) for k, v in data["key"].items()}
        body = "|".join(data["pattern"]) + "  " + ", ".join(f"{k}={v}" for k, v in sorted(keys.items()))
    else:
        ings = data.get("ingredients") or [data.get("ingredient")]
        body = ", ".join((i.get("item") or ("#" + i["tag"] if "tag" in i else i.get("fluid", "?"))) if isinstance(i, dict) else str(i)
                         for i in ings)
        if data.get("heat_requirement"):
            body += f"  [{data['heat_requirement']}]"
    return f"{rid} ({r.source.name}, {r.type}): {body}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    for mod in WILL:
        parser.add_argument(f"--{mod}", help=f"source tree of {mod} (default ~/Github/{WILL[mod]})")
    parser.add_argument("--symmetry", action="store_true", help="also list every asymmetric shaped pattern")
    args = parser.parse_args()
    pack = Pack(load_sources(args))
    model = Model(pack)
    will = [r for r in pack.recipes.values() if r.will]
    print(f"{len(pack.recipes)} recipes in the pack, {len(will)} of them Will's "
          f"({len([r for r in will if r.id in model.grid])} crafting-grid, {len([r for r in will if r.id in model.machine])} machine)")
    for kind, ids in sorted(model.unparsed.items()):
        if any(pack.recipes[i].will for i in ids):
            print(f"  not compared ({kind}): {', '.join(i for i in ids if pack.recipes[i].will)}")

    every = overlaps(pack, model)
    found = [o for o in every if not same_results(pack, o[1], o[2])]
    print(f"\n{len(found)} overlaps involving Will's recipes")
    for kind, a, b in sorted(found):
        print(f"- [{kind}]\n    {describe(pack, a)}\n    {describe(pack, b)}")
    benign = [o for o in every if o not in found]
    print(f"\n{len(benign)} harmless overlaps (both recipes give the same results)")
    for kind, a, b in sorted(benign):
        print(f"- [{kind}] {a} / {b}")

    loops = dupe_loops(pack, model)
    print(f"\n{len(loops)} dupe loops involving Will's recipes")
    for total, step in sorted(loops, key=lambda x: -x[0]):
        chain = " -> ".join(f"{s[0]} ({s[3].id} x{float(s[2]):g})" for s in step)
        print(f"- x{float(total):g}: {chain} -> {step[0][0]}")

    sym = symmetry(pack)
    asym = [s for s in sym if not s[2]]
    print(f"\n{len(sym)} shaped patterns in Will's mods, {len(asym)} not left-right symmetric")
    if args.symmetry:
        for recipe, pattern, lr, tb in asym:
            print(f"- {recipe.id}: {'|'.join(pattern)}")
    sys.exit(1 if found or loops else 0)


if __name__ == "__main__":
    main()
