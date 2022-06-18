package com.github.mnemotechnician.uidebugger.service.impl

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.scene.Element
import arc.scene.Group
import arc.util.Tmp
import com.github.mnemotechnician.uidebugger.service.Service
import com.github.mnemotechnician.uidebugger.util.Prefs
import mindustry.graphics.Layer

class BoundsDebuggerService : Service() {
	override fun start() {
	}

	override fun stop() {
	}

	override fun update() {
	}

	override fun draw() {
		if (!Prefs.isElementDebugEnabled) return

		Draw.draw(Layer.max) {
			Lines.stroke(2f)
			renderBounds(Core.scene.root, true)
			Draw.flush()
		}
	}

	private fun renderBounds(element: Element, parentVisible: Boolean) {
		val visible = element.visible && parentVisible

		Draw.color(when {
			visible && element.isTouchable -> Color.green // visible and touchable
			visible -> Color.yellow // visible but not touchable
			else -> Color.red // invisible
		}, 0.3f)

		val coords = element.localToStageCoordinates(Tmp.v1.set(0f, 0f))
		Lines.rect(coords.x, coords.y, element.width, element.height)

		if (element is Group) {
			for (child in element.children) {
				if (child.visible || Prefs.forceElementDebug) {
					renderBounds(child, visible)
				}
			}
		}
	}
}
