package com.github.mnemotechnician.uidebugger.element

import arc.graphics.Color
import arc.math.geom.Vec2
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Align
import com.github.mnemotechnician.mkui.extensions.dsl.*
import com.github.mnemotechnician.mkui.extensions.elements.scaleFont
import com.github.mnemotechnician.uidebugger.element.PropertyElement.Companion.modifiers
import com.github.mnemotechnician.uidebugger.util.Bundles
import com.github.mnemotechnician.uidebugger.util.createMutableProperty
import mindustry.graphics.Pal
import mindustry.ui.Styles
import kotlin.reflect.KMutableProperty1

/**
 * Note: the receiver of KMutableProperty and the output of objProvider must be of the same class.
 */
typealias InputConstructor<T> = Table.(KMutableProperty1<Any, T>, objProvider: () -> Any?) -> Unit
/** Same as [InputConstructor] but accepts a type argument */
typealias InputConstructorTyped<T> = Table.(KMutableProperty1<Any, T>, objProvider: () -> Any?, type: Class<T>) -> Unit

/**
 * A UI element that allows the user to modify a property depending on its type.
 * This element heavily relies on reflection and, due to the specifics of the JVM,
 * it can be very inefficient in some cases.
 *
 * For instance, it provides an input field for numbers and strings and a toggle for booleans.
 *
 * Support for new classes can be added by adding an entry to [modifiers].
 *
 * @param objProvider provides an instance of object that this element represents. Called repeatedly.
 * @param property the property this class represents.
 * @param propertyType this Class defines which way will be used to represent the property. Must be assignable from [T].
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
class PropertyElement<T, O: Any>(
	private val objProvider: () -> O?,
	private val property: KMutableProperty1<O, T>,
	propertyType: Class<out T>
) : Table() {
	init {
		// I'm not adding the reflection api and without it there may be an exception...
		val isConst = try { property.isConst } catch (e: Throwable) { false }

		addTable {
			left().defaults().fillX()
			addLabel(property.name).labelAlign(Align.left).row()
			addLabel(propertyType.toString().substringAfterLast('.')).labelAlign(Align.left).color(Color.gray)

			if (isConst) {
				addLabel(Bundles.constant).labelAlign(Align.left).color(Pal.darkestGray)
			}
		}.pad(5f)

		defaults().expandX().right()

		// GOSH I HATE JVM GENERICS
		val prop = property as KMutableProperty1<Any, Any?>
		val type = propertyType as Class<Any?>
		when {
			isConst -> fallbackModifier(property, objProvider)
			propertyType.isEnum -> enumModifier(prop, objProvider, type)
			else -> inputConstructorFor(type)(prop, objProvider)
		}

	}

	/**
	 * Creates a [PropertyElement] representing a field of [propertyClass],
	 * finding the said field by its name.
	 */
	constructor(objProvider: () -> O, propertyClass: Class<T>, propertyName: String)
		: this(objProvider, propertyClass.getDeclaredField(propertyName).createMutableProperty<T, O>(), propertyClass)

	companion object {
		// lots of ctrl+c ctrl+v...
		val charSeqModifier: InputConstructor<CharSequence?> = { prop, prov -> propertyField(prov, prop, { it }) }

		val intModifier: InputConstructor<Int?> = { prop, prov -> propertyField(prov, prop, { it.toInt() }) }

		val longModifier: InputConstructor<Long?> = { prop, prov -> propertyField(prov, prop, { it.toLong() }) }

		val floatModifier: InputConstructor<Float?> = { prop, prov -> propertyField(prov, prop, { it.toFloat() }) }

		val doubleModifier: InputConstructor<Double?> = { prop, prov -> propertyField(prov, prop, { it.toDouble() }) }

		val booleanModifier: InputConstructor<Boolean?> = { prop, prov ->
			textButton({ prov()?.let { prop.get(it).toString() } ?: "N / A" }, Styles.togglet) {
				val instance = prov() ?: return@textButton
				prop.set(instance, !(prop.get(instance) ?: return@textButton))
			}.checked { prop[prov] ?: false }
		}
		
		val colorModifier: InputConstructor<Color?> = { prop, prov ->
			propertyField(prov, prop, {
				Color.valueOf(prop[prov] ?: Color(), it)
			}).row()
		}

//		val arrayModifier: InputConstructor<Array<*>?> = { prop, prov ->
//			addCollapser({ prov()?.let { prop.get(it) } != null}) {
//				addLabel(Bundles.chooseElement + " (0..")
//				addLabel({ prov()?.let { prop.get(it) }?.size.toString() }) // element count
//				addLabel(") ")
//
//				lateinit var container: Table
//				textField() {
//					val index = it.toIntOrNull()
//					val array = prop[prov] ?: return@textField
//
//					if (index == null || index < 0 || index >= array.size) {
//						actions(color(Color.red), color(Color.white, 3f, Interp.sineIn))
//					} else {
//						container.clearChildren()
//						array[index]?.let {
//							inputConstructorFor(it::class.java)()
//						} ?: let {
//							container.addLabel(Bundles.elementIsNull)
//						}
//					}
//				}.growX().get().also {
//					it.hint = Bundles.index
//				}
//				row()
//				container = addTable().get()
//			}
//		}

		val vec2Modifier: InputConstructor<Vec2?> = { prop, prov ->
			addLabel("x ")
			propertyField({ prop[prov] }, Vec2::x, { it.toFloat() })
			addLabel(", y ")
			propertyField({ prop[prov] }, Vec2::y, { it.toFloat() })
		}

		val fallbackModifier: InputConstructor<Any?> = { prop, prov ->
			addLabel({ prov()?.let { prop.get(it).toString().substringBefore('\n') } ?: "N / A" }, wrap = true).scaleFont(0.6f)
		}

		/**
		 * A special modifier for enums.
		 */
		val enumModifier: InputConstructorTyped<Any?> = { prop, prov, type ->
			addLabel({ prop[prov].toString() }).growX()
			val toggle = textToggle(Bundles.change).get()

			row().addCollapser({ toggle.isEnabled }, animate = true) {
				type.enumConstants.forEachIndexed { index, value ->
					textButton(value.toString()) {
						prop[prov] = value
						toggle.isEnabled = false
					}.margin(5f)
					if (index % 2 == 1) row()
				}
			}
		}

		/**
		 * Maps property types to lambdas that build input elements for them.
		 */
		val modifiers = mutableMapOf(
			CharSequence::class.java to charSeqModifier,
			Int::class.java to intModifier,
			Long::class.java to longModifier,
			Float::class.java to floatModifier,
			Double::class.java to doubleModifier,
			Boolean::class.java to booleanModifier,
			Color::class.java to colorModifier,
			Vec2::class.java to vec2Modifier,
			Any::class.java to fallbackModifier
		) as MutableMap<out Class<*>, out InputConstructor<*>>

		/**
		 * Tries to find an input constructor in [modifiers] that matches the specified class.
		 * If there's no such constructor, it returns the most applicable one.
		 */
		fun <T> inputConstructorFor(cls: Class<T>): InputConstructor<T> {
			return (
				modifiers.getOrDefault(cls, null)
					?: modifiers.entries.find { it.key.isAssignableFrom(cls) }?.value
					?: fallbackModifier
			) as InputConstructor<T>
		}

		private operator fun <T> KMutableProperty1<Any, T>.get(prov: () -> Any?) = prov()?.let { get(it) }

		private operator fun <T> KMutableProperty1<Any, T>.set(prov: () -> Any?, value: T) = prov()?.let { set(it, value) }
	}
}

/**
 * Adds a [PropertyElement] to the table.
 * @see PropertyElement
 */
fun <T, O: Any> Table.propertyElement(
	objProvider: () -> O?,
	property: KMutableProperty1<O, T>,
	propertyType: Class<T>
): Cell<PropertyElement<T, O>> = add(PropertyElement(objProvider, property, propertyType))
