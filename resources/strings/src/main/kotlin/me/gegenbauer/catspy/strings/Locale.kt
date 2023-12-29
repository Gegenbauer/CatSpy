package me.gegenbauer.catspy.strings

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

private const val PROPERTY_KEY_LOCALE = "locale"

val supportLocales = arrayOf(Locale.CN, Locale.EN, Locale.KO)

// load when application launched
var globalLocale: Locale = java.util.Locale.getDefault().toLocale()
    set(value) {
        val oldLocale = field
        field = value
        if (oldLocale != value) {
            java.util.Locale.setDefault(value.locale)
            propertyChangeSupport.firePropertyChange(PROPERTY_KEY_LOCALE, oldLocale, value)
        }
    }

private val propertyChangeSupport = PropertyChangeSupport(globalLocale)

fun registerLocaleChangeListener(listener: LocaleChangeListener) {
    propertyChangeSupport.addPropertyChangeListener(listener)
}

fun unregisterLocaleChangeListener(listener: LocaleChangeListener) {
    propertyChangeSupport.removePropertyChangeListener(listener)
}

fun interface LocaleChangeListener: PropertyChangeListener {
    fun onLocaleChanged(oldLocale: Locale, newLocale: Locale)

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == PROPERTY_KEY_LOCALE) {
            onLocaleChanged(evt.oldValue as Locale, evt.newValue as Locale)
        }
    }
}

interface ILocale {
    val displayName: String

    val stringFile: String

    val helpText: String

    val locale: java.util.Locale
}

fun java.util.Locale.toLocale(): Locale {
    return supportLocales.firstOrNull { it.locale == this } ?: Locale.CN
}

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