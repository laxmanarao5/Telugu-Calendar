# Panchangam generator

Produces `app/src/main/assets/calendar/<year>.json` (format: [DATA_FORMAT.md](../../DATA_FORMAT.md)) for the Telugu calendar app.
Python + the Swiss Ephemeris (`pyswisseph`), run in Docker so nothing is installed on the host.

```bash
export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"     # Windows host only
cd tools/panchangam
docker compose build                                                  # once (compiles pyswisseph)
docker compose run --rm generator python generate.py 2026 2027        # writes ../../app/src/main/assets/calendar/2026.json, 2027.json
docker compose run --rm generator python verify.py                    # checks the files against published 2026 dates and invariants
```

## Adding a new year (e.g. 2028)
1. `docker compose run --rm generator python generate.py 2028`
2. `docker compose run --rm generator python verify.py` (the invariants run on every file; the published-date checks run for 2026 only).
3. Check the key festivals (Ugadi, Sankranti, Vinayaka Chavithi, Deepavali, Maha Shivaratri) against a Telugu panchangam before releasing.
4. If the government publishes that year's holiday lists, add them to `holidays.py` (see the 2026 block) and regenerate; until then only the fixed national days carry `h`.

## Files
| File | What it does |
|---|---|
| `astro.py` | Sun / Moon positions, tithi, nakshatra, yoga, karana, sunrise, lunar months (Adhika detection), sankrantis |
| `model.py` | Per-day facts for a year and the "which day is this tithi observed" logic (`pick_days`) |
| `festivals.py` | Festival and vratam rule table (month, tithi, time of day), Ekadasi names, samvatsara names, year overrides |
| `holidays.py` | Government holiday lists (2026) and the fixed national days |
| `generate.py` | Writes the JSON |
| `verify.py`, `expected-2026.json` | The published 2026 dates (with their sources) and the checker |

## Conventions
* **Place:** Hyderabad (17.385 N, 78.4867 E), IST. Other cities differ by minutes for sunrise and by a day for a few festivals.
* **Ayanamsa:** Lahiri, sidereal. Swiss Ephemeris Moshier mode (about 1 arcsecond for Sun and Moon, no data files).
* **Sunrise / sunset:** upper limb of the Sun on the horizon, with standard refraction (`swe.rise_trans` default flags), the convention of Drik Panchang.
* **Day:** a panchangam day runs sunrise to next sunrise and is "known by" the tithi, nakshatra, yoga and karana at sunrise.
  `t` / `n` / `y` / `k` list everything in force in that span, with end times rounded to the minute.
* **Karana:** `i` is the karana name 1..11 (Bava .. Kimstughna); the fixed karanas Shakuni, Chatushpada, Naga, Kimstughna fall at the end / start of the month as usual.
* **Lunar month:** amanta, named from the sankranti inside it; a month with no sankranti is Adhika (`ad: 1`) and takes the name of the month after it (2026: Adhika Jyeshtha, 17 May to 15 Jun).
* **Festival days:** each festival names the time of day its tithi must prevail (`festivals.py`). Where a skipped tithi never covers that moment the day it begins on is used.
  Festivals only fall in nija months; Ekadasi, Pradosham and the other vratams fall in every month.
* **Ekadasi:** one day per Ekadasi, the day Drik Panchang's Hyderabad list shows (the Vaishnava "Gauna" day is not listed). The rule (`Context.smarta_day`) was fitted to all 49 Ekadasi of
  2026 and 2027: no sunrise inside the tithi -> the day it begins; two sunrises -> the second; one sunrise -> that day, unless the tithi is still in force for less than 72 minutes after it, then the day before.
* **Pradosham:** the day on which the tithi covers most of the two hours after sunset. **Sankashti Chaturthi:** the day Krishna Chaturthi is in force at moonrise (nearest day when neither is).
  **Maha / Masa Shivaratri:** Chaturdashi in the midnight muhurta. **Akshaya Tritiya:** Tritiya in the forenoon window. **Raksha Bandhan:** Pournami at aparahna outside Bhadra, else the sunrise day.
