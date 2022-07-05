package com.github.mnemotechnician.uidebugger.element

import arc.Core
import arc.graphics.Color
import arc.math.Interp
import arc.scene.actions.Actions.*
import arc.scene.actions.ColorAction
import arc.scene.ui.TextField
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import com.github.mnemotechnician.mkui.extensions.elements.content
import mindustry.ui.Styles
import kotlin.reflect.KMutableProperty1

/**
 * A text field that automatically updates to match the underlying property
 * and automatically updates the underlying property.
 *
 * When [obj] is null, the field is disabled completely.
 *
 * Locking (setting [isLocked] to true) this element normally makes it discard any user input,
 * effectively making it read-only.
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
	/** Some properties might retain their reference but have the referenced object modified. */
	protected var lastHashCode = 0

	protected var objProvider: (() -> O?)? = null

	/**
	 * If true, any user input is discarded.
	 */
	var isLocked = false
	protected var lockProvider: (() -> Boolean)? = null

	var lastColorAction: ColorAction? = null

	init {
		changed {
			if (obj == null) return@changed

			if (isLocked) {
				// discard the input.
				Core.app.post { updateText(property.get(obj!!)) }
				return@changed
			}

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
		lastValue = value
		lastHashCode = value.hashCode()
	}

	/**
	 * Called when [updateProperty] throws an exception, informs the user about the failure.
	 */
	protected open fun fail() {
		clearActions()

		addAction(parallel(
			sequence(
				color(failColor, 0f),
				color(Color.white, 2.5f, Interp.bounceOut).also { lastColorAction = it }
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
			if (lockProvider != null) {
				isLocked = lockProvider!!()
			}

			val new = property.get(obj!!)
			if (new != lastValue || new.hashCode() != lastHashCode) {
				updateText(new)
				lastValue = new
				lastHashCode = new.hashCode()
			}
		} else {
			lastValue = null
			lastHashCode = 0
			content = "N / A"
		}

		val newColor = if (isLocked) Color.gray else Color.white
		if (!hasActions() || lastColorAction?.target != this) {
			color.set(newColor)
		} else {
			lastColorAction?.color = newColor
		}
	}

	/**
	 * Updates the content of the field in accordance with the providen value.
	 * Called when the value of the property changes.
	 */
	protected open fun updateText(value: T) {
		content = if (value != null) backConverter(value) else ""

		lastColorAction?.let { removeAction(it) }
		actions(
			color(updateColor),
			color(Color.white, 1.5f, Interp.circleIn).also { lastColorAction = it }
		)
	}

	/**
	 * Sets a provider for [obj]. This lambda will be called on every frame, __before__ updating the field.
	 *
	 * A null value removes the provider, making this field use the same object every time.
	 */
	fun objectProvider(provider: (() -> O?)?) {
		objProvider = provider
	}

	/**
	 * Sets a lock provider.
	 * This function is called whenever this element is updated. It's result determines whether this input field is locked.
	 * @see isLocked
	 */
	fun lockProvider(provider: (() -> Boolean)?) {
		lockProvider = provider
	}

	companion object {
		val failColor: Color = Color.red.cpy()
		val updateColor: Color = Color.valueOf("8040bb")
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
