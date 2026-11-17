"""Festival and vratam rules, evaluated against the astronomical facts in model.Context.

A rule names a Telugu (amanta) month, a tithi (1..15 Shukla, 16..30 Krishna) and the time of day at which that tithi must
prevail (see Context.time_of: sunrise, arunodaya, purvahna, madhyahna, aparahna, pradosha, nishita, moonrise, or `daymax`
= the day on which the tithi covers most of the daytime). Festivals fall only in the nija (non-adhika) month; vratams
(Ekadasi, Pradosham, ...) fall in every month, Adhika ones included.

The rules were tuned until 2026 matched the published Telugu panchangams (see expected-2026.json and verify.py).
`YEAR_OVERRIDES` lets a year pin a festival to a different date when the published panchangam deliberately differs from the rule.
"""
import datetime as dt

import astro
from holidays import holidays_for

S = lambda n: n  # noqa: E731  Shukla n
K = lambda n: 15 + n  # noqa: E731  Krishna n (K(15) = 30 = Amavasya)

MONTH_TE = ["చైత్రం", "వైశాఖం", "జ్యేష్ఠం", "ఆషాఢం", "శ్రావణం", "భాద్రపదం", "ఆశ్వయుజం", "కార్తీకం", "మార్గశిరం", "పుష్యం", "మాఘం", "ఫాల్గుణం"]
MONTH_EN = ["Chaitra", "Vaisakha", "Jyeshtha", "Ashadha", "Sravana", "Bhadrapada", "Ashwayuja", "Karthika", "Margasira", "Pushya", "Magha", "Phalguna"]
RASI_TE = ["మేష", "వృషభ", "మిథున", "కర్కాటక", "సింహ", "కన్య", "తుల", "వృశ్చిక", "ధనుస్సు", "మకర", "కుంభ", "మీన"]
RASI_EN = ["Mesha", "Vrishabha", "Mithuna", "Karkataka", "Simha", "Kanya", "Tula", "Vrischika", "Dhanus", "Makara", "Kumbha", "Meena"]

SAMVATSARA_EN = (
    "Prabhava Vibhava Shukla Pramodoota Prajotpatti Angirasa Srimukha Bhava Yuva Dhata Ishvara Bahudhanya Pramathi Vikrama Vrusha Chitrabhanu "
    "Svabhanu Tarana Parthiva Vyaya Sarvajit Sarvadhari Virodhi Vikruti Khara Nandana Vijaya Jaya Manmatha Durmukhi Hevilambi Vilambi Vikari "
    "Sarvari Plava Shubhakrit Shobhakrit Krodhi Vishvavasu Parabhava Plavanga Keelaka Saumya Sadharana Virodhikrit Paridhavi Pramadicha Ananda "
    "Rakshasa Nala Pingala Kalayukti Siddharthi Raudri Durmati Dundubhi Rudhirodgari Raktakshi Krodhana Akshaya"
).split()
SAMVATSARA_TE = (
    "ప్రభవ విభవ శుక్ల ప్రమోదూత ప్రజోత్పత్తి ఆంగీరస శ్రీముఖ భావ యువ ధాత ఈశ్వర బహుధాన్య ప్రమాథి విక్రమ వృష చిత్రభాను స్వభాను తారణ పార్థివ వ్యయ "
    "సర్వజిత్తు సర్వధారి విరోధి వికృతి ఖర నందన విజయ జయ మన్మథ దుర్ముఖి హేవిళంబి విళంబి వికారి శార్వరి ప్లవ శుభకృత్తు శోభకృత్తు క్రోధి విశ్వావసు పరాభవ "
    "ప్లవంగ కీలక సౌమ్య సాధారణ విరోధికృత్తు పరీధావి ప్రమాదీచ ఆనంద రాక్షస నల పింగళ కాళయుక్తి సిద్ధార్థి రౌద్రి దుర్మతి దుందుభి రుధిరోద్గారి రక్తాక్షి క్రోధన అక్షయ"
).split()
assert len(SAMVATSARA_EN) == 60 and len(SAMVATSARA_TE) == 60


def samvatsara(ugadi_year):
    """The Telugu year that begins at the Ugadi of this Gregorian year (2026 -> Parabhava, 2025 -> Vishvavasu)."""
    i = (ugadi_year - 1987) % 60
    return SAMVATSARA_TE[i], SAMVATSARA_EN[i]


