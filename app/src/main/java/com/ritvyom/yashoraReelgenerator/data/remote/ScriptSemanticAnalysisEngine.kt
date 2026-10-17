package com.ritvyom.yashoraReelgenerator.data.remote

import android.util.Log

/**
 * High-precision Semantic Analysis & Token Parsing Feedback Loop Engine.
 *
 * Parses script segments and voice-over tokens across English, Hindi (Devanagari),
 * and Hinglish to generate high-precision, concrete search queries strictly related
 * to the action or subject of the voice-over.
 *
 * Systematically eliminates all generic filler/mood fluff ("atmospheric", "cinematic",
 * "scene", "view", "moody", etc.) and enforces 2 to 4 concrete English nouns and action verbs
 * that index directly into Pexels, Pixabay, Unsplash, and Wikimedia Commons databases.
 */
object ScriptSemanticAnalysisEngine {

    private const val TAG = "SemanticAnalysisEngine"

    val BANNED_FLUFF_WORDS = setOf(
        "atmospheric", "cinematic", "scene", "visual", "narrative", "storytelling",
        "view", "moment", "perspective", "shot", "ambient", "moody", "aesthetic",
        "4k", "8k", "hd", "photorealistic", "style", "representation", "composition",
        "wallpaper", "footage", "vibe", "video", "photo", "image", "background",
        "cinematic narrative scene", "cinematic visual", "atmospheric view",
        "dramatic atmospheric storytelling view", "storytelling narration scene",
        "authentic scene", "candid moment", "beautiful view", "candid", "candid portrait",
        "candid photo", "candid snapshot", "real life candid", "real life candid people portrait",
        "real life candid portrait", "candid people", "candid shot", "real life candid people",
        // Banned generic single tags and hallucinated filler
        "vibrant", "crime", "vibrant crime", "crime vibrant", "vibrant, crime", "crime, vibrant",
        "vibrant scenic visual", "news studio television broadcast", "courtroom gavel",
        "generic", "placeholder", "trending", "viral", "headline", "breaking news",
        "news broadcast", "studio", "breaking", "update", "report", "news report", "clip", "broll", "b-roll", "stock",
        "documentary", "documentary photo", "documentary news", "documentary sequence", "historical documentary", "documentary visual", "documentary lifestyle", "documentary scene"
    )

    data class SemanticAnalysisResult(
        val criticalEntity: String?,
        val sceneType: MediaSelectionService.SceneType,
        val mandatoryTokens: List<String>,
        val refinedKeywords: List<String>,
        val pexelsQuery: String,
        val pixabayQuery: String,
        val pixabayImageType: String, // "photo", "illustration", "vector", "all"
        val wikimediaQuery: String,
        val aiPrompt: String
    )

