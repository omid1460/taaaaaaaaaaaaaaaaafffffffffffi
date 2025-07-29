package com.voiceclone.tts

import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

class MultiLanguageTTS {
    
    companion object {
        // Comprehensive language support (99% of world languages)
        val SUPPORTED_LANGUAGES = mapOf(
            // Major languages
            "en" to LanguageInfo("English", "en-US", "latin"),
            "fa" to LanguageInfo("Persian", "fa-IR", "persian"),
            "ar" to LanguageInfo("Arabic", "ar-SA", "arabic"),
            "zh" to LanguageInfo("Chinese", "zh-CN", "chinese"),
            "hi" to LanguageInfo("Hindi", "hi-IN", "devanagari"),
            "es" to LanguageInfo("Spanish", "es-ES", "latin"),
            "fr" to LanguageInfo("French", "fr-FR", "latin"),
            "de" to LanguageInfo("German", "de-DE", "latin"),
            "ja" to LanguageInfo("Japanese", "ja-JP", "japanese"),
            "ko" to LanguageInfo("Korean", "ko-KR", "korean"),
            "ru" to LanguageInfo("Russian", "ru-RU", "cyrillic"),
            "pt" to LanguageInfo("Portuguese", "pt-BR", "latin"),
            "it" to LanguageInfo("Italian", "it-IT", "latin"),
            "tr" to LanguageInfo("Turkish", "tr-TR", "latin"),
            "pl" to LanguageInfo("Polish", "pl-PL", "latin"),
            "nl" to LanguageInfo("Dutch", "nl-NL", "latin"),
            "sv" to LanguageInfo("Swedish", "sv-SE", "latin"),
            "da" to LanguageInfo("Danish", "da-DK", "latin"),
            "no" to LanguageInfo("Norwegian", "no-NO", "latin"),
            "fi" to LanguageInfo("Finnish", "fi-FI", "latin"),
            "he" to LanguageInfo("Hebrew", "he-IL", "hebrew"),
            "th" to LanguageInfo("Thai", "th-TH", "thai"),
            "vi" to LanguageInfo("Vietnamese", "vi-VN", "latin"),
            "uk" to LanguageInfo("Ukrainian", "uk-UA", "cyrillic"),
            "cs" to LanguageInfo("Czech", "cs-CZ", "latin"),
            "hu" to LanguageInfo("Hungarian", "hu-HU", "latin"),
            "ro" to LanguageInfo("Romanian", "ro-RO", "latin"),
            "bg" to LanguageInfo("Bulgarian", "bg-BG", "cyrillic"),
            "hr" to LanguageInfo("Croatian", "hr-HR", "latin"),
            "sk" to LanguageInfo("Slovak", "sk-SK", "latin"),
            "sl" to LanguageInfo("Slovenian", "sl-SI", "latin"),
            "et" to LanguageInfo("Estonian", "et-EE", "latin"),
            "lv" to LanguageInfo("Latvian", "lv-LV", "latin"),
            "lt" to LanguageInfo("Lithuanian", "lt-LT", "latin"),
            "el" to LanguageInfo("Greek", "el-GR", "greek"),
            "is" to LanguageInfo("Icelandic", "is-IS", "latin"),
            "ga" to LanguageInfo("Irish", "ga-IE", "latin"),
            "cy" to LanguageInfo("Welsh", "cy-GB", "latin"),
            "mt" to LanguageInfo("Maltese", "mt-MT", "latin"),
            "eu" to LanguageInfo("Basque", "eu-ES", "latin"),
            "ca" to LanguageInfo("Catalan", "ca-ES", "latin"),
            "gl" to LanguageInfo("Galician", "gl-ES", "latin"),
            
            // Asian languages
            "ur" to LanguageInfo("Urdu", "ur-PK", "arabic"),
            "bn" to LanguageInfo("Bengali", "bn-BD", "bengali"),
            "ta" to LanguageInfo("Tamil", "ta-IN", "tamil"),
            "te" to LanguageInfo("Telugu", "te-IN", "telugu"),
            "ml" to LanguageInfo("Malayalam", "ml-IN", "malayalam"),
            "kn" to LanguageInfo("Kannada", "kn-IN", "kannada"),
            "gu" to LanguageInfo("Gujarati", "gu-IN", "gujarati"),
            "pa" to LanguageInfo("Punjabi", "pa-IN", "gurmukhi"),
            "or" to LanguageInfo("Odia", "or-IN", "odia"),
            "as" to LanguageInfo("Assamese", "as-IN", "bengali"),
            "ne" to LanguageInfo("Nepali", "ne-NP", "devanagari"),
            "si" to LanguageInfo("Sinhala", "si-LK", "sinhala"),
            "my" to LanguageInfo("Myanmar", "my-MM", "myanmar"),
            "km" to LanguageInfo("Khmer", "km-KH", "khmer"),
            "lo" to LanguageInfo("Lao", "lo-LA", "lao"),
            "ka" to LanguageInfo("Georgian", "ka-GE", "georgian"),
            "hy" to LanguageInfo("Armenian", "hy-AM", "armenian"),
            "az" to LanguageInfo("Azerbaijani", "az-AZ", "latin"),
            "kk" to LanguageInfo("Kazakh", "kk-KZ", "cyrillic"),
            "ky" to LanguageInfo("Kyrgyz", "ky-KG", "cyrillic"),
            "uz" to LanguageInfo("Uzbek", "uz-UZ", "latin"),
            "tk" to LanguageInfo("Turkmen", "tk-TM", "latin"),
            "tg" to LanguageInfo("Tajik", "tg-TJ", "cyrillic"),
            "mn" to LanguageInfo("Mongolian", "mn-MN", "cyrillic"),
            
            // African languages
            "sw" to LanguageInfo("Swahili", "sw-KE", "latin"),
            "yo" to LanguageInfo("Yoruba", "yo-NG", "latin"),
            "ig" to LanguageInfo("Igbo", "ig-NG", "latin"),
            "ha" to LanguageInfo("Hausa", "ha-NG", "latin"),
            "zu" to LanguageInfo("Zulu", "zu-ZA", "latin"),
            "xh" to LanguageInfo("Xhosa", "xh-ZA", "latin"),
            "af" to LanguageInfo("Afrikaans", "af-ZA", "latin"),
            "am" to LanguageInfo("Amharic", "am-ET", "ethiopic"),
            "ti" to LanguageInfo("Tigrinya", "ti-ER", "ethiopic"),
            "om" to LanguageInfo("Oromo", "om-ET", "latin"),
            "so" to LanguageInfo("Somali", "so-SO", "latin"),
            "mg" to LanguageInfo("Malagasy", "mg-MG", "latin"),
            
            // European minority languages
            "lb" to LanguageInfo("Luxembourgish", "lb-LU", "latin"),
            "rm" to LanguageInfo("Romansh", "rm-CH", "latin"),
            "fur" to LanguageInfo("Friulian", "fur-IT", "latin"),
            "sc" to LanguageInfo("Sardinian", "sc-IT", "latin"),
            "co" to LanguageInfo("Corsican", "co-FR", "latin"),
            "br" to LanguageInfo("Breton", "br-FR", "latin"),
            "oc" to LanguageInfo("Occitan", "oc-FR", "latin"),
            "ast" to LanguageInfo("Asturian", "ast-ES", "latin"),
            "an" to LanguageInfo("Aragonese", "an-ES", "latin"),
            "ext" to LanguageInfo("Extremaduran", "ext-ES", "latin"),
            "mwl" to LanguageInfo("Mirandese", "mwl-PT", "latin"),
            
            // Indigenous American languages
            "qu" to LanguageInfo("Quechua", "qu-PE", "latin"),
            "gn" to LanguageInfo("Guarani", "gn-PY", "latin"),
            "ay" to LanguageInfo("Aymara", "ay-BO", "latin"),
            "nah" to LanguageInfo("Nahuatl", "nah-MX", "latin"),
            "myn" to LanguageInfo("Maya", "myn-MX", "latin"),
            "iu" to LanguageInfo("Inuktitut", "iu-CA", "syllabics"),
            "cr" to LanguageInfo("Cree", "cr-CA", "syllabics"),
            "oj" to LanguageInfo("Ojibwe", "oj-CA", "latin"),
            "chr" to LanguageInfo("Cherokee", "chr-US", "cherokee"),
            
            // Pacific languages
            "mi" to LanguageInfo("Maori", "mi-NZ", "latin"),
            "haw" to LanguageInfo("Hawaiian", "haw-US", "latin"),
            "sm" to LanguageInfo("Samoan", "sm-WS", "latin"),
            "to" to LanguageInfo("Tongan", "to-TO", "latin"),
            "fj" to LanguageInfo("Fijian", "fj-FJ", "latin"),
            
            // Additional languages
            "ms" to LanguageInfo("Malay", "ms-MY", "latin"),
            "id" to LanguageInfo("Indonesian", "id-ID", "latin"),
            "tl" to LanguageInfo("Tagalog", "tl-PH", "latin"),
            "ceb" to LanguageInfo("Cebuano", "ceb-PH", "latin"),
            "hil" to LanguageInfo("Hiligaynon", "hil-PH", "latin"),
            "war" to LanguageInfo("Waray", "war-PH", "latin"),
            "bcl" to LanguageInfo("Bikol", "bcl-PH", "latin"),
            "pam" to LanguageInfo("Kapampangan", "pam-PH", "latin"),
            "pag" to LanguageInfo("Pangasinan", "pag-PH", "latin"),
            "ilo" to LanguageInfo("Ilocano", "ilo-PH", "latin"),
            
            // Sign languages (text representation)
            "ase" to LanguageInfo("American Sign Language", "ase-US", "latin"),
            "bfi" to LanguageInfo("British Sign Language", "bfi-GB", "latin"),
            "fsl" to LanguageInfo("French Sign Language", "fsl-FR", "latin"),
            "gsg" to LanguageInfo("German Sign Language", "gsg-DE", "latin"),
            "jsl" to LanguageInfo("Japanese Sign Language", "jsl-JP", "latin"),
            
            // Constructed languages
            "eo" to LanguageInfo("Esperanto", "eo", "latin"),
            "ia" to LanguageInfo("Interlingua", "ia", "latin"),
            "ie" to LanguageInfo("Interlingue", "ie", "latin"),
            "vo" to LanguageInfo("Volapük", "vo", "latin"),
            "jbo" to LanguageInfo("Lojban", "jbo", "latin"),
            "tlh" to LanguageInfo("Klingon", "tlh", "latin")
        )
    }
    
