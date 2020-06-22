Serializing
===========

.. code-block:: kotlin

   package org.projectfluent.syntax.serializer

   import org.projectfluent.syntax.ast.*
   
   /**
    * Serialize Fluent nodes to `CharSequence`.
    *
    * @property withJunk serialize Junk entries or not.
    */
   class FluentSerializer(var withJunk: Boolean = false) {
       /**
        * Serialize a Resource.
        */
       fun serialize(resource: Resource): CharSequence {
       }
   
       /**
        * Serialize Message, Term, Whitespace, and Junk.
        */
       fun serialize(entry: TopLevel): CharSequence {
       }
   
       /**
        * Serialize an Expression.
        *
        * This is useful to get a string representation of a simple Placeable.
        */
       fun serialize(expr: Expression): CharSequence {
       }
   
       /**
        * Serialize a VariantKey.
        *
        * Useful when displaying the options of a SelectExpression.
        */
       fun serialize(key: VariantKey): CharSequence {
       }
   }
