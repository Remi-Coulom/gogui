#-----------------------------------------------------------------------------
# Tests for GmpToGtp.
# Run with: gmptogtp -color black "gnugo --mode gmp" (GNU Go version 3.6)
#
# $Id$
# $Source$
#-----------------------------------------------------------------------------

10 name
#? [GmpToGtp]

20 protocol_version
#? [2]

30 version

40 list_commands

50 gogui_interrupt

60 gogui_title
#? [Go Modem .*]

70 boardsize 19

80 clear_board

90 genmove b
#? [[A-T]1*[1-9]]

100 play white pass

110 genmove black
#? [[A-T]1*[1-9]]

120 undo

# GNU Go 3.6 immediately sends a new black move after undoing one move

130 genmove black
#? [[A-T]1*[1-9]]

140 play w pass

150 genmove black
#? [[A-T]1*[1-9]]

# Expected failure. GNU Go cannot handle talk text and terminates.
160 gmp_talk Test talk
#? []*
