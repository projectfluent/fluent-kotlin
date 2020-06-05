package org.projectfluent.syntax.ast

import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

abstract class Visitor {
    val handlers: MutableMap<String, Method> = mutableMapOf()
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