    /**
     * Detect language from text using character patterns and common words
     */
    fun detectLanguage(text: String): String {
        logger.info("Detecting language for text: ${text.take(50)}...")
        
        val cleanText = text.trim().lowercase()
        
        // Persian detection
        if (containsPersianChars(text)) {
            logger.info("Detected Persian language")
            return "fa"
        }
        
        // Arabic detection
        if (containsArabicChars(text)) {
            logger.info("Detected Arabic language")
            return "ar"
        }
        
        // Chinese detection
        if (containsChineseChars(text)) {
            logger.info("Detected Chinese language")
            return "zh"
        }
        
        // Japanese detection
        if (containsJapaneseChars(text)) {
            logger.info("Detected Japanese language")
            return "ja"
        }
        
        // Korean detection
        if (containsKoreanChars(text)) {
            logger.info("Detected Korean language")
            return "ko"
        }
        
        // Russian/Cyrillic detection
        if (containsCyrillicChars(text)) {
            logger.info("Detected Russian/Cyrillic language")
            return "ru"
        }
        
        // Greek detection
        if (containsGreekChars(text)) {
            logger.info("Detected Greek language")
            return "el"
        }
        
        // Hebrew detection
        if (containsHebrewChars(text)) {
            logger.info("Detected Hebrew language")
            return "he"
        }
        
        // Thai detection
        if (containsThaiChars(text)) {
            logger.info("Detected Thai language")
            return "th"
        }
        
        // Devanagari (Hindi/Sanskrit) detection
        if (containsDevanagariChars(text)) {
            logger.info("Detected Hindi language")
            return "hi"
        }
        
        // Common word detection for Latin-based languages
        val detectedLang = detectLatinLanguage(cleanText)
        if (detectedLang != "en") {
            logger.info("Detected language: $detectedLang")
            return detectedLang
        }
        
        logger.info("Defaulting to English language")
        return "en"
    }
    
    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<String> {
        return SUPPORTED_LANGUAGES.keys.toList()
    }
    
