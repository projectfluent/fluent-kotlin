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
    private val handlers: Map<KClass<BaseNode>, (BaseNode) -> Unit?> by lazy {
        this::class.java.declaredMethods
            .filter { it.name.startsWith("visit") }
            .filter { it.parameterCount == 1 && it.parameterTypes[0].kotlin.isSubclassOf(BaseNode::class) }
            .associate {
                it.parameterTypes[0].kotlin as KClass<BaseNode> to { node: BaseNode -> it.invoke(this, node) as Unit? }
            }
    }

    /**
     * Primary entry point for all visitors.
     *
     * This is the method you want to call on concrete visitor implementations.
     */
    fun visit(node: BaseNode) = (handlers[node::class] ?: this::visitProperties).invoke(node)

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
}