# id, te, en, month, tithi, rule, tie, ti, ei
# Order inside this list is the order of festival items on a day.
FESTIVALS = [
    ("ugadi", "ఉగాది", "Ugadi", 1, S(1), "daymax", "first", "తెలుగు వారి నూతన సంవత్సరం; ఉగాది పచ్చడి", "Telugu New Year; Ugadi pachadi is shared"),
    ("sri-rama-navami", "శ్రీ రామ నవమి", "Sri Rama Navami", 1, S(9), "sunrise", "first", "శ్రీరాముని జన్మదినం; సీతారాముల కళ్యాణం", "Birthday of Sri Rama; Sita Rama Kalyanam"),
    ("akshaya-tritiya", "అక్షయ తృతీయ", "Akshaya Tritiya", 2, S(3), "purvahna", "first", "కొత్త పనులు, దానధర్మాలకు శుభదినం", "Auspicious for new beginnings and charity"),
    ("sankara-jayanti", "శంకర జయంతి", "Sankara Jayanti", 2, S(5), "madhyahna", "first", None, None),
    ("narasimha-jayanti", "నృసింహ జయంతి", "Narasimha Jayanti", 2, S(14), "sunset", "first", "శ్రీ నరసింహ స్వామి అవతార దినం", "Avatar day of Lord Narasimha"),
    ("buddha-pournami", "బుద్ధ పూర్ణిమ", "Buddha Pournami", 2, S(15), "sunrise", "first", None, None),
    ("hanuman-jayanti", "హనుమజ్జయంతి", "Hanuman Jayanti", 2, K(10), "sunrise", "first", "తెలుగు రాష్ట్రాల్లో వైశాఖ బహుళ దశమి", "Celebrated in the Telugu states on Vaisakha Krishna Dasami"),
    ("ganga-dasara", "గంగా దసరా", "Ganga Dasara", 3, S(10), "sunrise", "first", None, None),
    ("ratha-yatra", "జగన్నాథ రథయాత్ర", "Jagannatha Ratha Yatra", 4, S(2), "sunrise", "first", None, None),
    ("tholi-ekadasi", "తొలి ఏకాదశి", "Tholi Ekadasi", 4, S(11), "sunrise", "first", "ఆషాఢ శుద్ధ ఏకాదశి; విష్ణుమూర్తి యోగనిద్ర ప్రారంభం", "First Ekadasi of the year; Vishnu's yoga nidra begins"),
    ("guru-pournami", "గురు పౌర్ణమి", "Guru Pournami", 4, S(15), "madhyahna", "first", "వ్యాస పూర్ణిమ; గురువులను పూజించే రోజు", "Vyasa Pournima; a day to honour gurus"),
    ("naga-panchami", "నాగ పంచమి", "Naga Panchami", 5, S(5), "sunrise", "first", None, None),
    ("raksha-bandhan", "రాఖీ పౌర్ణమి", "Raksha Bandhan (Rakhi Pournami)", 5, S(15), "rakhi", "first", "శ్రావణ పౌర్ణమి; రాఖీ కట్టే పండుగ", "Sravana Pournami; sisters tie rakhi"),
    ("krishna-janmashtami", "శ్రీకృష్ణ జన్మాష్టమి", "Sri Krishna Janmashtami", 5, K(8), "sunrise", "first", "శ్రీకృష్ణుని జన్మదినం", "Birth of Sri Krishna"),
    ("polala-amavasya", "పోలాల అమావాస్య", "Polala Amavasya", 5, K(15), "sunrise", "first", None, None),
    ("vinayaka-chavithi", "వినాయక చవితి", "Vinayaka Chavithi", 6, S(4), "madhyahna", "first", "గణపతి జన్మదినం; నవరాత్రి ఉత్సవాలు ప్రారంభం", "Birthday of Lord Ganesha; nine-day festival begins"),
    ("rishi-panchami", "ఋషి పంచమి", "Rishi Panchami", 6, S(5), "madhyahna", "first", None, None),
    ("vamana-jayanti", "వామన జయంతి", "Vamana Jayanti", 6, S(12), "sunrise", "first", None, None),
    ("ananta-chaturdashi", "అనంత పద్మనాభ చతుర్దశి", "Ananta Chaturdashi", 6, S(14), "sunrise", "first", "గణపతి నిమజ్జనం; అనంత పద్మనాభ వ్రతం", "Ganesh immersion; Ananta Padmanabha Vratam"),
    ("mahalaya-paksham", "మహాలయ పక్షం ప్రారంభం", "Mahalaya Paksham begins", 6, K(1), "sunrise", "first", "పితృ దేవతలను స్మరించే పక్షం", "Fortnight of remembering ancestors"),
    ("mahalaya-amavasya", "మహాలయ అమావాస్య", "Mahalaya Amavasya", 6, K(15), "aparahna", "first", "పితృ దేవతలకు తర్పణం", "Offerings to ancestors"),
    ("engili-pula-bathukamma", "ఎంగిలి పూల బతుకమ్మ", "Engili Pula Bathukamma", 6, K(15), "aparahna", "first", "తెలంగాణ పూల పండుగ ప్రారంభం", "Start of Telangana's festival of flowers"),
    ("devi-navaratri", "దేవీ నవరాత్రులు ప్రారంభం", "Devi Navaratri begins", 7, S(1), "sunrise", "first", "శరన్నవరాత్రులు ప్రారంభం", "Sharad Navaratri begins"),
    ("durgashtami", "దుర్గాష్టమి", "Durgashtami", 7, S(8), "sunrise", "first", None, None),
    ("saddula-bathukamma", "సద్దుల బతుకమ్మ", "Saddula Bathukamma", 7, S(8), "madhyahna", "first", "బతుకమ్మ ఉత్సవాల చివరి రోజు", "Last day of Bathukamma"),
    ("maha-navami", "మహా నవమి", "Maha Navami", 7, S(9), "aparahna", "first", None, None),
    ("vijayadasami", "విజయదశమి (దసరా)", "Vijayadasami (Dasara)", 7, S(10), "aparahna", "first", "చెడుపై మంచి విజయం", "Victory of good over evil"),
    ("atla-tadde", "అట్ల తద్దె", "Atla Tadde", 7, K(3), "sunrise", "first", None, None),
    ("dhanatrayodashi", "ధన త్రయోదశి", "Dhanatrayodashi", 7, K(13), "pradosha", "first", None, None),
    ("naraka-chaturdashi", "నరక చతుర్దశి", "Naraka Chaturdashi", 7, K(14), "arunodaya", "first", "తెల్లవారుజామున అభ్యంగన స్నానం", "Pre-dawn oil bath"),
    ("deepavali", "దీపావళి", "Deepavali", 7, K(15), "pradosha", "first", "దీపాల పండుగ; లక్ష్మీ పూజ", "Festival of lights; Lakshmi Puja"),
    ("bali-padyami", "బలి పాడ్యమి", "Bali Padyami", 8, S(1), "sunrise", "first", "గోవర్ధన పూజ; కార్తీక మాసం ప్రారంభం", "Govardhana Puja; Karthika masam begins"),
    ("bhagini-hastha-bhojanam", "భగినీ హస్త భోజనం", "Bhagini Hastha Bhojanam (Yama Dwitiya)", 8, S(2), "aparahna", "last", "అన్నాచెల్లెళ్ల పండుగ", "Brothers dine at their sisters' homes"),
    ("nagula-chavithi", "నాగుల చవితి", "Nagula Chavithi", 8, S(4), "madhyahna", "first", None, None),
    ("kshirabdi-dwadasi", "క్షీరాబ్ది ద్వాదశి", "Kshirabdi Dwadasi", 8, S(12), "sunrise", "first", "తులసి పూజ; తులసి కళ్యాణం", "Tulasi puja and Tulasi Kalyanam"),
    ("karthika-pournami", "కార్తీక పౌర్ణమి", "Karthika Pournami", 8, S(15), "pradosha", "first", "కార్తీక దీపారాధన; శివారాధన", "Lamps and Shiva worship"),
    ("subrahmanya-shashti", "సుబ్రహ్మణ్య షష్ఠి", "Subrahmanya Shashti", 9, S(6), "madhyahna", "first", None, None),
    ("gita-jayanti", "గీతా జయంతి", "Gita Jayanti", 9, S(11), "sunrise", "first", "మోక్షద ఏకాదశి నాడు", "On Mokshada Ekadasi"),
    ("dattatreya-jayanti", "దత్తాత్రేయ జయంతి", "Dattatreya Jayanti", 9, S(15), "pradosha", "first", None, None),
    ("vasanta-panchami", "వసంత పంచమి (శ్రీ పంచమి)", "Vasanta Panchami (Sri Panchami)", 11, S(5), "madhyahna", "first", "సరస్వతీ పూజ; అక్షరాభ్యాసం", "Saraswati Puja; Aksharabhyasam"),
    ("ratha-saptami", "రథసప్తమి", "Ratha Saptami", 11, S(7), "sunrise", "first", "సూర్య జయంతి", "Surya Jayanti"),
    ("bhishma-ekadasi", "భీష్మ ఏకాదశి", "Bhishma Ekadasi", 11, S(11), "sunrise", "first", None, None),
    ("maha-shivaratri", "మహా శివరాత్రి", "Maha Shivaratri", 11, K(14), "nishita", "first", "శివుని మహారాత్రి; ఉపవాసం, జాగరణ", "The great night of Shiva; fasting and night vigil"),
    ("kamuni-pournami", "కామదహనం (హోలీ)", "Kamuni Pournami (Holi)", 12, S(15), "pradosha", "first", "కామదహనం; రంగుల పండుగ", "Kamadahanam; festival of colours"),
]