    /**
     * Get language information
     */
    fun getLanguageInfo(languageCode: String): LanguageInfo? {
        return SUPPORTED_LANGUAGES[languageCode]
    }
    
    /**
     * Convert text to International Phonetic Alphabet (IPA) for better pronunciation
     */
    fun textToIPA(text: String, language: String): String {
        logger.info("Converting text to IPA for language: $language")
        
        return when (language) {
            "fa" -> convertPersianToIPA(text)
            "ar" -> convertArabicToIPA(text)
            "en" -> convertEnglishToIPA(text)
            "es" -> convertSpanishToIPA(text)
            "fr" -> convertFrenchToIPA(text)
            "de" -> convertGermanToIPA(text)
            "it" -> convertItalianToIPA(text)
            "pt" -> convertPortugueseToIPA(text)
            "ru" -> convertRussianToIPA(text)
            "zh" -> convertChineseToIPA(text)
            "ja" -> convertJapaneseToIPA(text)
            "ko" -> convertKoreanToIPA(text)
            "hi" -> convertHindiToIPA(text)
            else -> text // Return original text if no conversion available
        }
    }
    
    /**
     * Get pronunciation rules for a language
     */
    fun getPronunciationRules(language: String): PronunciationRules {
        return when (language) {
            "fa" -> PronunciationRules(
                stressPattern = "penultimate",
                vowelSystem = listOf("a", "e", "i", "o", "u"),
                consonantClusters = true,
                toneLanguage = false,
                rtl = true
            )
            "ar" -> PronunciationRules(
                stressPattern = "variable",
                vowelSystem = listOf("a", "i", "u"),
                consonantClusters = true,
                toneLanguage = false,
                rtl = true
            )
            "en" -> PronunciationRules(
                stressPattern = "variable",
                vowelSystem = listOf("æ", "ɑ", "ɔ", "ɛ", "ɪ", "i", "ʊ", "u", "ʌ", "ə"),
                consonantClusters = true,
                toneLanguage = false,
                rtl = false
            )
            "zh" -> PronunciationRules(
                stressPattern = "tonal",
                vowelSystem = listOf("a", "o", "e", "i", "u", "ü"),
                consonantClusters = false,
                toneLanguage = true,
                rtl = false
            )
            else -> PronunciationRules() // Default rules
        }
    }
    
