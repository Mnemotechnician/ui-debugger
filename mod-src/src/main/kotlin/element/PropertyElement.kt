package com.github.mnemotechnician.uidebugger.element

import arc.scene.ui.layout.Table
import kotlin.reflect.KMutableProperty1

/**
 * Note: the receiver of KMutableProperty and the output of objProvider must be of the same class.
 */
typealias InputConstructor<T> = Table.(KMutableProperty1<Any, T>, objProvider: () -> Any) -> Unit

/**
 * A UI element that allows the user to modify a property depending on it's type.
 *
 * For instance, it provides an input field for numbers and strings.
 */
class PropertyElement<T, O>(
	val objProvider: () -> O,
	val property: KMutableProperty1<O, T>
) : Table() {
	// todo this

	companion object {
		val stringModifier: InputConstructor<String?> = { prop, prov -> propertyField(prov, prop, { it }, { it.orEmpty() }) }

		val intModifier: InputConstructor<Int> = { prop, prov -> propertyField(prov, prop, { it.toInt() }) }

		val floatModifier: InputConstructor<Float> = { prop, prov -> propertyField(prov, prop, { it.toFloat() }) }

		/**
		 * Maps property types to lambdas that build input elements for them.
		 */
		val modifiers = mutableMapOf(
			String::class to stringModifier,
			Int::class to intModifier,
			Float::class to floatModifier
		)

		/**
		 * Tries to find an input constrcutor in [modifiers] that matches the specified class.
		 * If there's no such constrcutor, it returns the most applicable one.
		 */
		inline fun <reified T> inputConstructorFor(): InputConstructor<T> {
			return (modifiers.getOrDefault(T::class, null) ?: let {
				modifiers.entries.find { it.key.java.isAssignableFrom(T::class.java) }
			}) as? InputConstructor<T> ?: throw IllegalArgumentException("No input modified for class ${T::class}")
		}
	}
}
