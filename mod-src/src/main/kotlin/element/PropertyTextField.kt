package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.util.Log
import arc.math.Interp
import arc.scene.actions.Actions.*
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
 * When [obj] is null, the field is disabled completely.
 *
 * Disabling this element normally makes it ignore any user input, effectively making it read-only.
 *
 * @param T the type of the property.
 * @param O the type of the object this property belongs to.
 *
 * @param obj the object this property belongs to.
 * @param property the property this field represents.
 * @param converter converts the user input to the type of the property. Should throw an exception if the input is not valid.
 * @param backConverter converts the value of the property to string. Defaults to a simple `.toString()` call.
 */
@Suppress("MemberVisibilityCanBePrivate", "LeakingThis")
open class PropertyTextField<T, O>(
	var obj: O?,
	protected val property: KMutableProperty1<O, T>,
	protected val converter: (String) -> T,
	protected val backConverter: (T) -> String = { it.toString() },
	style: TextFieldStyle = Styles.defaultField
) : TextField(
	obj?.let { property.get(it).toString() }.orEmpty(),
	style
) {
	protected var lastValue: T? = obj?.let { property.get(it) }

	protected var objProvider: (() -> O?)? = null

	init {
		changed {
			Log.info("$this changed")

			if (obj == null) return@changed

			if (disabled) {
				// cancel the input, shouldn't be called since there's a text filter.
				updateText(property.get(obj!!))
				return@changed
			}

			try {
				updateProperty()
			} catch (e: Exception) {
				fail()
			}
		}

		setFilter { _, _ -> !disabled }
	}

	/**
	 * Updates the value of the property in accordance with the value of the field.
	 */
	open fun updateProperty() {
		Log.info("updated")
		val value = converter(content)
		Log.info("to $value")

		property.set(obj!!, value)
		lastValue = value
	}

	/**
	 * Called when [updateProperty] throws an exception, informs the user about the failure.
	 */
	protected open fun fail() {
		clearActions()

		addAction(parallel(
			sequence(
				color(Color.red, 0f),
				color(Color.white, 2.5f, Interp.bounceOut)
			),
			translateBy(25f, 0f, 0.75f, Interp.bounceOut),
			delay(0.25f, translateBy(-50f, 0f, 0.75f, Interp.bounceOut)),
			delay(0.5f, translateBy(25f, 0f, 0.75f, Interp.sineOut))
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
		} else {
			lastValue = null
			content = ""
		}
	}

	/**
	 * Updates the content of the field in accordance with the providen value.
	 * Called when the value of the property changes.
	 */
	protected open fun updateText(value: T) {
		content = if (value != null) backConverter(value) else ""
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
 * Adds a property text field with a dynamic object, supplied by [objProvider].
 *
 * @see PropertyTextField
 */
fun <T, O: Any> Table.propertyField(
	objProvider: () -> O?,
	property: KMutableProperty1<O, T>,
	converter: (String) -> T,
	backConverter: (T) -> String = { it.toString() }
): Cell<PropertyTextField<T, O>> {
	return add(PropertyTextField(objProvider(), property, converter, backConverter).also {
		it.objectProvider(objProvider)
	})
}
