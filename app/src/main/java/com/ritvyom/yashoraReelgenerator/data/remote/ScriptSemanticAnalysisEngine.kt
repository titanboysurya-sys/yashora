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
        "real life candid portrait", "candid people", "candid shot", "real life candid people"
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

        // 2. Police, Crime, Legal & Law
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
        "लैपटॉप" to "laptop keyboard typing", "कंप्यूटर" to "computer desktop screen",
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

        // 17. Politics & Government
        "सरकार" to "government parliament official building", "संसद" to "parliament house government democracy",
        "चुनाव" to "voting ballot election democracy", "वोट" to "voting booth democracy ballot",
        "नेता" to "political leader podium address", "मंत्री" to "minister government press conference",
        "फैसला" to "historic government decision announcement",

        // 18. Disasters & Weather
        "हादसा" to "accident emergency rescue disaster", "दुर्घटना" to "accident emergency ambulance rescue",
        "भूकंप" to "earthquake disaster destruction rescue", "बाढ़" to "flood disaster rescue operations",
        "तूफान" to "storm hurricane cyclone disaster", "आग" to "fire flames firefighters emergency rescue"
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

        val isKidsStory = lower.contains("कहानी") || lower.contains("कहानियां") || lower.contains("कहानियाँ") ||
                lower.contains("kahani") || lower.contains("story") || lower.contains("fable") ||
                lower.contains("fairytale") || lower.contains("fairy tale") || lower.contains("kids story") ||
                lower.contains("children story") || lower.contains("पंचतंत्र") || lower.contains("panchatantra") ||
                lower.contains("बच्चों") || lower.contains("बच्चे") || lower.contains("बाल कहानी") ||
                styleLower.contains("cartoon") || styleLower.contains("3d animation") || styleLower.contains("pixar") ||
                styleLower.contains("storytelling") || styleLower.contains("kids")

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
                aiPrompt = "Historical documentary archival portrait of Mahatma Gandhi, Birla House 1948, black and white authentic realism"
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
                aiPrompt = "Historical courtroom trial archive 1948, vintage documentary photograph"
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
                pexelsQuery = "$leader historical documentary",
                pixabayQuery = "$leader historical archive",
                pixabayImageType = "photo",
                wikimediaQuery = leader,
                aiPrompt = "Authentic historical documentary photo of $leader, vintage archive realism"
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
                aiPrompt = "Scene capturing \"${segmentText.take(120)}\" in $style style, realistic details, 4k"
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
                aiPrompt = "Authentic military defense documentary scene, $specificPexelsQuery, sharp photorealistic details, 4k"
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
            aiPrompt = "Scene in $style style capturing: \"${segmentText.take(120)}\", detailed realistic composition, 4k"
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
     * - Strips banned fluff words ("atmospheric", "cinematic", "scene", "view", etc.).
     * - If proposed query retains clean, concrete words matching the script context, accepts them (even 1-4 concise keywords).
     * - If proposed query is empty or purely generic, overrides with tokens parsed directly
     *   from the script segment narration/voiceover text.
     */
    fun refineHighPrecisionSearchQuery(
        segmentText: String,
        proposedQuery: String = "",
        style: String = ""
    ): String {
        val cleanProposed = proposedQuery.trim()
            .replace(Regex("[\"\'!?,.;:\\[\\]()]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Filter proposedQuery to remove banned fluff words
        val proposedWords = cleanProposed.split(" ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it !in BANNED_FLUFF_WORDS && it.length >= 2 }

        // If proposedQuery is valid and has at least 1 concrete word, and does not contain non-English characters
        val isCleanEnglish = cleanProposed.isNotEmpty() && cleanProposed.all { it.code <= 127 }
        if (isCleanEnglish && proposedWords.isNotEmpty()) {
            val candidate = proposedWords.take(4).joinToString(" ")
            // Ensure candidate doesn't match banned phrases, generic filler, or candid tags
            if (candidate !in BANNED_FLUFF_WORDS &&
                !candidate.contains("atmospheric") &&
                !candidate.contains("cinematic") &&
                !candidate.contains("candid") &&
                !candidate.contains("real life")
            ) {
                return candidate
            }
        }

        // FEEDBACK LOOP TRIGGER: Parse script segment directly to derive high-precision query
        val analysis = analyze(segmentText, style)
        val derived = analysis.pexelsQuery.trim()
        val derivedWords = derived.split(Regex("\\s+"))
            .filter { it.lowercase() !in BANNED_FLUFF_WORDS }

        return if (derivedWords.isNotEmpty()) {
            derivedWords.take(4).joinToString(" ")
        } else {
            val contentWords = extractContentTokens(segmentText.lowercase())
            if (contentWords.isNotEmpty()) {
                contentWords.take(3).joinToString(" ")
            } else {
                val cleanWords = segmentText.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                    .split(Regex("\\s+"))
                    .map { it.trim().lowercase() }
                    .filter { it.length >= 2 && it !in BANNED_FLUFF_WORDS }
                if (cleanWords.isNotEmpty()) {
                    cleanWords.take(3).joinToString(" ")
                } else {
                    "vibrant scenic visual"
                }
            }
        }
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
}
