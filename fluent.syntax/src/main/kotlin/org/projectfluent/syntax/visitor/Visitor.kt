package org.projectfluent.syntax.visitor

import org.projectfluent.syntax.ast.BaseNode
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Generic Visitor base class.
 *
 * Implement public visitNodeType methods to add handling to AST node types,
 * e.g. `visitResource(node: Resource)` to handle the `Resource` node type.
 */
abstract class Visitor {

    /**
     * Primary entry point for all visitors.
     *
     * This is the method you want to call on concrete visitor implementations.
     */
    fun visit(node: BaseNode) {
        val handler = handlers(this::class)[node::class]
        if (handler != null) {
            handler.invoke(this, node)
        } else {
            this.visitProperties(node)
        }
    }

    /**
     * From concrete `visitNodeType` implementations, call this
     * method to continue iteration into the AST if desired.
     */
    fun visitProperties(node: BaseNode) {
        node.properties().forEach { (_, value) ->
            when (value) {
                is BaseNode -> this.visit(value)
                is Collection<*> -> value.filterIsInstance<BaseNode>().map { this.visit(it) }
            }
        }
    }

    private companion object {
        private val handlersReflectionCache =
            mutableMapOf<KClass<out Visitor>, Map<KClass<out BaseNode>, (Visitor, BaseNode) -> Unit?>>()

        private fun handlers(clazz: KClass<out Visitor>) =
            handlersReflectionCache.getOrPut(
                clazz,
                {
                    clazz.java.declaredMethods
                        .filter { it.name.startsWith("visit") }
                        .filter { it.parameterCount == 1 && it.parameterTypes[0].kotlin.isSubclassOf(BaseNode::class) }
                        .associate {
                            Pair(
                                it.parameterTypes[0].kotlin as KClass<out BaseNode>,
                                { visitor: Visitor, node: BaseNode -> it.invoke(visitor, node) as Unit? }
                            )
                        }
                }
            )
    }
}
