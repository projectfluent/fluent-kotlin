package org.projectfluent.syntax.ast

import java.lang.reflect.Method
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 * Iterate over the properties of a node.
 *
 * Use this method if you want to control deep inspection
 * of an AST tree yourself.
 */
fun childrenOf(node: BaseNode) = sequence {
    node::class.memberProperties.forEach { prop ->
        if (prop.visibility == KVisibility.PUBLIC) {
            yield(Pair(prop.name, prop.getter.call(node)))
        }
    }
}

abstract class Visitor {
    private val handlers: MutableMap<String, Method> = mutableMapOf()
    init {
        this::class.java.declaredMethods.filter {
            it.name.startsWith("visit")
        }.map {
            handlers[it.name.substring("visit".length)] = it
        }
    }
    fun visit(node: BaseNode) {
        val cName = node::class.java.simpleName
        val handler = this.handlers[cName]
        if (handler != null) {
            handler.invoke(this, node)
        } else {
            this.genericVisit(node)
        }
    }
    fun genericVisit(node: BaseNode) {
        childrenOf(node).map { (_, value) -> value }.forEach { value ->
            when (value) {
                is BaseNode -> this.visit(value)
                is Collection<*> -> value.filterIsInstance<BaseNode>().map { this.visit(it) }
            }
        }
    }
}
