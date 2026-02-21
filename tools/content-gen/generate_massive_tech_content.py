
#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import argparse
import json
import math
import sys

BASE_METALS = [
    {"id": "copper", "title": "Copper", "color": "f08d49", "seed": "COPPER_INGOT"},
    {"id": "tin", "title": "Tin", "color": "c9d0d8", "seed": "IRON_INGOT"},
    {"id": "aluminum", "title": "Aluminum", "color": "e0e4ea", "seed": "IRON_NUGGET"},
    {"id": "nickel", "title": "Nickel", "color": "f5d17f", "seed": "GOLD_INGOT"},
    {"id": "silver", "title": "Silver", "color": "e7edf5", "seed": "QUARTZ"},
    {"id": "lead", "title": "Lead", "color": "6f7680", "seed": "COAL"},
    {"id": "zinc", "title": "Zinc", "color": "b7c0c6", "seed": "FLINT"},
    {"id": "titanium", "title": "Titanium", "color": "9ea8b8", "seed": "NETHERITE_SCRAP"},
    {"id": "tungsten", "title": "Tungsten", "color": "6d6f88", "seed": "OBSIDIAN"},
    {"id": "cobalt", "title": "Cobalt", "color": "4b7cff", "seed": "LAPIS_LAZULI"},
    {"id": "chromium", "title": "Chromium", "color": "ff5e5e", "seed": "REDSTONE"},
    {"id": "platinum", "title": "Platinum", "color": "7ef8ff", "seed": "DIAMOND"},
    {"id": "iridium", "title": "Iridium", "color": "73ffb8", "seed": "EMERALD"},
    {"id": "osmium", "title": "Osmium", "color": "b083ff", "seed": "AMETHYST_SHARD"},
    {"id": "uranium", "title": "Uranium", "color": "bbff66", "seed": "GLOWSTONE_DUST"},
    {"id": "palladium", "title": "Palladium", "color": "77d4e6", "seed": "PRISMARINE_CRYSTALS"},
]

DEFAULT_CATALYSTS = [
    "REDSTONE", "QUARTZ", "AMETHYST_SHARD", "LAPIS_LAZULI", "EMERALD", "DIAMOND",
    "BLAZE_POWDER", "ENDER_PEARL", "NETHER_STAR", "ECHO_SHARD", "NETHERITE_INGOT", "DRAGON_BREATH",
]

MODULE_UI = {
    "machines": {"display": "<gradient:yellow:gold><b>Machines</b></gradient>", "icon": "OBSERVER", "description": ["<gray>Multiblock machines and processing systems.</gray>"]},
    "materials": {"display": "<gradient:#f3a14c:#e3d09c><b>Materials</b></gradient>", "icon": "COPPER_INGOT", "description": ["<gray>Raw and refined industrial materials.</gray>"]},
    "components": {"display": "<gradient:#78c5ff:#bfd8ff><b>Components</b></gradient>", "icon": "CHAIN", "description": ["<gray>Plates, wires and assembly parts.</gray>"]},
    "automation": {"display": "<gradient:#d7d7d7:#a4adb8><b>Automation</b></gradient>", "icon": "CLOCK", "description": ["<gray>Mechanical transmission and gear logic.</gray>"]},
    "energy": {"display": "<gradient:#5ce8ff:#2c8bff><b>Energy</b></gradient>", "icon": "REDSTONE", "description": ["<gray>Power storage and high-density cells.</gray>"]},
    "weapons": {"display": "<gradient:#ff5c5c:#ffbf5c><b>Weapons</b></gradient>", "icon": "NETHERITE_SWORD", "description": ["<gray>Offensive tech progression tiers.</gray>"]},
    "armor": {"display": "<gradient:#5ce8ff:#9ad6ff><b>Armor</b></gradient>", "icon": "NETHERITE_CHESTPLATE", "description": ["<gray>Defensive gear with powered upgrades.</gray>"]},
    "enchantments": {"display": "<gradient:light_purple:blue><b>Enchantments</b></gradient>", "icon": "ENCHANTED_BOOK", "description": ["<gray>Specialized DrakesTech enhancements.</gray>"]},
}