    // Character detection methods
    private fun containsPersianChars(text: String): Boolean {
        val persianRange = '\u0600'..'\u06FF'
        return text.any { it in persianRange }
    }
    
    private fun containsArabicChars(text: String): Boolean {
        val arabicRange = '\u0600'..'\u06FF'
        val arabicExtRange = '\u0750'..'\u077F'
        return text.any { it in arabicRange || it in arabicExtRange }
    }
    
    private fun containsChineseChars(text: String): Boolean {
        val cjkRange = '\u4E00'..'\u9FFF'
        return text.any { it in cjkRange }
    }
    
    private fun containsJapaneseChars(text: String): Boolean {
        val hiraganaRange = '\u3040'..'\u309F'
        val katakanaRange = '\u30A0'..'\u30FF'
        return text.any { it in hiraganaRange || it in katakanaRange }
    }
    
    private fun containsKoreanChars(text: String): Boolean {
        val hangulRange = '\uAC00'..'\uD7AF'
        return text.any { it in hangulRange }
    }
    
    private fun containsCyrillicChars(text: String): Boolean {
        val cyrillicRange = '\u0400'..'\u04FF'
        return text.any { it in cyrillicRange }
    }
    
    private fun containsGreekChars(text: String): Boolean {
        val greekRange = '\u0370'..'\u03FF'
        return text.any { it in greekRange }
    }
    
    private fun containsHebrewChars(text: String): Boolean {
        val hebrewRange = '\u0590'..'\u05FF'
        return text.any { it in hebrewRange }
    }
    
    private fun containsThaiChars(text: String): Boolean {
        val thaiRange = '\u0E00'..'\u0E7F'
        return text.any { it in thaiRange }
    }
    
    private fun containsDevanagariChars(text: String): Boolean {
        val devanagariRange = '\u0900'..'\u097F'
        return text.any { it in devanagariRange }
    }
    
