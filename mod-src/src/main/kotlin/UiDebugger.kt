package com.github.mnemotechnician.uidebugger

import arc.Core
import arc.Events
import arc.math.Interp
import arc.scene.Element
import arc.scene.actions.Actions.*
import arc.scene.ui.Button
import arc.scene.ui.layout.Table
import arc.util.*
import com.github.mnemotechnician.mkui.childOrNull
import com.github.mnemotechnician.mkui.textButton
import com.github.mnemotechnician.mkui.windows.Window
import com.github.mnemotechnician.mkui.windows.WindowManager
import com.github.mnemotechnician.uidebugger.fragment.DebuggerMenuFragment
import com.github.mnemotechnician.uidebugger.service.ServiceManager
import com.github.mnemotechnician.uidebugger.service.impl.BoundsDebuggerService
import com.github.mnemotechnician.uidebugger.util.Bundles
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Icon
import mindustry.graphics.Pal
import mindustry.mod.Mod
import mindustry.ui.MobileButton
import mindustry.ui.dialogs.BaseDialog

class UiDebugger : Mod() {
	private var lastButton: Button? = null
	private val menuDialog by lazy {
		BaseDialog(Bundles.uiDebuggerTitle).apply {
			closeOnBack()

			titleTable.textButton(Bundles.showInWindow) {
				this@apply.hide()
				WindowManager.createWindow(DebuggerMenuWindow()) // todo: can i not recreate it every time?
			}.color(Pal.accent).minWidth(120f).align(Align.top)
		}
	}

	private val menuButtonAction = Runnable {
		DebuggerMenuFragment.apply(menuDialog.cont)
		DebuggerMenuFragment.onElementSelection({ menuDialog.hide() }, { menuDialog.show() })
		menuDialog.show()
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

		if (Vars.mobile) {
			if (Core.graphics.isPortrait) {
				container.row()
				lastButton = container.add(MobileButton(Icon.terminal, Bundles.uiDebugger, menuButtonAction)).colspan(2).get()
			} else {
				lastButton = container.add(MobileButton(Icon.terminal, Bundles.uiDebugger, menuButtonAction)).get()
				// move the button to the upper row
				container.children.let {
					it.insert(it.size - 2, it.pop())
				}
			}
		} else {
			container.row()
			lastButton = container.button(Bundles.uiDebugger, Icon.terminal, menuButtonAction).marginLeft(11f).update {
				it.isChecked = menuDialog.isShown
			}.get()
		}

		// we do a little juicy bit of trolling
		lastButton?.childOrNull<Element>(0)?.addAction(forever(
			delay(2.4f, parallel(
				rotateBy(720f, 2.4f) { Interp.swingOut.apply(Interp.sineIn.apply(it)) },
				sequence(
					translateBy(0f, 10f, 1.3f, Interp.sineOut),
					translateBy(0f, -10f, 1.1f) { Interp.bounceOut.apply(Interp.sineIn.apply(it)) }
				)
			))
		))

		container.invalidateHierarchy()
	}

	private class DebuggerMenuWindow : Window() {
		override val name = Bundles.uiDebugger
		override val closeable = true

		override fun onCreate() {
			DebuggerMenuFragment.apply(table)
			DebuggerMenuFragment.onElementSelection({ isCollapsed = true }, { isCollapsed = false })
		}
	}
}
