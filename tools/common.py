import json
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
RESOURCES = REPO / "src/main/resources"
ASSETS = RESOURCES / "assets/fundamentals"
DATA = RESOURCES / "data/fundamentals"
TEXTURES = ASSETS / "textures"
JAVA = REPO / "src/main/java/ai/gsmc/fundamentals"
LANG = ASSETS / "lang/en_us.json"
TFMG_JAR = next(Path.home().glob(".gradle/caches/modules-2/files-2.1/maven.modrinth/create-tfmg/*/*/create-tfmg-*.jar"))


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag(path, values):
    write(path, {"replace": False, "values": sorted(values)})


def read_lang():
    return json.loads(LANG.read_text(encoding="utf-8"))


def write_lang(lang):
    write(LANG, dict(sorted(lang.items())))


# Ores whose raw form is the vanilla item rather than one of ours.
VANILLA_RAW = {"native_gold": "minecraft:raw_gold"}


def raw_item(mineral):
    return VANILLA_RAW.get(mineral, f"fundamentals:raw_{mineral}")


def ns(id):
    return id if ":" in id else f"fundamentals:{id}"


def fluid(id, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": ns(id)}


def drop_self(name, conditions=()):
    return {"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"fundamentals:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}, *conditions]}


def cube(texture):
    return {"parent": "minecraft:block/cube_all", "textures": {"all": texture}}


def result(id, count=1):
    return {"id": ns(id), **({"count": count} if count > 1 else {})}


def result_fluid(id, amount):
    return {"id": ns(id), "amount": amount}


def mixing(path, ingredients, results, heat=None):
    recipe = {"type": "create:mixing", "ingredients": ingredients, "results": results}
    if heat:
        recipe["heat_requirement"] = heat
    write(path, recipe)


def vat(path, ingredients, results, machines=("tfmg:mixing",), heat="heated", time=100):
    recipe = {"type": "tfmg:vat_machine_recipe", "allowed_vat_types": ["tfmg:steel_vat", "tfmg:firebrick_lined_vat"],
              "machines": list(machines), "min_size": 1, "processing_time": time, "ingredients": ingredients, "results": results}
    if heat:
        recipe["heat_requirement"] = heat
    write(path, recipe)


def hide_dead(jar, namespace, candidate):
    """Hides from the recipe viewer the items of an optional mod that nothing makes once our data stands in for it:
    no recipe of ours or of its own (unless we override that file) gives them. Writes fundamentals:hidden/<namespace>."""
    def made(recipe):
        if any(c.get("type") == "neoforge:false" for c in recipe.get("neoforge:conditions", [])):
            return set()
        return {r.get("id", r.get("item")) for r in recipe.get("results", []) + [recipe.get("result") or {}] if isinstance(r, dict)}
    produced = set()
    for entry in RESOURCES.glob("data/**/recipe/**/*.json"):
        produced |= made(json.loads(entry.read_text()))
    prefix = f"data/{namespace}/recipe/"
    for entry in jar.namelist():
        if entry.startswith(prefix) and entry.endswith(".json") and not (RESOURCES / entry).exists():
            produced |= made(json.loads(jar.read(entry)))
    items = {f"{namespace}:{Path(e).stem}" for e in jar.namelist() if e.startswith(f"assets/{namespace}/models/item/") and e.endswith(".json")}
    dead = sorted(i for i in items if candidate(i) and i not in produced)
    write(DATA / f"tags/item/hidden/{namespace}.json", {"replace": False, "values": [{"id": i, "required": False} for i in dead]})
    write(RESOURCES / "data/c/tags/item/hidden_from_recipe_viewers.json", {"replace": False, "values": [
        {"id": f"#fundamentals:hidden/{mod}", "required": False} for mod in ("chemica", "cold_sweat", "createnuclear", "tfmg")]})
    return len(dead)
