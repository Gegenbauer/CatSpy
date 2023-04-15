package me.gegenbauer.logviewer.databinding

import me.gegenbauer.logviewer.log.GLog
import java.lang.reflect.Field
import javax.swing.JComboBox
import javax.swing.event.EventListenerList
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private const val TAG = "Reflection"

fun Any.invokeMethod(methodName: String, vararg args: Any?) {
    kotlin.runCatching {
        val method = this::class.declaredMemberFunctions.firstOrNull { it.name == methodName }
        if (method != null && method.isAccessible) {
            method.call(this, *args)
        } else {
            GLog.w(TAG, "[invokeMethod] no such method: $methodName")
        }
    }.onFailure {
        GLog.e(TAG, "[invokeMethod] failed! methodName: $methodName, args: $args", it)
    }
}

fun Any.setField(fieldName: String, value: Any?) {
    kotlin.runCatching {
        val property = this::class.declaredMemberProperties.firstOrNull { it.name == fieldName }
        if (property != null && property is KMutableProperty1) {
            property.isAccessible = true
            property.setter.call(this, value)
        } else {
            invokeMethod("set${fieldName.capitalize()}", value)
        }
    }.onFailure {
        GLog.e(TAG, "[setField] failed! $this fieldName: $fieldName, value: $value", it)
    }
}

/**
 * 从本类查询 field, 如果没有匹配到，则从父类查询，依次类推
 */
fun Any.getFieldDeeply(fieldName: String): Field {
    var clazz: Class<*>? = this::class.java
    while (clazz != null) {
        val clazzNonnull = clazz
        kotlin.runCatching {
            val field = clazzNonnull.getDeclaredField(fieldName)
            field.isAccessible = true
            return field
        }.onFailure {
            clazz = clazzNonnull.superclass
        }
    }
    throw NoSuchFieldException("no such field: $fieldName")
}

var JComboBox<*>.propertyChangeListenerList: Map<String, *>?
    get() {
        val changeSupportField = getFieldDeeply("changeSupport")
        val changeListenerMapField = changeSupportField.get(this).getFieldDeeply("map")
        val changeListenerMap = changeListenerMapField.get(changeSupportField.get(this))
        val listenerMap = changeListenerMap.getFieldDeeply("map")
        return listenerMap.get(changeListenerMap) as? Map<String, *>
    }
    set(value) {
        val changeSupportField = getFieldDeeply("changeSupport")
        val changeListenerMapField = changeSupportField.get(this).getFieldDeeply("map")
        val changeListenerMap = changeListenerMapField.get(changeSupportField.get(this))
        val listenerMap = changeListenerMap.getFieldDeeply("map")
        listenerMap.set(changeListenerMap, value)
    }

var JComboBox<*>.itemListenerList: EventListenerList?
    get() {
        val listenerListField = getFieldDeeply("listenerList")
        return listenerListField.get(this) as EventListenerList
    }
    set(value) {
        val listenerListField = getFieldDeeply("listenerList")
        listenerListField.set(this, value)
    }

var JComboBox<*>.listDataListenerList: EventListenerList?
    get() {
        val model = this.model
        val listenerListField = model.getFieldDeeply("listenerList")
        return listenerListField.get(model) as EventListenerList
    }
    set(value) {
        val model = this.model
        val listenerListField = model.getFieldDeeply("listenerList")
        listenerListField.set(model, value)
    }


fun <E> JComboBox<E>.withAllListenerDisabled(action: JComboBox<E>.() -> Unit) {
    val listDataListenerListCopy = listDataListenerList
    val itemListenerListCopy = itemListenerList
    val propertyChangeListenerListCopy = propertyChangeListenerList
    listDataListenerList = EventListenerList()
    itemListenerList = EventListenerList()
    propertyChangeListenerList = null
    action.invoke(this)
    listDataListenerList = listDataListenerListCopy
    itemListenerList = itemListenerListCopy
    propertyChangeListenerList = propertyChangeListenerListCopy
}

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }