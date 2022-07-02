package com.github.mnemotechnician.uidebugger.util

import arc.Core
import kotlin.reflect.KProperty

/**
 * Convenient aliases for [arc.Core.bundle] stuff.
 */
object Bundles {
	val propertyList by bundle()
	val currentObject by bundle()
	val resetToElement by bundle()
	val showIn by bundle()
	val inThisTable by bundle()
	val inWindow by bundle()
	val classHasNoMembers by bundle()

	val showInWindow by bundle()
	val debugBounds by bundle()
	val debugHiddenElements by bundle()
	val noElement by bundle()
	val currentElement by bundle()

	val clickConfirm by bundle()
	val elementSelectTitle by bundle()

	val enabled by bundle()
	val disabled by bundle()

	val uiDebuggerTitle by bundle()
	val uiDebugger by bundle()

	/**
	 * Creates a property delegate that returns the value of the corresponding mindustry bundle string.
	 * The returned delegate is not to be re-used.
	 */
	fun bundle(prefix: String = "uidebugger.") = BundleDelegate(prefix)

	class BundleDelegate internal constructor(val prefix: String) {
		var cachedName: String? = null

		operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
			return Core.bundle[computeName(property)]
		}

		private fun computeName(property: KProperty<*>) = cachedName ?: "$prefix${property.name}".also {
			cachedName = it
		}
	}
}
