//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package sgftotex;

import java.io.*;
import java.util.*;
import go.*;
import sgf.*;
import utils.*;
import version.*;

//-----------------------------------------------------------------------------

class SgfToTex
{
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "help",
                "version"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("SgfToTex " + Version.m_version);
                System.exit(0);
            }
            Vector arguments = opt.getArguments();
            InputStream in;
            OutputStream out;
            if (arguments.size() > 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            if (arguments.size() == 0)
            {
                in = System.in;
                out = System.out;
            }
            else
            {
                String inFileName = (String)arguments.get(0);
                File inFile = new File(inFileName);
                in = new FileInputStream(inFile);
                String outFileName;
                if (arguments.size() == 1)
                    outFileName =
                        FileUtils.replaceExtension(inFile, "sgf", "tex");
                else
                    outFileName = (String)arguments.get(1);
                File outFile = new File(outFileName);
                if (outFile.exists())
                    throw new Exception("File " + outFile + " already exists");
                out = new FileOutputStream(outFile);
            }
            convert(in, out);
        }
        catch (AssertionError e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            System.exit(-1);
        }
    }

    private static void convert(InputStream in, OutputStream out)
        throws sgf.Reader.Error
    {
        sgf.Reader reader = new sgf.Reader(new InputStreamReader(in), null);
        Board board = new Board(reader.getBoardSize());
        Vector setupBlack = reader.getSetupBlack();
        for (int i = 0; i < setupBlack.size(); ++i)
        {
            go.Point p = (go.Point)setupBlack.get(i);
            board.setup(new Move(p, go.Color.BLACK));
        }
        Vector setupWhite = reader.getSetupWhite();
        for (int i = 0; i < setupWhite.size(); ++i)
        {
            go.Point p = (go.Point)setupWhite.get(i);
            board.setup(new Move(p, go.Color.WHITE));
        }
        int numberMoves = reader.getMoves().size();
        Color toMove = reader.getToMove();
        if (numberMoves == 0 && board.getToMove() != toMove)
            board.setup(new Move(null, toMove.otherColor()));
        for (int i = 0; i < numberMoves; ++i)
            board.play(reader.getMove(i));
        boolean writePosition = (numberMoves == 0);
        new latex.Writer(out, board, writePosition, null, null, null);
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: java -jar sgftotex.jar [file.sgf [file.tex]]\n" +
                  "\n" +
                  "  -help    display this help and exit\n" +
                  "  -version print version and exit\n");
    }
}
    
//-----------------------------------------------------------------------------
