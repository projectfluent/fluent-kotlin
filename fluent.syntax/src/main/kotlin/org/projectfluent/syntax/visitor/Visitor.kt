package org.projectfluent.syntax.visitor

import org.projectfluent.syntax.ast.BaseNode
import java.io.InvalidClassException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

/**
 * Generic Visitor base class.
 *
 * Implement public visitNodeType methods to add handling to AST node types,
 * e.g. `visitResource(node: Resource)` to handle the `Resource` node type.
 */
abstract class Visitor {

    init {
        handlers(this::class)
    }

    /**
     * Primary entry point for all visitors.
     *
     * This is the method you want to call on concrete visitor implementations.
     */
    fun visit(node: BaseNode) {
        val handler = handlers(this::class)[node::class.jvmName]
        if (handler != null) {
            handler.invoke(this, node)
        } else {
            this.genericVisit(node)
        }
    }

    /**
     * From concrete `visitNodeType` implementations, call this
     * method to continue iteration into the AST if desired.
     */
    fun genericVisit(node: BaseNode) {
        node.properties().forEach { (_, value) ->
            when (value) {
                is BaseNode -> this.visit(value)
                is Collection<*> -> value.filterIsInstance<BaseNode>().map { this.visit(it) }
            }
        }
    }

    private companion object {
        private val handlersReflectionCache = ConcurrentHashMap<String, Map<String, (Visitor, BaseNode) -> Unit?>>()

        private fun handlers(clazz: KClass<out Visitor>) =
            handlersReflectionCache.getOrPut(
                clazz.jvmName,
                {
                    clazz.java.declaredMethods
                        .filter { it.name.startsWith("visit") && it.kotlinFunction?.visibility == KVisibility.PUBLIC }
                        .filter { it.parameterCount == 1 && it.parameterTypes[0].kotlin.isSubclassOf(BaseNode::class) }
                        .filter { method ->
                            val argumentClassName = method.parameterTypes[0].kotlin.simpleName
                            val expectedMethodName = "visit$argumentClassName"
                            if (method.name == expectedMethodName) {
                                true
                            } else {
                                throw InvalidClassException("Method '${clazz.simpleName}.${method.name}($argumentClassName)' must be called '$expectedMethodName', or must not be public.")
                            }
                        }
                        .associate {
                            Pair(
                                it.parameterTypes[0].kotlin.jvmName,
                                { visitor: Visitor, node: BaseNode -> it.invoke(visitor, node) as Unit? }
                            )
                        }
                }
            )
    }
}
