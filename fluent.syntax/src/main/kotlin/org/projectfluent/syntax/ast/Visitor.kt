package org.projectfluent.syntax.ast

import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

abstract class Visitor {
    val handlers: MutableMap<String, KFunction<*>> = mutableMapOf()
    init {
        this::class.memberFunctions.filter {
            it.name.startsWith("visit_")
        }.map {
            handlers[it.name.substring("visit_".length)] = it
        }
    }
    fun visit(node: BaseNode) {
        val cName = node::class.simpleName
        val handler = this.handlers[cName]
        if (handler != null) {
            handler.call(this, node)
        } else {
            this.generic_visit(node)
        }
    }
    fun generic_visit(node: BaseNode) {
        node::class.memberProperties.forEach {
            if (it.visibility == KVisibility.PUBLIC) {
                val value = it.getter.call(node)
                when (value) {
                    is BaseNode -> this.visit(value)
                    is Collection<*> -> value.filterIsInstance<BaseNode>().map { this.visit(it) }
                }
            }
        }
    }
}
