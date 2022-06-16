package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.scene.Element
import arc.scene.style.Drawable
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import mindustry.gen.Icon
import mindustry.ui.Styles

/**
 * Displays an element inside itself, even if the said element is already added to the scene.
 */
class ElementDisplay(
	var element: Element?
) : Element() {
	var background: Drawable = Styles.black3

	private var elementProvider: (() -> Element?)? = null

	override fun draw() {
		super.draw()
		background.draw(x, y, width, height)

		val element = element // please, kotlin
		if (element != null) {
			if (!clipBegin()) return

			// this is a circus... i wounder if there's a better way to do that
			val oldX = element.x
			val oldY = element.y
			val oldSclX = element.scaleX
			val oldSclY = element.scaleY
			val oldOriginX = element.originX
			val oldOriginY = element.originY

			element.x = x
			element.y = y
			element.scaleX = width / element.width
			element.scaleY = height / element.height
			element.originX = 0f
			element.draw()

			element.x = oldX
			element.y = oldY
			element.scaleX = oldSclX
			element.scaleY = oldSclY
			element.originX = oldOriginX
			element.originY = oldOriginY

			clipEnd()
		} else {
			// hardcoded padding of 3
			if (width < 6 || height < 6) return

			Draw.color(olor.red)
			Icon.cancel.draw(x + 3, y + 3, width - 3, height - 3)
		}
	}

	override fun act(delta: Float) {
		super.act(delta)

		if (elementProvider != null) element = elementProvider!!()
	}

	/**
	 * Sets an element provider, which is called on every frame to provide an [element].
	 */
	fun elementProvider(elementProvider:  (() -> Element?)? = null) {
		this.elementProvider = elementProvider
	}
}

/**
 * Adds a dynamic element display to the table.
 */
fun Table.elementDisplay(
	background: Drawable = Styles.black3,
	provider: () -> Element?
): Cell<ElementDisplay> = add(ElementDisplay(null).also {
	it.elementProvider(provider)
	it.background = background
})
