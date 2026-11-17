#!/usr/bin/env python3
"""Writes calendar/<year>.json (format: DATA_FORMAT.md at the repo root).

    docker compose run --rm generator python generate.py 2026 2027
"""
import datetime as dt
import json
import os
import sys

import swisseph as swe

import astro
import festivals
from model import Context

OUT_DIR = os.environ.get("OUT_DIR", "../../app/src/main/assets/calendar")

# Sunday-first weekday tables: which eighth of the daytime (1..8) each kalam covers.
RAHU = [8, 2, 7, 5, 6, 4, 3]
YAMA = [5, 4, 3, 2, 1, 7, 6]
GULIKA = [7, 6, 5, 4, 3, 2, 1]


def eighth(day, part):
    span = (day.ss - day.sr) / 8
    return [astro.hhmm(day.sr + (part - 1) * span), astro.hhmm(day.sr + part * span)]


def elements(fn, label, day):
    """[{"i": label, "e": end}] for every state between this sunrise and the next."""
    return [{"i": label(s), "e": astro.iso_minute(e)} for s, e in astro.segments(fn, day.sr, day.nsr)]


def eclipse_items(year):
    """Eclipses visible from the place, as {date: item}. Penumbral-only lunar eclipses are left out."""
    out = {}
    visible_bits = swe.ECL_VISIBLE | swe.ECL_MAX_VISIBLE | swe.ECL_1ST_VISIBLE | swe.ECL_2ND_VISIBLE | swe.ECL_3RD_VISIBLE | swe.ECL_4TH_VISIBLE
    partial_bits = swe.ECL_PARTBEG_VISIBLE | swe.ECL_PARTEND_VISIBLE | swe.ECL_TOTBEG_VISIBLE | swe.ECL_TOTEND_VISIBLE | swe.ECL_MAX_VISIBLE
    start = astro.jd_local(dt.date(year, 1, 1)) - 1
    end = astro.jd_local(dt.date(year, 12, 31)) + 2
    t = start
    while t < end:  # lunar
        flag, tret, _attr = swe.lun_eclipse_when_loc(t, astro.GEO, swe.FLG_MOSEPH)
        if tret[0] >= end:
            break
        t = tret[0] + 20
        if not (flag & (swe.ECL_TOTAL | swe.ECL_PARTIAL)) or not (flag & partial_bits):
            continue
        date = astro.to_local(tret[0]).date()
        if date.year == year:
            # The name is the eclipse's own type (as in the panchangams); from Hyderabad it may only be partly seen (e.g. the Moon rises during it)
            global_flag = swe.lun_eclipse_when(tret[0] - 5, swe.FLG_MOSEPH)[0]
            total = bool(global_flag & swe.ECL_TOTAL)
            kind, kind_en = ("సంపూర్ణ", "Total") if total else ("పాక్షిక", "Partial")
            if total and not flag & swe.ECL_TOTAL:
                info = ("హైదరాబాద్‌లో పాక్షికంగా కనిపిస్తుంది", "Partly visible from Hyderabad")
            else:
                info = ("హైదరాబాద్‌లో కనిపిస్తుంది", "Visible from Hyderabad")
            out.setdefault(date, []).append(festivals.item("chandra-grahanam", "eclipse", f"{kind} చంద్ర గ్రహణం", f"{kind_en} lunar eclipse", *info))
    t = start
    while t < end:  # solar
        flag, tret, attr = swe.sol_eclipse_when_loc(t, astro.GEO, swe.FLG_MOSEPH)
        if tret[0] >= end:
            break
        t = tret[0] + 20
        if not (flag & visible_bits) or attr[2] < 0.01:  # attr[2] = fraction of the Sun's disc covered
            continue
        date = astro.to_local(tret[0]).date()
        if date.year == year:
            kind = "సంపూర్ణ" if flag & swe.ECL_TOTAL else ("వలయాకార" if flag & swe.ECL_ANNULAR else "పాక్షిక")
            kind_en = "Total" if flag & swe.ECL_TOTAL else ("Annular" if flag & swe.ECL_ANNULAR else "Partial")
            pct = round(attr[2] * 100)
            out.setdefault(date, []).append(festivals.item("surya-grahanam", "eclipse", f"{kind} సూర్య గ్రహణం", f"{kind_en} solar eclipse",
                                                          f"హైదరాబాద్‌లో కనిపిస్తుంది; సూర్య బింబంలో సుమారు {pct}% గ్రహణగ్రస్తం", f"Visible from Hyderabad, about {pct}% of the Sun covered"))
    return out


def build(year):
    ctx = Context(year)
    items_by_date, festival_dates = festivals.compute(ctx, year)
    for date, its in eclipse_items(year).items():
        items_by_date.setdefault(date, []).extend(its)

    # Telugu year: the one that began at the previous Ugadi until this year's Ugadi, then this year's
    ugadi = sorted(d for d in festival_dates.get("ugadi", []) if d.year == year)[0]
    prev_te, prev_en = festivals.samvatsara(year - 1)
    cur_te, cur_en = festivals.samvatsara(year)
    samvatsara = [
        {"from": f"{year}-01-01", "te": prev_te, "en": prev_en},
        {"from": ugadi.isoformat(), "te": cur_te, "en": cur_en},
    ]

    days = []
    date = dt.date(year, 1, 1)
    while date.year == year:
        day = ctx.by_date[date]
        month, adhika = ctx.months.at(day.sr)
        sun = astro.sun_lon(day.sr)
        wd = (date.weekday() + 1) % 7  # 0 = Sunday
        span = (day.ss - day.sr) / 15
        entry = {
            "d": date.isoformat(),
            "sr": astro.hhmm(day.sr),
            "ss": astro.hhmm(day.ss),
            "ms": month,
        }
        if adhika:
            entry["ad"] = 1
        entry["ay"] = 1 if (sun >= 270 or sun < 90) else 2
        entry["t"] = elements(astro.tithi, lambda s: s, day)
        entry["n"] = elements(astro.nakshatra, lambda s: s, day)
        entry["y"] = elements(astro.yoga, lambda s: s, day)
        entry["k"] = elements(astro.half_tithi, astro.karana_name, day)
        entry["rk"] = eighth(day, RAHU[wd])
        entry["ym"] = eighth(day, YAMA[wd])
        entry["gk"] = eighth(day, GULIKA[wd])
        if wd != 3:  # Abhijit muhurtham does not apply on Wednesdays (Drik Panchang shows none), so `ab` is left out
            entry["ab"] = [astro.hhmm(day.sr + 7 * span), astro.hhmm(day.sr + 8 * span)]
        if date in items_by_date:
            entry["f"] = items_by_date[date]
        days.append(entry)
        date += dt.timedelta(days=1)

    return {
        "version": 1,
        "year": year,
        "place": astro.PLACE,
        "ayanamsa": "lahiri",
        "samvatsara": samvatsara,
        "days": days,
    }


def main(years):
    os.makedirs(OUT_DIR, exist_ok=True)
    for year in years:
        data = build(year)
        path = os.path.join(OUT_DIR, f"{year}.json")
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
            f.write("\n")
        count = sum(len(d.get("f", [])) for d in data["days"])
        print(f"{path}: {len(data['days'])} days, {count} festival items, {os.path.getsize(path) // 1024} KB")


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        sys.exit("usage: python generate.py 2026 [2027 ...]")
    main([int(a) for a in args])
