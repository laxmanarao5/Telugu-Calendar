"""Per-day astronomical facts for a year (plus a margin either side), shared by generate.py and festivals.py."""
import bisect
import datetime as dt

import astro

MARGIN_DAYS = 50


class Day:
    __slots__ = ("date", "sr", "ss", "nsr", "mr")

    def __init__(self, date, sr, ss):
        self.date, self.sr, self.ss, self.nsr, self.mr = date, sr, ss, None, None


class Context:
    def __init__(self, year):
        self.year = year
        first = dt.date(year, 1, 1) - dt.timedelta(days=MARGIN_DAYS)
        last = dt.date(year, 12, 31) + dt.timedelta(days=MARGIN_DAYS)
        self.days = []
        date = first
        while date <= last:
            sr = astro.sunrise(astro.jd_local(date))
            self.days.append(Day(date, sr, astro.sunset(sr)))
            date += dt.timedelta(days=1)
        for a, b in zip(self.days, self.days[1:]):
            a.nsr = b.sr
        self.days.pop()  # the last day has no next sunrise
        self.sunrises = [d.sr for d in self.days]
        self.by_date = {d.date: d for d in self.days}
        span0, span1 = self.days[0].sr - 1, self.days[-1].nsr + 1
        self.months = astro.Months(span0 - 40, span1 + 40)
        # every tithi occurrence: (index, start_jd, end_jd)
        self.tithis = []
        prev_end = None
        for idx, end in astro.segments(astro.tithi, span0, span1):
            if prev_end is not None:
                self.tithis.append((idx, prev_end, end))
            prev_end = end

    # ------------------------------------------------------------ lookups
    def day_containing(self, jd):
        """The panchangam day (sunrise to next sunrise) a moment belongs to."""
        i = bisect.bisect_right(self.sunrises, jd) - 1
        return self.days[max(0, min(i, len(self.days) - 1))]

    def moonrise(self, day):
        if day.mr is None:
            day.mr = astro.moonrise(astro.jd_local(day.date))
        return day.mr

    def time_of(self, day, rule):
        """The moment a time-of-day rule looks at on a day."""
        span = day.ss - day.sr
        if rule == "sunrise":
            return day.sr
        if rule == "arunodaya":
            return day.sr - 96 / 1440.0  # two ghatikas before sunrise
        if rule == "madhyahna":
            return day.sr + 0.5 * span
        if rule == "aparahna":
            return day.sr + 0.7 * span
        if rule == "sunset":
            return day.ss - 5 / 1440.0
        if rule == "pradosha":
            return day.ss + 40 / 1440.0
        if rule == "nishita":
            return (day.ss + day.nsr) / 2
        if rule == "moonrise":
            return self.moonrise(day)
        raise ValueError(rule)

    def overlap(self, day, start, end):
        """Hours of daytime (sunrise to sunset) that lie inside [start, end)."""
        return max(0.0, min(end, day.ss) - max(start, day.sr)) * 24

    def hits(self, day, rule, start, end):
        """Does the tithi [start, end) prevail at the moment (or window) a time-of-day rule looks at on this day?"""
        if rule == "nishita":  # the midnight muhurta: eighth of the fifteen muhurtas of the night, about 46 minutes
            mid = self.time_of(day, "nishita")
            return mid - 23 / 1440.0 < end and mid + 23 / 1440.0 > start
        return start <= self.time_of(day, rule) < end

    def smarta_day(self, span, start, end):
        """Ekadasi day, fitted to the 2026-2027 Hyderabad lists of Drik Panchang (49 Ekadasi, all reproduced):
        * Ekadasi at no sunrise (it begins after one sunrise and ends before the next): the day it begins on;
        * Ekadasi at two sunrises (longer than a day): the second;
        * Ekadasi at one sunrise: that day, unless it is still in force for less than 72 minutes (3 ghatikas) after that sunrise,
          when the day before (on which the Ekadasi began after sunrise and filled the day) is taken."""
        at_sunrise = [i for i, d in enumerate(span) if start <= d.sr < end]
        if not at_sunrise:
            return self.day_containing(start)
        if len(at_sunrise) >= 2:
            return span[at_sunrise[1]]
        i = at_sunrise[0]
        if end - span[i].sr < 72 / 1440.0 and i > 0:
            return span[i - 1]
        return span[i]

    def window_day(self, span, start, end, window):
        """The day on which the tithi covers most of a daily window (`window(day)` -> (from, to)); the first on a tie.
        When it covers none of any window, the day whose window lies nearest the tithi."""
        scored = []
        for d in span:
            a, b = window(d)
            scored.append((max(0.0, min(end, b) - max(start, a)), d, a))
        best = max(scored, key=lambda x: (round(x[0] * 1440), -x[1].date.toordinal()))
        if best[0] > 0:
            return best[1]
        return min(scored, key=lambda x: self.distance(x[2], start, end))[1]

    def pradosha_day(self, span, start, end):
        """Pradosha: the window from sunset to two hours after it (fitted to Drik Panchang's Hyderabad Pradosh Vrat dates)."""
        return self.window_day(span, start, end, lambda d: (d.ss, d.ss + 120 / 1440.0))

    def purvahna_day(self, span, start, end):
        """Forenoon: from a fifth of the day after sunrise to midday (Akshaya Tritiya, fitted to Drik Panchang 2026 and 2027)."""
        return self.window_day(span, start, end, lambda d: (d.sr + 0.2 * (d.ss - d.sr), d.sr + 0.5 * (d.ss - d.sr)))

    @staticmethod
    def distance(t, start, end):
        """Days between a moment and the interval [start, end) (0 when inside)."""
        return 0.0 if start <= t < end else min(abs(t - start), abs(t - end))

    def rakhi_day(self, span, start, end):
        """Raksha Bandhan: the first day whose aparahna lies in the Pournami and outside Bhadra (Vishti karana);
        otherwise the day on which Pournami is in force at sunrise (Bhadra took the first afternoon); otherwise the day with most daytime."""
        for d in span:
            ap = self.time_of(d, "aparahna")
            if start <= ap < end and astro.karana_name(astro.half_tithi(ap)) != 7:
                return d
        for d in span:
            if start <= d.sr < end:
                return d
        return max(span, key=lambda d: (self.overlap(d, start, end), -d.date.toordinal()))

    # ------------------------------------------------------------ tithi occurrences
    def pick_days(self, month, tithi_no, rule, tie="first", nija_only=True):
        """Dates on which a tithi (optionally in a given lunar month) is observed under `rule`.

        One date per occurrence of the tithi:
        * `daymax`: the day on which the tithi covers most of the daytime;
        * `smarta`: Ekadasi (see smarta_day); `rakhi`: Raksha Bandhan (see rakhi_day); `pradosha`: see pradosha_day;
        * a time-of-day rule: the day(s) whose rule moment falls inside the tithi (first or last of them per `tie`);
        * if the tithi never covers that moment, the day whose moment lies nearest to it.
        """
        out = []
        for idx, start, end in self.tithis:
            if idx != tithi_no:
                continue
            m, adhika = self.months.at(start + 1e-5)
            if month is not None and m != month:
                continue
            if nija_only and adhika:
                continue
            first_i = bisect.bisect_right(self.sunrises, start) - 1
            last_i = bisect.bisect_right(self.sunrises, end) - 1
            span = [self.days[i] for i in range(max(first_i, 0), min(last_i, len(self.days) - 1) + 1)]
            if rule == "daymax":
                scored = [(self.overlap(d, start, end), d) for d in span]
                scored = [x for x in scored if x[0] > 0]
                chosen = max(scored, key=lambda x: (x[0], -x[1].date.toordinal()))[1] if scored else span[0]
            elif rule == "smarta":
                chosen = self.smarta_day(span, start, end)
            elif rule == "rakhi":
                chosen = self.rakhi_day(span, start, end)
            elif rule == "pradosha":
                chosen = self.pradosha_day(span, start, end)
            elif rule == "purvahna":
                chosen = self.purvahna_day(span, start, end)
            else:
                hit = [d for d in span if self.hits(d, rule, start, end)]
                if not hit:  # a skipped tithi: the day on which the rule's moment lies nearest to it
                    chosen = min(span, key=lambda d: self.distance(self.time_of(d, rule), start, end))
                else:
                    chosen = hit[0] if tie == "first" else hit[-1]
            out.append((chosen.date, m, adhika))
        return out