# Each Ekadasi has its own name by (month, paksha). Adhika months have Padmini (Shukla) and Parama (Krishna).
EKADASI = {
    (1, "S"): ("కామద ఏకాదశి", "Kamada Ekadasi"), (1, "K"): ("వరూథిని ఏకాదశి", "Varuthini Ekadasi"),
    (2, "S"): ("మోహిని ఏకాదశి", "Mohini Ekadasi"), (2, "K"): ("అపర ఏకాదశి", "Apara Ekadasi"),
    (3, "S"): ("నిర్జల ఏకాదశి", "Nirjala Ekadasi"), (3, "K"): ("యోగిని ఏకాదశి", "Yogini Ekadasi"),
    (4, "S"): ("దేవశయని ఏకాదశి", "Devashayani Ekadasi"), (4, "K"): ("కామిక ఏకాదశి", "Kamika Ekadasi"),
    (5, "S"): ("శ్రావణ పుత్రద ఏకాదశి", "Sravana Putrada Ekadasi"), (5, "K"): ("అజ ఏకాదశి", "Aja Ekadasi"),
    (6, "S"): ("పరివర్తిని ఏకాదశి", "Parivartini Ekadasi"), (6, "K"): ("ఇందిర ఏకాదశి", "Indira Ekadasi"),
    (7, "S"): ("పాపాంకుశ ఏకాదశి", "Papankusha Ekadasi"), (7, "K"): ("రమా ఏకాదశి", "Rama Ekadasi"),
    (8, "S"): ("ప్రబోధిని ఏకాదశి", "Prabodhini Ekadasi"), (8, "K"): ("ఉత్పన్న ఏకాదశి", "Utpanna Ekadasi"),
    (9, "S"): ("మోక్షద ఏకాదశి", "Mokshada Ekadasi"), (9, "K"): ("సఫల ఏకాదశి", "Saphala Ekadasi"),
    (10, "S"): ("పౌష పుత్రద ఏకాదశి", "Pausha Putrada Ekadasi"), (10, "K"): ("షట్తిల ఏకాదశి", "Shattila Ekadasi"),
    (11, "S"): ("జయ ఏకాదశి", "Jaya Ekadasi"), (11, "K"): ("విజయ ఏకాదశి", "Vijaya Ekadasi"),
    (12, "S"): ("ఆమలకి ఏకాదశి", "Amalaki Ekadasi"), (12, "K"): ("పాపమోచని ఏకాదశి", "Papamochani Ekadasi"),
    ("adhika", "S"): ("పద్మిని ఏకాదశి", "Padmini Ekadasi"), ("adhika", "K"): ("పరమ ఏకాదశి", "Parama Ekadasi"),
}