* **Sankranti items** (`sankranti`, Uttarayana, Dakshinayana, Dhanurmasam) carry the calendar date on which the Sun enters the rasi, as Telugu panchangams list them.
  The **Makara Sankranti festival** is that day if the Sun enters before sunset, otherwise the next day (2027: entry 21:14 on 14 Jan, so Bhogi 14, Sankranti 15, Kanuma 16, Mukkanuma 17 Jan).
  Bhogi / Kanuma / Mukkanuma are the day before / after / two days after the festival. The governments' Bhogi, Sankranti and Kanuma holidays for 2026 (14, 15, 16 Jan) are one day later
  than the astronomical days (13, 14, 15 Jan) and appear as separate `holiday` items.
* **Month starts:** `masam-begins` is the first day whose sunrise lies in the new month (Chaitra: Ugadi). `ms` on a day is the month at that day's sunrise, so on a day where the new moon falls
  after sunrise (Ugadi 2026, 19 Mar) it is still the old month.
* **Abhijit:** left out (`ab` absent) on Wednesdays, as Drik Panchang shows none then.
* **Year overrides** (`YEAR_OVERRIDES` in `festivals.py`): only 2026 Kamadahanam (3 Mar, where the rule says 2 Mar; Bhadra covered the whole evening of 2 Mar and every Telugu calendar prints 3 Mar).
* **Vaikuntha (Mukkoti) Ekadasi:** the Shukla Ekadasi that falls while the Sun is in Dhanus (2026: 20 Dec, the same day as Mokshada Ekadasi).
* **Eclipses:** only those visible from Hyderabad (penumbral-only lunar eclipses are left out).

## Government holidays (`h`)
2026 comes from the Telangana (G.O.Rt.No.1715) and Andhra Pradesh (G.O.Rt.No.2276) lists. `h: 1` = a general holiday in BOTH states on that day;
`h: 2` = any other listed day (general in one state only, or optional). Where the two states give different dates (Eid, Muharram, Milad-un-Nabi) both dates appear, named "(AP)" and "(TS)".
Press copies of the lists contain typos; the corrections made (Vinayaka Chavithi 14 Sep not 14 Aug, Saddula Bathukamma 18 Oct not 10 Oct, Jagjivan Ram Jayanti 5 Apr, Bonalu 10 Aug) are marked in `holidays.py`.
Optional Islamic observances whose dates differ between the two lists (Shab-e-Meraj, Shab-e-Barat, Shab-e-Qadr, Eid-e-Ghadeer and similar) are not included.
Other years carry only the statutory fixed days (Republic Day, Ambedkar Jayanti, Independence Day, Gandhi Jayanti, Christmas); Islamic dates depend on moon sighting and are never guessed.

## What was checked (and how far to trust the other years)
`verify.py` reproduces, with no mismatch: sunrise, sunset, tithi / nakshatra / yoga / karana at sunrise with end times, Rahu / Yama / Gulika / Abhijit starts for 11 sample days of 2026 and 2027
(Drik Panchang, Hyderabad); all 2026 Ekadasi, Pradosham, Sankashti, Masa Shivaratri, Amavasya, Pournami, the 12 Sankrantis, 11 month starts, the Telugu festival list of telugucalendar.org and
Drik Panchang (every festival id in expected-2026.json), the government holidays that coincide with festivals, the eclipse visible from India, the Samvatsara name, and the real new / full moon times; for 2027 the
Ekadasi / Pradosham / Sankashti lists (Drik Hyderabad) and 23 key festivals. 2025 (not part of the checks) also gives the dates the Telugu states celebrated (Ugadi 30 Mar, Maha Shivaratri 26 Feb, Vinayaka Chavithi 27 Aug, Deepavali 20 Oct).
Several rules were tuned on 2026 and 2027 at once, so they are fitted, not proven: check the main festivals of any new year against a Telugu panchangam before releasing it.