DEFAULT_CONFIG = {
    "max_tier": 12,
    "start_custom_model_data": 30000,
    "enabled_metals": [m["id"] for m in BASE_METALS],
    "tier_catalysts": DEFAULT_CATALYSTS,
    "module_unlock_levels": {
        "machines": 0,
        "materials": 0,
        "components": 2,
        "automation": 4,
        "energy": 6,
        "weapons": 10,
        "armor": 10,
        "enchantments": 8,
    },
    "xp_model": {
        "materials": {"base": 0.5, "per_tier": 1.0, "power": 1.0},
        "components": {"base": 1.0, "per_tier": 1.0, "power": 1.0},
        "automation": {"base": 2.0, "per_tier": 1.15, "power": 1.0},
        "energy": {"base": 3.0, "per_tier": 1.2, "power": 1.05},
        "weapons": {"base": 4.0, "per_tier": 1.4, "power": 1.1},
        "armor": {"base": 4.0, "per_tier": 1.35, "power": 1.1},
        "default": {"base": 1.0, "per_tier": 1.0, "power": 1.0},
    },
    "difficulty_by_module": {
        "machines": 1.0,
        "materials": 1.0,
        "components": 1.0,
        "automation": 1.0,
        "energy": 1.0,
        "weapons": 1.0,
        "armor": 1.0,
        "enchantments": 1.0,
    },
    "output_multiplier_by_family": {
        "dust": 1.0,
        "ingot": 1.0,
        "plate": 1.0,
        "wire": 1.0,
        "coil": 1.0,
        "gear": 1.0,
        "core": 1.0,
        "cell": 1.0,
    },
}


def deep_merge(base: dict, override: dict) -> dict:
    result = dict(base)
    for key, value in override.items():
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = deep_merge(result[key], value)
        else:
            result[key] = value
    return result


def load_config(config_path: Path | None) -> dict:
    config = dict(DEFAULT_CONFIG)
    path = config_path
    if path is None:
        default_path = Path("tools/content-gen/generator-config.json")
        if default_path.exists():
            path = default_path

    if path is not None:
        payload = json.loads(path.read_text(encoding="utf-8"))
        config = deep_merge(config, payload)

    if config.get("max_tier", 0) < 1:
        raise ValueError("max_tier must be >= 1")
    if not config.get("tier_catalysts"):
        raise ValueError("tier_catalysts cannot be empty")
    if not config.get("enabled_metals"):
        raise ValueError("enabled_metals cannot be empty")

    return config


def q(text: str) -> str:
    return "'" + text.replace("'", "''") + "'"


def mat_title(material: str) -> str:
    return " ".join(part.capitalize() for part in material.lower().split("_") if part)


def normalize_token(token: str) -> str:
    clean = token.strip()
    if clean.startswith("item:") or clean.startswith("material:"):
        return clean
    return f"material:{clean}"


def token_to_text(token: str) -> str:
    if token.startswith("item:"):
        return token
    if token.startswith("material:"):
        return mat_title(token.split(":", 1)[1])
    return mat_title(token)

def recipe_lines(shape: list[str], ingredients: dict[str, str]) -> list[str]:
    rows: list[str] = []
    for row in shape:
        rows.append(" | ".join("-" if c == " " else token_to_text(ingredients[c]) for c in row))
    while len(rows) < 3:
        rows.append("- | - | -")
    return [
        f"<gray>Top row:</gray> <yellow>{rows[0]}</yellow>",
        f"<gray>Middle row:</gray> <yellow>{rows[1]}</yellow>",
        f"<gray>Bottom row:</gray> <yellow>{rows[2]}</yellow>",
    ]


def smelting_lines(source: str) -> list[str]:
    value = token_to_text(source)
    return [
        f"<gray>Top row:</gray> <yellow>- | {value} | -</yellow>",
        "<gray>Middle row:</gray> <yellow>- | Electric Furnace | -</yellow>",
        "<gray>Bottom row:</gray> <yellow>- | - | -</yellow>",
    ]


