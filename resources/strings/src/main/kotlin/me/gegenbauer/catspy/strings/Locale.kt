package me.gegenbauer.catspy.strings

// load when application launched
var globalLocale: Locale = Locale.EN
    set(value) {
        field = value
        java.util.Locale.setDefault(value.locale)
    }

interface ILocale {
    val displayName: String

    val stringFile: String

    val helpText: String

    val locale: java.util.Locale
}

val supportLocales = arrayOf(Locale.CN, Locale.EN, Locale.KO)

enum class Locale: ILocale {
    CN {
        override val displayName: String
            get() = "简体中文"

        override val stringFile: String
            get() = "zh_cn.json"

        override val helpText: String
            get() = HelpText.textCn
        override val locale: java.util.Locale
            get() = java.util.Locale.CHINESE
    },
    EN {
        override val displayName: String
            get() = "English"

        override val stringFile: String
            get() = "en.json"

        override val helpText: String
            get() = HelpText.textEn
        override val locale: java.util.Locale
            get() = java.util.Locale.ENGLISH
    },
    KO {
        override val displayName: String
            get() = "한국어"

        override val stringFile: String
            get() = "ko.json"

        override val helpText: String
            get() = HelpText.textKo
        override val locale: java.util.Locale
            get() = java.util.Locale.KOREAN
    };
}