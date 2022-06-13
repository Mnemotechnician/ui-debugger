package com.github.mnemotechnician.uidebugger.fragment

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.*
import arc.input.KeyCode
import arc.scene.Element
import arc.scene.Group
import arc.scene.event.*
import arc.scene.ui.Dialog
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.*
import com.github.mnemotechnician.mkui.*
import com.github.mnemotechnician.uidebugger.element.propertyField
import com.github.mnemotechnician.uidebugger.service.Service
import com.github.mnemotechnician.uidebugger.service.ServiceManager
import com.github.mnemotechnician.uidebugger.util.*
import mindustry.gen.Tex
import mindustry.graphics.Layer
import mindustry.ui.Styles

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
	/**
	 * The cell of the current element.
	 * Null if [currentElement] is null or not added to a table.
	 */
	val currentCell: Cell<Element>? get() = currentElement?.cell()

	/** Used by [selectorDialog] */
	private var lastSelectedElement: Element? = null

	private var selectionBeginListener: (() -> Unit)? = null
	private var selectionEndListener: (() -> Unit)? = null

	private var hierarchyTable: Table? = null

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
			toggleOption(Bundles.debugBounds, { Prefs.isElementDebugEnabled }, { Prefs.isElementDebugEnabled = it })
		}.growX().row()

		addCollapser({ Prefs.isElementDebugEnabled }, Tex.button, animate = true) {
			toggleOption(Bundles.debugHiddenElements, { Prefs.forceElementDebug }, { Prefs.forceElementDebug = it })
		}.growX().row()

		hsplitter()

		addTable(Tex.button) {
			pager {
				addPage("properties") {
					addTable(Styles.black3) {
						addLabel({
							if (currentElement == null) {
								Bundles.noElement
							} else {
								"${Bundles.currentElement}: ${currentElement.elementToString()}"
							}
						}).labelAlign(Align.left).growX()

						textButton("Select an element") {
							invokeElementSelection()
						}
					}.growX().marginBottom(5f).row()

					addCollapser({ currentElement != null }) {
						defaults().pad(5f)

						scrollPane {
							right().defaults().marginBottom(5f)

							properties(
								{ currentElement },
								{ it.toFloat() },
								"x" to "x",
								"y" to "y"
							).row()

							properties(
								{ currentElement },
								{ it.toFloat() },
								"width" to "width",
								"height" to "height"
							).row()

							addCollapser({ currentCell != null }, animate = false) {
								defaults().marginBottom(5f)

								addLabel("Cell-specific").growX().left().row()

								properties(
									{ currentCell },
									{ it.toFloat() },
									"min width" to "minWidth",
									"cell height" to "minHeight"
								).row()

								properties(
									{ currentCell },
									{ it.toFloat() },
									"max width" to "maxWidth",
									"max height" to "maxHeight"
								).row()

								properties(
									{ currentCell },
									{ it.toBooleanStrict() },
									"fill x" to "fillX",
									"fill y" to "fillY",
									"expand x" to "expandX",
									"expand y" to "expandY"
								).row()
							}.row()

							addLabel("Other").growX().left().row()

							val touchabilityMap = mapOf(
								"enabled" to Touchable.enabled,
								"childrenonly" to Touchable.childrenOnly,
								"disabled" to Touchable.disabled
							)

							addTable {
								defaults().pad(5f)

								addLabel("visible")
								propertyField({ currentElement }, Element::visible) { it.lowercase().toBooleanStrict() }
								addLabel("touchable")
								propertyField({ currentElement }, Element::touchable) { touchabilityMap[it.lowercase()] }
							}.growX().row()
						}.grow().row()

						addTable {
							defaults().pad(5f)

							addLabel("Actions:").labelAlign(Align.right).growX()

							textButton("cancel") {
								currentElement = null
							}
							textButton("invalidate") {
								currentElement?.invalidateHierarchy()
							}
							textButton("remove") {
								currentElement?.remove()
								currentElement = null
							}
						}.growX()
					}.growX()
				}

				addPage("hierarchy") {
					addTable {
						addLabel("Parent: ").growX()
						addLabel({ currentElement?.parent.elementToString() }).growX().labelAlign(Align.left)

						textButton("select") {
							currentElement = currentElement!!.parent
						}.disabled { currentElement?.parent == null }
					}.growX().row()

					addCollapser({ currentElement is Group }, animate = false) {
						hsplitter()

						addTable {
							addLabel("Children:").labelAlign(Align.left).growX()
							textButton("update") {
								updateHierarchyTable()
							}
						}.growX().row()

						addTable(Tex.button) {
							hierarchyTable = this
						}.growX()
					}.growX()
				}
			}.growX()
		}.growX()
	}

	/**
	 * Adds a toggle option.
	 */
	private inline fun Table.toggleOption(label: String, crossinline getter: () -> Boolean, crossinline setter: (Boolean) -> Unit) {
		addLabel(label).labelAlign(Align.left).growX()

		customButton({
			addLabel({ if (getter()) Bundles.enabled else Bundles.disabled })
		}, Styles.togglet) {
			setter(!getter())
		}.update {
			it.isChecked = getter()
		}.minWidth(80f)
	}

	/**
	 * Adds several multiple property fields based on the providen [Pair]s,
	 * whose [Pair.first] is the label and [Pair.second] is the property name.
	 */
	private inline fun <reified O: Any, T> Table.properties(
		noinline objProvider: () -> O?,
		noinline converter: (String) -> T,
		vararg fields: Pair<String, String>
	) = addTable {
		defaults().growX().pad(5f)

		fields.forEach { field ->
			addLabel(field.first)
			propertyField(objProvider, O::class.mutablePropertyFor(field.second), converter)
		}
	}.growX()

	/**
	 * Invokes the element selection, sets the [currentElement] field on success.
	 */
	private fun invokeElementSelection() {
		selectionBeginListener?.invoke()
		lastSelectedElement = null
		selectorDialog.show()
		ServiceManager.start(ElementHighlighterService())
	}

	/**
	 * Called when the element selector exits.
	 */
	private fun elementSelectionFinish() {
		ServiceManager.stop(ElementHighlighterService::class.java) // no need to keep updating it.
		selectionEndListener?.invoke()
		selectorDialog.hide()
		updateHierarchyTable()
	}

	/**
	 * Updates the hierarchy table.
	 * Does nothing if the fragment hasn't been built yet or there's no selected element.
	 */
	fun updateHierarchyTable() {
		val element = currentElement ?: return

		with(hierarchyTable ?: return) {
			clearChildren()
			defaults().pad(3f).marginBottom(2f)

			if (element is Group) {
				var index = 1 // no forEachIndexed.
				element.children.each { child ->
					addTable(Styles.black3) {
						addLabel("${index++}.").scaleFont(0.8f)
						addLabel(child.elementToString()).growX().labelAlign(Align.left).scaleFont(0.8f)

						vsplitter()

						textButton("select") {
							currentElement = child
						}.scaleFont(0.8f)
					}.row()
				}
			}
		}
	}

	/**
	 * Adds element selection listeners, overriding the previous ones.
	 * Use this to remove anything that can obscure the user's view and then show it again.
	 */
	fun onElementSelection(begin: () -> Unit, end: () -> Unit) {
		selectionBeginListener = begin
		selectionEndListener = end
	}

	private class ElementHighlighterService : Service() {
		override fun start() {}
		override fun stop() {}
		override fun update() {}

		override fun draw() {
			val element = lastSelectedElement ?: return
			val elementCoords = element.localToStageCoordinates(Tmp.v1.set(0f, 0f))

			Draw.draw(Layer.max) {
				Draw.color(Color.teal, 0.4f)
				// why is Fill.rect centered but Lines.rect is not
				Fill.rect(elementCoords.x + element.width / 2f, elementCoords.y + element.height / 2f, element.width, element.height)
				Draw.color(Color.blue)
				Lines.stroke(2f)
				Lines.rect(elementCoords.x, elementCoords.y, element.width, element.height)
				Draw.flush()
			}
		}

	}
}
