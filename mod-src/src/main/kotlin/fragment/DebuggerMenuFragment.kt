package com.github.mnemotechnician.uidebugger.fragment

import arc.Core
import arc.input.KeyCode
import arc.scene.*
import arc.scene.event.*
import arc.scene.ui.Dialog
import arc.scene.ui.layout.Table
import arc.util.*
import com.github.mnemotechnician.mkui.*
import com.github.mnemotechnician.uidebugger.util.*
import mindustry.ui.Styles
import mindustry.gen.Tex
import kotlin.math.roundToInt

/**
 * Allows the user to debug ui elements in some way.
 *
 * After applying the fragment to some table, use [DebuggerMenuFragment.onElementSelection]
 * to close any windows, dialogs and anything else that can obscure the user's view
 * and then show it again when the selection ends.
 */
object DebuggerMenuFragment : Fragment<Group, Table>() {
	/** Current selected element. Null if the user hasn't selected one. */
	var currentElement: Element? = null
		private set

	/** Used by [selectorDialog] */
	private var lastSelectedElement: Element? = null

	private var selectionBeginListener: (() -> Unit)? = null
	private var selectionEndListener: (() -> Unit)? = null

	/**
	 * A dialog that allows the user to select a ui element.
	 */
	private val selectorDialog = Dialog(Bundles.elementSelectTitle).apply {
		background = Styles.black3
		touchable = Touchable.enabled
		closeOnBack { elementSelectionFinish() }

		titleTable.apply {
			row()
			addLabel({ if (lastSelectedElement == null) "" else "${lastSelectedElement!!::class.simpleName} — ${Bundles.clickConfirm}"}).row()
		}.row()

		addListener(object : InputListener() {
			override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
				touchable = Touchable.disabled
				val coords = localToStageCoordinates(Tmp.v1.set(x, y))
				val element = Core.scene.root.hit(coords.x, coords.y, true)
				touchable = Touchable.enabled
				
				Log.info("click") //TODO remove

				if (lastSelectedElement == element && element != null) {
					currentElement = element
					elementSelectionFinish()
				} else {
					lastSelectedElement = element
				}

				return false
			}
		})
	}

	override fun build() = createTable(Styles.black5) {
		addLabel("UI Debugger", alignment = Align.center).row()
		hsplitter(padTop = 0f)

		addTable(Tex.button) {
			addLabel("Debug element bounds").labelAlign(Align.left).growX()

			textToggle({ if (Prefs.isElementDebugEnabled) Bundles.enabled else Bundles.disabled }) {
				Prefs.isElementDebugEnabled = it
			}
		}.growX().row()

		addCollapser({ Prefs.isElementDebugEnabled }, Tex.button, animate = true) {
			addLabel("Debug hidden elements").labelAlign(Align.left).growX()

			textToggle({ if (Prefs.forceElementDebug) Bundles.enabled else Bundles.disabled}) {
				Prefs.forceElementDebug = it
			}
		}.growX().row()

		hsplitter()

		addTable(Tex.button) {
			addTable(Styles.black3) {
				addLabel({ if (currentElement == null) {
					"No element selected"
				} else {
					"Current element: ${currentElement!!::class.simpleName}, x ${currentElement!!.x.roundToInt()}, y ${currentElement!!.y.roundToInt()}"
				} })

				textButton("Select an element") {
					invokeElementSelection()
				}
			}.marginBottom(5f).row()

			addCollapser({ currentElement != null }) {
				defaults().pad(5f)

				addTable {
					right().defaults().pad(5f)

					addLabel("width")
				//	propertyTextField({ currentElement!! }, Element::width) { it.toFloat() }

					addLabel("height")
				//	propertyTextField({ currentElement!! }, Element::height) { it.toFloat() }
				}.row()

				addTable {
					defaults().pad(5f)

					addLabel("Actions:").labelAlign(Align.left).growX()

					textButton("remove") {
						currentElement?.remove()
						currentElement = null
					}
					textButton("invalidate") {
						currentElement?.invalidateHierarchy()
					}
				}.growX()
			}.growX()
		}.growX()
	}

	/**
	 * Invokes the element selection, sets the [currentElement] field on success.
	 */
	private fun invokeElementSelection() {
		selectionBeginListener?.invoke()
		lastSelectedElement = null
		selectorDialog.show()
	}

	/**
	 * Called when the element selector exits.
	 */
	private fun elementSelectionFinish() {
		selectionEndListener?.invoke()
		selectorDialog.hide()
	}

	/**
	 * Adds element selection listeners, overriding the previous ones.
	 * Use this to remove anything that can obscure the user's view and then show it again.
	 */
	fun onElementSelection(begin: () -> Unit, end: () -> Unit) {
		selectionBeginListener = begin
		selectionEndListener = end
	}
}
