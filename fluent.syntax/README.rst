``fluent.syntax`` |fluent.syntax|
---------------------------------

Read, write, and transform `Fluent`_ files.

This package includes the parser, serializer, and traversal
utilities like Visitor. You’re looking for this package
if you work on tooling for Fluent in Kotlin or Java.

.. code-block:: kotlin

   import org.projectfluent.syntax.parser.FluentParser
   import org.projectfluent.syntax.serializer.FluentSerializer
   val parser = FluentParser()
   val resource = parser.parse("a-key = String to localize")
   println(resource.body.id.name)
   val serializer = FluentSerializer()
   println(serializer.serialize(resource))
   println(serializer.serialize(resource.body[0))


.. _fluent: https://projectfluent.org/
.. |fluent.syntax| image:: https://github.com/Pike/fluent-kotlin/workflows/fluent.syntax/badge.svg