    // Extensive Hindi/Hinglish to English Stock Keyword Dictionary
    private val HINDI_TO_ENGLISH_MAP = mapOf(
        // 1. Medical & Health
        "डॉक्टर" to "doctor", "डाक्टर" to "doctor", "मरीज" to "patient", "रोगी" to "patient",
        "अस्पताल" to "hospital", "हॉस्पिटल" to "hospital", "ऑपरेशन" to "surgery room", "दवा" to "medicine pharmacy",
        "दवाई" to "medicine", "इंजेक्शन" to "syringe injection", "इलाज" to "medical treatment", "नर्स" to "nurse hospital",
        "बीमार" to "sick patient", "बीमारी" to "disease illness", "क्लीनिक" to "medical clinic", "स्टेथोस्कोप" to "stethoscope doctor",
        "एम्बुलेंस" to "ambulance emergency", "खून" to "blood test lab", "दवाखाना" to "pharmacy drugstore",

        // 2. Police, Crime, Legal & Law (Only for actual crime/police stories)
        "पुलिस" to "police officer", "दारोगा" to "police inspector", "थाना" to "police station", "सिपाही" to "policeman patrol",
        "जेल" to "prison cell bars", "कैदी" to "prisoner inmate", "हथकड़ी" to "handcuffs arrest", "चोर" to "thief suspect",
        "चोरी" to "burglary crime", "डकैती" to "heist robbery", "अदालत" to "courtroom", "कोर्ट" to "courtroom judge",
        "जज" to "judge gavel courtroom", "वकील" to "lawyer attorney courtroom", "फैसला" to "judge verdict gavel",
        "मुकदमा" to "courtroom trial", "हत्या" to "crime scene evidence", "गुनाह" to "crime investigation",
        "सबूत" to "evidence proof facts", "जांच" to "examination analysis study",
        "हथियार" to "weapon defense", "गोली" to "bullet",

        // 3. Farming & Agriculture
        "किसान" to "farmer field", "खेती" to "agriculture farming", "खेत" to "farmland crops field",
        "ट्रैक्टर" to "tractor field plowing", "फसल" to "wheat crop harvest", "बीज" to "seeds soil planting",
        "गेंहू" to "golden wheat crop", "धान" to "rice paddy field", "हल" to "plowing soil agriculture",
        "गांव" to "rural village countryside", "हरियाली" to "green lush agriculture fields",

        // 4. Education, School & Learning
        "स्कूल" to "school classroom student", "कॉलेज" to "college campus students", "शिक्षक" to "teacher chalkboard classroom",
        "अध्यापक" to "teacher classroom", "गुरु" to "teacher mentor", "छात्र" to "student studying desk",
        "विद्यार्थी" to "student writing notebook", "पढ़ाई" to "studying open books", "किताब" to "open book library desk",
        "किताबें" to "library books bookshelf", "परीक्षा" to "student exam classroom", "कक्षा" to "classroom teacher students",
        "कलम" to "fountain pen writing paper", "पेंसिल" to "pencil writing notebook",

        // 5. Business, Finance & Wealth
        "पैसा" to "cash banknotes money", "पैसे" to "paper cash money", "धन" to "wealth gold coins",
        "दौलत" to "rich wealth banknotes", "रुपया" to "currency cash notes", "रुपये" to "money cash banknotes",
        "डॉलर" to "us dollar banknotes cash", "बैंक" to "bank vault teller", "खाता" to "banking financial account",
        "शेयर" to "stock market candlestick chart", "व्यापार" to "business meeting corporate",
        "बिजनेस" to "corporate businesswoman businessman", "अमीर" to "wealthy luxury lifestyle",
        "गरीब" to "poverty humble person", "नौकरी" to "office employee working", "दफ्तर" to "modern office workspace",
        "ऑफिस" to "corporate office desk", "इंटरव्यू" to "job interview professional", "बॉस" to "business manager boardroom",

        // 6. Technology & Digital
        "मोबाइल" to "smartphone screen hands", "फोन" to "smartphone screen", "स्मार्टफोन" to "smartphone modern technology",
        "लैपटॉप" to "computer desktop screen typing", "कंप्यूटर" to "computer desktop screen",
        "कोडिंग" to "programming code screen", "इंटरनेट" to "modern digital technology",
        "कैमरा" to "dslr camera filming lens", "वीडियो" to "video camera recording", "यूट्यूब" to "content creator video recording",

        // 7. Sports & Fitness
        "क्रिकेट" to "cricket match batsman stadium", "बल्ला" to "cricket bat batting", "गेंद" to "cricket ball bowler",
        "स्टेडियम" to "cricket stadium crowd floodlights", "मैच" to "cricket tournament stadium",
        "खिलाड़ी" to "athlete running sports", "दौड़" to "runner sprint track", "कसरत" to "gym workout training",
        "जिम" to "gym fitness dumbbells barbell", "फुटबॉल" to "soccer player stadium match",

        // 8. Travel, Roads & Vehicles
        "कार" to "modern car highway driving", "गाड़ी" to "car vehicle driving", "बस" to "passenger bus highway",
        "ट्रेन" to "railway passenger train tracks", "रेलगाड़ी" to "locomotive train railway",
        "हवाई जहाज" to "airplane flight clouds", "विमान" to "commercial airplane flying",
        "सड़क" to "highway asphalt road", "रास्ता" to "scenic road landscape", "सफर" to "travel suitcase journey",
        "यात्रा" to "travel tourism adventure", "बाइक" to "motorcycle rider road",

        // 9. Nature, Weather & Geography
        "पहाड़" to "snow mountain peaks", "पर्वत" to "majestic mountain landscape", "नदी" to "flowing river water",
        "झरना" to "waterfall cascade rocks", "जंगल" to "lush green forest trees", "पेड़" to "green nature trees",
        "बारिश" to "rain drops storm street", "धूप" to "warm golden sunlight morning", "सूरज" to "sunrise glowing dawn",
        "चांद" to "glowing crescent moon night", "तारे" to "starry night sky milky way",
        "समंदर" to "ocean waves beach shore", "समुद्र" to "blue ocean sea waves", "रेगिस्तान" to "desert sand dunes sunny",

        // 10. Family & Relationships
        "मां" to "mother loving child", "माता" to "mother baby caring", "पिता" to "father son daughter",
        "बाप" to "father loving family", "भाई" to "brothers together smiling", "बहन" to "sister sibling hug",
        "बेटा" to "young son boy smiling", "बेटी" to "daughter young girl smiling", "परिवार" to "happy family living room",
        "शादी" to "indian wedding bride groom", "विवाह" to "traditional wedding ceremony",
        "दोस्त" to "friends laughing together group", "दोस्ती" to "friends celebration lifestyle",

        // 11. Animals & Wildlife
        "शेर" to "african lion majestic", "बाघ" to "bengal tiger forest", "हाथी" to "wild elephant herd",
        "घोड़ा" to "horse running gallop field", "कुत्ता" to "cute dog puppy", "बिल्ली" to "cute cat kitten",
        "पक्षी" to "birds flying blue sky", "चिड़िया" to "sparrow bird branch", "सांप" to "snake reptile nature",

        // 12. Emotions & Human Action
        "आंसू" to "tears crying face", "रोना" to "crying sad emotional person", "उदास" to "lonely pensive sad person",
        "खुशी" to "smiling joyful happy person", "मुस्कान" to "smiling radiant face portrait",
        "जीत" to "victory champion celebrating trophy", "सफलता" to "success achievement victory",
        "मेहनत" to "hardworking person perspiration focused", "संघर्ष" to "determination struggle resilience",

        // 13. Food & Kitchen
        "खाना" to "delicious food dining table", "भोजन" to "tasty meal plate dining",
        "रसोई" to "chef cooking kitchen pan", "चाय" to "steaming tea cup masala chai",
        "रोटी" to "hot flatbread roti cooking", "सब्जी" to "fresh vegetables cooking pan",
        "दाल" to "cooked lentil soup curry", "चावल" to "steaming cooked rice bowl",
        "मसाला" to "aromatic spices cooking", "शेफ" to "chef preparing delicious food",
        "मिठाई" to "indian sweets dessert plate",

        // 14. Space, Astronomy & ISRO
        "इसरो" to "isro space rocket launch", "गगनयान" to "gaganyaan space astronaut mission",
        "चंद्रयान" to "chandrayaan moon mission space", "अंतरिक्ष" to "outer space astronaut cosmic",
        "यात्री" to "astronaut space mission", "रॉकेट" to "rocket launchpad space",
        "उपग्रह" to "satellite orbiting earth space", "वैज्ञानिक" to "scientists laboratory research",
        "मिशन" to "space mission launchpad", "लॉन्च" to "rocket launch liftoff space",
        "ग्रह" to "planet outer space cosmos", "चंद्रमा" to "moon lunar surface spacecraft",
        "नासा" to "nasa space agency rocket", "उड़ान" to "rocket flight launch space",

        // 15. News, Media & Journalism
        "न्यूज" to "news information", "समाचार" to "news headline update",
        "ताजा खबर" to "breaking news update", "ब्रेकिंग" to "breaking update",
        "रिपोर्टर" to "journalist reporting", "पत्रकार" to "journalist reporter",
        "प्रेस" to "press conference media", "मीडिया" to "press media journalists",
        "बयान" to "official statement speech", "रिपोर्ट" to "report analysis document",
        "अपडेट" to "news update", "हेडलाइन" to "headline topic",
        "खुलासा" to "revelation discovery expose", "तैयार" to "mission ready preparation",
        "ऐतिहासिक" to "historic milestone momentous achievement",

        // 16. Defense, Armed Forces & Military
        "सेना" to "military soldiers army armed forces", "सैनिक" to "soldiers armed forces army",
        "फौज" to "army military soldiers infantry", "जवान" to "indian army soldier patrol",
        "सुरक्षा" to "defense national security patrol", "युद्ध" to "military war combat defense",
        "बॉर्डर" to "border patrol defense military", "मिसाइल" to "missile defense military rocket",
        "एयरफोर्स" to "fighter jet air force defense", "नेवी" to "navy warship naval ship ocean",

        // 17. Politics, Governance & Elections
        "सरकार" to "government parliament official building", "संसद" to "parliament house government democracy",
        "चुनाव" to "voting ballot election democracy", "वोट" to "voting booth democracy ballot",
        "मतदान" to "polling station election queue", "रैली" to "huge political crowd rally podium",
        "भाषण" to "public leader speech podium microphones", "नेता" to "political leader podium address",
        "मंत्री" to "minister government press conference", "मुख्यमंत्री" to "chief minister administration office",
        "प्रधानमंत्री" to "prime minister official address", "फैसला" to "historic government decision announcement",

        // 18. Accidents, Emergencies & Collisions
        "हादसा" to "road accident crash", "दुर्घटना" to "traffic accident collision",
        "सड़क हादसा" to "car crash road accident", "टक्कर" to "car collision accident",
        "बस हादसा" to "bus crash accident highway", "ट्रेन हादसा" to "train railway accident",
        "पलटी" to "overturned vehicle crash", "घायल" to "injured rescue emergency ambulance",
        "मौत" to "tragic incident mourning", "अस्पताल भर्ती" to "hospital emergency admission patient",

        // 19. Weather, Climate & Environmental Disasters
        "मौसम" to "weather forecast sky", "बारिश" to "heavy rainfall storm",
        "मानसून" to "monsoon rain storm clouds", "गर्मी" to "hot summer bright sun",
        "सर्दी" to "cold winter fog mist", "कोहरा" to "dense winter fog road",
        "बाढ़" to "flood waters rescue boat", "तूफान" to "cyclone storm high winds",
        "भूकंप" to "earthquake disaster destruction rescue", "आग" to "fire flames firefighters emergency rescue",
        "धमाका" to "smoke explosion emergency", "विस्फोट" to "blast explosion rescue",

        // 20. Markets, Farming, Prices & Economy
        "मंडी" to "wholesale vegetable grain market", "दाम" to "market price commodities",
        "भाव" to "market rates prices", "महंगाई" to "market grocery prices shopping",
        "सब्जी" to "fresh vegetables market stall", "प्याज" to "onions farm market",
        "टमाटर" to "red tomatoes harvest crate", "गेहूं" to "golden wheat grain harvest",
        "चावल" to "rice paddy harvest field", "सोना" to "gold bullion jewelry market",
        "चांदी" to "silver bullion metal market", "शेयर बाजार" to "stock exchange trader screens",

        // 21. Aviation, Railways & Transport
        "विमान" to "commercial airplane flight airport", "हवाई जहाज" to "airplane flying sky runway",
        "एयरपोर्ट" to "airport terminal airplane", "फ्लाइट" to "airplane boarding flight runway",
        "मेट्रो" to "modern metro train city station", "रेल" to "railway train tracks journey",

        // 22. Public Protests, Movements & Crowds
        "प्रदर्शन" to "crowd protest demonstration rally", "हड़ताल" to "strike workers protest banners",
        "धरना" to "peaceful protest sit in demonstration", "आंदोलन" to "public demonstration movement crowd",
        "नारेबाजी" to "crowd shouting slogans rally",

        // 23. Everyday Actions, Life & Human Expressions
        "कहा" to "spoke statement announcement", "फैसला" to "important decision historic moment",
        "सोचा" to "contemplative thinking person", "शुरुआत" to "beginning journey opening",
        "खत्म" to "conclusion final achievement", "काम" to "focused dedicated work action",
        "सफलता" to "triumphant victory success celebration", "मेहनत" to "hardworking dedicated effort",
        "संघर्ष" to "courageous struggle determination", "मंजिल" to "destination goal horizon",
        "सपना" to "aspirational vision dream future", "उम्मीद" to "hope bright sunrise future",
        "विश्वास" to "trust confidence strength", "खुशी" to "joyful happy smiling celebration",
        "दुख" to "somber emotional moment sorrow", "हंसी" to "joyful laughter smiling people",
        "बातचीत" to "spokesperson interview statement", "चर्चा" to "public news report press",
        "तैयारी" to "active preparation planning setup", "बदलाव" to "revolutionary change transformation",
        "मदद" to "helping hands compassion support", "साथ" to "togetherness teamwork unity",

        // 24. Places, Architecture, Urban & Rural Environments
        "घर" to "home cozy interior", "मकान" to "modern residential house architecture",
        "कमरा" to "room sunlight interior", "दरवाजा" to "open door welcoming entrance",
        "खिड़की" to "window sunlight outside view", "सड़क" to "highway road open journey",
        "गली" to "historic city alleyway street", "बाजार" to "vibrant marketplace crowd shopping",
        "दुकान" to "local retail store shopping", "पुल" to "grand bridge over river",
        "स्टेशन" to "railway station platform arrival", "हवाई अड्डा" to "airport terminal aircraft",
        "नहर" to "flowing water canal waterway", "तालाब" to "peaceful lotus pond serene water",
        "मंदिर" to "ancient sacred stone temple architecture", "मस्जिद" to "historic mosque architecture",
        "गुरुद्वारा" to "golden gurudwara peaceful sarovar", "चर्च" to "historic church cathedral architecture",

        // 25. Nature, Elements, Water & Atmosphere
        "हवा" to "gentle wind green trees breeze", "आसमान" to "vast blue open sky horizon",
        "बादल" to "dramatic fluffy white clouds sky", "धूप" to "golden sunlight shining warmth",
        "छांव" to "peaceful tree shade resting", "मिट्टी" to "fertile rich earth soil",
        "पत्थर" to "ancient rock stone mountain", "फूल" to "blooming colorful garden flowers",
        "पत्ता" to "fresh green leaf dewdrops", "पानी" to "crystal clear pure flowing water",
        "झील" to "tranquil mountain lake reflections", "समुद्र" to "blue ocean waves scenic shore",

        // 26. People, Society, Generations & Family
        "लोग" to "people crowd public diversity", "जनता" to "citizens crowd civic community",
        "नागरिक" to "proud citizens nation flag", "बच्चे" to "happy children playing outdoors",
        "युवा" to "ambitious youth young generation", "बुजुर्ग" to "elderly person dignified portrait",
        "महिला" to "empowered woman smiling confident", "पुरुष" to "focused man working dedicated",
        "दोस्त" to "close friends laughing together", "परिवार" to "loving happy family home",
        "देश" to "patriotic nation cultural heritage", "भारत" to "vibrant india national pride culture",
        "संसार" to "global planet earth community", "दुनिया" to "world diverse cultures civilization",

        // 27. Narrative Transitions, Turning Points, Mystery & Revelations
        "रहस्य" to "mystery secret ancient puzzle", "सच्चाई" to "truth reality revealed facts",
        "सच" to "authentic truth facts", "झूठ" to "deception false claims",
        "खुलासा" to "shocking revelation discovery exposed", "सबूत" to "crucial evidence documents proof",
        "संदेह" to "suspicious investigation clues", "शक" to "suspicious mystery detective clues",
        "मोड़" to "unexpected dramatic turning point", "हैरान" to "astonished stunned facial expression",
        "चौंकाने" to "shocking dramatic turn of events", "अजीब" to "mysterious enigmatic visual",
        "सवाल" to "intense questioning scrutiny press", "जवाब" to "official answer response statement",
        "कारण" to "underlying reason root cause analysis", "नतीजा" to "consequence dramatic aftermath outcome",
        "परिणाम" to "impact consequence results", "सबक" to "inspirational lesson moral realization",
        "सीख" to "meaningful life lesson guidance", "संदेश" to "inspirational message vision",
        "उद्देश्य" to "purpose focused determination goal", "मंजिल" to "destination journey summit horizon",
        "कदम" to "bold forward initiative milestone", "फैसला" to "monumental decision official announcement",
        "कोशिश" to "relentless dedicated effort struggle", "संकट" to "critical crisis urgent situation",
        "चुनौती" to "formidable challenge obstacle hurdle", "समाधान" to "innovative breakthrough solution",
        "सपना" to "aspirational vision future dream", "हकीकत" to "harsh grounded reality authentic",
        "जीत" to "triumphant victory celebration trophy", "हार" to "resilient setback determination",
        "चिंता" to "worried serious concerned facial expression", "राहत" to "profound relief calm peaceful",
        "दावा" to "bold formal claim statement", "कार्रवाई" to "official law enforcement administrative action",
        "दौरा" to "official high level diplomatic tour visit", "स्वागत" to "grand ceremonial welcome celebration",
        "समीक्षा" to "high level review meeting analysis", "निरीक्षण" to "official inspection site walkthrough",
        "भेंट" to "diplomatic summit handshake meeting", "मुलाकात" to "important bilateral leaders conference",
        "घोषणा" to "grand official policy declaration announcement", "चेतावनी" to "stern urgent warning alert advisory",
        "योजना" to "strategic blueprint development project", "परियोजना" to "large infrastructure construction project",
        "ऐतिहासिक" to "historic milestone momentous occasion"
    )

