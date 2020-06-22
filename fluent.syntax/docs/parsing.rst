Parsing
=======

.. code-block:: kotlin

   package org.projectfluent.syntax.parser

   import org.projectfluent.syntax.ast.Resource

   /**
    * Parse Fluent resources.
    *
    * @property withSpans create source positions for nodes (not supported yet)
    */
   class FluentParser(var withSpans: Boolean = false) {

       fun parse(source: String): Resource {
       }
