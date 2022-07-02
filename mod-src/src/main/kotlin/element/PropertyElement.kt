package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Align
import com.github.mnemotechnician.mkui.*
import com.github.mnemotechnician.uidebugger.element.PropertyElement.Companion.modifiers
import com.github.mnemotechnician.uidebugger.util.createMutableProperty
import mindustry.ui.Styles
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
	private val property: KMutableProperty1<O, T>,
	propertyType: Class<out T>
) : Table() {
	init {
		addTable {
			left()
			addLabel(property.name).labelAlign(Align.left).row()
			addLabel(propertyType.toString().substringAfterLast('.')).labelAlign(Align.left).color(Color.gray)
		}.pad(5f)

		val constructor = (
			modifiers.getOrDefault(propertyType, null)
			?: modifiers.entries.find { it.key.isAssignableFrom(propertyType) }?.value
			?: fallbackModifier
		) as InputConstructor<T>

		addTable {
			right()
			constructor(property as KMutableProperty1<Any, T>, objProvider)
		}.growX().pad(5f)
	}

	/**
	 * Creates a [PropertyElement] representing a field of [propertyClass],
	 * finding the said field by its name.
	 */
	constructor(objProvider: () -> O, propertyClass: Class<T>, propertyName: String)
		: this(objProvider, propertyClass.getDeclaredField(propertyName).createMutableProperty<T, O>(), propertyClass)

	companion object {
		// lots of ctrl+c ctrl+v...
		val stringModifier: InputConstructor<String?> = { prop, prov -> propertyField(prov, prop, { it }) }

		val intModifier: InputConstructor<Int?> = { prop, prov -> propertyField(prov, prop, { it.toInt() }) }

		val longModifier: InputConstructor<Long?> = { prop, prov -> propertyField(prov, prop, { it.toLong() }) }

		val floatModifier: InputConstructor<Float?> = { prop, prov -> propertyField(prov, prop, { it.toFloat() }) }

		val doubleModifier: InputConstructor<Double?> = { prop, prov -> propertyField(prov, prop, { it.toDouble() }) }

		val booleanModifier: InputConstructor<Boolean?> = { prop, prov ->
			textButton({ prov()?.let { prop.get(it).toString() } ?: "N / A" }, Styles.togglet) {
				val instance = prov() ?: return@textButton
				prop.set(instance, !(prop.get(instance) ?: return@textButton))
			}.checked { prov()?.let { prop.get(it) } ?: false }
		}
		
		val colorModifier: InputConstructor<Color?> = { prop, prov ->
			propertyField(prov, prop, {
				val obj = prov()!! // only gets called when prov() returns a non-null value
				Color.valueOf(prop.get(obj) ?: Color(), it)
			}).row()
		}

		val fallbackModifier: InputConstructor<Any?> = { prop, prov ->
			addLabel("Unsupported: ")
			addLabel({ prov()?.let { prop.get(it).toString().substringBefore('\n') } ?: "N / A" }, wrap = false).scaleFont(0.6f)
		}

		/**
		 * Maps property types to lambdas that build input elements for them.
		 */
		val modifiers = mutableMapOf(
			String::class.java to stringModifier,
			Int::class.java to intModifier,
			Long::class.java to longModifier,
			Float::class.java to floatModifier,
			Double::class.java to doubleModifier,
			Boolean::class.java to booleanModifier,
			Color::class.java to colorModifier,
			Any::class.java to fallbackModifier
		) as MutableMap<out Class<*>, out InputConstructor<*>>

		/**
		 * Tries to find an input constrcutor in [modifiers] that matches the specified class.
		 * If there's no such constrcutor, it returns the most applicable one.
		 */
		inline fun <reified T> inputConstructorFor(): InputConstructor<T> {
			return (modifiers.getOrDefault(T::class.java, null) ?: let {
				val cls = T::class.java
				modifiers.entries.find { it.key.isAssignableFrom(cls) }
			}?.value ?: fallbackModifier) as InputConstructor<T>
		}
	}
}

/**
 * Adds a [PropertyElement] to the table.
 * @see PropertyElement
 */
fun <T, O: Any> Table.propertyElement(
	objProvider: () -> O?,
	property: KMutableProperty1<O, T>,
	propertyType: Class<out T>
): Cell<PropertyElement<T, O>> = add(PropertyElement(objProvider, property, propertyType))
