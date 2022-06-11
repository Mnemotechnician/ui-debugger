package com.github.mnemotechnician.uidebugger.util

import arc.Core
import kotlin.reflect.KProperty

/**
 * Convenient aliases for [arc.Core.settings] stuff.
 */
@Suppress("SpellCheckingInspection")
object Prefs {
	var isElementDebugEnabled by setting()
	var forceElementDebug by setting()

	/**
	 * Creates a property delegate that returns the value of the corresponding mindustry setting.
	 */
	fun setting(prefix: String = "__uidebugger__.") = SettingDelegate(prefix)

	class SettingDelegate internal constructor(val prefix: String) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>) : Boolean {
			return Core.settings.getBool("$prefix${property.name}")
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
			Core.settings.put("$prefix${property.name}", value)
		}
	}
}
