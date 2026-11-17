#!/usr/bin/env python3
"""Checks the generated calendar/<year>.json against published 2026 dates (expected-2026.json) and against internal invariants.

    docker compose run --rm generator python verify.py          # checks 2026 against the published dates, and every generated year for the invariants

Exit code 1 on any mismatch; each mismatch is printed with what was expected and what was generated.
"""
import datetime as dt
import glob
import json
import os
import sys

import astro
import festivals

OUT_DIR = os.environ.get("OUT_DIR", "../../app/src/main/assets/calendar")
HERE = os.path.dirname(os.path.abspath(__file__))
problems = []


def bad(msg):
    problems.append(msg)
    print("MISMATCH:", msg)


def load(year):
    with open(os.path.join(OUT_DIR, f"{year}.json"), encoding="utf-8") as f:
        return json.load(f)


def index_by_id(data):
    idx = {}
    for d in data["days"]:
        for it in d.get("f", []):
            idx.setdefault(it["id"], []).append(d["d"])
    return idx


def hm_to_min(s):
    return int(s[:2]) * 60 + int(s[3:5])


# ------------------------------------------------------------------ invariants (any year)
def invariants(data):
    year = data["year"]
    days = data["days"]
    expected_days = 366 if year % 4 == 0 and (year % 100 != 0 or year % 400 == 0) else 365
    if len(days) != expected_days:
        bad(f"{year}: {len(days)} days, expected {expected_days}")
    for a, b in zip(days, days[1:]):
        # tithi sequence: next sunrise tithi is the last tithi of today, or one (two when a tithi is skipped) further on
        last = a["t"][-1]["i"]
        first = b["t"][0]["i"]
        if (first - last) % 30 not in (0, 1, 2):
            bad(f"{a['d']}: tithi sequence jumps from {last} to {first}")
        # within a day the listed tithis must run in order
        for x, y in zip(a["t"], a["t"][1:]):
            if (y["i"] - x["i"]) % 30 != 1:
                bad(f"{a['d']}: tithis inside the day are not consecutive ({x['i']} then {y['i']})")
        # the lunar month changes only across an amavasya
        if (a["ms"], a.get("ad", 0)) != (b["ms"], b.get("ad", 0)):
            if not any(t["i"] == 30 for t in a["t"]) and a["t"][-1]["i"] != 30:
                bad(f"{a['d']}->{b['d']}: month changes without an amavasya")
        if a["sr"] >= a["ss"]:
            bad(f"{a['d']}: sunrise {a['sr']} is not before sunset {a['ss']}")
    idx = index_by_id(data)
    eka = sorted(idx.get("ekadasi", []))
    if not 23 <= len(eka) <= 26:
        bad(f"{year}: {len(eka)} Ekadasi dates (expected 23 to 26)")
    for a, b in zip(eka, eka[1:]):
        gap = (dt.date.fromisoformat(b) - dt.date.fromisoformat(a)).days
        if not 13 <= gap <= 17:
            bad(f"{year}: Ekadasi {a} and {b} are {gap} days apart")
    adhika = {d["ms"] for d in days if d.get("ad")}
    print(f"  {year}: {len(days)} days, {len(eka)} Ekadasi, adhika month(s): {sorted(adhika) or 'none'}, "
          f"{sum(len(d.get('f', [])) for d in days)} festival items")
    # Pournami and Amavasya days must have that tithi in force during the panchangam day, or the day before
    by_day = {d["d"]: d for d in days}
    for kind, tn in (("pournami", 15), ("amavasya", 30)):
        for date in idx.get(kind, []):
            here = by_day[date]
            before = by_day.get((dt.date.fromisoformat(date) - dt.timedelta(days=1)).isoformat())
            if not any(t["i"] == tn for t in here["t"]) and not (before and any(t["i"] == tn for t in before["t"])):
                bad(f"{kind} on {date} but tithi {tn} is not in force that day or the day before")
    return idx


