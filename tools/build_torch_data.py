#!/usr/bin/env python3
"""Torches that burn down: the burnt-out stub's models, blockstates, loot, re-dipping recipe and lang."""
from common import ASSETS, DATA, read_lang, write, write_lang

# the wall torch's model faces east; vanilla turns it for the other three
WALL = {"east": 0, "north": 270, "south": 90, "west": 180}


def main():
    lang = read_lang()
    lang["block.fundamentals.burnt_torch"] = "Burnt-Out Torch"
    write_lang(lang)
    write(ASSETS / "blockstates/burnt_torch.json", {"variants": {"": {"model": "fundamentals:block/burnt_torch"}}})
    write(ASSETS / "blockstates/burnt_wall_torch.json", {"variants": {
        f"facing={side}": {"model": "fundamentals:block/burnt_wall_torch", **({"y": y} if y else {})} for side, y in WALL.items()}})
    write(ASSETS / "models/block/burnt_torch.json", {"parent": "minecraft:block/template_torch", "textures": {"torch": "fundamentals:block/burnt_torch"}})
    write(ASSETS / "models/block/burnt_wall_torch.json", {"parent": "minecraft:block/template_torch_wall",
                                                         "textures": {"torch": "fundamentals:block/burnt_torch"}})
    write(ASSETS / "models/item/burnt_torch.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "fundamentals:block/burnt_torch"}})
    # the wall stub drops like the standing one
    write(DATA / "loot_table/blocks/burnt_torch.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "fundamentals:burnt_torch"}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # dipped in fresh coal, the stub is a torch again
    write(DATA / "recipe/torch_from_burnt_torch.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                       "ingredients": [{"item": "fundamentals:burnt_torch"}, {"tag": "minecraft:coals"}],
                                                       "result": {"id": "minecraft:torch", "count": 1}})
    print("torches written")


if __name__ == "__main__":
    main()
