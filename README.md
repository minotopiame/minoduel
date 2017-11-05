MinoDuel
========

MinoDuel is a plugin for the MinoTopia Minecraft server that allows players to fight
each other (in pairs) in a fair way. That means standardised maps and kits. Players
interact with the plugin through an inventory GUI or via the full-featured CLI.

**Note:** This plugin has been created a long time ago and hasn't seen much refactoring since.
The main branch is `arena-object` for historical reasons.

Further be aware that this project optionally depends on
[MinoTopiaCore](https://github.com/xxyy/minotopiacore) as well as
[xLogin](https://bitbucket.org/xxyy/xlogin) and may not work perfectly without these,
since it has only ever run in Production with all dependencies present.

Compilation
===========

This project uses Apache Maven for compilation:

````bash
mvn clean install
````

The artifacts are placed in `target/minoduel-*.jar`.

License
=======
This project is licensed under a MIT license, a copy of which can be found in the `LICENSE` file.
