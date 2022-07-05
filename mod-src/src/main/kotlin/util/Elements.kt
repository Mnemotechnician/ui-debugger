package com.github.mnemotechnician.uidebugger.util

import arc.Core
import arc.scene.Element
import arc.scene.Group
import arc.scene.ui.Label
import arc.scene.ui.TextButton
import com.github.mnemotechnician.mkui.extensions.elements.content

/**
 * Returns the simple name of the object's class.
 * If it doesn't have one, returns the simple name of it's closest superclass that has one.
 */
fun Any.simpleName(): String = this::class.simpleName ?: let {
	var cls = this::class.java.superclass
	while (cls != null) {
		cls.superclass?.simpleName?.let {
			return it
		}
		cls = cls.superclass
	}
	return "Any"
}

fun Element?.elementToString(): String {
	if (this == null) return "<none>"

	return when (this) {
		Core.scene.root -> "<Scene root>"
		is Label -> "${simpleName()} (${content.ifEmpty { "empty" }.truncate()})"
		is TextButton -> "${simpleName()} (${content.ifEmpty { "empty" }.truncate()})"
		is Group -> "${simpleName()} (${ when (val num = children.size) {
			0 -> "empty"
			1 -> "1 child"
			else -> "$num children"
		} })"
		else -> simpleName()
	}.let {
		if (name != null && name.isNotEmpty()) "$name — $it" else it
	}
}

private fun CharSequence.truncate(target: Int = 30) = if (length - 3 > target) "${take(length - 3)}..." else this
