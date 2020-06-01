package org.projectfluent.syntax.parser

import kotlin.Exception

class ParseError : Exception {
    var code: String = ""
    val args: MutableList<Any> = mutableListOf()

    constructor(code: String, vararg args: Any) {
        this.code = code
        this.args.addAll(args)
    }
}