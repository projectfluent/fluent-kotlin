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
abstract class SyntaxNode() : BaseNode() {
    var span: Span? = null
    fun addSpan(start: Int, end: Int) {
        this.span = Span(start, end)
    }
}

/**
 * A Fluent file representation
 */
class Resource : SyntaxNode {
    constructor(vararg children:TopLevel) : super() {
        this.body += children
    }
    val body: MutableList<TopLevel> = mutableListOf()
}

abstract class TopLevel : SyntaxNode()

/**
 * An abstract base class for useful elements of Resource.body.
 */
abstract class Entry : TopLevel()

data class Message(var id: Identifier, var value: Pattern?) : Entry() {
    var attributes: MutableList<Attribute> = mutableListOf()
    var comment: Comment? = null
}

data class Term(var id: Identifier, var value: Pattern) : Entry() {
    var attributes: MutableList<Attribute> = mutableListOf()
    var comment: Comment? = null
}

class Pattern : SyntaxNode {
    val elements: MutableList<PatternElement> = mutableListOf()
    constructor(vararg elements: PatternElement) : super() {
        this.elements += elements
    }
}

abstract class PatternElement : SyntaxNode()

data class TextElement(var value:String): PatternElement()

data class Placeable (var expression: SyntaxNode): PatternElement()

abstract class Expression : SyntaxNode()

abstract class Literal(val value:String) : Expression()

class StringLiteral : Literal {
    constructor(value: String) : super(value)
}

class NumberLiteral : Literal {
    constructor(value: String) : super(value)
}

data class Attribute(var id:Identifier, var value: Pattern): SyntaxNode()

data class Identifier(var name: String): SyntaxNode()

abstract class BaseComment(var content:String) : Entry()

class Comment : BaseComment {
    constructor(content: String) : super(content)
}

class GroupComment : BaseComment {
    constructor(content: String) : super(content)
}

class ResourceComment : BaseComment {
    constructor(content: String) : super(content)
}

data class Junk(val content: String) : TopLevel() {
    val annotations: MutableList<Annotation> = mutableListOf()
    fun addAnnotation(annotation: Annotation) {
        this.annotations.add(annotation)
    }
}

/**
 * Represents top-level whitespace
 *
 * Extension of the data model in other implementations.
 */
data class Whitespace(val content: String) : TopLevel()

data class Span(var start: Int, var end: Int) : BaseNode()

data class Annotation(var code: String, var message: String) : SyntaxNode() {
    val args: MutableList<Any> = mutableListOf()
}
