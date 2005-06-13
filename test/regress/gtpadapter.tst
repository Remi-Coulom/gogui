#-----------------------------------------------------------------------------
# Tests for GtpAdapter.
# Run with:
# gtpadapter gtpdummy
#
# $Id$
# $Source$
#-----------------------------------------------------------------------------

# Name should be passed through from GtpDummy
10 name
#? [GtpDummy]

20 protocol_version
#? [2]

30 version

40 list_commands

50 boardsize 19

60 clear_board

70 play black q17

80 genmove w
#? [[A-T]1?[0-9]]

90 genmove black
#? [[A-T]1?[0-9]]
