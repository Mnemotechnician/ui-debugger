package com.github.mnemotechnician.uidebugger.fragment

import arc.Core
import arc.func.Floatc
import arc.graphics.Color
import arc.graphics.g2d.*
import arc.input.KeyCode
import arc.math.Interp
import arc.scene.Element
import arc.scene.Group
import arc.scene.actions.Actions.color
import arc.scene.event.*
import arc.scene.ui.Dialog
import arc.scene.ui.Slider
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.*
import com.github.mnemotechnician.mkui.extensions.dsl.*
import com.github.mnemotechnician.mkui.extensions.elements.cell
import com.github.mnemotechnician.uidebugger.element.elementDisplay
import com.github.mnemotechnician.uidebugger.element.propertyField
import com.github.mnemotechnician.uidebugger.service.Service
import com.github.mnemotechnician.uidebugger.service.ServiceManager
import com.github.mnemotechnician.uidebugger.util.*
import mindustry.gen.Tex
import mindustry.graphics.Layer
import mindustry.graphics.Pal
import mindustry.ui.Styles

/**
 * Allows the user to debug ui elements in some way.
 *
 * After applying the fragment to some table, use [DebuggerMenuFragment.onElementSelection]
 * to close any windows, dialogs and anything else that can obscure the user's view
 * and then show them again when the selection ends.
 */
object DebuggerMenuFragment : Fragment<Group, Table>() {
	/** Current selected element. Null if the user hasn't selected one. */
	var currentElement: Element? = null
		private set
	/**
	 * The cell of the current element.
	 * Null if [currentElement] is null or not a child of a [Table].
	 */
	val currentCell: Cell<Element>? get() = currentElement?.cell()

	/** Used by [selectorDialog] */
	private var lastSelectedElement: Element? = null

	private var selectionBeginListener: (() -> Unit)? = null
	private var selectionEndListener: (() -> Unit)? = null

	private lateinit var elementInfoTable: Table
	private var hierarchyTable: Table? = null

	init {
		ServiceManager.start(ElementInvalidatorService())
	}

	/**
	 * A dialog that allows the user to select a ui element.
	 */
	private val selectorDialog = Dialog(Bundles.elementSelectTitle).apply {
		background = Styles.black3
		touchable = Touchable.enabled
		closeOnBack { elementSelectionFinish() }

		titleTable.apply {
			row()
			addLabel({ if (lastSelectedElement == null) "" else "${lastSelectedElement!!::class.simpleName} — ${Bundles.clickConfirm}" }).row()
		}.row()

		addListener(SelectionInputListener(this))
	}

