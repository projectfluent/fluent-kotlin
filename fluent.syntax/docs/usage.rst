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


Pattern Processing
------------------

The :py:class:`syntax.processor.Processor` class can be used to transform
patterns in a way that is friendly to localization workflows which want to
allow text characters which are special in Fluent to be written as regular
text.

According to the Fluent syntax, characters like the curly braces must be
enclosed in :py:class:`syntax.ast.StringLiteral` instances if they are
supposed to be part of the translation content. Otherwise, an open curly
brace would start a :py:class:`syntax.ast.Placeable` and likely lead to a
syntax error.

Workflows in which the support for Fluent placeables is limited may choose to
provide their own visual cues for them. This often comes in form of visual
placeholders which can be rearranged within a translation segment by the
translator, but whose contents cannot be modified.

In these workflows, the special meaning of the character like the curly
braces is void; the translator is not able to insert new placeables by
opening a curly brace anyways. Thus, for translators' convenience, the curly
brace can be treated as a regular text character and part of the translation
content.

The :py:class:`syntax.processor.Processor`'s methods allow baking
:py:class:`syntax.ast.StringLiteral` instances into surrounding
:py:class:`syntax.ast.TextElement` instances, and then "un-baking" them again
if required by the Fluent syntax. Note that all string literals are baked,
while only some are un-baked. The processing is a lossy transformation.

.. note::
   Processed patterns are not valid Fluent AST nodes anymore and must not be
   serialized without first un-processing them.


Baking literals into text
^^^^^^^^^^^^^^^^^^^^^^^^^

Use the :py:func:`unescapeLiteralsToText` method to bake the values of string
literals into the surrounding text elements. This is a lossy transformation
for literals which are not special in Fluent syntax.

Examples::

   →Hello, {"{-_-}"}.
   ←Hello, {-_-}.

   →{"     "}Hello, world!
   ←     Hello, world!

   →A multiline pattern:
    {"*"} Asterisk is special
   ←A multiline pattern:
    * Asterisk is special

   →Copyright {"\u00A9"} 2020
   ←Copyright © 2020


Un-baking special characters into literals
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Use the :py:func:`escapeTextToLiterals` method to un-bake special characters
into string literals, which would otherwise cause syntax errors with Fluent
parsers. Character sequences which might have been previously enclosed in
string literals will not be un-baked as long as they are valid text
characters in Fluent syntax.

Examples::

   →Hello, {-_-}.
   ←Hello, {"{"}-_-{"}"}.

   →     Hello, world!
   ←{""}     Hello, world!

   →A multiline pattern:
    * Asterisk is special
   ←A multiline pattern:
    {"*"} Asterisk is special

   →Copyright © 2020
   ←Copyright © 2020
