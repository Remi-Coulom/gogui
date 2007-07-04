//----------------------------------------------------------------------------
// $Id: GtpUtil.java 3804 2007-01-18 19:34:23Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

/** Utility functions for using a GtpClient. */
public final class GtpClientUtil
{
    /** Construct a gogui-play_sequence command from a list of moves. */
    public static String getPlaySequenceCommand(GtpClientBase gtp,
                                                ArrayList moves)
    {
        assert isPlaySequenceSupported(gtp);
        StringBuffer cmd = new StringBuffer(2048);
        cmd.append(getPlaySequenceCommand(gtp));
        for (int i = 0; i < moves.size(); ++i)
        {
            Move move = (Move)moves.get(i);
            GoColor color = move.getColor();
            if (color == BLACK)
                cmd.append(" b ");
            else if (color == WHITE)
                cmd.append(" w ");
            else
                cmd.append(" empty ");
            cmd.append(GoPoint.toString(move.getPoint()));
        }
        return cmd.toString();
    }

    public static String getPlaySequenceCommand(GtpClientBase gtp)
    {
        if (gtp.isSupported("gogui-play_sequence"))
            return "gogui-play_sequence";
        if (gtp.isSupported("play_sequence"))
            return "play_sequence";
        return null;
    }

    public static boolean isPlaySequenceSupported(GtpClientBase gtp)
    {
        return (getPlaySequenceCommand(gtp) != null);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GtpClientUtil()
    {
    }
}
