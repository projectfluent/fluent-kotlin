Visitor & childrenOf
====================

.. code-block:: kotlin

   package org.projectfluent.syntax.visitor

   import org.projectfluent.syntax.ast.BaseNode

   /**
    * Iterate over the properties of a node.
    *
    * Use this method if you want to control deep inspection
    * of an AST tree yourself.
    */
   fun childrenOf(node: BaseNode) = sequence<Pair<String, Any?>>


   /**
    * Generic Visitor base class.
    *
    * Implement public visitNodeType methods to add handling to AST node types,
    * e.g. `visitResource(node: Resource)` to handle the `Resource` node type.
    */
   abstract class Visitor {

       /**
        * Primary entry point for all visitors.
        *
        * This is the method you want to call on concrete visitor implementations.
        */
       fun visit(node: BaseNode)

   
       /**
        * From concrete `visitNodeType` implementations, call this
        * method to continue iteration into the AST if desired.
        */
       fun genericVisit(node: BaseNode)
   }
