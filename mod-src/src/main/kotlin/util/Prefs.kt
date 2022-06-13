package com.github.mnemotechnician.uidebugger.util

import arc.Core
import kotlin.reflect.KProperty

/**
 * Convenient aliases for [arc.Core.settings] stuff.
 */
@Suppress("SpellCheckingInspection", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
object Prefs {
	var isElementDebugEnabled by setting(false)
	var forceElementDebug by setting(false)

	/**
	 * Creates a property delegate that returns the value of the corresponding mindustry setting.
	 */
	inline fun <reified T> setting(default: T, prefix: String = "__uidebugger__."): SettingDelegate<T> {
		return SettingDelegate(prefix, default)
	}

	class SettingDelegate<T>(val prefix: String, val default: T) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>) : T {
			return Core.settings.get("$prefix${property.name}", default) as T
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
			Core.settings.put("$prefix${property.name}", value)
		}
	}
}
