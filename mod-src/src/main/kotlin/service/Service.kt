package com.github.mnemotechnician.uidebugger.service

/**
 * Represents a mindustry service.
 *
 * Service instances are not unique, and two instances of the same service are considered equal.
 */
abstract class Service {
	/**
	 * Whether this service is active.
	 */
	val isActive get() = ServiceManager.isActive(this::class.java)

	/**
	 * Called when the service is started
	 */
	open fun start() {}

	/**
	 * Called when the service is stopped
	 */
	open fun stop() {}

	/**
	 * Called on every tick when this service is active.
	 */
	abstract fun update()

	/**
	 * Called during the drawing period.
	 */
	abstract fun draw()

	/**
	 * Returns true if the instances belong to the same service class.
	 */
	final override fun equals(other: Any?) = other != null && other::class == this::class

	/**
	 * Returns the has code of this service class.
	 */
	final override fun hashCode() = this::class.hashCode()
}
