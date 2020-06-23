Using syntax
============

The ``org.projectfluent:syntax`` package provides a parser, a serializer, and libraries
for analysis and processing of Fluent files.

Parsing
-------

To parse a full resource, you can use the :py:class:`syntax.parser.Parser`:

.. code-block:: kotlin

   import org.projectfluent.syntax.parser.FluentParser
   val parser = FluentParser()
   val resource = parser.parse("a-key = String to localize")


Serialization
-------------

To create Fluent syntax from AST objects, use
:py:class:`syntax.serializer.FluentSerializer`.

.. code-block:: kotlin

   import org.projectfluent.syntax.serializer.FluentSerializer
   val serializer = FluentSerializer()
   serializer.serialize(resource)
   serializer.serialize(resource.body[0])

Analysis (Visitor)
------------------

To analyze an AST tree in a read-only fashion, you can subclass
:py:class:`syntax.visitor.Visitor`.

You overload individual :py:func:`visitNodeName` methods to
handle nodes of that type, and then call into :py:func`self.genericVisit`
to continue iteration.

.. code-block:: kotlin

   import org.projectfluent.syntax.ast.TextElement
   import org.projectfluent.syntax.visitor.Visitor
   class TestableVisitor : Visitor() {
       var wordCount = 0
       val WORDS = Regex("\\w+")
       fun visitTextElement(node: TextElement) {
           wordCount += WORDS.findAll(node.value).count()
       }
   }

Custom Traversal (childrenOf)
-----------------------------

The Python and TypeScript implementations feature a ``Transformer``
pattern, which allows to process and transform an AST in-place. Those
implementations rely heavily on dynamic typing to allow the return of
different types, which doesn't work in Kotlin. Thus, transformations of
syntax trees need to be written manually, but there's a helper to
iterate over children.

.. code-block:: kotlin

   val variant = Variant(Identifier("other"), Pattern(), true)
   val variant_props = childrenOf(variant)
   assertEquals(
       listOf("default", "key", "span", "value"),
       variant_props.map { (name, _) -> name }.sorted().toList()
   )