# Pins for a year when the published Telugu panchangam deliberately differs from the rule: {year: {festival id: [dates]}}.
YEAR_OVERRIDES = {
    # Holika Dahan 2026: Bhadra (Vishti karana) covers the whole Pournami evening of 2 Mar, so Drik Panchang and the Telugu
    # panchangams place Kamadahanam on 3 Mar, although Pournami has ended by 17:07 that day. The rule would say 2 Mar.
    2026: {"kamuni-pournami": [dt.date(2026, 3, 3)]},
}

GROUP = {"festival": 0, "holiday": 1, "vratam": 2, "sankranti": 3, "eclipse": 3, "other": 3}


def item(id_, kind, te, en, ti=None, ei=None, h=None):
    d = {"id": id_, "k": kind, "te": te, "en": en}
    if h:
        d["h"] = h
    if ti:
        d["ti"] = ti
    if ei:
        d["ei"] = ei
    return d


def compute(ctx, year):
    """{date: [item, ...]} for one Gregorian year."""
    out = {}

    def add(date, it):
        if date.year == year:
            out.setdefault(date, []).append(it)

    # ---- table festivals
    festival_dates = {}  # id -> [dates], used by holidays and by the dependent rules below
    for id_, te, en, month, tn, rule, tie, ti, ei in FESTIVALS:
        dates = [d for d, _m, _a in ctx.pick_days(month, tn, rule, tie)]
        dates = YEAR_OVERRIDES.get(year, {}).get(id_, dates)
        for d in dates:
            festival_dates.setdefault(id_, []).append(d)
            add(d, item(id_, "festival", te, en, ti, ei))

    # ---- solar festivals and sankrantis
    jd0 = ctx.days[0].sr
    jd1 = ctx.days[-1].nsr
    # The sankranti items (and Uttarayana / Dakshinayana / Dhanurmasam) carry the calendar date on which the Sun enters the rasi,
    # as the Telugu panchangams list them. The Makara Sankranti FESTIVAL (and Bhogi / Kanuma / Mukkanuma around it) is the day of
    # entry if the Sun enters before sunset, otherwise the next day (2027: entry 21:14 on 14 Jan, so the festival is 15 Jan).
    for jd, sign in astro.sankrantis(jd0, jd1):
        civil = astro.to_local(jd).date()
        add(civil, item("sankranti", "sankranti", f"{RASI_TE[sign]} సంక్రమణం", f"{RASI_EN[sign]} Sankranti"))
        if sign == 9:  # Makara
            day = ctx.by_date.get(civil)
            festival = civil if day is None or jd < day.ss else civil + dt.timedelta(days=1)
            add(festival - dt.timedelta(days=1), item("bhogi", "festival", "భోగి", "Bhogi", "భోగి మంటలు; పిల్లలకు భోగి పళ్ళు", "Bonfires of old things; Bhogi pallu for children"))
            add(festival, item("makara-sankranti", "festival", "మకర సంక్రాంతి", "Makara Sankranti", "సూర్యుడు మకర రాశిలోకి; ఉత్తరాయణం ప్రారంభం", "Sun enters Makara; Uttarayana begins"))
            add(festival + dt.timedelta(days=1), item("kanuma", "festival", "కనుమ", "Kanuma", "పశువుల పండుగ", "Festival of cattle"))
            add(festival + dt.timedelta(days=2), item("mukkanuma", "festival", "ముక్కనుమ", "Mukkanuma"))
            add(civil, item("uttarayana", "other", "ఉత్తరాయణ పుణ్యకాలం ప్రారంభం", "Uttarayana begins"))
        if sign == 3:
            add(civil, item("dakshinayana", "other", "దక్షిణాయన పుణ్యకాలం ప్రారంభం", "Dakshinayana begins"))
        if sign == 8:
            add(civil, item("dhanurmasam", "other", "ధనుర్మాసం ప్రారంభం", "Dhanurmasam begins"))

    # ---- Varalakshmi Vratham: the Friday before Sravana Pournami
    for d, _m, _a in ctx.pick_days(5, 15, "sunrise"):
        v = d - dt.timedelta(days=1)
        while v.weekday() != 4:
            v -= dt.timedelta(days=1)
        festival_dates.setdefault("varalakshmi-vratham", []).append(v)
        add(v, item("varalakshmi-vratham", "festival", "వరలక్ష్మీ వ్రతం", "Varalakshmi Vratham", "శ్రావణ మాసంలో లక్ష్మీదేవి పూజ", "Worship of Goddess Lakshmi on a Friday of Sravana"))

    # ---- Saraswati Puja: Moola nakshatra during Devi Navaratri
    for nav in festival_dates.get("devi-navaratri", []):
        for i in range(10):
            d = nav + dt.timedelta(days=i)
            day = ctx.by_date.get(d)
            if day and astro.nakshatra(day.sr) == 19:
                add(d, item("saraswati-puja", "festival", "సరస్వతీ పూజ (మూల)", "Saraswati Puja (Moola)", "మూల నక్షత్రం నాడు సరస్వతీ పూజ", "Saraswati Puja on Moola nakshatra"))
                break

    # ---- Masam begins (every lunar month, Adhika included): the first day whose sunrise lies in the new month.
    # Chaitra begins on Ugadi, as the panchangams print it (the new moon can fall just after that sunrise).
    previous = ctx.months.at(ctx.days[0].sr)
    for day in ctx.days[1:]:
        current = ctx.months.at(day.sr)
        if current != previous:
            m, adhika = current
            date = day.date
            if m == 1 and not adhika and festival_dates.get("ugadi"):
                date = min(festival_dates["ugadi"], key=lambda u: abs((u - date).days))
            kind = f"అధిక {MONTH_TE[m - 1]}" if adhika else MONTH_TE[m - 1]
            kind_en = f"Adhika {MONTH_EN[m - 1]}" if adhika else MONTH_EN[m - 1]
            add(date, item("masam-begins", "other", f"{kind} మాసం ప్రారంభం", f"{kind_en} masam begins"))
        previous = current

    # ---- vratams
    ekadasi_shukla = []
    for paksha, tn in (("S", 11), ("K", 26)):
        for d, m, adhika in ctx.pick_days(None, tn, "smarta", nija_only=False):
            te, en = EKADASI[("adhika" if adhika else m, paksha)]
            add(d, item("ekadasi", "vratam", te, en))
            if paksha == "S" and not adhika:
                ekadasi_shukla.append(d)
    # Vaikuntha (Mukkoti) Ekadasi: the Shukla Ekadasi that falls in Dhanurmasam (Sun in Dhanus)
    for d in ekadasi_shukla:
        if astro.sun_sign(ctx.by_date[d].sr) == 8:
            festival_dates.setdefault("vaikuntha-ekadasi", []).append(d)
            add(d, item("vaikuntha-ekadasi", "festival", "వైకుంఠ ఏకాదశి (ముక్కోటి)", "Vaikuntha (Mukkoti) Ekadasi", "ఉత్తర ద్వార దర్శనం", "Darshan through the northern gate"))
    for d, m, _a in ctx.pick_days(None, 13, "pradosha", nija_only=False):
        add(d, item("pradosham", "vratam", "ప్రదోష వ్రతం (శుక్ల)", "Pradosham (Shukla)"))
    for d, m, _a in ctx.pick_days(None, 28, "pradosha", nija_only=False):
        add(d, item("pradosham", "vratam", "ప్రదోష వ్రతం (కృష్ణ)", "Pradosham (Krishna)"))
    for d, m, _a in ctx.pick_days(None, 19, "moonrise", nija_only=False):
        add(d, item("sankashti-chaturthi", "vratam", "సంకష్టహర చతుర్థి", "Sankashti Chaturthi"))
    for d, m, _a in ctx.pick_days(None, 29, "nishita", nija_only=False):
        if m != 11:  # Magha is Maha Shivaratri
            add(d, item("masa-shivaratri", "vratam", "మాస శివరాత్రి", "Masa Shivaratri"))
    for d, m, _a in ctx.pick_days(None, 30, "sunrise", nija_only=False):
        add(d, item("amavasya", "vratam", "అమావాస్య", "Amavasya"))
    for d, m, _a in ctx.pick_days(None, 15, "sunrise", nija_only=False):
        add(d, item("pournami", "vratam", "పౌర్ణమి", "Pournami"))

    # ---- government holidays: set h on a matching festival, otherwise add a holiday item
    for entry in holidays_for(year):
        date = entry["date"]
        if date.year != year:
            continue
        matched = False
        if entry.get("fest"):
            for it in out.get(date, []):
                if it["id"] == entry["fest"]:
                    if entry["h"] and not it.get("h"):
                        it["h"] = entry["h"]
                    matched = True
        if not matched:
            if entry.get("fest"):
                print(f"  note: holiday '{entry['id']}' on {date} has no festival '{entry['fest']}' that day; added as a plain holiday")
            add(date, item(entry["id"], "holiday", entry["te"], entry["en"], h=entry.get("h")))

    # ---- keep a stable order: festivals, holidays, vratams, sankranti / eclipse / other
    for date, items in out.items():
        items.sort(key=lambda it: GROUP[it["k"]])  # sort is stable: table order is kept inside a group
    return out, festival_dates
