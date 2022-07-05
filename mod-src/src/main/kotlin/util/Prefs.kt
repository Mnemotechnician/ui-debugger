package com.github.mnemotechnician.uidebugger.util

import com.github.mnemotechnician.mkui.delegates.setting

/**
 * Convenient aliases for [arc.Core.settings] stuff.
 */
object Prefs {
	var isElementDebugEnabled by udsetting(false)
	var isCellDebugEnabled by udsetting(false)
		
	var forceElementDebug by udsetting(false)
	var boundsThickness by udsetting(1f)
	var boundsOpacity by udsetting(100f / 256)
	
	inline fun <reified T> udsetting(default: T) = setting(default, "uidebugger")
}
