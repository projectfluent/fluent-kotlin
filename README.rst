Project Fluent
==============

Scott changed this.

This is a collection of Kotlin packages to use the `Fluent localization
system <http://projectfluent.org/>`__.

fluent-kotlin consists of these packages:

``fluent.syntax``
-----------------

The `syntax package <fluent.syntax>`_ includes the parser, serializer, and traversal
utilities like Visitor. You’re looking for this package
if you work on tooling for Fluent in Kotlin or Java.

Contribute
----------

The Kotlin sources in this project use `ktlint <https://ktlint.github.io/>`__.
To run tests and resolve linting errors, run the following commands before submitting
patches.

- ``./fluent.syntax/gradlew test -p './fluent.syntax'``
- ``./fluent.syntax/gradlew ktlintFormat -p './fluent.syntax'``

Discuss
-------

We’d love to hear your thoughts on Project Fluent! Whether you’re a
localizer looking for a better way to express yourself in your language,
or a developer trying to make your app localizable and multilingual, or
a hacker looking for a project to contribute to, please do get in touch
on the mailing list and the Matrix channel.

-  Mozilla Discourse: https://discourse.mozilla.org/c/fluent
-  Matrix channel:
   `#fluent:mozilla.org <https://chat.mozilla.org/#/room/#fluent:mozilla.org>`__

Get Involved
------------

fluent-kotlin is open-source, licensed under the Apache License, Version
2.0. We encourage everyone to take a look at our code and we’ll listen
to your feedback.
