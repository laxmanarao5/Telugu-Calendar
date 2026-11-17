"""Reads and writes the calendar data files: one file per month, `<year>-<MM>.json` (format: DATA_FORMAT.md).

The app reads the current month's file first so it can draw at once, and the other months in the background.
The generator and the checks still think in whole years: `write_year` splits a year into its twelve files and `load_year` puts them back together.
"""
import glob
import json
import os
import re

NAME = re.compile(r"^(\d{4})-(\d{2})\.json$")


def month_path(out_dir, year, month):
    return os.path.join(out_dir, f"{year}-{month:02d}.json")


def years_in(out_dir):
    """Years that have at least one month file, ascending."""
    found = set()
    for path in glob.glob(os.path.join(out_dir, "*.json")):
        m = NAME.match(os.path.basename(path))
        if m:
            found.add(int(m.group(1)))
    return sorted(found)


def write_year(out_dir, data):
    """Splits a whole-year dict (as built by generate.py) into 12 month files; returns their paths. Removes any old whole-year file."""
    os.makedirs(out_dir, exist_ok=True)
    year = data["year"]
    old = os.path.join(out_dir, f"{year}.json")
    if os.path.exists(old):
        os.remove(old)
    paths = []
    for month in range(1, 13):
        days = [d for d in data["days"] if int(d["d"][5:7]) == month]
        chunk = {k: v for k, v in data.items() if k != "days"}
        chunk["month"] = month
        chunk["days"] = days
        path = month_path(out_dir, year, month)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(chunk, f, ensure_ascii=False, separators=(",", ":"))
            f.write("\n")
        paths.append(path)
    return paths


def load_year(out_dir, year):
    """The whole-year dict again: the header of the first month file with the days of all twelve."""
    data = None
    days = []
    for month in range(1, 13):
        path = month_path(out_dir, year, month)
        with open(path, encoding="utf-8") as f:
            chunk = json.load(f)
        if data is None:
            data = {k: v for k, v in chunk.items() if k not in ("days", "month")}
        days.extend(chunk["days"])
    data["days"] = days
    return data
