package com.telugucalendar.app.domain

enum class Lang(val code: String) {
    TE("te"), EN("en");

    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code } ?: TE
    }
}

/** A Telugu / English pair. */
class Str(val te: String, val en: String) {
    fun of(lang: Lang) = if (lang == Lang.TE) te else en
}

private fun s(te: String, en: String) = Str(te, en)

/** Telugu and English names for the panchangam elements; the data files only carry numbers (see DATA_FORMAT.md). */
object Names {
    private val tithis = listOf(
        s("పాడ్యమి", "Prathama"), s("విదియ", "Dwitiya"), s("తదియ", "Tritiya"), s("చవితి", "Chaturthi"),
        s("పంచమి", "Panchami"), s("షష్ఠి", "Shashthi"), s("సప్తమి", "Saptami"), s("అష్టమి", "Ashtami"),
        s("నవమి", "Navami"), s("దశమి", "Dashami"), s("ఏకాదశి", "Ekadashi"), s("ద్వాదశి", "Dwadashi"),
        s("త్రయోదశి", "Trayodashi"), s("చతుర్దశి", "Chaturdashi"),
    )
    private val pournami = s("పౌర్ణమి", "Pournami")
    private val amavasya = s("అమావాస్య", "Amavasya")

    /** Tithi number 1..30 (1..15 Shukla, 16..30 Krishna). */
    fun tithi(i: Int, lang: Lang): String {
        val n = ((i - 1).coerceIn(0, 29) % 15) + 1
        return when {
            n < 15 -> tithis[n - 1].of(lang)
            i <= 15 -> pournami.of(lang)
            else -> amavasya.of(lang)
        }
    }

    fun paksha(i: Int, lang: Lang) = if (i <= 15) s("శుక్ల పక్షం", "Shukla Paksha").of(lang) else s("బహుళ పక్షం", "Krishna Paksha").of(lang)

    /** Short paksha mark for tight places: శు / బ. */
    fun pakshaShort(i: Int, lang: Lang) = if (i <= 15) s("శు", "Sh").of(lang) else s("బ", "Kr").of(lang)

    private val nakshatras = listOf(
        s("అశ్విని", "Ashwini"), s("భరణి", "Bharani"), s("కృత్తిక", "Krittika"), s("రోహిణి", "Rohini"), s("మృగశిర", "Mrigashira"),
        s("ఆర్ద్ర", "Ardra"), s("పునర్వసు", "Punarvasu"), s("పుష్యమి", "Pushya"), s("ఆశ్లేష", "Ashlesha"), s("మఖ", "Magha"),
        s("పుబ్బ", "Purva Phalguni"), s("ఉత్తర", "Uttara Phalguni"), s("హస్త", "Hasta"), s("చిత్త", "Chitra"), s("స్వాతి", "Swati"),
        s("విశాఖ", "Vishakha"), s("అనూరాధ", "Anuradha"), s("జ్యేష్ఠ", "Jyeshtha"), s("మూల", "Moola"), s("పూర్వాషాఢ", "Purva Ashadha"),
        s("ఉత్తరాషాఢ", "Uttara Ashadha"), s("శ్రవణం", "Shravana"), s("ధనిష్ఠ", "Dhanishta"), s("శతభిషం", "Shatabhisha"),
        s("పూర్వాభాద్ర", "Purva Bhadrapada"), s("ఉత్తరాభాద్ర", "Uttara Bhadrapada"), s("రేవతి", "Revati"),
    )

    fun nakshatra(i: Int, lang: Lang) = nakshatras[(i - 1).coerceIn(0, 26)].of(lang)

    private val yogas = listOf(
        s("విష్కంభం", "Vishkambha"), s("ప్రీతి", "Preeti"), s("ఆయుష్మాన్", "Ayushman"), s("సౌభాగ్యం", "Saubhagya"), s("శోభనం", "Shobhana"),
        s("అతిగండం", "Atiganda"), s("సుకర్మ", "Sukarma"), s("ధృతి", "Dhriti"), s("శూలం", "Shula"), s("గండం", "Ganda"),
        s("వృద్ధి", "Vriddhi"), s("ధ్రువం", "Dhruva"), s("వ్యాఘాతం", "Vyaghata"), s("హర్షణం", "Harshana"), s("వజ్రం", "Vajra"),
        s("సిద్ధి", "Siddhi"), s("వ్యతీపాతం", "Vyatipata"), s("వరీయాన్", "Variyan"), s("పరిఘం", "Parigha"), s("శివం", "Shiva"),
        s("సిద్ధం", "Siddha"), s("సాధ్యం", "Sadhya"), s("శుభం", "Shubha"), s("శుక్లం", "Shukla"), s("బ్రహ్మం", "Brahma"),
        s("ఐంద్రం", "Indra"), s("వైధృతి", "Vaidhriti"),
    )

