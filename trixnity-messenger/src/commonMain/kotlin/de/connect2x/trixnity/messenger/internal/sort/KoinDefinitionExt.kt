package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import org.koin.core.definition.KoinDefinition
import org.koin.core.qualifier.TypeQualifier

@TrixnityMessengerPrivateApi
fun <T : Any> KoinDefinition<out T>.sorted(builder: SortableScope<T>.() -> Unit): KoinDefinition<out T> {
    val clazz = factory.beanDefinition.primaryType

    module.single<SortedMetadata<*>>(qualifier = TypeQualifier(clazz), definition = { SortedMetadata(clazz, builder) })

    return this
}
