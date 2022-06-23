package com.github.mnemotechnician.uidebugger.element

import arc.scene.ui.layout.Table
import arc.util.Align
import com.github.mnemotechnician.mkui.*
import com.github.mnemotechnician.uidebugger.element.PropertyElement.Companion.modifiers
import com.github.mnemotechnician.uidebugger.util.createMutableProperty
import kotlin.reflect.KMutableProperty1

/**
 * Note: the receiver of KMutableProperty and the output of objProvider must be of the same class.
 */
typealias InputConstructor<T> = Table.(KMutableProperty1<Any, T>, objProvider: () -> Any?) -> Unit

/**
 * A UI element that allows the user to modify a property depending on its type.
 *
 * For instance, it provides an input field for numbers and strings and a toggle for booleans.
 *
 * Support for new classes can be added by adding an entry to [modifiers].
 *
 * @param objProvider provides an instance of object that this element represents. Called repeatedly.
 * @param property the property this class represents.
 * @param propertyType this Class defines which way will be used to represent the property. Must be assignable from [T].
 */
@Suppress("UNCHECKED_CAST")
class PropertyElement<T, O: Any>(
	private val objProvider: () -> O?,
	val property: KMutableProperty1<O, T>,
	propertyType: Class<T>
) : Table() {
	/**
	 * Creates a [PropertyElement] representing a field of [propertyClass],
	 * finding the said field by its name.
	 */
	constructor(objProvider: () -> O, propertyClass: Class<T>, propertyName: String)
		: this(objProvider, propertyClass.getDeclaredField(propertyName).createMutableProperty<T, O>(), propertyClass)

	init {
		addLabel(property.name).labelAlign(Align.left).pad(5f).growX()

		val constructor = (modifiers.getOrDefault(propertyType, null) ?: let {
			modifiers.entries.find { it.key.isAssignableFrom(propertyType) }
		}) as? InputConstructor<T> ?: throw IllegalArgumentException("No input modified for class $propertyType")

		addTable {
			constructor(property as KMutableProperty1<Any, T>, objProvider)
		}.pad(5f)
	}

	companion object {
		val stringModifier: InputConstructor<String?> = { prop, prov -> propertyField(prov, prop, { it }) }

		val intModifier: InputConstructor<Int?> = { prop, prov -> propertyField(prov, prop, { it.toInt() ?: 0 }) }

		val floatModifier: InputConstructor<Float?> = { prop, prov -> propertyField(prov, prop, { it.toFloat() ?: 0f }) }

		val booleanModifier: InputConstructor<Boolean?> = { prop, prov ->
			customButton({ prov()?.let { prop.get(it) ?: "null" } ?: "N / A" }) {
				val instance = prov() ?: return@customButton
				prop.set(instance, !(prop.get(instance) ?: return@customButton))
			}.checked { prov()?.let { prop.get(it) } ?: false }
		}

		val fallbackModifier: InputConstructor<Boolean> = { prop, prov ->
			addLabel({ "Unsupported: ${prov()?.let { prop.get(it) ?: "null" } ?: "N / A"}" }).scaleFont(0.6f)
		}

		/**
		 * Maps property types to lambdas that build input elements for them.
		 */
		val modifiers = mutableMapOf(
			String::class.java to stringModifier,
			Int::class.java to intModifier,
			Float::class.java to floatModifier,
			Boolean::class.java to booleanModifier,
			Any::class.java to fallbackModifier
		) as MutableMap<out Class<*>, out InputConstructor<*>>

		/**
		 * Tries to find an input constrcutor in [modifiers] that matches the specified class.
		 * If there's no such constrcutor, it returns the most applicable one.
		 */
		inline fun <reified T> inputConstructorFor(): InputConstructor<T> {
			return (modifiers.getOrDefault(T::class.java, null) ?: let {
				modifiers.entries.find { it.key.isAssignableFrom(T::class.java) }
			}) as? InputConstructor<T> ?: throw IllegalArgumentException("No input modified for class ${T::class}")
		}
	}
}
