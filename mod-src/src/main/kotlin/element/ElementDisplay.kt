package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.math.geom.Vec2
import arc.scene.Element
import arc.scene.Group
import arc.scene.style.Drawable
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Align
import mindustry.gen.Icon
import mindustry.ui.Styles

/**
 * Displays an element inside itself, even if the said element is already added to the scene.
 *
 * Despite being a group, this class __does not__ hold children, and any attempts to add a child will throw an exception.
 */
class ElementDisplay(
	var element: Element?
) : Group() {
	var background: Drawable = Styles.black3

	private var elementProvider: (() -> Element?)? = null

	var lastSize = Vec2(0f, 0f)

	init {
		transform = true
	}

	override fun drawChildren() {
		background.draw(x, y, width, height)

		val element = element // please, kotlin
		if (element != null) {
			if (!clipBegin()) return

			// this is a circus... i wounder if there's a better way to do that
			val oldX = element.x
			val oldY = element.y
			val oldOriginX = element.originX
			val oldOriginY = element.originY
			val oldParent = element.parent

			element.x = width / 2f
			element.y = height / 2f
			element.setOrigin(Align.center)
			element.parent = this
			element.draw()

			element.x = oldX
			element.y = oldY
			element.originX = oldOriginX
			element.originY = oldOriginY
			element.parent = oldParent

			clipEnd()
		} else {
			// hardcoded padding of 3
			if (width < 6 || height < 6) return

			Draw.color(Color.red)
			Icon.cancel.draw(3f, 3f, width - 3, height - 3)
		}
	}

	override fun act(delta: Float) {
		super.act(delta)

		if (elementProvider != null) {
			val newElement = elementProvider!!()

			if (element != newElement) {
				element = newElement
			}
		}

		if (element != null && (element!!.width != lastSize.x || element!!.height != lastSize.y)) {
			lastSize.set(element!!.width, element!!.height)
			invalidateHierarchy()
			pack()
		}
	}

	override fun getPrefWidth() = element?.prefWidth ?: 30f

	override fun getPrefHeight() = element?.prefHeight ?: 30f

	/**
	 * Sets an element provider, which is called on every frame to provide an [element].
	 */
	fun elementProvider(elementProvider:  (() -> Element?)? = null) {
		this.elementProvider = elementProvider
	}

	override fun childrenChanged() {
		children.clear()
		throw IllegalStateException("You must not modify children of ElementDisplay.")
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
