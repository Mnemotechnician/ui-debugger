package com.github.mnemotechnician.uidebugger.service.impl

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.scene.Element
import arc.scene.Group
import arc.scene.ui.layout.Table
import arc.util.Tmp
import com.github.mnemotechnician.uidebugger.service.Service
import com.github.mnemotechnician.uidebugger.util.Prefs
import mindustry.graphics.Layer

class BoundsDebuggerService : Service() {
	private var alpha = 0f
	private var forceDebug = false
	private var debugElements = false
	private var debugCells = false

	override fun start() {
	}

	override fun stop() {
	}

	override fun update() {
	}

	override fun draw() {
		debugElements = Prefs.isElementDebugEnabled
		debugCells = Prefs.isCellDebugEnabled

		if (!debugCells && !debugElements) return

		alpha = Prefs.boundsOpacity
		forceDebug = Prefs.forceElementDebug

		Draw.draw(Layer.max) {
			Lines.stroke(Prefs.boundsThickness)
			renderBounds(Core.scene.root, true)
			Draw.flush()
		}
	}

	private fun renderBounds(element: Element, parentVisible: Boolean) {
		val visible = element.visible && parentVisible

		if (debugElements) {
			Draw.color(when {
				visible && element.isTouchable -> Color.green // visible and touchable
				visible -> Color.yellow // visible but not touchable
				else -> Color.red // invisible
			}, alpha)

			val coords = element.localToStageCoordinates(Tmp.v1.set(0f, 0f))
			Lines.rect(coords.x, coords.y, element.width, element.height)
		}

		if (parentVisible && debugCells && element is Table && !element.needsLayout()) {
			Draw.color(Color.green, alpha)

			var pos = 0f
			repeat(element.columns) {
				val width = element.getColumnWidth(it)
				Lines.line(element.x + pos, element.y, element.x + pos, element.y + element.height)
				pos += width
			}
			pos = 0f
			repeat(element.rows) {
				val height = element.getRowHeight(it)
				Lines.line(element.x, element.y + pos, element.x + element.width, height + pos)
				pos += height
			}
		}

		if (element is Group) {
			for (child in element.children) {
				if (child.visible || forceDebug) {
					renderBounds(child, visible)
				}
			}
		}
	}
}
