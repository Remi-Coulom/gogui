GoGui
=====

GoGui is a graphical interface to board-game AI software, using the [GTP
protocol](http://www.lysator.liu.se/~gunnar/gtp/). GoGui and GTP were designed
for Go programs, but the protocol was
[extended](https://www.kayufu.com/gogui/rules.html) to handle any board game.

Besides the basic ability of visualizing the board, saving game records in sgf
format, and letting humans play against the AI, GoGui offers many interesting
features for AI developers:

 - A GTP engine can define
[analyze commands](https://www.kayufu.com/gogui/analyze.html). This will
extend GoGui with new custom commands that can be used to display information
graphically on the board, or open dialog boxes to set engine parameters.
 - Tools such as [gogui-twogtp](https://www.kayufu.com/gogui/reference-twogtp.html) can be used to run matches between two programs and collect statistics.

Screenshots
-----------

Policy visualization with analyze commands:

![gogui_policy](https://www.kayufu.com/files/go_policy.png)

Chess engine displaying its board:

![gogui_chess](https://www.kayufu.com/files/gogui_chess.jpg)

Download and Install
--------------------

[Link to the latest release](https://github.com/Remi-Coulom/gogui/releases/latest).

Starting from version 1.5.2, GoGui is distributed as a zip file containing its
source code as well as the compiled jars. In order to run GoGui, a [Java
Runtime Environment](https://www.java.com/) must be installed on your machine.
The jars are located in the lib subdirectory. For linux, executable scripts are
located in the bin subdirectory. Your operating system may make you jump
through hoops to let you execute the code because of the security risk of
running software downloaded from the internet.

Note
----
GoGui was initially developed by [Markus Enzenberger](https://github.com/enz), and hosted on [sourceforge](http://gogui.sourceforge.net/).