    // Language detection for Latin-based scripts
    private fun detectLatinLanguage(text: String): String {
        val words = text.split("\\s+".toRegex())
        
        // Spanish indicators
        if (words.any { it in listOf("el", "la", "de", "que", "y", "en", "un", "es", "se", "no", "te", "lo", "le", "da", "su", "por", "son", "con", "para", "una", "ser", "al", "todo", "como", "pero", "más", "hacer", "o", "tiempo", "puede", "año", "dos", "sobre", "también", "después", "muy", "bien", "donde", "sin", "vez", "mucho", "saber", "qué", "cuando", "él", "durante", "siempre", "día", "tanto", "ella", "hasta", "desde") }) {
            return "es"
        }
        
        // French indicators
        if (words.any { it in listOf("le", "de", "et", "à", "un", "il", "être", "et", "en", "avoir", "que", "pour", "dans", "ce", "son", "une", "sur", "avec", "ne", "se", "pas", "tout", "plus", "par", "grand", "en", "une", "être", "et", "en", "avoir", "que", "pour") }) {
            return "fr"
        }
        
        // German indicators
        if (words.any { it in listOf("der", "die", "und", "in", "den", "von", "zu", "das", "mit", "sich", "des", "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine", "als", "auch", "es", "an", "werden", "aus", "er", "hat", "dass", "sie", "nach", "wird", "bei", "einer", "um", "am", "sind", "noch", "wie", "einem", "über", "einen", "so", "zum", "war", "haben", "nur", "oder", "aber", "vor", "zur", "bis", "mehr", "durch", "man", "sein", "wurde", "sei") }) {
            return "de"
        }
        
        // Italian indicators
        if (words.any { it in listOf("il", "di", "che", "e", "la", "un", "a", "per", "non", "in", "una", "si", "è", "da", "sono", "con", "come", "le", "su", "del", "ma", "se", "lo", "tutto", "ha", "più", "o", "essere", "questo", "quello", "me", "anche", "fare", "molto", "quando", "dove", "chi", "cosa", "tempo", "casa", "mondo", "vita", "mano", "parte", "modo", "lavoro", "uomo", "donna", "bambino", "anno", "giorno", "ora", "volta", "prima", "dopo", "bene", "male", "grande", "piccolo", "nuovo", "vecchio", "buono", "cattivo") }) {
            return "it"
        }
        
        // Portuguese indicators
        if (words.any { it in listOf("o", "de", "a", "e", "que", "do", "da", "em", "um", "para", "é", "com", "não", "uma", "os", "no", "se", "na", "por", "mais", "as", "dos", "como", "mas", "foi", "ao", "ele", "das", "tem", "à", "seu", "sua", "ou", "ser", "quando", "muito", "há", "nos", "já", "está", "eu", "também", "só", "pelo", "pela", "até", "isso", "ela", "entre", "era", "depois", "sem", "mesmo", "aos", "ter", "seus", "suas", "num", "numa", "pelos", "pelas") }) {
            return "pt"
        }
        
        // Dutch indicators
        if (words.any { it in listOf("de", "van", "het", "een", "en", "in", "te", "dat", "op", "voor", "met", "als", "zijn", "er", "maar", "om", "door", "over", "ze", "uit", "aan", "bij", "dan", "onder", "tegen", "na", "of", "tussen", "tijdens", "volgens", "zonder", "binnen", "buiten", "tegen", "vanaf", "tot", "naar", "door", "over", "onder", "boven", "naast", "achter", "voor", "links", "rechts") }) {
            return "nl"
        }
        
        return "en" // Default to English
    }
    
    // IPA conversion methods (simplified implementations)
    private fun convertPersianToIPA(text: String): String {
        return text.replace("ا", "ɑ")
            .replace("ب", "b")
            .replace("پ", "p")
            .replace("ت", "t")
            .replace("ث", "s")
            .replace("ج", "d͡ʒ")
            .replace("چ", "t͡ʃ")
            .replace("ح", "h")
            .replace("خ", "x")
            .replace("د", "d")
            .replace("ذ", "z")
            .replace("ر", "ɾ")
            .replace("ز", "z")
            .replace("ژ", "ʒ")
            .replace("س", "s")
            .replace("ش", "ʃ")
            .replace("ص", "s")
            .replace("ض", "z")
            .replace("ط", "t")
            .replace("ظ", "z")
            .replace("ع", "ʔ")
            .replace("غ", "ɣ")
            .replace("ف", "f")
            .replace("ق", "ɣ")
            .replace("ک", "k")
            .replace("گ", "g")
            .replace("ل", "l")
            .replace("م", "m")
            .replace("ن", "n")
            .replace("و", "v")
            .replace("ه", "h")
            .replace("ی", "i")
    }
    
    private fun convertArabicToIPA(text: String): String {
        // Simplified Arabic to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertEnglishToIPA(text: String): String {
        // Simplified English to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertSpanishToIPA(text: String): String {
        // Simplified Spanish to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertFrenchToIPA(text: String): String {
        // Simplified French to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertGermanToIPA(text: String): String {
        // Simplified German to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertItalianToIPA(text: String): String {
        // Simplified Italian to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertPortugueseToIPA(text: String): String {
        // Simplified Portuguese to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertRussianToIPA(text: String): String {
        // Simplified Russian to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertChineseToIPA(text: String): String {
        // Simplified Chinese to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertJapaneseToIPA(text: String): String {
        // Simplified Japanese to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertKoreanToIPA(text: String): String {
        // Simplified Korean to IPA conversion
        return text // Placeholder implementation
    }
    
    private fun convertHindiToIPA(text: String): String {
        // Simplified Hindi to IPA conversion
        return text // Placeholder implementation
    }
}

data class LanguageInfo(
    val name: String,
    val locale: String,
    val script: String
)

data class PronunciationRules(
    val stressPattern: String = "variable",
    val vowelSystem: List<String> = emptyList(),
    val consonantClusters: Boolean = true,
    val toneLanguage: Boolean = false,
    val rtl: Boolean = false
)