class Builder:
    def __init__(self, config: dict):
        self.config = config
        self.max_tier = int(config["max_tier"])
        self.cmd = int(config["start_custom_model_data"])

        enabled = {name.lower() for name in config["enabled_metals"]}
        self.metals = [m for m in BASE_METALS if m["id"] in enabled]
        if not self.metals:
            raise ValueError("No enabled metals were found in BASE_METALS")

        self.items: dict[str, dict] = {}
        self.smelting: dict[str, dict] = {}
        self.crafting: dict[str, dict] = {}
        self.entries: dict[str, dict] = {}

    def catalyst_for_tier(self, tier: int) -> str:
        catalysts = self.config["tier_catalysts"]
        return catalysts[(tier - 1) % len(catalysts)]

    def output_amount(self, family: str, base_amount: int) -> int:
        mult = float(self.config["output_multiplier_by_family"].get(family, 1.0))
        return max(1, int(round(base_amount * mult)))

    def unlock_cost(self, module: str, tier: int) -> int:
        model = self.config["xp_model"].get(module, self.config["xp_model"]["default"])
        difficulty = float(self.config["difficulty_by_module"].get(module, 1.0))
        base = float(model.get("base", 1.0))
        per_tier = float(model.get("per_tier", 1.0))
        power = float(model.get("power", 1.0))
        raw = (base + per_tier * math.pow(float(tier), power)) * difficulty
        return max(0, int(round(raw)))

    def add_item(self, item_id: str, base: str, name: str, desc: list[str], module: str, icon: str | None = None) -> None:
        if item_id in self.items:
            raise ValueError(f"Duplicate item id: {item_id}")
        self.items[item_id] = {
            "base": base,
            "name": name,
            "desc": desc,
            "cmd": self.cmd,
            "module": module,
            "icon": icon or base,
        }
        self.cmd += 1

    def add_smelting(self, recipe_id: str, input_token: str, output_item: str, amount: int) -> None:
        if recipe_id in self.smelting:
            raise ValueError(f"Duplicate smelting recipe: {recipe_id}")
        self.smelting[recipe_id] = {
            "in": normalize_token(input_token),
            "out": f"item:{output_item}",
            "amount": max(1, amount),
        }

    def add_shaped(self, recipe_id: str, output_item: str, amount: int, shape: list[str], ingredients: dict[str, str]) -> None:
        if recipe_id in self.crafting:
            raise ValueError(f"Duplicate crafting recipe: {recipe_id}")
        self.crafting[recipe_id] = {
            "out": f"item:{output_item}",
            "amount": max(1, amount),
            "shape": shape,
            "ing": {k: normalize_token(v) for k, v in ingredients.items()},
        }

    def add_entry(self, entry_id: str, module: str, name: str, icon: str, preview: str, cost: int, desc: list[str], recipe: list[str]) -> None:
        if entry_id in self.entries:
            raise ValueError(f"Duplicate entry id: {entry_id}")
        self.entries[entry_id] = {
            "module": module,
            "name": name,
            "icon": icon,
            "preview": preview,
            "cost": max(0, cost),
            "desc": desc,
            "recipe": recipe,
        }

    def generate(self) -> None:
        for metal in self.metals:
            mid = metal["id"]
            mtitle = metal["title"]
            color = metal["color"]
            seed = metal["seed"]
            open_tag = f"<#{color}>"
            close_tag = f"</#{color}>"

            for tier in range(1, self.max_tier + 1):
                catalyst = self.catalyst_for_tier(tier)
                dust = f"{mid}_dust_t{tier}"
                ingot = f"{mid}_ingot_t{tier}"
                plate = f"{mid}_plate_t{tier}"
                wire = f"{mid}_wire_t{tier}"
                coil = f"{mid}_coil_t{tier}"
                gear = f"{mid}_gear_t{tier}"

                self.add_item(dust, "GUNPOWDER", f"{open_tag}<b>{mtitle} Dust T{tier}</b>{close_tag}", [f"<gray>Tier {tier} powdered {mtitle.lower()} for industrial synthesis.</gray>", f"<gray>Catalyst:</gray> <yellow>{mat_title(catalyst)}</yellow>"], "materials")
                self.add_item(ingot, seed, f"{open_tag}<b>{mtitle} Ingot T{tier}</b>{close_tag}", [f"<gray>Refined smelted ingot for tier {tier} structures.</gray>"], "materials")
                self.add_item(plate, "IRON_NUGGET", f"{open_tag}<b>{mtitle} Plate T{tier}</b>{close_tag}", ["<gray>Pressed alloy plate used in machines and armor.</gray>"], "components")
                self.add_item(wire, "CHAIN", f"{open_tag}<b>{mtitle} Wire T{tier}</b>{close_tag}", ["<gray>Conductive wire segment for power routing.</gray>"], "components")
                self.add_item(coil, "LIGHTNING_ROD", f"{open_tag}<b>{mtitle} Coil T{tier}</b>{close_tag}", ["<gray>Energy coil for generators and high-voltage tools.</gray>"], "energy")
                self.add_item(gear, "CLOCK", f"{open_tag}<b>{mtitle} Gear T{tier}</b>{close_tag}", ["<gray>Mechanical transmission component for automation.</gray>"], "automation")

                if tier == 1:
                    shape = ["SCS", "CRC", "SCS"]
                    ingredients = {"S": f"material:{seed}", "C": "material:COBBLESTONE", "R": "material:REDSTONE"}
                    dust_out = self.output_amount("dust", 2)
                else:
                    shape = ["PCP", "CDC", "PCP"]
                    ingredients = {"P": f"item:{mid}_dust_t{tier - 1}", "C": f"material:{catalyst}", "D": "material:DEEPSLATE"}
                    dust_out = self.output_amount("dust", 1)

                rid = f"{dust}_craft"
                self.add_shaped(rid, dust, dust_out, shape, ingredients)
                self.add_entry(dust, "materials", self.items[dust]["name"], self.items[dust]["icon"], dust, self.unlock_cost("materials", tier), self.items[dust]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

                self.add_smelting(f"{ingot}_smelt", f"item:{dust}", ingot, self.output_amount("ingot", 1))
                self.add_entry(ingot, "materials", self.items[ingot]["name"], self.items[ingot]["icon"], ingot, self.unlock_cost("materials", tier), self.items[ingot]["desc"], smelting_lines(f"item:{dust}"))

                shape = ["III", "III", "   "]
                ingredients = {"I": f"item:{ingot}"}
                rid = f"{plate}_craft"
                self.add_shaped(rid, plate, self.output_amount("plate", 3), shape, ingredients)
                self.add_entry(plate, "components", self.items[plate]["name"], self.items[plate]["icon"], plate, self.unlock_cost("components", tier), self.items[plate]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

                shape = [" P ", "PSP", " P "]
                ingredients = {"P": f"item:{plate}", "S": "material:STRING"}
                rid = f"{wire}_craft"
                self.add_shaped(rid, wire, self.output_amount("wire", 4), shape, ingredients)
                self.add_entry(wire, "components", self.items[wire]["name"], self.items[wire]["icon"], wire, self.unlock_cost("components", tier), self.items[wire]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

                shape = ["WWW", "WRW", "WWW"]
                ingredients = {"W": f"item:{wire}", "R": f"material:{catalyst}"}
                rid = f"{coil}_craft"
                self.add_shaped(rid, coil, self.output_amount("coil", 2), shape, ingredients)
                self.add_entry(coil, "energy", self.items[coil]["name"], self.items[coil]["icon"], coil, self.unlock_cost("energy", tier), self.items[coil]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

                shape = ["PCP", "CIC", "PCP"]
                ingredients = {"P": f"item:{plate}", "C": f"item:{coil}", "I": f"item:{ingot}"}
                rid = f"{gear}_craft"
                self.add_shaped(rid, gear, self.output_amount("gear", 1), shape, ingredients)
                self.add_entry(gear, "automation", self.items[gear]["name"], self.items[gear]["icon"], gear, self.unlock_cost("automation", tier), self.items[gear]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))
        for tier in range(1, self.max_tier + 1):
            catalyst = self.catalyst_for_tier(tier)
            ma = self.metals[(tier - 1) % len(self.metals)]["id"]
            mb = self.metals[(tier + 4) % len(self.metals)]["id"]
            mc = self.metals[(tier + 8) % len(self.metals)]["id"]

            core = f"power_core_t{tier}"
            cell = f"plasma_cell_t{tier}"
            blade = f"drake_blade_t{tier}"
            chest = f"aegis_chestplate_t{tier}"

            self.add_item(core, "HEART_OF_THE_SEA", f"<gradient:aqua:blue><b>Power Core T{tier}</b></gradient>", ["<gray>Dense energy nucleus for advanced devices.</gray>"], "energy")
            self.add_item(cell, "FIRE_CHARGE", f"<gradient:gold:red><b>Plasma Cell T{tier}</b></gradient>", ["<gray>Thermal cartridge used in combat assemblies.</gray>"], "energy")
            self.add_item(blade, "NETHERITE_SWORD", f"<gradient:red:gold><b>Drake Blade T{tier}</b></gradient>", ["<gray>Progressive weapon line for late-game combat.</gray>"], "weapons")
            self.add_item(chest, "NETHERITE_CHESTPLATE", f"<gradient:aqua:white><b>Aegis Chestplate T{tier}</b></gradient>", ["<gray>Layered defensive core armor.</gray>"], "armor")

            shape = ["ABA", "CXC", "ABA"]
            ingredients = {"A": f"item:{ma}_coil_t{tier}", "B": f"item:{mb}_gear_t{tier}", "C": f"item:{mc}_wire_t{tier}", "X": f"material:{catalyst}"}
            rid = f"{core}_craft"
            self.add_shaped(rid, core, self.output_amount("core", 1), shape, ingredients)
            self.add_entry(core, "energy", self.items[core]["name"], self.items[core]["icon"], core, self.unlock_cost("energy", tier), self.items[core]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

            shape = ["PBP", "BCB", "PBP"]
            ingredients = {"P": f"item:{core}", "B": "material:BLAZE_POWDER", "C": "material:MAGMA_CREAM"}
            rid = f"{cell}_craft"
            self.add_shaped(rid, cell, self.output_amount("cell", 1), shape, ingredients)
            self.add_entry(cell, "energy", self.items[cell]["name"], self.items[cell]["icon"], cell, self.unlock_cost("energy", tier + 1), self.items[cell]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

            blade_src = "material:NETHERITE_SWORD" if tier == 1 else f"item:drake_blade_t{tier - 1}"
            chest_src = "material:NETHERITE_CHESTPLATE" if tier == 1 else f"item:aegis_chestplate_t{tier - 1}"

            shape = [" P ", " S ", " G "]
            ingredients = {"P": f"item:{cell}", "S": blade_src, "G": f"item:{ma}_gear_t{tier}"}
            rid = f"{blade}_craft"
            self.add_shaped(rid, blade, 1, shape, ingredients)
            self.add_entry(blade, "weapons", self.items[blade]["name"], self.items[blade]["icon"], blade, self.unlock_cost("weapons", tier), self.items[blade]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

            shape = ["P P", "PSP", "GGG"]
            ingredients = {"P": f"item:{core}", "S": chest_src, "G": f"item:{mb}_plate_t{tier}"}
            rid = f"{chest}_craft"
            self.add_shaped(rid, chest, 1, shape, ingredients)
            self.add_entry(chest, "armor", self.items[chest]["name"], self.items[chest]["icon"], chest, self.unlock_cost("armor", tier), self.items[chest]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

            red_alloy = f"redstone_alloy_ingot_t{tier}"
            hardened = f"hardened_metal_t{tier}"
            self.add_item(red_alloy, "REDSTONE", f"<gradient:red:gold><b>Redstone Alloy Ingot T{tier}</b></gradient>", ["<gray>Conductive alloy used for powered machine frames.</gray>"], "components")
            self.add_item(hardened, "NETHERITE_INGOT", f"<gradient:gray:dark_gray><b>Hardened Metal T{tier}</b></gradient>", ["<gray>Impact-resistant industrial alloy plate.</gray>"], "components")

            shape = ["ABA", "BRB", "ABA"]
            ingredients = {"A": f"item:{ma}_ingot_t{tier}", "B": f"item:{mb}_ingot_t{tier}", "R": "material:REDSTONE_BLOCK"}
            rid = f"{red_alloy}_craft"
            self.add_shaped(rid, red_alloy, self.output_amount("ingot", 1), shape, ingredients)
            self.add_entry(red_alloy, "components", self.items[red_alloy]["name"], self.items[red_alloy]["icon"], red_alloy, self.unlock_cost("components", tier + 1), self.items[red_alloy]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

            shape = ["HAH", "AOA", "HAH"]
            ingredients = {"H": f"item:{red_alloy}", "A": f"item:{mc}_plate_t{tier}", "O": "material:OBSIDIAN"}
            rid = f"{hardened}_craft"
            self.add_shaped(rid, hardened, self.output_amount("plate", 1), shape, ingredients)
            self.add_entry(hardened, "components", self.items[hardened]["name"], self.items[hardened]["icon"], hardened, self.unlock_cost("components", tier + 2), self.items[hardened]["desc"], recipe_lines(shape, self.crafting[rid]["ing"]))

        self.add_entry(
            "solar_generator", "machines", "<gold><b>Solar Generator</b></gold>", "DAYLIGHT_DETECTOR", "", 0,
            ["<gray>Generates passive energy under clear daylight.</gray>"],
            [
                "<gray>Top row:</gray> <yellow>Glass | Glass | Glass</yellow>",
                "<gray>Middle row:</gray> <yellow>item:copper_plate_t1 | Daylight Detector | item:copper_plate_t1</yellow>",
                "<gray>Bottom row:</gray> <yellow>item:power_core_t1 | item:copper_wire_t1 | item:power_core_t1</yellow>",
                "<gray>Assembly:</gray> <yellow>Center DAYLIGHT_DETECTOR + copper blocks on cross.</yellow>",
            ],
        )
        self.add_entry(
            "electric_furnace", "machines", "<aqua><b>Electric Furnace</b></aqua>", "FURNACE", "", 1,
            ["<gray>Processes DrakesTech smelting recipes using energy.</gray>"],
            [
                "<gray>Top row:</gray> <yellow>Iron Ingot | Furnace | Iron Ingot</yellow>",
                "<gray>Middle row:</gray> <yellow>item:copper_wire_t1 | item:power_core_t1 | item:copper_wire_t1</yellow>",
                "<gray>Bottom row:</gray> <yellow>Iron Ingot | Hopper | Iron Ingot</yellow>",
                "<gray>Assembly:</gray> <yellow>Center CRAFTING_TABLE + DISPENSER on side.</yellow>",
            ],
        )
        self.add_entry(
            "cobblestone_generator", "machines", "<gray><b>Cobblestone Generator</b></gray>", "COBBLESTONE", "", 2,
            ["<gray>Generates cobblestone automatically while powered.</gray>"],
            [
                "<gray>Top row:</gray> <yellow>item:hardened_metal_t1 | item:power_core_t1 | item:hardened_metal_t1</yellow>",
                "<gray>Middle row:</gray> <yellow>Cobblestone | Furnace | Cobblestone</yellow>",
                "<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t1 | item:copper_wire_t1 | item:redstone_alloy_ingot_t1</yellow>",
            ],
        )
        self.add_entry(
            "iron_generator", "machines", "<white><b>Iron Generator</b></white>", "IRON_INGOT", "", 4,
            ["<gray>Produces iron ingots continuously with enough energy.</gray>"],
            [
                "<gray>Top row:</gray> <yellow>item:hardened_metal_t2 | item:power_core_t2 | item:hardened_metal_t2</yellow>",
                "<gray>Middle row:</gray> <yellow>Iron Block | Blast Furnace | Iron Block</yellow>",
                "<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t2 | item:copper_wire_t2 | item:redstone_alloy_ingot_t2</yellow>",
            ],
        )
        self.add_entry(
            "redstone_generator", "machines", "<red><b>Redstone Generator</b></red>", "REDSTONE", "", 5,
            ["<gray>Automates redstone supply for high-tier circuits.</gray>"],
            [
                "<gray>Top row:</gray> <yellow>item:hardened_metal_t3 | item:power_core_t3 | item:hardened_metal_t3</yellow>",
                "<gray>Middle row:</gray> <yellow>Redstone Block | Observer | Redstone Block</yellow>",
                "<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t3 | item:copper_wire_t3 | item:redstone_alloy_ingot_t3</yellow>",
            ],
        )
        self.add_entry(
            "tech_storage_chest", "machines", "<gold><b>Tech Storage Chest</b></gold>", "CHEST", "", 3,
            ["<gray>Special storage that links with adjacent DrakesTech machines.</gray>"],
            [
                "<gray>Top row:</gray> <yellow>item:hardened_metal_t1 | Chest | item:hardened_metal_t1</yellow>",
                "<gray>Middle row:</gray> <yellow>item:redstone_alloy_ingot_t1 | item:power_core_t1 | item:redstone_alloy_ingot_t1</yellow>",
                "<gray>Bottom row:</gray> <yellow>item:copper_wire_t1 | Hopper | item:copper_wire_t1</yellow>",
            ],
        )

        self.validate_progression()

    def validate_progression(self) -> None:
        item_ids = set(self.items.keys())
        if any(item_id not in self.entries for item_id in item_ids):
            raise ValueError("There are items without guide entries")

        produced = set()
        required_inputs: dict[str, list[list[str]]] = {}

        for recipe in self.smelting.values():
            out = recipe["out"].split(":", 1)[1]
            produced.add(out)
            required_inputs.setdefault(out, []).append([recipe["in"]])

        for recipe in self.crafting.values():
            out = recipe["out"].split(":", 1)[1]
            produced.add(out)
            used = {c for row in recipe["shape"] for c in row if c != " "}
            required_inputs.setdefault(out, []).append([recipe["ing"][c] for c in used])

        missing_outputs = item_ids - produced
        if missing_outputs:
            raise ValueError(f"Items without recipe output: {len(missing_outputs)}")

        base_materials = {token for variants in required_inputs.values() for row in variants for token in row if token.startswith("material:")}
        reachable_items: set[str] = set()

        changed = True
        while changed:
            changed = False
            for out, variants in required_inputs.items():
                if out in reachable_items:
                    continue
                for tokens in variants:
                    ok = True
                    for token in tokens:
                        if token.startswith("material:"):
                            ok = ok and (token in base_materials)
                        elif token.startswith("item:"):
                            ok = ok and (token.split(":", 1)[1] in reachable_items)
                    if ok:
                        reachable_items.add(out)
                        changed = True
                        break

        unreachable = item_ids - reachable_items
        if unreachable:
            raise ValueError(f"Unreachable custom items: {len(unreachable)}")

    def render_yaml(self) -> str:
        lines: list[str] = []
        lines += [
            "# DrakesTech Massive Content Registry",
            "# Auto-generated by tools/content-gen/generate_massive_tech_content.py",
            "# [DANGER] Edit the generator or JSON config, not this output directly.",
            "",
            "guide:",
            "  auto-create-enchantment-entries: true",
            "",
            "items:",
            "  # [DANGER] Do not rename ids after release.",
        ]
        for item_id in sorted(self.items):
            item = self.items[item_id]
            lines += [f"  {item_id}:", "    enabled: true", f"    base-material: {item['base']}", f"    custom-model-data: {item['cmd']}", f"    display-name: {q(item['name'])}", "    description:"]
            lines += [f"      - {q(text)}" for text in item["desc"]] + [""]

        lines += ["recipes:", "  use-vanilla-fallback: false", "", "  smelting:"]
        for rid in sorted(self.smelting):
            recipe = self.smelting[rid]
            lines += [f"    {rid}:", "      enabled: true", f"      input: {recipe['in']}", f"      output: {recipe['out']}", f"      amount: {recipe['amount']}"]

        lines += ["", "  crafting:"]
        for rid in sorted(self.crafting):
            recipe = self.crafting[rid]
            lines += [f"    {rid}:", "      enabled: true", "      type: shaped", f"      output: {recipe['out']}", f"      amount: {recipe['amount']}", "      shape:"]
            lines += [f"        - {q(row)}" for row in recipe["shape"]] + ["      ingredients:"]
            lines += [f"        {key}: {recipe['ing'][key]}" for key in sorted(recipe["ing"])]

        lines += ["", "modules:"]
        order = ["machines", "materials", "components", "automation", "energy", "weapons", "armor", "enchantments"]
        for module_id in order:
            ui = MODULE_UI[module_id]
            unlock_levels = int(self.config["module_unlock_levels"].get(module_id, 0))
            lines += [f"  {module_id}:", "    enabled: true", f"    display-name: {q(ui['display'])}", f"    icon: {ui['icon']}", f"    unlock-cost-levels: {unlock_levels}", "    description:"]
            lines += [f"      - {q(text)}" for text in ui["description"]]

        lines += [
            "",
            "machines:",
            "  # [DANGER] Do not rename ids after placing machines in worlds.",
            "  solar_generator:",
            "    enabled: true",
            "    template: solar_generator",
            "    module: machines",
            "    display-name: '<gold><b>Solar Generator</b></gold>'",
            "    icon: DAYLIGHT_DETECTOR",
            "    description:",
            "      - '<gray>Generates passive energy under clear daylight.</gray>'",
            "    recipe:",
            "      - '<gray>Top row:</gray> <yellow>Glass | Glass | Glass</yellow>'",
            "      - '<gray>Middle row:</gray> <yellow>item:copper_plate_t1 | Daylight Detector | item:copper_plate_t1</yellow>'",
            "      - '<gray>Bottom row:</gray> <yellow>item:power_core_t1 | item:copper_wire_t1 | item:power_core_t1</yellow>'",
            "",
            "  electric_furnace:",
            "    enabled: true",
            "    template: electric_furnace",
            "    module: machines",
            "    display-name: '<aqua><b>Electric Furnace</b></aqua>'",
            "    icon: FURNACE",
            "    description:",
            "      - '<gray>Processes DrakesTech smelting recipes using stored power.</gray>'",
            "    recipe:",
            "      - '<gray>Top row:</gray> <yellow>Iron Ingot | Furnace | Iron Ingot</yellow>'",
            "      - '<gray>Middle row:</gray> <yellow>item:copper_wire_t1 | item:power_core_t1 | item:copper_wire_t1</yellow>'",
            "      - '<gray>Bottom row:</gray> <yellow>Iron Ingot | Hopper | Iron Ingot</yellow>'",
            "",
            "  cobblestone_generator:",
            "    enabled: true",
            "    template: resource_generator",
            "    module: machines",
            "    display-name: '<gray><b>Cobblestone Generator</b></gray>'",
            "    icon: COBBLESTONE",
            "    output-material: COBBLESTONE",
            "    output-amount: 1",
            "    ticks-per-cycle: 40",
            "    energy-per-cycle: 20",
            "    max-energy: 3000",
            "    description:",
            "      - '<gray>Generates cobblestone automatically while powered.</gray>'",
            "    recipe:",
            "      - '<gray>Top row:</gray> <yellow>item:hardened_metal_t1 | item:power_core_t1 | item:hardened_metal_t1</yellow>'",
            "      - '<gray>Middle row:</gray> <yellow>Cobblestone | Furnace | Cobblestone</yellow>'",
            "      - '<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t1 | item:copper_wire_t1 | item:redstone_alloy_ingot_t1</yellow>'",
            "",
            "  iron_generator:",
            "    enabled: true",
            "    template: resource_generator",
            "    module: machines",
            "    display-name: '<white><b>Iron Generator</b></white>'",
            "    icon: IRON_INGOT",
            "    output-material: IRON_INGOT",
            "    output-amount: 1",
            "    ticks-per-cycle: 60",
            "    energy-per-cycle: 35",
            "    max-energy: 5000",
            "    description:",
            "      - '<gray>Produces iron ingots continuously with enough energy.</gray>'",
            "    recipe:",
            "      - '<gray>Top row:</gray> <yellow>item:hardened_metal_t2 | item:power_core_t2 | item:hardened_metal_t2</yellow>'",
            "      - '<gray>Middle row:</gray> <yellow>Iron Block | Blast Furnace | Iron Block</yellow>'",
            "      - '<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t2 | item:copper_wire_t2 | item:redstone_alloy_ingot_t2</yellow>'",
            "",
            "  redstone_generator:",
            "    enabled: true",
            "    template: resource_generator",
            "    module: machines",
            "    display-name: '<red><b>Redstone Generator</b></red>'",
            "    icon: REDSTONE",
            "    output-material: REDSTONE",
            "    output-amount: 2",
            "    ticks-per-cycle: 50",
            "    energy-per-cycle: 30",
            "    max-energy: 4500",
            "    description:",
            "      - '<gray>Automates redstone supply for high-tier circuits.</gray>'",
            "    recipe:",
            "      - '<gray>Top row:</gray> <yellow>item:hardened_metal_t3 | item:power_core_t3 | item:hardened_metal_t3</yellow>'",
            "      - '<gray>Middle row:</gray> <yellow>Redstone Block | Observer | Redstone Block</yellow>'",
            "      - '<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t3 | item:copper_wire_t3 | item:redstone_alloy_ingot_t3</yellow>'",
            "",
            "  tech_storage_chest:",
            "    enabled: true",
            "    template: tech_storage_chest",
            "    module: machines",
            "    display-name: '<gold><b>Tech Storage Chest</b></gold>'",
            "    icon: CHEST",
            "    inventory-size: 54",
            "    only-plugin-items: true",
            "    description:",
            "      - '<gray>Special storage that links with adjacent DrakesTech machines.</gray>'",
            "    recipe:",
            "      - '<gray>Top row:</gray> <yellow>item:hardened_metal_t1 | Chest | item:hardened_metal_t1</yellow>'",
            "      - '<gray>Middle row:</gray> <yellow>item:redstone_alloy_ingot_t1 | item:power_core_t1 | item:redstone_alloy_ingot_t1</yellow>'",
            "      - '<gray>Bottom row:</gray> <yellow>item:copper_wire_t1 | Hopper | item:copper_wire_t1</yellow>'",
            "",
            "multiblocks:",
            "  # [DANGER] machine-id must match a machine id above.",
            "  dispenser_table_furnace:",
            "    enabled: true",
            "    machine-id: electric_furnace",
            "    center: CRAFTING_TABLE",
            "    auto-assemble-on-place: true",
            "    consume-components: true",
            "    parts:",
            "      - '1,0,0:DISPENSER'",
            "",
            "  solar_cross_generator:",
            "    enabled: true",
            "    machine-id: solar_generator",
            "    center: DAYLIGHT_DETECTOR",
            "    auto-assemble-on-place: true",
            "    consume-components: true",
            "    parts:",
            "      - '1,0,0:COPPER_BLOCK'",
            "      - '-1,0,0:COPPER_BLOCK'",
            "      - '0,0,1:COPPER_BLOCK'",
            "      - '0,0,-1:COPPER_BLOCK'",
            "",
            "enchantments:",
            "  drake_fury:",
            "    enabled: true",
            "    display-name: '<gradient:red:gold><b>Drake Fury</b></gradient>'",
            "    max-level: 3",
            "    description:",
            "      - '<gray>Increases weapon burst damage after charged hits.</gray>'",
            "",
            "  energy_guard:",
            "    enabled: true",
            "    display-name: '<gradient:aqua:blue><b>Energy Guard</b></gradient>'",
            "    max-level: 2",
            "    description:",
            "      - '<gray>Converts part of damage into stored energy drain.</gray>'",
            "",
            "entries:",
        ]

        for entry_id in sorted(self.entries):
            entry = self.entries[entry_id]
            lines += [f"  {entry_id}:", "    enabled: true", f"    module: {entry['module']}", f"    display-name: {q(entry['name'])}", f"    icon: {entry['icon']}"]
            if entry["preview"]:
                lines += [f"    preview-item: {entry['preview']}"]
            else:
                lines += [f"    preview-material: {entry['icon']}"]
            lines += [f"    unlock-cost-levels: {entry['cost']}", "    description:"]
            lines += [f"      - {q(text)}" for text in entry["desc"]]
            lines += ["    recipe:"]
            lines += [f"      - {q(text)}" for text in entry["recipe"]]
            lines += [""]

        return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate massive DrakesTech content with configurable balance.")
    parser.add_argument("--config", type=Path, default=None, help="Path to JSON config file.")
    parser.add_argument("--output", type=Path, default=Path("src/main/resources/tech-content.yml"), help="Output YAML path.")
    parser.add_argument("--write-effective-config", type=Path, default=None, help="Write merged effective config to this JSON file.")
    args = parser.parse_args()

    config = load_config(args.config)
    if args.write_effective_config is not None:
        args.write_effective_config.parent.mkdir(parents=True, exist_ok=True)
        args.write_effective_config.write_text(json.dumps(config, indent=2), encoding="utf-8")

    builder = Builder(config)
    builder.generate()
    text = builder.render_yaml()

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(text, encoding="utf-8")

    print("[OK] Generated tech content")
    print(f"[OK] Output: {args.output}")
    print(f"[OK] Tiers: {config['max_tier']}")
    print(f"[OK] Metals enabled: {len(builder.metals)}")
    print(f"[OK] Items: {len(builder.items)}")
    print(f"[OK] Smelting recipes: {len(builder.smelting)}")
    print(f"[OK] Crafting recipes: {len(builder.crafting)}")
    print(f"[OK] Guide entries: {len(builder.entries)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
