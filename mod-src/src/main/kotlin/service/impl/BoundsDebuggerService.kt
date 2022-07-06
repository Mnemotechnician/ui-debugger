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
		val coords = element.localToStageCoordinates(Tmp.v1.set(0f, 0f))

		if (debugElements) {
			Draw.color(when {
				visible && element.isTouchable -> Color.green // visible and touchable
				visible -> Color.yellow // visible but not touchable
				else -> Color.red // invisible
			}, alpha)

			Lines.rect(coords.x, coords.y, element.width, element.height)
		}

		if (parentVisible && debugCells && element is Table && !element.needsLayout()) {
			Draw.color(Color.green, alpha)

			var pos = 0f
			repeat(element.columns) {
				Lines.line(coords.x + pos, coords.y, coords.x + pos, coords.y + element.height)
				pos += element.getColumnWidth(it)
			}
			pos = 0f
			repeat(element.rows) {
				Lines.line(coords.x, coords.y + pos, coords.x + element.width, coords.y + pos)
				pos += element.getRowHeight(it)
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
