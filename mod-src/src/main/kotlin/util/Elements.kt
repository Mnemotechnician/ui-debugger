package com.github.mnemotechnician.uidebugger.util

import arc.scene.Element
import arc.scene.Group
import arc.scene.ui.TextButton
import com.github.mnemotechnician.mkui.content

/**
 * Returns the simple name of the object's class.
 * If it doesn't have one, returns the simple name of it's closest superclass that has one.
 */
fun Any.simpleName(): String = this::class.simpleName ?: let {
	var cls = this::class.java.superclass
	while (cls != null) {
		cls.superclass?.simpleName?.let { return it }
		cls = cls.superclass
	}
	return "Any"
}

fun Element?.elementToString(): String = when (this) {
	null -> "none"
	is TextButton -> "${simpleName()} ($content)"
	is Group -> "${simpleName()} (${ when (val num = children.size) {
		0 -> "empty"
		1 -> "1 child"
		else -> "$num children"
	} })"
	else -> simpleName()
}
