#-----------------------------------------------------------------------------
# Tests for TwoGtp.
# Run with:
# twogtp -black gtpdummy -white "gnugo --mode gtp" (GNU Go version 3.6)
#
# $Id$
# $Source$
#-----------------------------------------------------------------------------

10 name
#? [TwoGtp]

20 protocol_version
#? [1]

30 version

40 help

50 gogui_interrupt

60 gogui_title
#? [GtpDummy vs GNU Go]

70 twogtp_black name
#? [GtpDummy]

80 twogtp_white name
#? [GNU Go]

90 black c3

100 genmove_white
#? [[A-T]1?[0-9]]

