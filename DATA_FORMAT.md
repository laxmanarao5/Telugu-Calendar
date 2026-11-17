# Calendar data format (`app/src/main/assets/calendar/<year>.json`)

One file per calendar year, produced by the generator in `tools/panchangam/` (Python + Swiss Ephemeris, run in Docker).
The app only reads these files; it never computes a panchangam itself. To add 2028, run the generator for 2028 and rebuild.
All times are local clock time for the reference place (Hyderabad, IST, UTC+05:30) as `HH:mm`, or `yyyy-MM-ddTHH:mm` when a date is needed.

```jsonc
{
  "version": 1,
  "year": 2026,
  "place": { "name": "Hyderabad", "lat": 17.385, "lon": 78.4867 },
  "ayanamsa": "lahiri",
  "samvatsara": [                      // Telugu year names that apply in this calendar year, with the first day each applies
    { "from": "2026-01-01", "te": "విశ్వావసు", "en": "Vishvavasu" },
    { "from": "2026-03-19", "te": "పరాభవ",     "en": "Parabhava" }
  ],
  "days": [ /* one object per date, 1 Jan .. 31 Dec, in order */ ]
}
```

## Day object
```jsonc
{
  "d": "2026-01-01",
  "sr": "06:48", "ss": "17:56",        // sunrise, sunset
  "ms": 10,                            // Telugu (amanta) lunar month at sunrise: 1 Chaitra, 2 Vaisakha, 3 Jyeshtha, 4 Ashadha, 5 Sravana,
                                       //   6 Bhadrapada, 7 Ashwayuja, 8 Karthika, 9 Margasira, 10 Pushya, 11 Magha, 12 Phalguna
  "ad": 1,                             // present (1) only when that month is an Adhika (extra) month
  "ay": 1,                             // 1 Uttarayana, 2 Dakshinayana
  "t": [ { "i": 12, "e": "2026-01-01T19:05" }, { "i": 13, "e": "2026-01-02T21:40" } ],   // tithis in force between this sunrise and the next
  "n": [ { "i": 8,  "e": "2026-01-02T03:11" } ],                                         // nakshatras
  "y": [ { "i": 3,  "e": "2026-01-01T22:30" } ],                                         // yogas
  "k": [ { "i": 4,  "e": "2026-01-01T19:05" }, { "i": 5, "e": "2026-01-02T08:02" } ],    // karanas
  "rk": ["15:00","16:30"],             // Rahu kalam
  "ym": ["09:00","10:30"],             // Yamagandam
  "gk": ["12:00","13:30"],             // Gulika kalam
  "ab": ["11:40","12:28"],             // Abhijit muhurtham
  "f":  [ /* festival items, see below; omitted when empty */ ]
}
```
* The **first** element of `t`, `n`, `y`, `k` is the one at sunrise (the one the day is "known by"); `e` is when it ends. The last `e` may be after the next sunrise.
* Tithi `i`: 1..15 Shukla (15 = Pournami), 16..30 Krishna (30 = Amavasya). Nakshatra `i`: 1 Ashwini .. 27 Revati. Yoga `i`: 1 Vishkambha .. 27 Vaidhriti.
* Karana `i`: 1 Bava, 2 Balava, 3 Kaulava, 4 Taitila, 5 Garija, 6 Vanija, 7 Vishti (Bhadra), 8 Shakuni, 9 Chatushpada, 10 Naga, 11 Kimstughna.
* The app owns the Telugu/English names for tithi, nakshatra, yoga, karana, weekday and month; only festival names live in the data.

## Festival item (`f`)
```jsonc
{ "id": "ugadi", "k": "festival", "te": "ఉగాది", "en": "Ugadi", "h": 1, "ti": "తెలుగు సంవత్సరాది", "ei": "Telugu New Year" }
```
* `id` stable key (`vinayaka-chavithi`, `ekadasi`, `makara-sankranti`, ...). May repeat across the year for monthly items.
* `k` kind: `festival` (major Hindu festival), `vratam` (Ekadasi, Sankashti Chaturthi, Pradosham, Masa Shivaratri, Amavasya, Pournami), `holiday` (government holiday that is not a religious festival: Republic Day ...),
  `sankranti` (Sun entering a rasi, `te`/`en` name the rasi), `eclipse` (only when visible from the place), `other` (e.g. Mahalaya Paksham begins, Dhanurmasam begins).
* `h` public-holiday level, optional: `1` general holiday in Telangana / Andhra Pradesh, `2` optional holiday. Taken from the published government list for that year only; when none is published leave it out.
* `ti` / `ei` optional one-line meaning (Telugu / English), for major festivals only.
* Items on a day are ordered: festivals, then holidays, then vratams, then sankranti / eclipse / other.