    fun analyze(segmentText: String, style: String = "", sceneIndex: Int = 0): SemanticAnalysisResult {
        val lower = segmentText.lowercase().trim()
        val styleLower = style.lowercase().trim()

        // 1. Specific Named Entities & Leaders
        val hasPremanand = lower.contains("premanand") || lower.contains("प्रेमानंद")
        val hasVirat = lower.contains("virat") || lower.contains("kohli") || lower.contains("विराट") || lower.contains("कोहली")
        val hasRssb = lower.contains("rssb") || lower.contains("राधा स्वामी") || lower.contains("राधास्वामी") ||
                lower.contains("radha soami") || lower.contains("ब्यास") || lower.contains("beas") ||
                lower.contains("गुरिंदर सिंह") || lower.contains("gurinder singh") || lower.contains("jasdeep") || lower.contains("जसदीप")
        val hasModi = lower.contains("modi") || lower.contains("मोदी") || lower.contains("narendra modi") || lower.contains("नरेंद्र मोदी")
        val hasYogi = lower.contains("yogi") || lower.contains("योगी") || lower.contains("adityanath")
        val hasGandhi = lower.contains("gandhi") || lower.contains("गांधी") || lower.contains("mahatma") || lower.contains("महात्मा")
        val hasGodse = lower.contains("godse") || lower.contains("गोडसे") || lower.contains("nathuram")
        val hasPatel = lower.contains("patel") || lower.contains("पटेल") || lower.contains("sardar") || lower.contains("सरदार")
        val hasNehru = lower.contains("nehru") || lower.contains("नेहरू") || lower.contains("jawaharlal")
        val hasBose = lower.contains("bose") || lower.contains("बोस") || lower.contains("subhash") || lower.contains("सुभाष")
        val hasBhagat = lower.contains("bhagat singh") || lower.contains("भगत सिंह")
        val hasDhoni = lower.contains("dhoni") || lower.contains("धोनी") || lower.contains("msd")

        // 2. Domain / Genre Indicators
        val hasSatsang = lower.contains("सत्संग") || lower.contains("satsang") || lower.contains("प्रवचन") ||
                lower.contains("pravachan") || lower.contains("महाराज") || lower.contains("maharaj") ||
                lower.contains("संत") || lower.contains("saint") || lower.contains("वृंदावन") || lower.contains("vrindavan") ||
                lower.contains("आश्रम") || lower.contains("ashram") || lower.contains("भक्ति") || lower.contains("bhakti")

        val isKidsStory = (lower.contains("पंचतंत्र") || lower.contains("panchatantra") ||
                lower.contains("बच्चों की कहानी") || lower.contains("बाल कहानी") ||
                lower.contains("fairytale") || lower.contains("fairy tale") ||
                styleLower.contains("cartoon") || styleLower.contains("3d animation") || styleLower.contains("pixar") ||
                (styleLower.contains("kids") && !lower.contains("news") && !lower.contains("हादसा"))) &&
                !lower.contains("news") && !lower.contains("न्यूज") && !lower.contains("हादसा") && !lower.contains("फैसला")

        // Kids Fable Characters
        val hasLion = lower.contains("शेर") || lower.contains("lion") || lower.contains("sher")
        val hasMouse = lower.contains("चूहा") || lower.contains("mouse") || lower.contains("chuha")
        val hasTortoise = lower.contains("कछुआ") || lower.contains("tortoise") || lower.contains("kachua")
        val hasRabbit = lower.contains("खरगोश") || lower.contains("rabbit") || lower.contains("hare") || lower.contains("khargosh")
        val hasFox = lower.contains("लोमड़ी") || lower.contains("fox") || lower.contains("lomdi")
        val hasCrow = lower.contains("कौवा") || lower.contains("कौआ") || lower.contains("crow") || lower.contains("kauwa")
        val hasMonkey = lower.contains("बंदर") || lower.contains("monkey") || lower.contains("bandar")
        val hasElephant = lower.contains("हाथी") || lower.contains("elephant") || lower.contains("hathi")
        val hasFairy = lower.contains("परी") || lower.contains("fairy") || lower.contains("pari") || lower.contains("जादू") || lower.contains("जादुई") || lower.contains("magic")
        val hasPrincess = lower.contains("राजकुमारी") || lower.contains("princess") || lower.contains("राजकुमार") || lower.contains("prince") || lower.contains("राजा") || lower.contains("king") || lower.contains("रानी") || lower.contains("queen")

        // 3. Police, Crime & Legal (Strictly require explicit police/crime words, never general words like जांच or सबूत)
        val isPoliceCrime = (lower.contains("पुलिस") || lower.contains("police") || lower.contains("दारोगा") ||
                lower.contains("थाना") || lower.contains("गिरफ्तार") || lower.contains("arrest") ||
                lower.contains("हथकड़ी") || lower.contains("handcuffs") || lower.contains("चोरी") ||
                lower.contains("डकैती") || (lower.contains("चोर") && !lower.contains("चोरी छिपे")) ||
                lower.contains("thief") || lower.contains("robbery") || lower.contains("murder") ||
                lower.contains("कत्ल") || lower.contains("खूनी") || lower.contains("हथियार")) &&
                !lower.contains("इसरो") && !lower.contains("space") && !lower.contains("rocket")

        // 4. Medical, Health & Hospital
        val isMedical = lower.contains("डॉक्टर") || lower.contains("doctor") || lower.contains("मरीज") || lower.contains("patient") ||
                lower.contains("hospital") || lower.contains("अस्पताल") || lower.contains("surgery") || lower.contains("ऑपरेशन") ||
                lower.contains("clinic") || lower.contains("medicine") || lower.contains("दवाई") || lower.contains("nurse") ||
                lower.contains("नर्स") || lower.contains("injection") || lower.contains("इंजेक्शन") || lower.contains("इलाज")

        // 5. Farming & Agriculture
        val isFarming = lower.contains("किसान") || lower.contains("farmer") || lower.contains("खेती") || lower.contains("farming") ||
                lower.contains("खेत") || lower.contains("field") || lower.contains("tractor") || lower.contains("ट्रैक्टर") ||
                lower.contains("crops") || lower.contains("फसल") || lower.contains("harvest") || lower.contains("agriculture")

        // 6. Education & School
        val isEducation = lower.contains("स्कूल") || lower.contains("school") || lower.contains("कॉलेज") || lower.contains("college") ||
                lower.contains("teacher") || lower.contains("शिक्षक") || lower.contains("student") || lower.contains("छात्र") ||
                lower.contains("classroom") || lower.contains("कक्षा") || lower.contains("study") || lower.contains("पढ़ाई") ||
                lower.contains("exam") || lower.contains("परीक्षा") || lower.contains("library") || lower.contains("किताब")

        // 7. Business, Money & Wealth
        val isFinance = lower.contains("पैसा") || lower.contains("money") || lower.contains("cash") || lower.contains("रुपये") ||
                lower.contains("dollar") || lower.contains("wealth") || lower.contains("धन") || lower.contains("bank") ||
                lower.contains("बैंक") || lower.contains("stock market") || lower.contains("शेयर") || lower.contains("business") ||
                lower.contains("बिजनेस") || lower.contains("corporate") || lower.contains("office") || lower.contains("दफ्तर")

        // 8. Food & Recipe
        val isFood = lower.contains("recipe") || lower.contains("खाना") || lower.contains("भोजन") ||
                lower.contains("cooking") || lower.contains("dish") || lower.contains("kitchen") ||
                lower.contains("रसोई") || lower.contains("मसाला") || lower.contains("चाय") || lower.contains("दाल") ||
                lower.contains("chef") || lower.contains("शेफ") || lower.contains("restaurant")

        // --- SPECIFIC ENTITY HANDLERS ---
        if (hasPremanand && hasVirat) {
            return SemanticAnalysisResult(
                criticalEntity = "Virat Kohli with Premanand Maharaj",
                sceneType = MediaSelectionService.SceneType.SPIRITUAL_SATSANG,
                mandatoryTokens = listOf("virat", "kohli", "premanand", "vrindavan"),
                refinedKeywords = listOf("Virat Kohli", "Premanand Maharaj", "Vrindavan"),
                pexelsQuery = "cricketer spiritual meeting ashram",
                pixabayQuery = "spiritual temple guru",
                pixabayImageType = "photo",
                wikimediaQuery = "Premanand Govind Sharan",
                aiPrompt = "Photorealistic Indian cricketer Virat Kohli folding hands respectfully before spiritual saint Premanand Ji Maharaj in Vrindavan ashram, authentic news photo, 8k"
            )
        }

        if (hasPremanand) {
            return SemanticAnalysisResult(
                criticalEntity = "Premanand Ji Maharaj",
                sceneType = MediaSelectionService.SceneType.SPIRITUAL_SATSANG,
                mandatoryTokens = listOf("premanand", "maharaj", "vrindavan", "saint"),
                refinedKeywords = listOf("Premanand Maharaj", "Vrindavan", "Satsang"),
                pexelsQuery = "hindu saint meditation ashram",
                pixabayQuery = "hindu saint temple meditation",
                pixabayImageType = "photo",
                wikimediaQuery = "Premanand Govind Sharan",
                aiPrompt = "Photorealistic serene portrait of Shri Hit Premanand Govind Sharan Ji Maharaj in Vrindavan ashram, yellow pitambari robes, peaceful divine spiritual aura, 8k"
            )
        }

        if (hasRssb) {
            return SemanticAnalysisResult(
                criticalEntity = "Radha Soami Satsang Beas",
                sceneType = MediaSelectionService.SceneType.SPIRITUAL_SATSANG,
                mandatoryTokens = listOf("rssb", "satsang", "beas", "dera", "radha soami"),
                refinedKeywords = listOf("Radha Soami Satsang Beas", "Dera Beas", "Gurinder Singh"),
                pexelsQuery = "peaceful spiritual congregation meditation",
                pixabayQuery = "spiritual congregation temple",
                pixabayImageType = "photo",
                wikimediaQuery = "Radha Soami Satsang Beas",
                aiPrompt = "Peaceful spiritual congregation at Radha Soami Satsang Beas Dera, serene atmosphere, white clothes satsangis, spiritual master discourse, golden hour, 8k"
            )
        }

        if (hasVirat) {
            return SemanticAnalysisResult(
                criticalEntity = "Virat Kohli",
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("virat", "kohli", "cricketer"),
                refinedKeywords = listOf("Virat Kohli", "Cricket", "Stadium"),
                pexelsQuery = "cricket stadium batsman match",
                pixabayQuery = "cricket stadium match",
                pixabayImageType = "photo",
                wikimediaQuery = "Virat Kohli",
                aiPrompt = "Photorealistic portrait of Indian cricketer Virat Kohli in blue jersey, stadium lights, sharp focus, authentic 8k sports photography"
            )
        }

        if (hasDhoni) {
            return SemanticAnalysisResult(
                criticalEntity = "MS Dhoni",
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("dhoni", "cricket"),
                refinedKeywords = listOf("MS Dhoni", "Cricket", "Wicketkeeper"),
                pexelsQuery = "cricket match captain stadium",
                pixabayQuery = "cricket match stadium",
                pixabayImageType = "photo",
                wikimediaQuery = "MS Dhoni",
                aiPrompt = "Photorealistic portrait of MS Dhoni leading team on cricket stadium ground, 8k"
            )
        }

        if (hasModi) {
            return SemanticAnalysisResult(
                criticalEntity = "Narendra Modi",
                sceneType = MediaSelectionService.SceneType.HISTORICAL_PERSON,
                mandatoryTokens = listOf("modi", "leader"),
                refinedKeywords = listOf("Narendra Modi", "Prime Minister", "Speech"),
                pexelsQuery = "prime minister podium speech flags",
                pixabayQuery = "prime minister speech podium",
                pixabayImageType = "photo",
                wikimediaQuery = "Narendra Modi",
                aiPrompt = "Photorealistic portrait of Prime Minister Narendra Modi delivering speech, Indian flag background, high detail 8k"
            )
        }

        if (hasGandhi) {
            return SemanticAnalysisResult(
                criticalEntity = "Mahatma Gandhi",
                sceneType = MediaSelectionService.SceneType.HISTORICAL_PERSON,
                mandatoryTokens = listOf("gandhi", "mahatma"),
                refinedKeywords = listOf("Mahatma Gandhi", "1948", "Historical"),
                pexelsQuery = "mahatma gandhi 1948 historical",
                pixabayQuery = "gandhi historical archive",
                pixabayImageType = "photo",
                wikimediaQuery = "Mahatma Gandhi",
                aiPrompt = "Historical archival vintage portrait of Mahatma Gandhi, Birla House 1948, black and white authentic realism"
            )
        }

        if (hasGodse) {
            return SemanticAnalysisResult(
                criticalEntity = "Nathuram Godse",
                sceneType = MediaSelectionService.SceneType.HISTORICAL_PERSON,
                mandatoryTokens = listOf("godse", "nathuram"),
                refinedKeywords = listOf("Nathuram Godse", "Historical", "Archive"),
                pexelsQuery = "court trial historical archive 1948",
                pixabayQuery = "court trial historical archive",
                pixabayImageType = "photo",
                wikimediaQuery = "Nathuram Godse",
                aiPrompt = "Historical courtroom trial archive 1948, vintage authentic photograph"
            )
        }

        if (hasPatel || hasNehru || hasBose || hasBhagat) {
            val leader = when {
                hasPatel -> "Sardar Vallabhbhai Patel"
                hasNehru -> "Jawaharlal Nehru"
                hasBose -> "Subhash Chandra Bose"
                else -> "Bhagat Singh"
            }
            return SemanticAnalysisResult(
                criticalEntity = leader,
                sceneType = MediaSelectionService.SceneType.HISTORICAL_PERSON,
                mandatoryTokens = listOf(leader.lowercase(), "historical"),
                refinedKeywords = listOf(leader, "Freedom Fighter", "India"),
                pexelsQuery = "$leader historical archive",
                pixabayQuery = "$leader historical archive",
                pixabayImageType = "photo",
                wikimediaQuery = leader,
                aiPrompt = "Authentic historical archival photo of $leader, vintage archive realism"
            )
        }

        // --- KIDS FABLES & STORIES ---
        if (hasLion && hasMouse) {
            return SemanticAnalysisResult(
                criticalEntity = "Lion and Mouse Fable",
                sceneType = MediaSelectionService.SceneType.KIDS_STORY,
                mandatoryTokens = listOf("lion", "mouse", "fable", "animated"),
                refinedKeywords = listOf("lion", "mouse", "fairytale"),
                pexelsQuery = "cute cartoon lion mouse animal forest",
                pixabayQuery = "cute lion mouse illustration",
                pixabayImageType = "illustration",
                wikimediaQuery = "The Lion and the Mouse",
                aiPrompt = "Enchanting 3D Pixar animated storybook illustration, adorable little mouse looking up at friendly smiling fluffy lion in colorful fairytale jungle, 8k render"
            )
        }

        if (hasTortoise && hasRabbit) {
            return SemanticAnalysisResult(
                criticalEntity = "Tortoise and Hare Fable",
                sceneType = MediaSelectionService.SceneType.KIDS_STORY,
                mandatoryTokens = listOf("tortoise", "rabbit", "fable", "race"),
                refinedKeywords = listOf("tortoise", "rabbit", "race"),
                pexelsQuery = "cute cartoon tortoise rabbit race",
                pixabayQuery = "tortoise rabbit race illustration",
                pixabayImageType = "illustration",
                wikimediaQuery = "The Tortoise and the Hare",
                aiPrompt = "Enchanting 3D Pixar animated storybook illustration, cute friendly tortoise and energetic fluffy rabbit racing in vibrant sunny green meadow, 8k render"
            )
        }

        if (hasFairy || hasPrincess) {
            val subject = if (hasPrincess) "cute princess royal fairytale castle" else "magical glowing fairy enchanted forest"
            return SemanticAnalysisResult(
                criticalEntity = if (hasPrincess) "Princess Fairytale" else "Magical Fairy Tale",
                sceneType = MediaSelectionService.SceneType.KIDS_STORY,
                mandatoryTokens = listOf("fairytale", "storybook"),
                refinedKeywords = listOf(if (hasPrincess) "princess" else "fairy", "fairytale", "castle"),
                pexelsQuery = "fairytale fantasy enchanted castle storybook",
                pixabayQuery = if (hasPrincess) "princess castle fairytale illustration" else "fairy enchanted forest illustration",
                pixabayImageType = "illustration",
                wikimediaQuery = "Fairy tale",
                aiPrompt = "Enchanting 3D Pixar animated storybook illustration, $subject, soft glowing magical aura, colorful storybook art, 8k render"
            )
        }

        if (isKidsStory || hasFox || hasCrow || hasMonkey || hasElephant) {
            val animalPart = when {
                hasFox -> "clever fox"
                hasCrow -> "thirsty crow"
                hasMonkey -> "playful monkey"
                hasElephant -> "friendly baby elephant"
                hasLion -> "friendly lion"
                else -> "cute animal characters"
            }
            val kw = extractContentTokens(lower, listOf(animalPart, "fairytale", "forest"))
            return SemanticAnalysisResult(
                criticalEntity = "Panchatantra Animal Fable",
                sceneType = MediaSelectionService.SceneType.KIDS_STORY,
                mandatoryTokens = listOf("cartoon", "storybook", "illustration"),
                refinedKeywords = kw.take(3),
                pexelsQuery = "cute 3d cartoon $animalPart colorful fairytale forest",
                pixabayQuery = "cute $animalPart storybook illustration",
                pixabayImageType = "illustration",
                wikimediaQuery = "Panchatantra",
                aiPrompt = "Enchanting 3D Pixar animated storybook illustration, $animalPart in whimsical colorful fairytale woods, soft warm cinematic lighting, 8k render"
            )
        }

        // --- SPIRITUAL & SATSANG ---
        if (hasSatsang) {
            val kw = extractContentTokens(lower, listOf("satsang", "discourse", "guru", "ashram"))
            return SemanticAnalysisResult(
                criticalEntity = "Spiritual Satsang",
                sceneType = MediaSelectionService.SceneType.SPIRITUAL_SATSANG,
                mandatoryTokens = listOf("satsang", "guru", "ashram"),
                refinedKeywords = kw.take(3),
                pexelsQuery = "peaceful spiritual guru discourse ashram meditation",
                pixabayQuery = "peaceful meditation temple ashram",
                pixabayImageType = "photo",
                wikimediaQuery = "Satsang",
                aiPrompt = "Serene spiritual discourse at peaceful ashram, spiritual guru delivering sacred satsang to devotees, divine atmosphere, soft golden sunlight, 8k photo"
            )
        }

        // --- POLICE, CRIME, LEGAL & COURT ---
        if (isPoliceCrime) {
            val actionTokens = extractContentTokens(lower)
            val specificPexelsQuery = when {
                lower.contains("arrest") || lower.contains("हथकड़ी") || lower.contains("गिरफ्तार") -> "police handcuffs suspect arrest"
                lower.contains("jail") || lower.contains("जेल") || lower.contains("कैदी") -> "prison cell inmate behind bars"
                lower.contains("judge") || lower.contains("जज") -> "courtroom judge gavel verdict"
                lower.contains("court") || lower.contains("अदालत") || lower.contains("वकील") -> "courtroom lawyer trial prosecution"
                lower.contains("forensic") -> "forensic detective crime scene evidence"
                lower.contains("थाना") || lower.contains("दारोगा") || lower.contains("पुलिस") || lower.contains("police") -> "police officer patrol car"
                actionTokens.isNotEmpty() -> actionTokens.take(3).joinToString(" ")
                else -> "investigation documentation law enforcement"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.DOCUMENT,
                mandatoryTokens = listOf("police"),
                refinedKeywords = if (actionTokens.isNotEmpty()) actionTokens.take(3) else listOf("police", "investigation"),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Police officer",
                aiPrompt = "Dramatic law enforcement investigation scene, $specificPexelsQuery, authentic details, 4k"
            )
        }

        // --- MEDICAL, HEALTH & HOSPITAL ---
        if (isMedical) {
            val actionTokens = extractContentTokens(lower)
            val specificPexelsQuery = when {
                lower.contains("surgery") || lower.contains("ऑपरेशन") -> "surgeons performing operation hospital room"
                lower.contains("injection") || lower.contains("इंजेक्शन") || lower.contains("दवाई") -> "doctor medicine patient treatment clinic"
                lower.contains("patient") || lower.contains("मरीज") -> "doctor examining patient clinic hospital"
                else -> "doctor hospital stethoscope healthcare"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("doctor", "hospital"),
                refinedKeywords = actionTokens.take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Physician",
                aiPrompt = "Authentic professional hospital setting, $specificPexelsQuery, clinical lighting, 4k"
            )
        }

        // --- FARMING & AGRICULTURE ---
        if (isFarming) {
            val specificPexelsQuery = when {
                lower.contains("tractor") || lower.contains("ट्रैक्टर") -> "farmer driving tractor agricultural field"
                lower.contains("harvest") || lower.contains("कटाई") || lower.contains("फसल") -> "golden wheat harvest crops field"
                else -> "farmer standing in agricultural field sunset"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.NATURE,
                mandatoryTokens = listOf("farmer", "crops"),
                refinedKeywords = extractContentTokens(lower).take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Agriculture",
                aiPrompt = "Authentic agriculture scene, $specificPexelsQuery, warm natural sunlight, 4k"
            )
        }

        // --- EDUCATION & SCHOOL ---
        if (isEducation) {
            val specificPexelsQuery = when {
                lower.contains("teacher") || lower.contains("शिक्षक") -> "teacher writing on chalkboard classroom"
                lower.contains("exam") || lower.contains("परीक्षा") -> "student writing exam paper classroom"
                lower.contains("library") || lower.contains("किताब") -> "student studying open books library"
                else -> "classroom students studying teacher"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("school", "student"),
                refinedKeywords = extractContentTokens(lower).take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "School",
                aiPrompt = "Realistic school classroom scene, $specificPexelsQuery, natural window lighting, 4k"
            )
        }

        // --- BUSINESS & FINANCE ---
        if (isFinance) {
            val specificPexelsQuery = when {
                lower.contains("cash") || lower.contains("पैसा") || lower.contains("रुपये") || lower.contains("dollar") -> "businessman counting cash money bills"
                lower.contains("stock") || lower.contains("शेयर") -> "stock market candlestick chart screens trader"
                lower.contains("meeting") || lower.contains("interview") -> "corporate business meeting boardroom colleagues"
                else -> "businessman modern office corporate workspace"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("business", "money"),
                refinedKeywords = extractContentTokens(lower).take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Business",
                aiPrompt = "Modern corporate business scene, $specificPexelsQuery, sleek glass office, 4k"
            )
        }

        // --- SPACE, ASTRONOMY & ISRO ---
        val hasIsro = lower.contains("isro") || lower.contains("इसरो") || lower.contains("गगनयान") || lower.contains("gaganyaan") ||
                lower.contains("चंद्रयान") || lower.contains("chandrayaan") || lower.contains("अंतरिक्ष") || lower.contains("space") ||
                lower.contains("astronaut") || lower.contains("रॉकेट") || lower.contains("rocket") || lower.contains("satellite") || lower.contains("उपग्रह")
        if (hasIsro) {
            val isGaganyaan = lower.contains("गगनयान") || lower.contains("gaganyaan") || lower.contains("यात्री") || lower.contains("astronaut")
            val isMoon = lower.contains("चंद्रयान") || lower.contains("chandrayaan") || lower.contains("moon") || lower.contains("चंद्रमा")
            val critical = when {
                isGaganyaan -> "ISRO Gaganyaan Astronaut Space Mission"
                isMoon -> "ISRO Chandrayaan Lunar Space Mission"
                lower.contains("रॉकेट") || lower.contains("rocket") || lower.contains("लॉन्च") || lower.contains("launch") -> "ISRO Rocket Launch Space Mission"
                else -> "ISRO Space Exploration Mission"
            }
            val pexelsQ = when {
                isGaganyaan -> "astronaut spacesuit rocket space mission launchpad"
                isMoon -> "lunar rover spacecraft lander moon surface"
                lower.contains("रॉकेट") || lower.contains("rocket") || lower.contains("लॉन्च") || lower.contains("launch") -> "rocket launch space center launchpad smoke"
                else -> "satellite orbiting earth space cosmic technology"
            }
            val aiDesc = when {
                isGaganyaan -> "Photorealistic Indian astronaut in Gaganyaan space suit preparing inside spacecraft cabin, launchpad visible, 8k spaceflight"
                isMoon -> "Photorealistic Chandrayaan lunar lander spacecraft on the surface of the moon, starry cosmos, 8k"
                else -> "Photorealistic ISRO heavy rocket launch with intense flame and exhaust smoke rising from Sriharikota launchpad, 8k space exploration"
            }
            return SemanticAnalysisResult(
                criticalEntity = critical,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("isro", "space", "rocket"),
                refinedKeywords = listOf("ISRO", if (isGaganyaan) "Gaganyaan" else "Space", "Rocket"),
                pexelsQuery = pexelsQ,
                pixabayQuery = pexelsQ,
                pixabayImageType = "photo",
                wikimediaQuery = if (isGaganyaan) "Gaganyaan" else "Indian Space Research Organisation",
                aiPrompt = aiDesc
            )
        }

        // --- NEWS, MEDIA & BROADCAST (Strictly for explicit TV / Broadcast studio contexts) ---
        val isNews = lower.contains("breaking news") || lower.contains("ब्रेकिंग न्यूज") ||
                lower.contains("news studio") || lower.contains("न्यूज स्टूडियो") ||
                lower.contains("news anchor") || lower.contains("समाचार बुलेटिन") ||
                lower.contains("tv broadcast") || lower.contains("live reporting")
        if (isNews) {
            val contentTokens = extractContentTokens(lower, listOf("news"))
            val specificPexelsQuery = when {
                lower.contains("प्रेस") || lower.contains("press conference") -> "press conference microphones media"
                lower.contains("रिपोर्टर") || lower.contains("reporter") -> "television news reporter live broadcast"
                lower.contains("anchor") || lower.contains("एंकर") -> "news studio anchor television broadcast"
                contentTokens.isNotEmpty() -> contentTokens.take(3).joinToString(" ")
                else -> "news studio television broadcast"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.DOCUMENT,
                mandatoryTokens = listOf("news"),
                refinedKeywords = if (contentTokens.isNotEmpty()) contentTokens.take(3) else listOf("news", "broadcast"),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Television news",
                aiPrompt = "Professional television news broadcast studio, $specificPexelsQuery, cinematic newsroom lighting, 4k"
            )
        }

        // --- DEFENSE & ARMED FORCES ---
        val isDefense = lower.contains("सेना") || lower.contains("army") || lower.contains("military") || lower.contains("सैनिक") ||
                lower.contains("फौज") || lower.contains("जवान") || lower.contains("soldier") || lower.contains("एयरफोर्स") ||
                lower.contains("air force") || lower.contains("fighter jet") || lower.contains("नेवी") || lower.contains("navy") ||
                lower.contains("warship") || lower.contains("मिसाइल") || lower.contains("missile") || lower.contains("बॉर्डर")
        if (isDefense) {
            val specificPexelsQuery = when {
                lower.contains("एयरफोर्स") || lower.contains("air force") || lower.contains("fighter jet") -> "supersonic fighter jet military aircraft sky"
                lower.contains("नेवी") || lower.contains("navy") || lower.contains("warship") -> "navy warship military fleet ocean"
                lower.contains("मिसाइल") || lower.contains("missile") -> "military missile launch defense system"
                lower.contains("बॉर्डर") || lower.contains("border") -> "soldiers patrolling international border outpost"
                else -> "armed forces military soldiers patrol uniform"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("military", "army"),
                refinedKeywords = extractContentTokens(lower, listOf("military", "defense")).take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Armed forces",
                aiPrompt = "Authentic military defense tactical scene, $specificPexelsQuery, sharp photorealistic details, 4k"
            )
        }

        // --- POLITICS & GOVERNMENT ---
        val isPolitics = lower.contains("सरकार") || lower.contains("government") || lower.contains("संसद") || lower.contains("parliament") ||
                lower.contains("चुनाव") || lower.contains("election") || lower.contains("वोट") || lower.contains("voting") ||
                lower.contains("प्रधानमंत्री") || lower.contains("prime minister") || lower.contains("मुख्यमंत्री") || lower.contains("नेता")
        if (isPolitics) {
            val specificPexelsQuery = when {
                lower.contains("संसद") || lower.contains("parliament") -> "parliament house government democracy architecture"
                lower.contains("चुनाव") || lower.contains("election") || lower.contains("वोट") -> "voting booth election democracy ballot voters"
                else -> "government official podium speech flags debate"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("government", "parliament"),
                refinedKeywords = extractContentTokens(lower, listOf("government", "politics")).take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Government",
                aiPrompt = "Authoritative government leadership press scene, $specificPexelsQuery, formal cinematic composition, 4k"
            )
        }

        // --- DISASTERS & ACCIDENTS ---
        val isDisaster = lower.contains("हादसा") || lower.contains("दुर्घटना") || lower.contains("accident") || lower.contains("crash") ||
                lower.contains("भूकंप") || lower.contains("earthquake") || lower.contains("बाढ़") || lower.contains("flood") ||
                lower.contains("तूफान") || lower.contains("storm") || lower.contains("आग") || lower.contains("fire")
        if (isDisaster) {
            val specificPexelsQuery = when {
                lower.contains("आग") || lower.contains("fire") -> "firefighters emergency rescue extinguishing building fire"
                lower.contains("बाढ़") || lower.contains("flood") -> "flood rescue boat disaster emergency relief"
                lower.contains("भूकंप") || lower.contains("earthquake") -> "earthquake rubble disaster search rescue team"
                else -> "emergency response ambulance rescue team operation"
            }
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.CINEMATIC_SYMBOLIC,
                mandatoryTokens = listOf("emergency", "rescue"),
                refinedKeywords = extractContentTokens(lower, listOf("emergency", "disaster")).take(3),
                pexelsQuery = specificPexelsQuery,
                pixabayQuery = specificPexelsQuery,
                pixabayImageType = "photo",
                wikimediaQuery = "Emergency service",
                aiPrompt = "Urgent emergency rescue response, $specificPexelsQuery, dramatic realism, 4k"
            )
        }

