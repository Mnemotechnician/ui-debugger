package com.github.mnemotechnician.uidebugger.util

import com.github.mnemotechnician.mkui.delegates.bundle

/**
 * Convenient aliases for [arc.Core.bundle] stuff.
 */
object Bundles {
	val propertyList by udbundle()
	val currentObject by udbundle()
	val resetToElement by udbundle()
	val showIn by udbundle()
	val inThisTable by udbundle()
	val inWindow by udbundle()
	val classHasNoMembers by udbundle()
	val change by udbundle()
	val constant by udbundle()

	val showInWindow by udbundle()
	val noElement by udbundle()
	val currentElement by udbundle()

	val debugBounds by udbundle()
	val debugHiddenElements by udbundle()
	val debugCells by udbundle()
	val boundsOpacity by udbundle()
	val boundsThickness by udbundle()

	val clickConfirm by udbundle()
	val elementSelectTitle by udbundle()

	val enabled by udbundle()
	val disabled by udbundle()

	val uiDebuggerTitle by udbundle()
	val uiDebugger by udbundle()

	private fun udbundle() = bundle("uidebugger.")
}
