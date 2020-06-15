package org.projectfluent.syntax.ast

import java.lang.reflect.Method
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

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
        node::class.memberProperties.forEach { prop ->
            if (prop.visibility == KVisibility.PUBLIC) {
                when (val value = prop.getter.call(node)) {
                    is BaseNode -> this.visit(value)
                    is Collection<*> -> value.filterIsInstance<BaseNode>().map { this.visit(it) }
                }
            }
        }
    }
}
