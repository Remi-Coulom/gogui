#-----------------------------------------------------------------------------
# Tests for GtpDummy.
#
# $Id$
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
#? [[A-T]1?[0-9]]

90 genmove b
#? [[A-T]1?[0-9]]

100 play w pass

110 echo Test text
#? [Test text]

120 echo_err Test text

130 gtpdummy-bwboard
#? [\n?(([WB] ){18}[WB]\n){18}([WB] ){18}[WB]]

140 gtpdummy-delay
#? [0]

150 gtpdummy-delay 0

160 gtpdummy-eplist show
#? []

170 gtpdummy-eplist a1 B2 C18

180 gtpdummy-eplist show
#? [A1 B2 C18]

190 gtpdummy-eplist

200 gtpdummy-eplist show
#? []

210 gtpdummy-long_response 5
#? [([1-4]\n){4}[5]]

220 gtpdummy-sleep 0

230 gtpdummy-next_success Test response
#? []

240 unknown_command
#? [Test response]

250 gogui-interrupt
