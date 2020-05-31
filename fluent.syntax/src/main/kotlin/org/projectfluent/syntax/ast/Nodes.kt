package org.projectfluent.syntax.ast

import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 * Base class for all Fluent AST nodes.
 *
 * All productions described in the ASDL subclass BaseNode, including Span and
 * Annotation.
 *
 */
abstract class BaseNode {
    fun equals(other: BaseNode) : Boolean {
        if (this::class != other::class) return false
        val other_members = hashMapOf<String, Any?>()
        other::class.memberProperties.forEach {
            if (it.visibility == KVisibility.PUBLIC) {
                other_members[it.name] = it.getter.call(other)
            }
        }
        this::class.memberProperties.forEach {
            if (it.name in other_members) {
                val value = it.getter.call(this)
                if (value != other_members[it.name]) return false
            }
        }
        return true
    }
}

/**
 * Base class for AST nodes which can have Spans.
 */
abstract class SyntaxNode() : BaseNode()

data class Message(var id: String, var value: String?) : SyntaxNode()

data class Term(var id: String, var value: String) : SyntaxNode()
