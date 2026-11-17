"""Panchangam astronomy for one place (Hyderabad, IST), using the Swiss Ephemeris (Moshier mode, no data files).

Conventions (they match what Drik Panchang and the Telugu panchangams publish):
* Sidereal zodiac, Lahiri ayanamsa.
* Sunrise / sunset: upper limb of the Sun on the horizon, with standard refraction (swe.rise_trans default, no BIT_DISC_CENTER).
* Tithi = 12 degrees of Moon-minus-Sun elongation, Nakshatra = 13d20' of Moon, Yoga = 13d20' of Sun+Moon,
  Karana = half tithi. A day is "known by" the tithi, nakshatra, yoga and karana at sunrise.
* Lunar month is amanta (new moon to new moon). It is named after the Sun's sankranti inside it
  (Mesha sankranti -> Chaitra). A month with no sankranti is Adhika and takes the name of the month that follows it.
"""
import datetime as dt

import swisseph as swe

PLACE = {"name": "Hyderabad", "lat": 17.385, "lon": 78.4867}
IST_DAYS = 5.5 / 24.0

swe.set_sid_mode(swe.SIDM_LAHIRI)
FLAGS = swe.FLG_MOSEPH | swe.FLG_SIDEREAL
GEO = (PLACE["lon"], PLACE["lat"], 0.0)
J2000 = 2451545.0  # 2000-01-01 12:00 UT


# ---------------------------------------------------------------- time helpers
def jd_local(date, hour=0.0):
    """Julian day (UT) of a local (IST) clock time on a datetime.date."""
    return swe.julday(date.year, date.month, date.day, hour) - IST_DAYS


def to_local(jd):
    """datetime (naive, IST) for a Julian day (UT)."""
    return dt.datetime(2000, 1, 1, 12) + dt.timedelta(days=jd - J2000 + IST_DAYS)


def round_minute(t):
    return (t + dt.timedelta(seconds=30)).replace(second=0, microsecond=0)


def hhmm(jd):
    return round_minute(to_local(jd)).strftime("%H:%M")


def iso_minute(jd):
    return round_minute(to_local(jd)).strftime("%Y-%m-%dT%H:%M")


# ---------------------------------------------------------------- positions
def sun_lon(jd):
    return swe.calc_ut(jd, swe.SUN, FLAGS)[0][0]


def moon_lon(jd):
    return swe.calc_ut(jd, swe.MOON, FLAGS)[0][0]


def phase(jd):
    return (moon_lon(jd) - sun_lon(jd)) % 360.0


def tithi(jd):
    """1..30 (15 Pournami, 30 Amavasya)."""
    return int(phase(jd) / 12.0) + 1


def nakshatra(jd):
    return int(moon_lon(jd) / (360.0 / 27)) + 1


def yoga(jd):
    return int(((sun_lon(jd) + moon_lon(jd)) % 360.0) / (360.0 / 27)) + 1


def half_tithi(jd):
    """0..59, the karana slot."""
    return int(phase(jd) / 6.0)


def karana_name(k):
    """Karana label 1..11 for a half-tithi slot 0..59."""
    if k == 0:
        return 11  # Kimstughna
    if k >= 57:
        return {57: 8, 58: 9, 59: 10}[k]  # Shakuni, Chatushpada, Naga
    return (k - 1) % 7 + 1  # Bava .. Vishti


def sun_sign(jd):
    """0 Mesha .. 11 Meena (sidereal)."""
    return int(sun_lon(jd) / 30.0) % 12


# ---------------------------------------------------------------- root finding
def next_change(fn, jd, step=1.0 / 24):
    """The instant, after jd, at which fn() changes value (scan by `step`, then bisect to about a millisecond)."""
    s = fn(jd)
    lo = jd
    hi = jd + step
    while fn(hi) == s:
        lo = hi
        hi += step
    for _ in range(40):
        mid = (lo + hi) / 2
        if fn(mid) == s:
            lo = mid
        else:
            hi = mid
    return hi


def segments(fn, jd0, jd1):
    """[(state, end_jd)] covering jd0..jd1; the last segment may end after jd1."""
    out = []
    t = jd0
    while True:
        s = fn(t)
        e = next_change(fn, t)
        out.append((s, e))
        if e >= jd1:
            return out
        t = e + 1e-5


# ---------------------------------------------------------------- sun and moon rising
def _rise_set(body, which, jd_from):
    res = swe.rise_trans(jd_from, body, which, GEO, 1013.25, 15.0, swe.FLG_MOSEPH)
    return res[1][0]


def sunrise(jd_from):
    return _rise_set(swe.SUN, swe.CALC_RISE, jd_from)


def sunset(jd_from):
    return _rise_set(swe.SUN, swe.CALC_SET, jd_from)


def moonrise(jd_from):
    return _rise_set(swe.MOON, swe.CALC_RISE, jd_from)


# ---------------------------------------------------------------- lunar months
class Months:
    """Amanta lunar months between two dates: [(start_jd, end_jd, month 1..12, adhika)]."""

    def __init__(self, jd0, jd1):
        new_moons = []
        for s, e in segments(tithi, jd0, jd1):
            # the boundary where tithi 30 turns into tithi 1 is the new moon
            if s == 30:
                new_moons.append(e)
        self.items = []
        for a, b in zip(new_moons, new_moons[1:]):
            start_sign, end_sign = sun_sign(a + 1e-5), sun_sign(b - 1e-5)
            n = (end_sign - start_sign) % 12  # sankrantis inside the month
            if n == 0:
                month, adhika = (end_sign + 1) % 12 + 1, True  # no sankranti: Adhika, named after the next month
            else:
                month, adhika = end_sign % 12 + 1, False  # month of the (last) sankranti inside it; n == 2 is a kshaya month
            self.items.append((a, b, month, adhika))

    def at(self, jd):
        for a, b, month, adhika in self.items:
            if a <= jd < b:
                return month, adhika
        raise ValueError("jd outside the computed months")

    def start_of(self, jd):
        for a, b, _m, _ad in self.items:
            if a <= jd < b:
                return a
        raise ValueError("jd outside the computed months")


# ---------------------------------------------------------------- sankrantis
def sankrantis(jd0, jd1):
    """[(jd, sign)] each time the Sun enters a sign (sign 0 Mesha .. 11 Meena)."""
    out = []
    for s, e in segments(sun_sign, jd0, jd1):
        out.append((e, (s + 1) % 12))
    return [x for x in out if x[0] < jd1]
