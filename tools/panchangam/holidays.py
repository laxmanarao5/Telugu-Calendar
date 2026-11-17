"""Government holidays.

2026: transcribed from the published Telangana (G.O.Rt.No.1715, 6 Dec 2025) and Andhra Pradesh (G.O.Rt.No.2276, 4 Dec 2025)
lists as reported by Sakshi Post, Siasat and telugucalendar.org. Press copies of both lists contain obvious typos
(see the README); where one was clear it was corrected and is noted next to the entry.

Level `h`: 1 = general holiday listed by BOTH states, 2 = anything else listed (a general holiday in one state only, or optional).
Entries whose two states give different dates are kept as two entries named "(AP)" / "(TS)", each level 2.
`fest` links an entry to a computed festival id: when that festival falls on the same date it gets `h`, otherwise a plain
`holiday` item is added (this is how the government's Bhogi / Sankranti days, which differ from the astronomical ones, appear).

Other years: only the fixed national days, which are statutory every year. Islamic dates depend on moon sighting and no
list has been published, so none is invented.
"""
import datetime as dt


def _d(m, d, y=2026):
    return dt.date(y, m, d)


def _e(date, id_, te, en, ts=None, ap=None, fest=None):
    """ts / ap: 'G' general, 'O' optional, None not listed."""
    h = 1 if (ts == "G" and ap == "G") else 2
    return {"date": date, "id": id_, "te": te, "en": en, "h": h, "fest": fest}