    fun yoga(i: Int, lang: Lang) = yogas[(i - 1).coerceIn(0, 26)].of(lang)

    private val karanas = listOf(
        s("బవ", "Bava"), s("బాలవ", "Balava"), s("కౌలవ", "Kaulava"), s("తైతుల", "Taitila"), s("గరజ", "Garija"), s("వణిజ", "Vanija"),
        s("విష్టి (భద్ర)", "Vishti (Bhadra)"), s("శకుని", "Shakuni"), s("చతుష్పాద", "Chatushpada"), s("నాగవ", "Naga"), s("కింస్తుఘ్నం", "Kimstughna"),
    )

    fun karana(i: Int, lang: Lang) = karanas[(i - 1).coerceIn(0, 10)].of(lang)

    private val lunarMonths = listOf(
        s("చైత్రం", "Chaitra"), s("వైశాఖం", "Vaisakha"), s("జ్యేష్ఠం", "Jyeshtha"), s("ఆషాఢం", "Ashadha"), s("శ్రావణం", "Sravana"),
        s("భాద్రపదం", "Bhadrapada"), s("ఆశ్వయుజం", "Ashwayuja"), s("కార్తీకం", "Karthika"), s("మార్గశిరం", "Margasira"),
        s("పుష్యం", "Pushya"), s("మాఘం", "Magha"), s("ఫాల్గుణం", "Phalguna"),
    )

    fun lunarMonth(ms: Int, adhika: Boolean, lang: Lang): String {
        val name = lunarMonths[(ms - 1).coerceIn(0, 11)].of(lang)
        return if (adhika) s("అధిక $name", "Adhika $name").of(lang) else name
    }

    private val rituNames = listOf(
        s("వసంత ఋతువు", "Vasanta (spring)"), s("గ్రీష్మ ఋతువు", "Grishma (summer)"), s("వర్ష ఋతువు", "Varsha (monsoon)"),
        s("శరద్ ఋతువు", "Sharad (autumn)"), s("హేమంత ఋతువు", "Hemanta (early winter)"), s("శిశిర ఋతువు", "Shishira (late winter)"),
    )

    /** The season for a lunar month: two months each, starting with Chaitra. */
    fun ritu(ms: Int, lang: Lang) = rituNames[((ms - 1).coerceIn(0, 11)) / 2].of(lang)

    fun ayana(ay: Int, lang: Lang) = if (ay == 2) s("దక్షిణాయనం", "Dakshinayana").of(lang) else s("ఉత్తరాయణం", "Uttarayana").of(lang)

    private val weekdays = listOf(
        s("ఆదివారం", "Sunday"), s("సోమవారం", "Monday"), s("మంగళవారం", "Tuesday"), s("బుధవారం", "Wednesday"),
        s("గురువారం", "Thursday"), s("శుక్రవారం", "Friday"), s("శనివారం", "Saturday"),
    )
    private val weekdaysShort = listOf(
        s("ఆది", "Sun"), s("సోమ", "Mon"), s("మంగళ", "Tue"), s("బుధ", "Wed"), s("గురు", "Thu"), s("శుక్ర", "Fri"), s("శని", "Sat"),
    )

    /** 0 = Sunday .. 6 = Saturday. */
    fun weekday(sundayIndex: Int, lang: Lang) = weekdays[sundayIndex.mod(7)].of(lang)
    fun weekdayShort(sundayIndex: Int, lang: Lang) = weekdaysShort[sundayIndex.mod(7)].of(lang)

    private val gregorianMonths = listOf(
        s("జనవరి", "January"), s("ఫిబ్రవరి", "February"), s("మార్చి", "March"), s("ఏప్రిల్", "April"), s("మే", "May"), s("జూన్", "June"),
        s("జూలై", "July"), s("ఆగస్టు", "August"), s("సెప్టెంబర్", "September"), s("అక్టోబర్", "October"), s("నవంబర్", "November"), s("డిసెంబర్", "December"),
    )

    fun gregorianMonth(month: Int, lang: Lang) = gregorianMonths[(month - 1).coerceIn(0, 11)].of(lang)
    fun gregorianMonthShort(month: Int, lang: Lang) =
        if (lang == Lang.EN) gregorianMonths[(month - 1).coerceIn(0, 11)].en.take(3) else gregorianMonths[(month - 1).coerceIn(0, 11)].te

    /** "పరాభవ నామ సంవత్సరం" / "Parabhava year". */
    fun samvatsaraTitle(name: Str, lang: Lang) = if (lang == Lang.TE) "${name.te} నామ సంవత్సరం" else "${name.en} year"
}
