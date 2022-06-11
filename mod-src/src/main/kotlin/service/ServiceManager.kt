package com.github.mnemotechnician.uidebugger.service

import arc.Events
import mindustry.game.EventType

/**
 * Manages mindustry services, allowing to start and stop them.
 *
 * Only one instance of a specific service can be running at the same time.
 */
object ServiceManager {
	private val services = HashSet<Service>(10)
	private lateinit var updateListener: Runnable
	private lateinit var drawListener: Runnable

	init {
		createListeners()
	}

	private fun createListeners() {
		updateListener = Runnable {
			services.forEach { it.update() }
		}.also { Events.run(EventType.Trigger.update, it) }

		drawListener = Runnable {
			services.forEach { it.draw() }
		}.also { Events.run(EventType.Trigger.uiDrawEnd, it) }
	}

	/**
	 * Starts a service if it hasn't been started yet.
	 */
	fun start(service: Service) {
		services.add(service)
		service.start()
	}

	/**
	 * Stops a service by its class.
	 * Does nothing if the service hasn't been started.
	 */
	fun stop(service: Class<out Service>) {
		services.removeAll { instance ->
			service.isInstance(instance).also {
				if (it) instance.stop()
			}
		}
	}

	/**
	 * Checks whether a service is active.
	 */
	fun isActive(service: Class<out Service>) = services.any { it::class == service }
}