HOLIDAYS_2026 = [
    _e(_d(1, 1), "new-year", "ఆంగ్ల సంవత్సరాది", "New Year's Day", ts="O", ap="O"),
    _e(_d(1, 3), "hazrat-ali-birthday", "హజరత్ అలీ జయంతి", "Birthday of Hazrat Ali", ts="O", ap="O"),
    _e(_d(1, 14), "bhogi-holiday", "భోగి (ప్రభుత్వ సెలవు)", "Bhogi (government holiday)", ts="G", ap="G"),
    _e(_d(1, 15), "sankranti-holiday", "సంక్రాంతి (ప్రభుత్వ సెలవు)", "Sankranti (government holiday)", ts="G", ap="G"),
    _e(_d(1, 16), "kanuma-holiday", "కనుమ (ప్రభుత్వ సెలవు)", "Kanuma (government holiday)", ts="O", ap="G"),
    _e(_d(1, 26), "republic-day", "గణతంత్ర దినోత్సవం", "Republic Day", ts="G", ap="G"),
    _e(_d(2, 15), "maha-shivaratri-holiday", "మహా శివరాత్రి", "Maha Shivaratri", ts="G", ap="G", fest="maha-shivaratri"),
    _e(_d(3, 3), "holi-holiday", "హోళీ", "Holi", ts="G", ap="G", fest="kamuni-pournami"),
    _e(_d(3, 13), "jumatul-vida", "జుమాతుల్-విదా", "Jumatul-Vida", ts="O", ap="O"),
    _e(_d(3, 19), "ugadi-holiday", "ఉగాది", "Ugadi", ts="G", ap="G", fest="ugadi"),
    _e(_d(3, 20), "ramzan-ap", "రంజాన్ (ఈద్-ఉల్-ఫితర్) (AP)", "Ramzan, Eid-ul-Fitr (AP)", ap="G"),
    _e(_d(3, 21), "ramzan-ts", "రంజాన్ (ఈద్-ఉల్-ఫితర్) (TS)", "Ramzan, Eid-ul-Fitr (TS)", ts="G"),
    _e(_d(3, 22), "ramzan-next-day-ts", "రంజాన్ మరుసటి రోజు (TS)", "Day after Ramzan (TS)", ts="G"),
    _e(_d(3, 27), "rama-navami-holiday", "శ్రీ రామ నవమి", "Sri Rama Navami", ts="G", ap="G", fest="sri-rama-navami"),
    _e(_d(3, 31), "mahavir-jayanti", "మహావీర్ జయంతి", "Mahavir Jayanti", ts="O", ap="O"),
    _e(_d(4, 3), "good-friday", "గుడ్ ఫ్రైడే", "Good Friday", ts="G", ap="G"),
    _e(_d(4, 5), "jagjivan-ram-jayanti", "బాబూ జగ్జీవన్ రామ్ జయంతి", "Babu Jagjivan Ram Jayanti", ap="G"),  # TS press copy says 14 Apr (typo)
    _e(_d(4, 14), "ambedkar-jayanti", "డా. బి.ఆర్. అంబేద్కర్ జయంతి", "Dr B.R. Ambedkar Jayanti", ts="G", ap="G"),
    _e(_d(4, 20), "basava-jayanti", "బసవ జయంతి", "Basava Jayanti", ts="O", ap="O"),
    _e(_d(5, 1), "buddha-pournami-holiday", "బుద్ధ పూర్ణిమ", "Buddha Pournami", ts="O", ap="O", fest="buddha-pournami"),
    _e(_d(5, 27), "bakrid", "బక్రీద్ (ఈద్-ఉల్-అజ్హా)", "Bakrid (Eid-ul-Adha)", ts="G", ap="G"),
    _e(_d(6, 25), "muharram-ap", "మొహర్రం (AP)", "Muharram (AP)", ts="O", ap="G"),
    _e(_d(6, 27), "muharram-ts", "మొహర్రం, 10వ రోజు (TS)", "Muharram, 10th day (TS)", ts="G"),
    _e(_d(7, 16), "ratha-yatra-holiday", "జగన్నాథ రథయాత్ర", "Jagannatha Ratha Yatra", ts="O", ap="O", fest="ratha-yatra"),
    _e(_d(8, 10), "bonalu-holiday", "బోనాలు (TS సెలవు)", "Bonalu (TS holiday)", ts="G"),  # press copies say 8 Aug / 10 Aug; 10 Aug used (Siasat, telugucalendar)
    _e(_d(8, 15), "independence-day", "స్వాతంత్ర్య దినోత్సవం", "Independence Day", ts="G", ap="G"),
    _e(_d(8, 21), "varalakshmi-holiday", "వరలక్ష్మీ వ్రతం", "Varalakshmi Vratham", ts="O", ap="G", fest="varalakshmi-vratham"),
    _e(_d(8, 25), "milad-un-nabi-ap", "మిలాద్-ఉన్-నబీ (AP)", "Milad-un-Nabi (AP)", ap="G"),
    _e(_d(8, 26), "milad-un-nabi-ts", "మిలాద్-ఉన్-నబీ (TS)", "Milad-un-Nabi (TS)", ts="G"),
    _e(_d(8, 28), "rakhi-holiday", "రాఖీ పౌర్ణమి", "Rakhi Pournami", ts="O", fest="raksha-bandhan"),
    _e(_d(9, 4), "krishnashtami-holiday", "శ్రీకృష్ణ జన్మాష్టమి", "Sri Krishnashtami", ts="G", ap="G", fest="krishna-janmashtami"),
    _e(_d(9, 14), "vinayaka-chavithi-holiday", "వినాయక చవితి", "Vinayaka Chavithi", ts="G", ap="G", fest="vinayaka-chavithi"),  # TS press copy says 14 Aug (typo)
    _e(_d(10, 2), "gandhi-jayanti", "గాంధీ జయంతి", "Gandhi Jayanti", ts="G", ap="G"),
    _e(_d(10, 18), "saddula-bathukamma-holiday", "సద్దుల బతుకమ్మ (TS సెలవు)", "Saddula Bathukamma (TS holiday)", ts="G", fest="saddula-bathukamma"),  # TS press copy says 10 Oct (typo; it is a Sunday)
    _e(_d(10, 18), "durgashtami-holiday", "దుర్గాష్టమి (AP సెలవు)", "Durgashtami (AP holiday)", ap="G", fest="durgashtami"),
    _e(_d(10, 20), "vijayadasami-holiday", "విజయదశమి (దసరా)", "Vijayadasami (Dasara)", ts="G", ap="G", fest="vijayadasami"),
    _e(_d(10, 21), "dasara-next-day", "విజయదశమి మరుసటి రోజు (TS)", "Day after Vijayadasami (TS)", ts="G"),
    _e(_d(11, 8), "deepavali-holiday", "దీపావళి", "Deepavali", ts="G", ap="G", fest="deepavali"),
    _e(_d(11, 8), "naraka-chaturdashi-holiday", "నరక చతుర్దశి", "Naraka Chaturdashi", ts="O", fest="naraka-chaturdashi"),
    _e(_d(11, 24), "karthika-pournami-holiday", "కార్తీక పౌర్ణమి", "Karthika Pournami", ts="G", fest="karthika-pournami"),
    _e(_d(11, 24), "guru-nanak-jayanti", "గురునానక్ జయంతి", "Guru Nanak Jayanti", ts="G", ap="O"),
    _e(_d(12, 24), "christmas-eve", "క్రిస్మస్ ఈవ్", "Christmas Eve", ts="O", ap="O"),
    _e(_d(12, 25), "christmas", "క్రిస్మస్", "Christmas", ts="G", ap="G"),
    _e(_d(12, 26), "boxing-day", "బాక్సింగ్ డే", "Boxing Day", ts="G", ap="O"),
]


def _fixed(year):
    """Statutory national days that fall on the same date every year."""
    def e(m, d, id_, te, en):
        return {"date": dt.date(year, m, d), "id": id_, "te": te, "en": en, "h": 1, "fest": None}
    return [
        e(1, 26, "republic-day", "గణతంత్ర దినోత్సవం", "Republic Day"),
        e(4, 14, "ambedkar-jayanti", "డా. బి.ఆర్. అంబేద్కర్ జయంతి", "Dr B.R. Ambedkar Jayanti"),
        e(8, 15, "independence-day", "స్వాతంత్ర్య దినోత్సవం", "Independence Day"),
        e(10, 2, "gandhi-jayanti", "గాంధీ జయంతి", "Gandhi Jayanti"),
        e(12, 25, "christmas", "క్రిస్మస్", "Christmas"),
    ]


def holidays_for(year):
    return HOLIDAYS_2026 if year == 2026 else _fixed(year)
