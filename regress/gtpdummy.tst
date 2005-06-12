#-----------------------------------------------------------------------------
# Tests for GtpDummy.
#
# $Id$
# $Source$
#-----------------------------------------------------------------------------

10 name
#? [GtpDummy]

20 protocol_version
#? [2]

30 version

40 list_commands

50 boardsize 19

60 clear_board

70 play black c3

80 genmove white
#? [[A-T]1*[1-9]]

90 genmove b
#? [[A-T]1*[1-9]]

100 play w pass

110 echo Test text
#? [Test text]

120 echo_err Test text

130 dummy_bwboard
#? [\n?(([WB] ){18}[WB]\n){18}([WB] ){18}[WB]]

140 dummy_delay
#? [0]

150 dummy_delay 0

160 dummy_eplist show
#? []

170 dummy_eplist a1 B2 C18

180 dummy_eplist show
#? [A1 B2 C18]

190 dummy_eplist

200 dummy_eplist show
#? []

210 dummy_long_response 5
#? [([1-4]\n){4}[5]]

220 dummy_sleep 0

230 dummy_next_success Test response
#? []

240 unknown_command
#? [Test response]

250 gogui_interrupt