# ------------------------------------------------------------------ published 2026 dates
def check_2026(data, exp):
    idx = index_by_id(data)

    for e in exp["festivals"]:
        got = idx.get(e["id"], [])
        if e["date"] in got:
            continue
        if any(a in got for a in e.get("alt", [])):
            print(f"  note: {e['id']} generated on an accepted alternative date {got} (expected {e['date']}, {e['source']})")
            continue
        bad(f"festival {e['id']}: expected {e['date']} ({e['source']}), generated {got}")

    for key in ("ekadasi", "pradosham", "sankashti-chaturthi", "masa-shivaratri", "amavasya", "pournami"):
        want = set(exp[key]["dates"])
        got = set(idx.get(key, []))
        alt = exp[key].get("alt", {})
        for d in sorted(want - got):
            if any(a in got for a in alt.get(d, [])):
                continue
            bad(f"{key}: expected {d} ({exp[key]['source']}) but not generated")
        for d in sorted(got - want):
            if any(d in alts for alts in alt.values()):
                continue
            bad(f"{key}: generated {d} which is not in the published list ({exp[key]['source']})")

    begins = set(idx.get("masam-begins", []))
    for d in exp["masam_begins"]:
        if d not in begins:
            bad(f"masam-begins: expected {d}, generated {sorted(begins)}")

    # Sankrantis: the `sankranti` items (named after the rasi) plus Makara as makara-sankranti
    by_rasi = {}
    for d in data["days"]:
        for it in d.get("f", []):
            if it["id"] == "sankranti":
                for i, name in enumerate(festivals.RASI_TE):
                    if it["te"].startswith(name + " "):
                        by_rasi.setdefault(i, []).append(d["d"])
    for d in idx.get("makara-sankranti", []):
        by_rasi.setdefault(9, []).append(d)
    for sign, date in exp["sankranti"]["dates"].items():
        if date not in by_rasi.get(int(sign), []):
            bad(f"sankranti {festivals.RASI_EN[int(sign)]}: expected {date}, generated {by_rasi.get(int(sign))}")

    day = {d["d"]: d for d in data["days"]}
    for e in exp["tithi_end"]:
        d = day[e["date"]]
        match = [t for t in d["t"] if t["i"] == e["tithi"]]
        if not match:
            bad(f"tithi {e['tithi']} not in force on {e['date']} (got {[t['i'] for t in d['t']]})")
            continue
        end = match[0]["e"]
        if end[:10] != e["date"] or abs(hm_to_min(end[11:]) - hm_to_min(e["end"])) > exp["tithi_end_tolerance_minutes"]:
            bad(f"tithi {e['tithi']} on {e['date']} ends {end}, published {e['end']} ({e['source']})")

    cur = [s for s in data["samvatsara"] if s["from"] == "2026-03-19"]
    if not cur or cur[0]["en"] != exp["samvatsara"]["2026-03-19"]:
        bad(f"samvatsara from 2026-03-19: expected {exp['samvatsara']['2026-03-19']}, generated {data['samvatsara']}")

    ecl = {(d["d"], it["id"]) for d in data["days"] for it in d.get("f", []) if it["k"] == "eclipse"}
    for e in exp["eclipses"]:
        if (e["date"], e["id"]) not in ecl:
            bad(f"eclipse {e['id']} on {e['date']} not generated (got {sorted(ecl)})")
    extra = {x for x in ecl if x[0] not in {e["date"] for e in exp["eclipses"]}}
    if extra:
        bad(f"eclipses generated but not expected to be visible from India: {sorted(extra)}")

    # Real moon phases (independent of the tithi table)
    def moon_event(utc, want):
        t0 = dt.datetime.fromisoformat(utc)
        jd = astro.J2000 + (t0 - dt.datetime(2000, 1, 1, 12)).days + (t0 - dt.datetime(2000, 1, 1, 12)).seconds / 86400.0
        lo, hi = jd - 1, jd + 1
        f = (lambda x: (astro.phase(x) + 180) % 360 - 180) if want == "new" else (lambda x: astro.phase(x) - 180)
        for _ in range(60):
            mid = (lo + hi) / 2
            if f(mid) < 0:
                lo = mid
            else:
                hi = mid
        return abs(((lo + hi) / 2) - jd) * 1440

    for name in ("new", "full"):
        off = moon_event(exp["moon"][name + "_moon_utc"], name)
        if off > exp["moon"]["tolerance_minutes"]:
            bad(f"{name} moon is {off:.0f} minutes away from the published {exp['moon'][name + '_moon_utc']} UTC")


