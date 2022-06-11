package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.math.Interp
import arc.scene.actions.*
import arc.scene.ui.TextField
import com.github.mnemotechnician.mkui.content
import mindustry.ui.Styles
import kotlin.reflect.*

/**
 * A text field that automatically updates to match the underlying property
 * and automatically updates the underlying property.
 *
 * @param T the type of the property. Must be toString()-able.
 * @param O the type of the object this property belongs to.
 *
 * @param obj the object this property belongs to.
 * @param property the property this field represents.
 * @param converter converts the user input to the type of the property. Should throw an exception if the input is not valid.
 */
class PropertyTextField<T, O>(
	var obj: O,
	private val property: KMutableProperty1<O, T>,
	private val converter: (String) -> T,
	style: TextField.TextFieldStyle = Styles.defaultField
) : TextField(property.get(obj).toString(), style) {
	private var lastValue: T = property.get(obj)
	private var objProvider: (() -> O)? = null

	init {
		changed {
			try {
				val value = converter(content)
				property.set(obj, value)
			} catch (e: Exception) {
				clearActions()

				addAction(Actions.sequence(
					Actions.color(Color.red, 0f),
					Actions.color(Color.white, 1f, Interp.elasticIn)
				))
			}
		}
	}

	override fun act(delta: Float) {
		super.act(delta)

		if (objProvider != null) {
			obj = objProvider!!()
		}

		val new = property.get(obj)
		if (new != lastValue) {
			content = new.toString()
			lastValue = new
		}
	}

	/**
	 * Sets a provider for [obj]. This lambda will be called on every frame, __before__ updating the field.
	 *
	 * A null value removes the provider, making this field use the same object every time.
	 */
	fun objectProvider(provider: (() -> O)?) {
		objProvider = provider
	}
}

/**
 * Adds a property text field with a constant object.
 *
 * @see PropertyTextField
 */
//fun <T, O> Table.propertyField(obj: O, property: KMutableProperty<O, T>, converter: (String) -> T): Cell<PropertyTextField> {
//	return add(PropertyTextField(obj, property, converter))
//}

/**
 * Adds a property text field with a dynamic object, supplied by [objProvider].
 *
 * @see PropertyTextField
 */
//fun <T, O> Table.propertyField(objProvider: () -> O, property: KMutableProperty<O, T>, converter: (String) -> T): Cell<PropertyTextField> {
//	return add(PropertyTextField(objProvider(), property, converter).also {
//		it.objProvider(objProvider)
//	})
//}