        // --- FOOD & RECIPE ---
        if (isFood) {
            val foodTokens = extractContentTokens(lower, listOf("food", "dish", "recipe"))
            val dishName = foodTokens.firstOrNull() ?: "delicious culinary dish"
            return SemanticAnalysisResult(
                criticalEntity = null,
                sceneType = MediaSelectionService.SceneType.FOOD,
                mandatoryTokens = listOf("food", "dish"),
                refinedKeywords = foodTokens.take(3),
                pexelsQuery = "$dishName delicious culinary food photography kitchen",
                pixabayQuery = "$dishName food cooking dish",
                pixabayImageType = "photo",
                wikimediaQuery = dishName,
                aiPrompt = "Gourmet culinary food photography, appetizing $dishName, restaurant presentation, warm appetizing lighting, 8k photorealistic"
            )
        }

        // --- GENERAL NARRATIVE TOKEN PARSING (NO FLUFF, NEVER GENERIC CANDID) ---
        val contentTokens = extractContentTokens(lower)
        val cleanKeywordQuery = if (contentTokens.isNotEmpty()) {
            contentTokens.take(3).joinToString(" ")
        } else {
            // Intelligent domain context inference directly from voiceover words
            when {
                lower.contains("isro") || lower.contains("इसरो") || lower.contains("गगनयान") || lower.contains("अंतरिक्ष") || lower.contains("space") || lower.contains("rocket") || lower.contains("रॉकेट") -> "rocket launch space mission astronaut"
                lower.contains("न्यूज") || lower.contains("समाचार") || lower.contains("news") || lower.contains("ताजा") || lower.contains("breaking") -> "breaking news broadcast television journalism"
                lower.contains("सरकार") || lower.contains("नेता") || lower.contains("संसद") || lower.contains("मंत्री") -> "government parliament speech official"
                lower.contains("सेना") || lower.contains("सैनिक") || lower.contains("फौज") || lower.contains("सुरक्षा") -> "military soldiers army armed forces"
                lower.contains("हादसा") || lower.contains("दुर्घटना") || lower.contains("आग") || lower.contains("तूफान") -> "emergency rescue operations disaster"
                lower.contains("पैसा") || lower.contains("व्यापार") || lower.contains("मार्केट") || lower.contains("कंपनी") -> "business corporate market finance"
                lower.contains("डॉक्टर") || lower.contains("अस्पताल") || lower.contains("दवा") || lower.contains("इलाज") -> "doctor hospital healthcare medical"
                else -> {
                    val fallbackTokens = lower.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                        .split(Regex("\\s+"))
                        .map { it.trim() }
                        .filter { it.length >= 2 && it !in BANNED_FLUFF_WORDS }
                    if (fallbackTokens.isNotEmpty()) {
                        fallbackTokens.take(3).joinToString(" ")
                    } else {
                        "visual storytelling narrative"
                    }
                }
            }
        }