def minutes_between(a, b):
    """Absolute difference in minutes between two 'yyyy-MM-ddTHH:mm' strings."""
    return abs((dt.datetime.fromisoformat(a) - dt.datetime.fromisoformat(b)).total_seconds()) / 60


def check_samples(exp, cache):
    """Day panchang values published by Drik Panchang for Hyderabad (2026 and 2027 sample days)."""
    tol = exp["day_samples_tolerance_minutes"]
    for s in exp["day_samples"]:
        year = int(s["date"][:4])
        if year not in cache:
            path = os.path.join(OUT_DIR, f"{year}.json")
            if not os.path.exists(path):
                continue
            cache[year] = {d["d"]: d for d in load(year)["days"]}
        d = cache[year].get(s["date"])
        if d is None:
            bad(f"sample {s['date']}: no such day generated")
            continue
        stamp = lambda hm: f"{s['date']}T{hm}"  # noqa: E731
        for key in ("sr", "ss"):
            if minutes_between(stamp(d[key]), stamp(s[key])) > tol["clock"]:
                bad(f"{s['date']} {key}: generated {d[key]}, Drik {s[key]}")
        for key, label in (("t", "tithi"), ("n", "nakshatra"), ("y", "yoga")):
            first = d[key][0]
            if first["i"] != s[key][0]:
                bad(f"{s['date']} {label} at sunrise: generated {first['i']}, Drik {s[key][0]}")
            elif minutes_between(first["e"], s[key][1]) > tol["end"]:
                bad(f"{s['date']} {label} {first['i']} ends {first['e']}, Drik {s[key][1]}")
        if d["k"][0]["i"] != s["k"]:
            bad(f"{s['date']} karana at sunrise: generated {d['k'][0]['i']}, Drik {s['k']}")
        for key in ("rk", "ym", "gk", "ab"):
            want = s[key]
            if want is None:
                if key in d:
                    bad(f"{s['date']}: {key} should be absent (Drik shows none), generated {d[key]}")
            elif key not in d:
                bad(f"{s['date']}: {key} missing, Drik {want}")
            elif minutes_between(stamp(d[key][0]), stamp(want)) > tol["clock"]:
                bad(f"{s['date']} {key}: generated start {d[key][0]}, Drik {want}")


def main():
    exp = json.load(open(os.path.join(HERE, "expected-2026.json"), encoding="utf-8"))
    files = sorted(glob.glob(os.path.join(OUT_DIR, "*.json")))
    if not files:
        sys.exit("no generated files: run generate.py first")
    for path in files:
        data = json.load(open(path, encoding="utf-8"))
        print(f"{os.path.basename(path)}:")
        invariants(data)
        if data["year"] == 2026:
            check_2026(data, exp)
        if data["year"] == 2027:
            idx = index_by_id(data)
            for e in exp["festivals_2027"]:
                if e["date"] not in idx.get(e["id"], []):
                    bad(f"2027 festival {e['id']}: expected {e['date']} ({e['source']}), generated {idx.get(e['id'], [])}")
            for key in ("ekadasi", "pradosham", "sankashti-chaturthi"):
                want, got = set(exp["vratams_2027"][key]), set(idx.get(key, []))
                for d in sorted(want - got):
                    bad(f"2027 {key}: expected {d} ({exp['vratams_2027']['source']}) but not generated")
                for d in sorted(got - want):
                    bad(f"2027 {key}: generated {d} which is not in the published list ({exp['vratams_2027']['source']})")
    check_samples(exp, {})
    print(f"\n{len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
