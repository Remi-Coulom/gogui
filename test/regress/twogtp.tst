#-----------------------------------------------------------------------------
# Tests for TwoGtp.
# Run with:
# twogtp -black gtpdummy -white "gnugo --mode gtp" (GNU Go version 3.6)
#
# $Id$
#-----------------------------------------------------------------------------

10 name
#? [TwoGtp]

20 protocol_version
#? [2]

30 version

40 list_commands

50 gogui-interrupt

60 gogui-title
#? [GNU Go vs GtpDummy \(B\)]

70 twogtp-black name
#? [GtpDummy]

80 twogtp-white name
#? [GNU Go]

90 play b c3

100 genmove w
#? [[A-T]1?[0-9]]

110 play b pass
