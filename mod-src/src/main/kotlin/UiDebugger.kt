package com.github.mnemotechnician.uidebugger

import arc.Core
import arc.Events
import arc.graphics.Color
import arc.math.Interp
import arc.scene.actions.Actions.*
import arc.scene.ui.Button
import arc.util.*
import com.github.mnemotechnician.mkui.extensions.dsl.*
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

				Reflect()

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

	init {
		Events.on(EventType.ClientLoadEvent::class.java) {
			Vars.ui.menufrag.addButton(Bundles.uiDebugger, Icon.terminal) {
				lastWindow?.destroy()?.also { lastWindow = null }
				DebuggerMenuFragment.apply(menuDialog.cont)
				DebuggerMenuFragment.onElementSelection({ menuDialog.hide() }, { menuDialog.show() })
				menuDialog.show()
			}
		}
		ServiceManager.start(BoundsDebuggerService())
	}
}