	override fun build() = createTable(Styles.black5) {
		addTable(Tex.button) {
			elementInfoTable = this

			addTable(Tex.whiteui) {
				addLabel({
					if (currentElement == null) {
						Bundles.noElement
					} else {
						"${Bundles.currentElement}: ${currentElement.elementToString()}"
					}
				}, wrap = false).labelAlign(Align.left).growX()

				textButton("Select an element") {
					invokeElementSelection()
				}
			}.growX().pad(5f).color(Pal.darkestGray).row()

			pager {
				addPage("preferences") {
					defaults().pad(3f)

					toggleOption(Bundles.debugBounds, { Prefs.isElementDebugEnabled }, { Prefs.isElementDebugEnabled = it })

					toggleOption(Bundles.debugCells, { Prefs.isCellDebugEnabled }, { Prefs.isCellDebugEnabled = it }).row()

					// row 2
					addCollapser({ Prefs.isElementDebugEnabled }, animate = true) {
						toggleOption(Bundles.debugHiddenElements, { Prefs.forceElementDebug }, { Prefs.forceElementDebug = it })
					}.growX().row()

					// row 3
					addCollapser({ Prefs.isElementDebugEnabled || Prefs.isElementDebugEnabled }, animate = true) {
						sliderOption(
							Bundles.boundsOpacity,
							1f / 256, 1f, 1f / 256,
							{ Prefs.boundsOpacity }, { Prefs.boundsOpacity = it }
						)

						sliderOption(
							Bundles.boundsThickness,
							0.1f, 10f, 0.1f,
							{ Prefs.boundsThickness }, { Prefs.boundsThickness = it }
						)
					}.colspan(2).growX().row()
				}

				addPage("preview") {
					elementDisplay(Tex.button) { currentElement }
				}

				addPage("properties") {
					addCollapser({ currentElement != null }) {
						defaults().pad(5f)

						scrollPane {
							right().defaults().fillX().pad(5f).marginBottom(5f)

							addLabel("name")
							propertyField({ currentElement }, Element::name, { it }, { it })
							addLabel("color")
							propertyField<Color, _>({ currentElement }, Element::class.mutablePropertyFor("color"), {
								Color.valueOf(currentElement!!.color, it)
							}).row()
							
							properties(
								{ currentElement },
								{ it.toFloat() },
								{ true }, // in most cases it will have effect, as the properties are modified reflectively.
								"x" to "x",
								"y" to "y",
								"width" to "width",
								"height" to "height"
							)

							addLabel("translation")
							propertyField({ currentElement }, Element::translation, {
								it.split(",").let { currentElement!!.translation.set(it[0].toFloat(), it[1].toFloat()) }
							}, { "${it.x}, ${it.y}" }).colspan(3).row()

							addLabel("Cell-specific").colspan(4).labelAlign(Align.left).growX().row()

							properties(
								{ currentCell },
								{ it.toFloat() },
								{ currentCell != null },
								"min width" to "minWidth",
								"min height" to "minHeight",
								"max width" to "maxWidth",
								"max height" to "maxHeight",
								"fill x" to "fillX",
								"fill y" to "fillY",
								"expand x" to "expandX",
								"expand y" to "expandY"
							)

							addLabel("Other").labelAlign(Align.left).colspan(4).growX().row()

							val touchabilityMap = mapOf(
								"enabled" to Touchable.enabled,
								"childrenonly" to Touchable.childrenOnly,
								"disabled" to Touchable.disabled
							)

							addLabel("visible")
							propertyField({ currentElement }, Element::visible, { it.lowercase().toBooleanStrict() })
								.disabled { currentElement?.let { it.visibility != null } ?: false }

							addLabel("touchable")
							propertyField({ currentElement }, Element::touchable, { touchabilityMap[it.lowercase()] })
								.disabled { currentElement?.let { it.touchablility != null } ?: false }

							row()
						}.grow().row()

						addTable {
							defaults().pad(5f)

							addLabel("Actions: ").labelAlign(Align.right).growX()

							textButton("cancel") {
								currentElement = null
							}
							textButton("shrink") {
								fun Element.deepShrink() {
									setSize(0f, 0f)
									if (this is Group) children.each { it.deepShrink() }
								}
								currentElement?.deepShrink()
								currentElement?.pack()
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

				addPage("other properties") {
					lateinit var table: Table

					addTable {
						defaults().pad(5f)

						addLabel(Bundles.showIn)
						textButton(Bundles.inThisTable) {
							PropertyListFragment.apply(table)
						}
						textButton(Bundles.inWindow) {
							PropertyListFragment.showInWindow()
						}
					}.row()

					addTable {
						table = this
					}
				}

				addPage("hierarchy") {
					addTable {
						addLabel("Parent: ").growX()
						addLabel({ currentElement?.parent.elementToString() }, wrap = false).growX().labelAlign(Align.left)

						textButton("select") {
							currentElement = currentElement!!.parent
							updateHierarchyTable()
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
							scrollPane {
								hierarchyTable = this
							}.growX()
						}.grow()
					}.growX()
				}
			}.growX().minSize(150f, 150f)
		}
	}

	/** Adds a toggle option. */
	private inline fun Table.toggleOption(label: String, crossinline getter: () -> Boolean, crossinline setter: (Boolean) -> Unit) = run {
		customButton({
			addLabel(label).labelAlign(Align.left).growX()
			addLabel({ if (getter()) Bundles.enabled else Bundles.disabled }).labelAlign(Align.right)
		}, Styles.togglet) {
			setter(!getter())
		}.update {
			it.isChecked = getter()
		}.pad(2f).growX()
	}

	/** Adds a slider option */
	private inline fun Table.sliderOption(text: String, min: Float, max: Float, step: Float, crossinline getter: () -> Float, setter: Floatc) = run {
		lateinit var slider: Slider
		addStack(
			Slider(min, max, step, false).also {
				slider = it
				it.fillParent = true
				it.moved(setter)
				it.update {
					if (!it.isDragging) it.value = getter()
				}
			},
			createTable {
				defaults().pad(5f)
				touchable = Touchable.disabled

				addLabel("$text: ").labelAlign(Align.left).growX()
				addLabel({ Strings.autoFixed(slider.value, 2) })
			}
		).pad(2f).growX()
	}

	/**
	 * Adds several multiple property fields based on the providen [Pair]s.
	 * Each pair of strings represents the following: (label, property name).
	 */
	private inline fun <reified O: Any, T> Table.properties(
		noinline objProvider: () -> O?,
		noinline converter: (String) -> T,
		crossinline condition: () -> Boolean,
		vararg fields: Pair<String, String>
	) {
		fields.forEachIndexed { index, field ->
			addLabel(field.first)
			propertyField(objProvider, O::class.mutablePropertyFor(field.second), converter).also {
				it.get().lockProvider { !condition() }
			}

			if (index % 2 == 1) row()
		}

		row()
	}

	/**
	 * Updates the hierarchy table.
	 * Does nothing if the fragment hasn't been built yet or there's no selected element.
	 */
	private fun updateHierarchyTable() {
		val element = currentElement ?: return

		with(hierarchyTable ?: return) {
			clearChildren()
			defaults().pad(3f).marginBottom(2f).growX()

			if (element is Group) {
				var index = 1 // no forEachIndexed.
				element.children.each { child ->
					textButton("${index++}. ${child.elementToString()}") {
						currentElement = child
						updateHierarchyTable()
					}.color(Pal.accent).row()
				}
			}
		}
	}

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
	 * Adds element selection listeners, overriding the previous ones.
	 * Use this to remove anything that can obscure the user's view and then show it again.
	 */
	fun onElementSelection(begin: (() -> Unit)?, end: (() -> Unit)?) {
		selectionBeginListener = begin
		selectionEndListener = end
	}

	/**
	 * Highlights [lastSelectedElement] when active.
	 */
	private class ElementHighlighterService : Service() {
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

	/**
	 * Checks if [currentElement] is valid and resets it if it is not.
	 */
	private class ElementInvalidatorService : Service() {
		override fun update() {
			if (currentElement == null) return

			run valid@ {
				var parent = currentElement?.parent
				while (parent != Core.scene.root) {
					if (parent == null) return@valid
					parent = parent.parent
				}
				return
			}

			currentElement = null
			elementInfoTable.actions(
				color(Color.red, 0f), color(Color.white, 5f, Interp.sineOut)
			)
		}
		override fun draw() {}
	}

	/**
	 * When the user touches the element this listener is applied to,
	 * [targetElement] is made non-touchable, it's checked which element is located
	 * at the position of touch, the target element is made touchable again
	 * and then the touched element is assigned to either [lastSelectedElement] or [currentElement].
	 */
	class SelectionInputListener(val targetElement: Element) : InputListener() {
		override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
			val element = elementAt(x, y)

			if (lastSelectedElement == element && element != null) {
				currentElement = element
				elementSelectionFinish()
			} else {
				lastSelectedElement = element
			}

			return true
		}

		override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
			lastSelectedElement = elementAt(x, y)
		}

		private fun elementAt(x: Float, y: Float): Element? {
			val isVisible = targetElement.visible
			targetElement.visible = false

			val coords = targetElement.localToStageCoordinates(Tmp.v1.set(x, y))

			return Core.scene.root.hit(coords.x, coords.y, true).also {
				targetElement.visible = isVisible
			}
		}
	}
}
