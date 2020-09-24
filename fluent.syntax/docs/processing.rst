Processing
==========

.. code-block:: kotlin

   package org.projectfluent.syntax.processor

   import org.projectfluent.syntax.ast.*

   /**
    * Process patterns by returning new patterns with elements transformed.
    */
   class Processor {
       /**
        * "Bake" the values of StringLiterals into TextElements. This is a lossy
        * transformation for literals which are not special in Fluent syntax.
        */
       fun unescapeLiteralsToText(pattern: Pattern): Pattern

       /**
        * "Un-bake" special characters into StringLiterals, which would otherwise
        * cause syntax errors with Fluent parsers.
        */
       fun escapeTextToLiterals(pattern: Pattern): Pattern
   }
