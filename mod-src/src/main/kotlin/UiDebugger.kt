package com.github.mnemotechnician.uidebugger

import arc.Core
import arc.Events
import arc.graphics.Color
import arc.math.Interp
import arc.scene.actions.Actions.*
import arc.scene.ui.Button
import arc.scene.ui.Image
import arc.scene.ui.layout.Table
import arc.util.*
import com.github.mnemotechnician.mkui.extensions.dsl.textButton
import com.github.mnemotechnician.mkui.extensions.elements.findElement
import com.github.mnemotechnician.mkui.extensions.elements.findOrNull
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
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog

class UiDebugger : Mod() {
	private var lastButton: Button? = null
	private var lastWindow: Window? = null
	private val menuDialog by lazy {
		BaseDialog(Bundles.uiDebuggerTitle).apply {
			closeOnBack()

			titleTable.textButton(Bundles.showInWindow) {
				this@apply.hide()

				lastWindow = object : Window() {
					override val name = Bundles.uiDebugger
					override val closeable = true

					override fun onCreate() {
						DebuggerMenuFragment.apply(table)
						DebuggerMenuFragment.onElementSelection({ isCollapsed = true }, { isCollapsed = false })
					}
				}.also {
					WindowManager.createWindow(it)
				}
			}.color(Pal.accent).height(40f).align(Align.top).with {
				it.translation.y += 20f // make it occupy two rows visually

				it.addAction(forever(sequence(
					color(Color.green, 1f, Interp.sine),
					color(Pal.accent, 1f, Interp.sine)
				)))
			}
		}
	}

	private val menuButtonAction = Runnable {
		lastWindow?.destroy()?.also { lastWindow = null }
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
		var container = try {
			(Reflect.get(Vars.ui.menufrag, "container") as Table).findElement<Table>("buttons")
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
				// move the button
				container.cells.let {
					it.insert(it.size - 2, it.pop())
				}
			}
		} else {
			container.row()
			lastButton = container.button(
				Bundles.uiDebugger, Icon.terminal, Styles.flatToggleMenut, menuButtonAction
			).marginLeft(11f).update {
				it.isChecked = menuDialog.isShown
			}.checked { menuDialog.isShown }.get()
		}

		// we do a little juicy bit of trolling
		lastButton?.findOrNull<Image>()?.addAction(forever(
			delay(2.4f, parallel(
				rotateBy(720f, 2.4f) { Interp.swingOut.apply(Interp.sineIn.apply(it)) },
				sequence(
					translateBy(0f, 20f, 1.3f, Interp.sineOut),
					translateBy(0f, -20f, 1.1f) { Interp.bounceOut.apply(Interp.sineIn.apply(it)) }
				)
			))
		))

		container.invalidateHierarchy()
	}
}
