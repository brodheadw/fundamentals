import json
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
RESOURCES = REPO / "src/main/resources"
ASSETS = RESOURCES / "assets/fundamentals"
DATA = RESOURCES / "data/fundamentals"
TEXTURES = ASSETS / "textures"
JAVA = REPO / "src/main/java/ai/gsmc/fundamentals"
LANG = ASSETS / "lang/en_us.json"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag(path, values):
    write(path, {"replace": False, "values": sorted(values)})


def read_lang():
    return json.loads(LANG.read_text(encoding="utf-8"))


def write_lang(lang):
    write(LANG, dict(sorted(lang.items())))


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