        return SemanticAnalysisResult(
            criticalEntity = null,
            sceneType = MediaSelectionService.classifyScene(segmentText),
            mandatoryTokens = emptyList(),
            refinedKeywords = contentTokens.take(3),
            pexelsQuery = cleanKeywordQuery,
            pixabayQuery = cleanKeywordQuery,
            pixabayImageType = if (styleLower.contains("illustration") || styleLower.contains("art") || styleLower.contains("cartoon")) "illustration" else "photo",
            wikimediaQuery = contentTokens.firstOrNull() ?: "",
            aiPrompt = "Cinematic $style sequence depicting $cleanKeywordQuery, highly detailed authentic composition, 4k"
        )
    }

    /**
     * Extracts concrete content tokens from text:
     * - Maps Hindi/Devanagari vocabulary directly to English stock keywords.
     * - Filters out stopwords, scriptwriting markers, and generic mood words.
     * - Keeps only high-signal nouns and action verbs.
     */
    private fun extractContentTokens(text: String, seedList: List<String> = emptyList()): List<String> {
        val stopwords = setOf(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "of", "to", "in", "on", "at", "by", "for", "with",
            "this", "that", "it", "its", "you", "your", "my", "we", "they", "some", "any", "no", "not", "so", "can", "will", "get",
            "from", "about", "there", "their", "them", "then", "into", "over", "after", "before", "while", "here", "just",
            // Generic video & mood fluff stopwords
            "cinematic", "photorealistic", "sequence", "atmosphere", "atmospheric", "style", "scene", "visual", "view",
            "4k", "8k", "render", "moody", "ambient", "storytelling", "moment", "perspective", "vibe", "aesthetic", "look",
            // Hindi / Hinglish stopwords
            "का", "के", "की", "में", "से", "को", "पर", "और", "है", "हैं", "था", "थी", "थे", "हुआ", "हुई", "हुए", "तो", "भी", "ही",
            "एक", "दो", "यह", "वह", "इस", "उस", "अपने", "अपनी", "अपना", "बहुत", "गया", "गई", "गए", "रहा", "रही", "रहे", "कर", "किया",
            "kare", "karo", "karna", "karne", "kaise", "hota", "hote", "hoti", "hoga", "hogi", "hoge", "raha", "rahe", "rahi", "hai",
            "hain", "hoon", "tha", "the", "thi", "mera", "meri", "mere", "apna", "apne", "apni", "tera", "teri", "tere", "uska",
            "uske", "uski", "inka", "inke", "unki", "yeh", "ye", "woh", "wo", "isme", "usme", "isse", "usse", "jisse", "jisme",
            "agar", "magar", "lekin", "aur", "ya", "par", "pe", "ko", "se", "me", "mein", "ka", "ke", "ki", "bhi", "toh", "to",
            "hi", "ab", "kab", "jab", "tab", "kuch", "koi", "kisi", "sab", "sabhi"
        )

        val resultList = mutableListOf<String>()
        resultList.addAll(seedList)

        // 1. Check Hindi dictionary mappings for script tokens
        for ((hindiWord, englishEquivalent) in HINDI_TO_ENGLISH_MAP) {
            if (text.contains(hindiWord)) {
                englishEquivalent.split(Regex("\\s+")).forEach { w ->
                    if (w.length >= 2 && w !in stopwords && w !in resultList) {
                        resultList.add(w)
                    }
                }
                if (resultList.size >= 5) break
            }
        }

        // 2. Extract Latin/English content tokens
        val rawWords = text.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 && it !in stopwords && it !in BANNED_FLUFF_WORDS }

        for (word in rawWords) {
            if (word !in resultList && word.all { it in 'a'..'z' || it in '0'..'9' }) {
                resultList.add(word)
            }
            if (resultList.size >= 6) break
        }

        return resultList.distinct()
    }

    /**
     * CORE FEEDBACK LOOP:
     * Validates and refines any proposed query against the actual script segment tokens.
     * - Strips banned fluff words ("atmospheric", "cinematic", "scene", "view", "documentary", etc.).
     * - If proposed query retains clean, concrete words matching the script context, accepts them (even 1-4 concise keywords).
     * - ZERO REPETITION ENFORCEMENT: Never accepts a proposed query if it was already used in an earlier scene of this reel!
     * - If proposed query is empty, repeated, or purely generic, overrides with tokens parsed directly
     *   from the script segment narration/voiceover text.
     */
    fun refineHighPrecisionSearchQuery(
        segmentText: String,
        proposedQuery: String = "",
        style: String = "",
        sceneNumber: Int = 1,
        alreadyUsedQueries: Set<String> = emptySet(),
        topicContext: String = ""
    ): String {
        val cleanSeg = segmentText.trim()
        val segLower = cleanSeg.lowercase()
        val analysis = if (cleanSeg.isNotEmpty()) analyze(cleanSeg, style, sceneNumber - 1) else null
        val derived = analysis?.pexelsQuery?.trim() ?: ""
        val derivedWords = derived.split(Regex("\\s+"))
            .filter { it.lowercase() !in BANNED_FLUFF_WORDS && !it.equals("documentary", ignoreCase = true) }
        val derivedQuery = if (derivedWords.isNotEmpty()) derivedWords.take(4).joinToString(" ") else ""

        val cleanProposed = proposedQuery.trim()
            .replace(Regex("(?i)\\bdocumentary\\b"), " ")
            .replace(Regex("[\"\'!?,.;:\\[\\]()]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Filter proposedQuery to remove banned fluff words
        val proposedWords = cleanProposed.split(" ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it !in BANNED_FLUFF_WORDS && it.length >= 2 && it != "documentary" }

        val isCleanEnglish = cleanProposed.isNotEmpty() && cleanProposed.all { it.code <= 127 }
        if (isCleanEnglish && proposedWords.isNotEmpty()) {
            val candidate = proposedWords.take(4).joinToString(" ")
            val candLower = candidate.lowercase()

            val isGenericTag = BANNED_FLUFF_WORDS.any { candLower == it || candLower.contains(it) } ||
                    candLower.contains("documentary") ||
                    candLower in setOf("crime", "vibrant", "news", "report", "clip", "footage", "video", "photo", "image", "media", "story", "documentary")

            // Check if candidate is falsely attributing crime or police to an unrelated scene
            val hasExplicitCrime = segLower.contains("police") || segLower.contains("पुलिस") ||
                    segLower.contains("crime") || segLower.contains("arrest") || segLower.contains("गिरफ्तार") ||
                    segLower.contains("थाना") || segLower.contains("चोरी") || segLower.contains("कत्ल") || segLower.contains("murder")
            val isCrimeHallucination = (candLower.contains("crime") || candLower.contains("police") || candLower.contains("court")) && !hasExplicitCrime

            // Zero-Repetition Check across scenes in the video reel
            val isAlreadyUsedInReel = alreadyUsedQueries.isNotEmpty() && alreadyUsedQueries.any { used ->
                val u = used.lowercase().trim()
                u == candLower || (u.length > 5 && candLower == u) || (candLower.length > 6 && u.contains(candLower)) || (u.length > 6 && candLower.contains(u))
            }

            if (!isGenericTag && !isCrimeHallucination && !isAlreadyUsedInReel) {
                return candidate
            }
        }

        // 1. First priority: extract concrete content tokens directly from THIS scene's spoken words
        val contentWords = extractContentTokens(segLower)
        if (contentWords.isNotEmpty()) {
            val cand = contentWords.take(3).joinToString(" ")
            val isUsed = alreadyUsedQueries.any { it.equals(cand, ignoreCase = true) }
            if (!isUsed) {
                return cand
            }
            if (contentWords.size > 3) {
                val alt = contentWords.drop(1).take(3).joinToString(" ")
                if (!alreadyUsedQueries.any { it.equals(alt, ignoreCase = true) }) {
                    return alt
                }
            }
            val stageModifier = when ((sceneNumber - 1).coerceAtLeast(0) % 5) {
                0 -> "establishing"
                1 -> "action"
                2 -> "close up"
                3 -> "detail"
                else -> "perspective"
            }
            return "$cand $stageModifier"
        }

        // 2. Return derived query parsed from semantic entity analysis
        if (derivedQuery.isNotEmpty() && !derivedQuery.contains("documentary") && !derivedQuery.contains("narrative visual")) {
            val isUsed = alreadyUsedQueries.any { it.equals(derivedQuery, ignoreCase = true) }
            if (!isUsed) {
                return derivedQuery
            }
        }

        val cleanWords = cleanSeg.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 && it !in BANNED_FLUFF_WORDS && it != "documentary" }
        if (cleanWords.isNotEmpty()) {
            val cand = cleanWords.take(3).joinToString(" ")
            if (!alreadyUsedQueries.any { it.equals(cand, ignoreCase = true) }) {
                return cand
            }
        }

        // 3. Dynamic contextual fallback based on topic, narrative phrasing, and scene number (NEVER generic 'documentary')
        val topicLower = topicContext.lowercase().trim()
        val contextualCandidates = when {
            topicLower.contains("space") || topicLower.contains("इसरो") || topicLower.contains("rocket") -> listOf(
                "rocket launchpad space", "satellite orbiting earth space", "astronaut spacesuit cabin",
                "space control room screens", "starry cosmos galaxy", "lunar moon surface crater"
            )
            topicLower.contains("news") || topicLower.contains("crime") || topicLower.contains("court") -> listOf(
                "breaking news broadcast television", "courtroom gavel law books", "press conference microphones media",
                "police car flashing lights", "official document paperwork desk", "investigation detective files"
            )
            topicLower.contains("tech") || topicLower.contains("ai") || topicLower.contains("code") -> listOf(
                "programmer coding screen terminal", "futuristic digital chip technology", "modern technology screens",
                "data center server lights", "digital interface visual", "artificial intelligence robotic"
            )
            topicLower.contains("food") || topicLower.contains("recipe") || topicLower.contains("cooking") -> listOf(
                "chopping vegetables cutting board", "sizzling pan cooking oil", "fresh delicious recipe dish",
                "chef garnishing plate food", "aromatic spices cooking pot", "delicious meal restaurant table"
            )
            topicLower.contains("finance") || topicLower.contains("business") || topicLower.contains("market") -> listOf(
                "corporate financial strategy", "stock market financial chart", "corporate office skyscrapers",
                "handshake business deal", "cash currency money banking", "financial trader monitors"
            )
            segLower.contains("शुरुआत") || segLower.contains("पहला") || segLower.contains("begin") || segLower.contains("start") -> listOf(
                "story opening scene", "sunrise morning open road", "focused person looking forward"
            )
            segLower.contains("लेकिन") || segLower.contains("मगर") || segLower.contains("turning") -> listOf(
                "dramatic turning point", "person thinking deeply", "serious contemplation decision"
            )
            segLower.contains("सोच") || segLower.contains("विचार") || segLower.contains("thought") -> listOf(
                "contemplative thoughtful person", "person sitting by window", "thoughtful reflection expression"
            )
            segLower.contains("बात") || segLower.contains("चर्चा") || segLower.contains("speak") -> listOf(
                "public speech podium", "press statement microphone", "media interview speaking"
            )
            segLower.contains("काम") || segLower.contains("work") -> listOf(
                "focused dedicated action", "creative workspace desk", "dedicated effort craftsmanship"
            )
            segLower.contains("सफर") || segLower.contains("यात्रा") || segLower.contains("journey") -> listOf(
                "travel journey adventure road", "highway landscape open road", "scenic mountain path route"
            )
            else -> listOf(
                "cinematic storytelling scene",
                "city street crowd movement",
                "scenic landscape horizon",
                "dramatic moment atmosphere",
                "close up hands writing document",
                "modern digital technology screen",
                "open road sunrise journey"
            )
        }

        // Return first candidate in list that has not already been used in an earlier scene
        for (cand in contextualCandidates) {
            val candLower = cand.lowercase()
            val alreadyUsed = alreadyUsedQueries.any { used ->
                val u = used.lowercase().trim()
                u == candLower || (u.length > 5 && candLower == u) || (candLower.length > 6 && u.contains(candLower))
            }
            if (!alreadyUsed) {
                return cand
            }
        }

        // If all candidates in the list have been used, pick with modulo and append scene modifier
        val baseCand = contextualCandidates[(sceneNumber - 1).coerceAtLeast(0) % contextualCandidates.size]
        val stageSuffix = when ((sceneNumber - 1).coerceAtLeast(0) % 4) {
            0 -> "wide angle"
            1 -> "close up"
            2 -> "detail view"
            else -> "perspective"
        }
        return "$baseCand $stageSuffix"
    }

    /**
     * Guarantees that any query returned for stock media (Pexels, Pixabay, Unsplash, Pollinations AI)
     * is 100% in English (ASCII characters only), regardless of what language the user's script
     * was written in (Hindi, Hinglish, Marathi, Bengali, Spanish, etc.).
     */
    /**
     * Guarantees that any query returned for stock media (Pexels, Pixabay, Unsplash, Pollinations AI)
     * is 100% in English (ASCII characters only), regardless of what language the user's script
     * was written in (Hindi, Hinglish, Marathi, Bengali, Spanish, etc.), and never contains 'documentary'.
     */
    fun ensureEnglishSearchQuery(
        query: String,
        segmentContext: String = "",
        style: String = "",
        sceneNumber: Int = 1,
        alreadyUsedQueries: Set<String> = emptySet()
    ): String {
        val trimmed = query.trim()
            .replace(Regex("(?i)\\bdocumentary\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val hasNonAscii = trimmed.any { it.code > 127 }
        if (!hasNonAscii && trimmed.isNotEmpty()) {
            val words = trimmed.split(Regex("\\s+")).filter { it.lowercase() !in BANNED_FLUFF_WORDS && it.lowercase() != "documentary" }
            if (words.isNotEmpty()) {
                val cand = words.take(4).joinToString(" ")
                val isUsed = alreadyUsedQueries.any { it.equals(cand, ignoreCase = true) }
                if (cand.all { it.code <= 127 } && cand.isNotBlank() && !isUsed && !cand.contains("documentary", ignoreCase = true)) {
                    return cand
                }
            }
        }

        // Try extracting high-precision English query from segment or query
        val candidate = refineHighPrecisionSearchQuery(
            segmentText = if (segmentContext.isNotBlank()) segmentContext else trimmed,
            proposedQuery = if (!hasNonAscii) trimmed else "",
            style = style,
            sceneNumber = sceneNumber,
            alreadyUsedQueries = alreadyUsedQueries
        )
        if (candidate.isNotEmpty() && !candidate.any { it.code > 127 } && !candidate.contains("documentary", ignoreCase = true)) {
            return candidate
        }

        // Check local translation tokens
        val contentTokens = extractContentTokens((trimmed + " " + segmentContext).lowercase())
        val englishOnlyTokens = contentTokens.filter { it.all { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == ' ' } && it !in BANNED_FLUFF_WORDS && it != "documentary" }
        if (englishOnlyTokens.isNotEmpty()) {
            val cand = englishOnlyTokens.take(3).joinToString(" ")
            if (!alreadyUsedQueries.any { it.equals(cand, ignoreCase = true) }) {
                return cand
            }
        }

        // Contextual category fallback
        val fullLower = (trimmed + " " + segmentContext).lowercase()
        val catFallback = when {
            fullLower.contains("accident") || fullLower.contains("हादसा") || fullLower.contains("घायल") || fullLower.contains("bus") || fullLower.contains("बस") -> "passenger bus road incident"
            fullLower.contains("police") || fullLower.contains("पुलिस") || fullLower.contains("arrest") || fullLower.contains("गिरफ्तार") -> "police officer patrol car"
            fullLower.contains("court") || fullLower.contains("कोर्ट") || fullLower.contains("अदालत") || fullLower.contains("judge") -> "law courtroom gavel justice"
            fullLower.contains("doctor") || fullLower.contains("डॉक्टर") || fullLower.contains("hospital") || fullLower.contains("अस्पताल") -> "doctor hospital medical clinic"
            fullLower.contains("farmer") || fullLower.contains("किसान") || fullLower.contains("खेती") || fullLower.contains("crop") -> "farmer agriculture field harvest"
            fullLower.contains("school") || fullLower.contains("स्कूल") || fullLower.contains("छात्र") || fullLower.contains("student") -> "school classroom students study"
            fullLower.contains("modi") || fullLower.contains("मोदी") || fullLower.contains("minister") || fullLower.contains("नेता") -> "government official speech conference"
            fullLower.contains("cricket") || fullLower.contains("क्रिकेट") || fullLower.contains("kohli") || fullLower.contains("मैच") -> "cricket stadium match action"
            fullLower.contains("space") || fullLower.contains("इसरो") || fullLower.contains("रॉकेट") || fullLower.contains("rocket") -> "space rocket launch mission"
            fullLower.contains("food") || fullLower.contains("खाना") || fullLower.contains("रेसिपी") || fullLower.contains("recipe") -> "delicious cooking food recipe"
            fullLower.contains("car") || fullLower.contains("गाड़ी") || fullLower.contains("कार") || fullLower.contains("road") -> "modern highway driving road"
            else -> when ((sceneNumber - 1).coerceAtLeast(0) % 6) {
                0 -> "cinematic storytelling visual"
                1 -> "focused subject closeup portrait"
                2 -> "city street crowd movement"
                3 -> "scenic highway landscape horizon"
                4 -> "dramatic lighting atmosphere"
                else -> "open road sunrise journey"
            }
        }
        return catFallback
    }

    /**
     * Refines search query for Pexels API from script segment text.
     */
    fun refinePexelsQuery(segmentText: String, style: String = ""): String {
        return refineHighPrecisionSearchQuery(segmentText = segmentText, proposedQuery = "", style = style)
    }

    /**
     * Refines search query and imageType (photo / illustration) for Pixabay API.
     */
    fun refinePixabayQuery(
        segmentText: String,
        proposedQuery: String = "",
        style: String = ""
    ): Pair<String, String> {
        val query = refineHighPrecisionSearchQuery(segmentText, proposedQuery, style)
        val styleLower = style.lowercase().trim()
        val isIllustration = styleLower.contains("illustration") || styleLower.contains("cartoon") ||
                styleLower.contains("anime") || styleLower.contains("3d animation") ||
                styleLower.contains("pixar") || styleLower.contains("art") ||
                segmentText.lowercase().contains("कहानी") || segmentText.lowercase().contains("fable") ||
                segmentText.lowercase().contains("fairytale")
        return Pair(query, if (isIllustration) "illustration" else "photo")
    }

    /**
     * Refines generative prompt for Pollinations AI from script segment text.
     */
    fun refineAiPrompt(segmentText: String, style: String = ""): String {
        return analyze(segmentText, style).aiPrompt
    }

    /**
     * Extracts high-signal semantic tokens from script segment.
     */
    fun refineKeywords(segmentText: String): List<String> {
        return analyze(segmentText).refinedKeywords
    }

    /**
     * Generates a vivid, detailed, 100% English visual prompt strictly matching the
     * voiceover narration, topic context, concrete narrative action, camera angle, and lighting.
     *
     * Completely eliminates broken single-word/generic prompts like "vibrant, crime" or "crime",
     * never embeds raw Devanagari Hindi characters into the English prompt, and guarantees
     * the prompt accurately reflects what the narrator is speaking about in that scene.
     */
    fun generateRichContextualVisualPrompt(
        narrationText: String,
        style: String = "Cinematic",
        topicContext: String = "",
        sceneNumber: Int = 1,
        sceneTotal: Int = 1
    ): String {
        val cleanNarr = com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer
            .cleanScriptText(narrationText).ifBlank {
                com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer
                    .cleanNewsTopic(topicContext)
            }
        val lower = cleanNarr.lowercase().trim()
        val cleanTopic = com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer
            .cleanNewsTopic(topicContext)
        val styleClean = if (style.isNotBlank()) style.trim() else "Cinematic"

        val cameraAngles = listOf(
            "Cinematic dynamic low-angle tracking shot",
            "High-detail close-up framing focusing on key subject expressions and micro-actions",
            "Wide cinematic establishing perspective capturing the full environment",
            "Intense eye-level framing capturing authentic dramatic focus",
            "Smooth panning camera tracking moving alongside the central action",
            "Crisp medium focal shot capturing characters and immediate surroundings"
        )
        val lightingStyles = listOf(
            "volumetric dramatic natural sunlight with deep cinematic shadows",
            "high-contrast chiaroscuro lighting with crisp depth of field and specular highlights",
            "warm golden hour side-lighting with atmospheric glow and soft haze",
            "crisp professional studio illumination with ultra-sharp contour clarity",
            "moody dusk twilight lighting with sharp reflections and authentic color grading"
        )

        val chosenAngle = cameraAngles[(sceneNumber - 1).coerceAtLeast(0) % cameraAngles.size]
        val chosenLighting = lightingStyles[(sceneNumber - 1).coerceAtLeast(0) % lightingStyles.size]

        // 1. Run semantic token analysis to identify critical entities and domain
        val analysis = analyze(cleanNarr, styleClean, sceneNumber)
        val entity = analysis.criticalEntity

        // 2. Build concrete subject & action narrative in pure English (never raw Hindi)
        val concreteActionDescription: String = when {
            // Famous public figures & leaders
            entity == "Narendra Modi" || lower.contains("modi") || lower.contains("मोदी") ->
                "Prime Minister Narendra Modi in formal Indian attire addressing an official gathering with the Indian national flag visible in the background"
            entity == "Virat Kohli with Premanand Maharaj" ->
                "Indian cricketer Virat Kohli folding hands with deep reverence before spiritual saint Premanand Ji Maharaj inside a peaceful Vrindavan ashram"
            entity == "Premanand Ji Maharaj" || lower.contains("premanand") || lower.contains("प्रेमानंद") ->
                "Revered saint Shri Hit Premanand Govind Sharan Ji Maharaj in traditional yellow robes delivering a serene spiritual discourse in Vrindavan"
            entity == "Virat Kohli" || lower.contains("virat") || lower.contains("विराट") ->
                "Indian cricketer Virat Kohli focused on the pitch in team jersey under bright stadium floodlights"
            entity == "Radha Soami Satsang Beas" || lower.contains("राधा स्वामी") || lower.contains("radha soami") ->
                "Peaceful congregation of white-clad devotees gathered at Radha Soami Satsang Beas Dera listening to the spiritual master's discourse"
            lower.contains("dhoni") || lower.contains("धोनी") ->
                "Cricket legend MS Dhoni in jersey walking on the pitch with calm determination"
            lower.contains("yogi") || lower.contains("योगी") ->
                "Chief Minister Yogi Adityanath seated at an official administration governance table reviewing documents"
            lower.contains("gandhi") || lower.contains("गांधी") ->
                "Historical reconstruction of Mahatma Gandhi leading a peaceful freedom march"
            lower.contains("bhagat singh") || lower.contains("भगत सिंह") ->
                "Historical portrait of Indian revolutionary freedom fighter Bhagat Singh in vintage archival setting"

            // ISRO, Space & Astronomy
            lower.contains("isro") || lower.contains("इसरो") || lower.contains("गगनयान") || lower.contains("gaganyaan") ||
                    lower.contains("चंद्रयान") || lower.contains("chandrayaan") || lower.contains("अंतरिक्ष") || lower.contains("space") ||
                    lower.contains("रॉकेट") || lower.contains("rocket") || lower.contains("satellite") || lower.contains("उपग्रह") -> {
                when {
                    lower.contains("गगनयान") || lower.contains("gaganyaan") || lower.contains("यात्री") || lower.contains("astronaut") ->
                        "Indian astronaut in advanced white Gaganyaan spacesuit inside spacecraft cabin monitoring navigation control consoles"
                    lower.contains("चंद्रयान") || lower.contains("chandrayaan") || lower.contains("moon") || lower.contains("चंद्रमा") ->
                        "Chandrayaan lunar lander spacecraft safely deployed on the cratered lunar surface against deep starry cosmos"
                    lower.contains("रॉकेट") || lower.contains("rocket") || lower.contains("लॉन्च") || lower.contains("launch") ->
                        "ISRO heavy rocket launching into clear sky with billowing exhaust smoke and fiery orange thruster blast at Sriharikota"
                    else ->
                        "High-tech communications satellite orbiting planet Earth with solar panels deployed against the cosmic blackness"
                }
            }

            // Police, Crime, CBI, Law, Courts & Investigation
            lower.contains("पुलिस") || lower.contains("police") || lower.contains("दारोगा") || lower.contains("थाना") ||
                    lower.contains("गिरफ्तार") || lower.contains("arrest") || lower.contains("हथकड़ी") || lower.contains("handcuffs") ||
                    lower.contains("जेल") || lower.contains("jail") || lower.contains("prison") || lower.contains("सीबीआई") ||
                    lower.contains("cbi") || lower.contains("court") || lower.contains("कोर्ट") || lower.contains("अदालत") ||
                    lower.contains("न्यायाधीश") || lower.contains("judge") || lower.contains("वकील") || lower.contains("lawyer") ||
                    lower.contains("चोरी") || lower.contains("डकैती") || lower.contains("कत्ल") || lower.contains("murder") ||
                    lower.contains("सबूत") || lower.contains("evidence") || lower.contains("चार्जशीट") || lower.contains("closure report") -> {
                when {
                    lower.contains("गिरफ्तार") || lower.contains("arrest") || lower.contains("हथकड़ी") || lower.contains("handcuffs") || lower.contains("custody") ->
                        "Police officers in uniform arresting the handcuffed suspect and leading them past flashing emergency patrol vehicle lights"
                    lower.contains("जेल") || lower.contains("jail") || lower.contains("prison") || lower.contains("सलाखें") ->
                        "Inmate standing inside high-security correctional prison cell behind heavy steel bars with dramatic shadow lighting"
                    lower.contains("judge") || lower.contains("जज") || lower.contains("न्यायाधीश") || lower.contains("फैसला") || lower.contains("verdict") ->
                        "Presiding judge striking the wooden gavel on the judicial bench delivering a verdict in the formal courtroom"
                    lower.contains("court") || lower.contains("अदालत") || lower.contains("कोर्ट") || lower.contains("वकील") || lower.contains("lawyer") ->
                        "Attorneys in black robes presenting legal case arguments and evidence documents before the Supreme Court bench"
                    lower.contains("सीबीआई") || lower.contains("cbi") || lower.contains("चार्जशीट") || lower.contains("क्लोजर रिपोर्ट") || lower.contains("closure report") ->
                        "Central Bureau of Investigation (CBI) detectives in formal suits carrying official confidential case document folders"
                    lower.contains("हत्या") || lower.contains("murder") || lower.contains("कत्ल") || lower.contains("forensic") || lower.contains("सबूत") ->
                        "Forensic investigators and detectives in protective gloves inspecting crime scene evidence markers behind yellow police barrier tape"
                    else ->
                        "Law enforcement police officers patrolling the urban street with flashing red and blue siren lights"
                }
            }

            // News, TV Broadcast, Reporter
            lower.contains("रिपोर्टर") || lower.contains("reporter") || lower.contains("प्रेस") || lower.contains("press") ||
                    lower.contains("न्यूज") || lower.contains("anchor") || lower.contains("एंकर") || lower.contains("लाइव") -> {
                when {
                    lower.contains("प्रेस") || lower.contains("press conference") ->
                        "Spokesperson addressing a packed press conference with banks of news microphones and flashing camera strobes"
                    lower.contains("रिपोर्टर") || lower.contains("reporter") ->
                        "Television news reporter holding a microphone broadcasting live from an active urban news location"
                    else ->
                        "Television news anchor seated at a modern high-tech broadcasting studio desk with live news graphics screens"
                }
            }

            // Politics, Parliament, Government & Elections
            lower.contains("संसद") || lower.contains("parliament") || lower.contains("विधानसभा") ||
                    lower.contains("सरकार") || lower.contains("government") || lower.contains("चुनाव") ||
                    lower.contains("election") || lower.contains("वोट") || lower.contains("voting") -> {
                when {
                    lower.contains("संसद") || lower.contains("parliament") ->
                        "Elected leaders and parliamentarians actively engaged in debate inside the grand democratic parliament chamber"
                    lower.contains("चुनाव") || lower.contains("election") || lower.contains("वोट") || lower.contains("voting") ->
                        "Voters lining up at a democratic voting station to cast their ballots with indelible ink on finger"
                    else ->
                        "Government leaders holding an official policy summit in a grand neoclassical hall with state emblems"
                }
            }

            // Defense, Military, Army & Border
            lower.contains("सेना") || lower.contains("army") || lower.contains("सैनिक") || lower.contains("soldier") ||
                    lower.contains("फौज") || lower.contains("जवान") || lower.contains("बॉर्डर") || lower.contains("border") ||
                    lower.contains("मिसाइल") || lower.contains("missile") || lower.contains("एयरफोर्स") || lower.contains("air force") ||
                    lower.contains("नेवी") || lower.contains("navy") -> {
                when {
                    lower.contains("एयरफोर्स") || lower.contains("air force") || lower.contains("fighter jet") ->
                        "Supersonic military fighter jet roaring across the sky leaving twin contrails during air defense patrol"
                    lower.contains("नेवी") || lower.contains("navy") || lower.contains("warship") ->
                        "Naval guided-missile destroyer warship cutting through rough ocean waves during maritime fleet maneuvers"
                    lower.contains("मिसाइल") || lower.contains("missile") ->
                        "Advanced ballistic missile defense system positioned on mobile launcher platform in defense readiness"
                    else ->
                        "Disciplined military soldiers in camouflage tactical gear patrolling a mountainous international border outpost"
                }
            }

            // Medical, Hospital & Healthcare
            lower.contains("डॉक्टर") || lower.contains("doctor") || lower.contains("अस्पताल") || lower.contains("hospital") ||
                    lower.contains("मरीज") || lower.contains("patient") || lower.contains("surgery") || lower.contains("ऑपरेशन") -> {
                when {
                    lower.contains("surgery") || lower.contains("ऑपरेशन") ->
                        "Surgical team in scrubs and masks performing a precision operation under bright hospital surgical lamps"
                    else ->
                        "Compassionate doctor wearing white coat and stethoscope consulting with a patient in a modern hospital room"
                }
            }

            // Farming & Agriculture
            lower.contains("किसान") || lower.contains("farmer") || lower.contains("खेत") || lower.contains("field") ||
                    lower.contains("फसल") || lower.contains("crop") || lower.contains("harvest") || lower.contains("ट्रैक्टर") -> {
                when {
                    lower.contains("ट्रैक्टर") || lower.contains("tractor") ->
                        "Modern agricultural tractor tilling the fertile soil of vast open farmland under golden morning sun"
                    else ->
                        "Indian farmer standing proudly amidst lush golden wheat crops during sunset harvest season"
                }
            }

            // Business, Finance & Stock Market
            lower.contains("शेयर") || lower.contains("stock") || lower.contains("मार्केट") || lower.contains("market") ||
                    lower.contains("कंपनी") || lower.contains("company") || lower.contains("पैसा") || lower.contains("money") ||
                    lower.contains("बैंक") || lower.contains("bank") -> {
                when {
                    lower.contains("शेयर") || lower.contains("stock") || lower.contains("ट्रेडर") ->
                        "Financial trader analyzing dynamic candlestick charts and stock tickers across multi-monitor trading station"
                    else ->
                        "Corporate business professionals in modern glass high-rise conference room discussing economic strategy"
                }
            }

            // Accidents & Disasters
            lower.contains("हादसा") || lower.contains("accident") || lower.contains("आग") || lower.contains("fire") ||
                    lower.contains("बाढ़") || lower.contains("flood") || lower.contains("भूकंप") || lower.contains("earthquake") -> {
                when {
                    lower.contains("आग") || lower.contains("fire") ->
                        "Emergency firefighters with high-pressure hoses extinguishing a blazing building fire amidst smoke"
                    lower.contains("बाढ़") || lower.contains("flood") ->
                        "Disaster relief rescue boats navigating flooded streets helping stranded residents to safety"
                    else ->
                        "First responders and ambulance paramedics executing urgent emergency rescue operations"
                }
            }

            // Food & Recipe
            lower.contains("खाना") || lower.contains("food") || lower.contains("रेसिपी") || lower.contains("recipe") ||
                    lower.contains("रसोई") || lower.contains("kitchen") || lower.contains("मसाला") || lower.contains("dish") -> {
                "Professional chef in clean uniform garnishing an appetizing gourmet culinary dish in a gleaming restaurant kitchen"
            }

            // Kids & Fables
            (lower.contains("पंचतंत्र") || lower.contains("panchatantra") || lower.contains("fairy tale") || lower.contains("fairytale") ||
                    (lower.contains("fable") && (lower.contains("lion") || lower.contains("tortoise") || lower.contains("rabbit")))) -> {
                "Charming 3D Pixar animated characters exploring an enchanted vibrant storybook world filled with whimsical flora"
            }

            // Fallback derived directly from mapped semantic tokens
            else -> {
                val derivedTokens = extractContentTokens(lower)
                if (derivedTokens.isNotEmpty()) {
                    val joinedSubject = derivedTokens.joinToString(" ")
                    "Authentic visual sequence depicting $joinedSubject, capturing the unfolding real-world events"
                } else {
                    // Generate scene-stage varied visuals so scenes 2, 3, 4, 5... never repeat identical prompts
                    val stageVisual = when ((sceneNumber - 1).coerceAtLeast(0) % 6) {
                        0 -> "Establishing wide panoramic environment with atmospheric depth"
                        1 -> "Dynamic character actions and movements in central focus"
                        2 -> "Close-up detail framing highlighting key objects and meaningful elements"
                        3 -> "Atmospheric setting details with dynamic subject interaction"
                        4 -> "Climactic dramatic action moment capturing narrative momentum"
                        else -> "Inspiring conclusive perspective with expansive horizon"
                    }
                    if (cleanTopic.isNotBlank() && sceneNumber == 1) {
                        "$stageVisual representing $cleanTopic"
                    } else {
                        stageVisual
                    }
                }
            }
        }

        // Only anchor overall topic on scene 1, never repeat topic string across subsequent scenes!
        val topicSentence = if (cleanTopic.isNotBlank() && sceneNumber == 1 && !concreteActionDescription.contains(cleanTopic, ignoreCase = true)) {
            " Directly establishing: \"$cleanTopic\"."
        } else ""

        val safeStyle = when {
            styleClean.contains("documentary", ignoreCase = true) -> "Cinematic Editorial"
            styleClean.isBlank() -> "Cinematic"
            else -> styleClean
        }

        return "A cinematic $safeStyle sequence: $concreteActionDescription.$topicSentence " +
                "$chosenAngle. " +
                "Rendered with $chosenLighting, highly detailed authentic textures, and realistic 4K depth of field."
    }
}
