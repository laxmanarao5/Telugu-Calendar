#!/usr/bin/env python3
"""Developer helper: print the tithis (with end times), nakshatras and festival ids of a date range from a generated file.

    docker compose run --rm generator python inspect_days.py 2026 2026-09-02:2026-09-05 [more ranges]
"""
import datetime as dt
import json
import os
import sys

import calendar_io

OUT_DIR = os.environ.get("OUT_DIR", "../../app/src/main/assets/calendar")
year = sys.argv[1]
days = {x["d"]: x for x in calendar_io.load_year(OUT_DIR, int(year))["days"]}
for rng in sys.argv[2:]:
    a, b = (dt.date.fromisoformat(x) for x in rng.split(":"))
    while a <= b:
        x = days[a.isoformat()]
        print(x["d"], "sr", x["sr"], "ss", x["ss"], "ms", x["ms"], "t", [(t["i"], t["e"][5:]) for t in x["t"]],
              "n", [t["i"] for t in x["n"]], "f", [f["id"] for f in x.get("f", [])])
        a += dt.timedelta(days=1)
    print()
