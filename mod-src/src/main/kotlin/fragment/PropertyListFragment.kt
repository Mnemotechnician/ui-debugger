package com.github.mnemotechnician.uidebugger.fragment

import arc.graphics.Color
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.layout.Table
import arc.util.Align
import com.github.mnemotechnician.mkui.extensions.dsl.*
import com.github.mnemotechnician.mkui.windows.Window
import com.github.mnemotechnician.mkui.windows.WindowManager
import com.github.mnemotechnician.uidebugger.element.propertyElement
import com.github.mnemotechnician.uidebugger.service.Service
import com.github.mnemotechnician.uidebugger.service.ServiceManager
import com.github.mnemotechnician.uidebugger.util.*
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.graphics.Pal

/**
 * An extension for [DebuggerMenuFragment].
 * Allows to modify properties not listed in the former.
 */
object PropertyListFragment : Fragment<Table, Table>() {
	var currentObject: Any? = null
		set(value) {
			field = value
			if (value != null) rebuild(value::class.java)
		}

	private lateinit var hierarchyRoot: Table
	private var window: Window? = null

	init {
		ServiceManager.start(CompanionService())
	}

	override fun build(): Table = createTable {
		addTable(Tex.button) {
			addTable {
				left()
				addLabel(Bundles.currentObject + ": ")
				addLabel({ currentObject?.simpleName() ?: "null" }, wrap = false)
				addLabel(" (")
				addLabel({ currentObject.toString().substringBefore('\n') }, wrap = false)
				addLabel(")")
			}.growX()

			textButton(Bundles.resetToElement) {
				currentObject = DebuggerMenuFragment.currentElement
			}.update {
				it.setColor(if (DebuggerMenuFragment.currentElement != null) Color.white else Pal.redLight)
			}
		}.color(Pal.darkestGray).pad(5f).row()

		scrollPane() {
			addCollapser({ currentObject != null }) {
				hierarchyRoot = this
			}
		}.growY()
	}

	override fun applied() {
		window?.destroy()
		window = null
	}

	/**
	 * Rebuilds the property list.
	 */
	private fun rebuild(forClass: Class<*>) {
		hierarchyRoot.clearChildren()
		forClass.getHierarchy().forEach {
			hierarchyRoot.add(FieldCategory(it)).fillX().row()
		}
	}

	/**
	 * Shows this fragment in a window.
	 */
	fun showInWindow() {
		val newWindow = object : Window() {
			override val name = Bundles.propertyList
			override val closeable = true
			override fun onCreate() {
				apply(table)
			}
		}
		WindowManager.createWindow(newWindow)
		window = newWindow
	}

	/**
	 * A category containing the fields and methods of the given class.
	 */
	class FieldCategory(val cls: Class<*>) : Table() {
		var isShown = false

		init {
			name = cls.canonicalName

			addTable {
				left()
				addLabel(cls.toString()).labelAlign(Align.left).pad(5f).growX()
				addImage({ if (isShown) iconUp else iconDown })
					
				hsplitter(padTop = 3f, padBottom = 7f)

				clicked {
					isShown = !isShown
				}
			}.growX().row()

			addCollapser({ isShown }, animate = true) {
				if (cls.declaredFields.isNotEmpty()) {
					cls.declaredFields.forEach { field ->
						propertyElement(
							{ currentObject },
							field.createMutableProperty(),
							field.type as Class<out Any>
						).fillX()

						imageButton(Icon.right) {
							currentObject?.let { field.get(it) }?.let { currentObject = it }
						}.disabled { _ -> currentObject == null || field.get(currentObject!!) == null }

						row()
					}
				} else {
					addLabel(Bundles.classHasNoMembers)
				}
			}

		}

		override fun act(delta: Float) {
			super.act(delta)
			background = if (isShown) Tex.underline else Tex.underlineDisabled
		}

		companion object {
			val iconUp = TextureRegionDrawable(Icon.up)
			val iconDown = TextureRegionDrawable(Icon.down)
		}
	}

	// todo might be utterly useless
	/**
	 * Utility service for [PropertyListFragment].
	 */
	class CompanionService : Service() {
		override fun update() {
			if (!isApplied) return
		}

		override fun draw() {
		}
	}
}
