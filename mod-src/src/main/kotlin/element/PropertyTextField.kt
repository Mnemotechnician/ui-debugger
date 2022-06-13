package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.math.Interp
import arc.scene.actions.Actions.color
import arc.scene.actions.Actions.sequence
import arc.scene.ui.TextField
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import com.github.mnemotechnician.mkui.content
import mindustry.ui.Styles
import kotlin.reflect.KMutableProperty1

/**
 * A text field that automatically updates to match the underlying property
 * and automatically updates the underlying property.
 *
 * When [obj] is null, the field is disabled.
 *
 * @param T the type of the property. Must be toString()-able.
 * @param O the type of the object this property belongs to.
 *
 * @param obj the object this property belongs to.
 * @param property the property this field represents.
 * @param converter converts the user input to the type of the property. Should throw an exception if the input is not valid.
 */
@Suppress("MemberVisibilityCanBePrivate", "LeakingThis")
open class PropertyTextField<T, O>(
	var obj: O?,
	protected val property: KMutableProperty1<O, T>,
	protected val converter: (String) -> T,
	style: TextFieldStyle = Styles.defaultField
) : TextField(
	obj?.let { property.get(it).toString() }.orEmpty(),
	style
) {
	protected var lastValue: T? = obj?.let { property.get(it) }

	protected var objProvider: (() -> O?)? = null

	init {
		changed {
			if (obj == null) return@changed

			try {
				updateProperty()
			} catch (e: Exception) {
				fail()
			}
		}
	}

	/**
	 * Updates the value of the property in accordance with the value of the field.
	 */
	open fun updateProperty() {
		val value = converter(content)
		property.set(obj!!, value)
	}

	/**
	 * Called when [updateProperty] throws an exception, informs the user about the failure.
	 */
	protected open fun fail() {
		clearActions()

		addAction(sequence(
			color(Color.red, 0f),
			color(Color.white, 2.5f, Interp.bounceOut)
		))
	}

	override fun act(delta: Float) {
		super.act(delta)

		if (objProvider != null) {
			obj = objProvider!!()
		}

		if (obj != null) {
			val new = property.get(obj!!)
			if (new != lastValue) {
				updateText(new)
				lastValue = new
			}
		}
	}

	/**
	 * Updates the content of the field in accordance with the providen value.
	 * Called when the value of the property changes.
	 */
	protected open fun updateText(value: T) {
		content = value.toString()
	}

	/**
	 * Sets a provider for [obj]. This lambda will be called on every frame, __before__ updating the field.
	 *
	 * A null value removes the provider, making this field use the same object every time.
	 */
	fun objectProvider(provider: (() -> O?)?) {
		objProvider = provider
	}
}

/**
 * Adds a property text field with a constant object.
 *
 * @see PropertyTextField
 */
fun <T, O: Any> Table.propertyFieldConst(obj: O, property: KMutableProperty1<O, T>, converter: (String) -> T): Cell<PropertyTextField<T, O>> {
	return add(PropertyTextField(obj, property, converter))
}

/**
 * Adds a property text field with a dynamic object, supplied by [objProvider].
 *
 * @see PropertyTextField
 */
fun <T, O: Any> Table.propertyField(objProvider: () -> O?, property: KMutableProperty1<O, T>, converter: (String) -> T): Cell<PropertyTextField<T, O>> {
	return add(PropertyTextField(objProvider(), property, converter).also {
		it.objectProvider(objProvider)
	})
}
