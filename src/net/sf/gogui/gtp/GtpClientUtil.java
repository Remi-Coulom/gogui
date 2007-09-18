//----------------------------------------------------------------------------
// $Id: GtpUtil.java 3804 2007-01-18 19:34:23Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.ErrorMessage;

/** Utility functions for using a GtpClient. */
public final class GtpClientUtil
{
    /** Query analyze commands configuration from the program.
        Sends the command gogui-analyze_commands (or gogui_analyze_commands
        as used by older versions of GoGui, if the program does not support
        the new command).
        Note: call GtpClientBase.querySupportedCommands() first.
        @return The response to gogui-analyze_commands or null, if this
        command is not supported or returns an error.
    */
    public static String getAnalyzeCommands(GtpClientBase gtp)
    {
        String command;
        if (gtp.isSupported("gogui-analyze_commands"))
            command = "gogui-analyze_commands";
        else if (gtp.isSupported("gogui_analyze_commands"))
            // Used by old versions of GoGui
            command = "gogui_analyze_commands";
        else
            return null;
        try
        {
            return gtp.send(command);
        }
        catch (GtpError e)
        {
            return null;
        }
    }

    /** Construct a gogui-play_sequence command from a list of moves. */
    public static String getPlaySequenceCommand(GtpClientBase gtp,
                                                ArrayList<Move> moves)
    {
        assert isPlaySequenceSupported(gtp);
        StringBuilder cmd = new StringBuilder(2048);
        cmd.append(getPlaySequenceCommand(gtp));
        for (int i = 0; i < moves.size(); ++i)
        {
            cmd.append(' ');
            cmd.append(moves.get(i));
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

    /** Get title for current game from program.
        Uses gogui-title (see GoGui documentation) or the deprectated
        command gogui_title.
        Note: call GtpClientBase.querySupportedCommands() first.
        @return The response to the command or null, if neither command
        is supported or the command failed.
    */
    public static String getTitle(GtpClientBase gtp)
    {
        try
        {
            if (gtp.isSupported("gogui-title"))
                return gtp.send("gogui-title");
            else if (gtp.isSupported("gogui_title"))
                return gtp.send("gogui_title");
        }
        catch (GtpError e)
        {
        }
        return null;
    }

    public static boolean isPlaySequenceSupported(GtpClientBase gtp)
    {
        return (getPlaySequenceCommand(gtp) != null);
    }

    /** Save parameters to a GTP file.
        Sends all analyze commands of type "param" to the program to query the
        current parameter values and creates a config file with GTP commands
        that allows to restore the values.
        @param gtp The GTP connection.
        @param analyzeCommands The analyze command definitions (e.g. from
        AnalyzeDefinition.read()).
        @throws ErrorMessage If writing the file fails or none of the analyze
        commands have the type "param" (error responses from the program are
        written in comment lines in the resulting file).
        See GoGui documentation, chapter "Analyze Commands"
    */
    public static void
        saveParameters(GtpClientBase gtp,
                       ArrayList<AnalyzeDefinition> analyzeCommands,
                       File file) throws ErrorMessage
    {
        boolean anyParamCommand = false;
        for (AnalyzeDefinition definition : analyzeCommands)
            if (definition.getType() == AnalyzeType.PARAM)
            {
                anyParamCommand = true;
                break;
            }
        if (! anyParamCommand)
            throw new ErrorMessage("The program does not support any"
                                   + " analyze commands of type \"param\"");
        try
        {
            PrintStream out = new PrintStream(file);
            for (AnalyzeDefinition definition : analyzeCommands)
                if (definition.getType() == AnalyzeType.PARAM)
                {
                    out.print("# ");
                    out.println(definition.getLabel());
                    String response = gtp.send(definition.getCommand());
                    BufferedReader reader
                        = new BufferedReader(new StringReader(response));
                    while (true)
                    {
                    }
                }
        }
        catch (IOException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        throw new ErrorMessage("GtpClientUtil not yet fully implemented");
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GtpClientUtil()
    {
    }
}
