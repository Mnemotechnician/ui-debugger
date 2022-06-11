package com.github.mnemotechnician.uidebugger

import arc.*
import arc.scene.ui.Button
import arc.scene.ui.layout.Table
import arc.util.*
import com.github.mnemotechnician.uidebugger.fragment.DebuggerMenuFragment
import com.github.mnemotechnician.uidebugger.service.ServiceManager
import com.github.mnemotechnician.uidebugger.service.impl.BoundsDebuggerService
import com.github.mnemotechnician.uidebugger.util.Bundles
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Icon
import mindustry.mod.Mod
import mindustry.ui.MobileButton
import mindustry.ui.dialogs.BaseDialog

class UiDebugger : Mod() {
	private var lastButton: Button? = null
	private val menuDialog by lazy {
		BaseDialog(Bundles.uiDebuggerTitle).apply {
			closeOnBack()
		}
	}

	init {
		Events.on(EventType.ResizeEvent::class.java) {
			if (Vars.state.isMenu) {
				// why is a delay needed? how do I know? ask Anuke.
				Time.run(0f) { createMenuButton() }
			}
		}

		ServiceManager.start(BoundsDebuggerService())
	}

	private fun createMenuButton() {
		lastButton?.remove() // ensure there's no duplicates.

		// reflective access is no good, but MenuFragment.container is private
		val container = try {
			Reflect.get(Vars.ui.menufrag, "container") as Table
		} catch (e: Throwable) {
			Log.err("Cannot access 'Vars.ui.menufrag.container'", e)
			return
		}

		val action = Runnable {
			DebuggerMenuFragment.apply(menuDialog.cont)
			DebuggerMenuFragment.onElementSelection({ menuDialog.hide() }, { menuDialog.show() })
			menuDialog.show()
		}

		if (Vars.mobile) {
			if (Core.graphics.isPortrait) {
				container.row()
				lastButton = container.add(MobileButton(Icon.terminal, Bundles.uiDebugger, action)).colspan(2).get()
			} else {
				lastButton = container.add(MobileButton(Icon.terminal, Bundles.uiDebugger, action)).get()
				// move the button to the upper row
				container.children.let {
					it.insert(it.size - 2, it.pop())
				}
			}
		} else {
			container.row()
			lastButton = container.button(Bundles.uiDebugger, Icon.terminal, action).marginLeft(11f).update {
				it.isChecked = menuDialog.isShown
			}.get()
		}

		container.invalidateHierarchy()
	}
}
