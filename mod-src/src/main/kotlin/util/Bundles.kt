package com.github.mnemotechnician.uidebugger.util

import arc.Core
import kotlin.reflect.KProperty

/**
 * Convenient aliases for [arc.Core.bundle] stuff.
 */
object Bundles {
	val clickConfirm by bundle()
	val elementSelectTitle by bundle()

	val enabled by bundle()
	val disabled by bundle()

	val uiDebuggerTitle by bundle()
	val uiDebugger by bundle()

	/**
	 * Creates a property delegate that returns the value of the corresponding mindustry bundle string.
	 */
	fun bundle(prefix: String = "uidebugger.") = BundleDelegate(prefix)

	class BundleDelegate internal constructor(val prefix: String) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
			return Core.bundle["$prefix${property.name}"]
		}
	}
}
