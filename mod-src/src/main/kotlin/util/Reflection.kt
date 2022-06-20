package com.github.mnemotechnician.uidebugger.util

import kotlin.reflect.*

/**
 * Utility: creates a KMutableProperty for an arbitrary java field.
 * Used in cases when the kotlin compiler fails to resolve a class::field reference.
 *
 * The returned property is only a partial implementation, which doesn't support all features.
 *
 * @throws NoSuchFieldException if the field doesn't exist.
 * @throws ClassCastException (when accessed) if the field and [T] have different types.
 * @throws NotImplementedError when a non-implemented feature is used.
 */
fun <T, O: Any> KClass<O>.mutablePropertyFor(fieldName: String): KMutableProperty1<O, T> {
	val javaField = java.getDeclaredField(fieldName)
	javaField.isAccessible = true

	return object : KMutableProperty1<O, T> {
		override val annotations get() = listOf<Annotation>()
		override val getter get() = throw NotImplementedError()
		override val isAbstract get() = false
		override val isConst get() = false
		override val isFinal get() = false
		override val isLateinit get() = false
		override val isOpen get() = false
		override val isSuspend get() = false
		override val name get() = fieldName
		override val parameters get() = listOf<KParameter>()
		override val returnType get() = throw NotImplementedError()
		override val setter get() = throw NotImplementedError()
		override val typeParameters get() = listOf<KTypeParameter>()
		override val visibility get() = throw NotImplementedError()

		override fun call(vararg args: Any?) = throw NotImplementedError()

		override fun callBy(args: Map<KParameter, Any?>) = throw NotImplementedError()

		override fun getDelegate(receiver: O) = throw NotImplementedError()

		override fun get(receiver: O) = javaField.get(receiver) as T

		override fun invoke(receiver: O) = get(receiver)

		override fun set(receiver: O, value: T) {
			javaField.set(receiver, value)
		}
	}
}