package com.github.mnemotechnician.uidebugger.fragment

import arc.scene.Element
import arc.scene.Group
import arc.scene.ui.layout.Table

/**
 * Represents a reusable UI fragment.
 *
 * The same fragment can be applied multiple times, however, only one instance of the same fragment
 * can be present in the ui tree at the same time, building the fragment again simply moves that
 * instance.
 *
 * @param T the element type to which this fragment can be applied.
 * @param E the type of this fragment, aka the element this fragment adds to the target group.
 */
abstract class Fragment<T: Group, E: Element> {
	/**
	 * The current location of this fragment.
	 * Null if this fragment hasn't been applied anywhere.
	 */
	private var instance: T? = null

	/** Whether this fragment is applied to any group */
	val isApplied get() = instance != null

	/**
	 * Applies this fragment to the target group, building it if necessary.
	 *
	 * If this fragment is currently applied somewhere else, it will be removed from its current parent.
	 *
	 * @param target the group to which this fragment is applied.
	 * @param fillTarget whether the fragment should fill the target group. Has no effect if the target is a table.
	 */
	fun apply(
		target: T,
		fillTarget: Boolean = true
	) {
		if (instance == null) {
			instance = build()
		}
		val instance = instance!! // please, kotlin, it's definitely not null!

		instance.parent?.let {
			it.removeChild(instance, true)
			it.invalidateHierarchy()
		}
		instance.clearActions()

		// tables are special, addChild() doesn't work with them.
		if (target is Table) {
			target.add(instance).also {
				if (fillTarget) it.grow()
			}
		} else {
			instance.setFillParent(fillTarget)
			target.addChild(instance)
		}

		instance.invalidateHierarchy()
	}

	/**
	 * Builds this fragment. Only called once, when the fragment is applied for the first time.
	 *
	 * @return an element which will be added to the target groups.
	 */
	protected abstract fun build(): T
}
