// XmlReader.java

package net.sf.gogui.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.sgf.SgfUtil;
import net.sf.gogui.util.ByteCountInputStream;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.ProgressShow;

/** Read files in Jago's XML format.
    This class reads files in Jago's XML format, see
    http://www.rene-grothmann.de/jago. It can understand valid XML files
    according to the go.dtd from the Jago webpage (10/2007) and also handles
    some deviations used by Jago or in the examples used on the Jago
    webpage, see also the appendix "XML Format" of the GoGui documentation.
    The implementation uses SAX for memory efficient parsing of large files. */
public final class XmlReader
{
    /** Construct reader and read.
        @param progressShow Callback to show progress, can be null
        @param streamSize Size of stream if progressShow != null */
    public XmlReader(InputStream in, ProgressShow progressShow,
                     long streamSize)
        throws ErrorMessage
    {
        m_progressShow = progressShow;
        m_streamSize = streamSize;
        if (progressShow != null)
        {
            progressShow.showProgress(0);
            m_byteCountInputStream = new ByteCountInputStream(in);
            in = m_byteCountInputStream;
        }
        try
        {
            m_isFirstElement = true;
            m_isFirstNode = true;
            m_gameInfoPreByoyomi = -1;
            m_root = new Node();
            // Don't create game info yet, because implicit empty root
            // might be truncated later
            m_info = new GameInfo();
            m_node = m_root;
            XMLReader reader = XMLReaderFactory.createXMLReader();
            try
            {
                reader.setFeature("http://xml.org/sax/features/validation",
                                  false);
            }
            catch (SAXException e)
            {
            }
            Handler handler = new Handler();
            reader.setContentHandler(handler);
            reader.setEntityResolver(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(in));
            int size;
            if (m_isBoardSizeKnown)
                size = m_boardSize;
            else
                size = Math.max(DEFAULT_BOARDSIZE, m_boardSize);
            m_tree = new GameTree(size, m_root);
            m_tree.getGameInfo(m_root).copyFrom(m_info);
            if (m_gameName != null)
                m_root.addSgfProperty("GN", m_gameName);
        }
        catch (SAXException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        catch (IOException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public GameTree getTree()
    {
        return m_tree;
    }

    public String getWarnings()
    {
        if (m_warnings.isEmpty())
            return null;
        StringBuilder result = new StringBuilder(m_warnings.size() * 80);
        for (String s : m_warnings)
        {
            result.append(s);
            result.append('\n');
        }
        return result.toString();
    }

    private class Handler
        extends DefaultHandler
    {
        public void startElement(String namespaceURI, String name,
                                 String qualifiedName, Attributes atts)
            throws SAXException
        {
            if (m_progressShow != null)
                showProgress();
            checkNoCharacters();
            m_element = name;
            m_atts = atts;
            if (m_isFirstElement)
            {
                if (! m_element.equals("Go"))
                    throw new SAXException("Not a Go game");
                m_isFirstElement = false;
            }
            if (name.equals("Annotation"))
                startInfoElemWithoutFormat();
            else if (name.equals("Application"))
                startInfoElemWithFormat();
            else if (name.equals("AddBlack"))
                startSetup(BLACK);
            else if (name.equals("AddWhite"))
                startSetup(WHITE);
            else if (name.equals("Arg"))
                checkParent("SGF");
            else if (name.equals("at"))
                checkParent("Black", "White", "AddBlack", "AddWhite", "Delete",
                            "Mark");
            else if (name.equals("Black"))
                startMove(BLACK);
            else if (name.equals("BlackPlayer"))
                startInfoElemWithFormat();
            else if (name.equals("BlackRank"))
                startInfoElemWithFormat();
            else if (name.equals("BlackTeam"))
                startInfoElemWithoutFormat();
            else if (name.equals("BlackToPlay"))
                startToPlay(BLACK);
            else if (name.equals("BoardSize"))
                startInfoElemWithFormat();
            else if (name.equals("Comment"))
                startComment();
            else if (name.equals("Copyright"))
                startCopyright();
            else if (name.equals("Date"))
                startInfoElemWithFormat();
            else if (name.equals("Delete"))
                startSetup(EMPTY);
            else if (name.equals("Go"))
                startGo();
            else if (name.equals("GoGame"))
                startGoGame();
            else if (name.equals("Handicap"))
                startInfoElemWithFormat();
            else if (name.equals("Information"))
                startInformation();
            else if (name.equals("Line"))
                startLine();
            else if (name.equals("Komi"))
                startInfoElemWithFormat();
            else if (name.equals("Mark"))
                startMark();
            else if (name.equals("Node"))
                startNode();
            else if (name.equals("Nodes"))
                startNodes();
            else if (name.equals("P"))
                startP();
            else if (name.equals("Result"))
                startInfoElemWithFormat();
            else if (name.equals("Round"))
                startInfoElemWithoutFormat();
            else if (name.equals("Rules"))
                startInfoElemWithFormat();
            else if (name.equals("Source"))
                startInfoElemWithFormat();
            else if (name.equals("SGF"))
                startSGF();
            else if (name.equals("Time"))
                startInfoElemWithFormat();
            else if (name.equals("User"))
                startInfoElemWithoutFormat();
            else if (name.equals("Variation"))
                startVariation();
            else if (name.equals("White"))
                startMove(WHITE);
            else if (name.equals("WhitePlayer"))
                startInfoElemWithFormat();
            else if (name.equals("WhiteRank"))
                startInfoElemWithFormat();
            else if (name.equals("WhiteTeam"))
                startInfoElemWithoutFormat();
            else if (name.equals("WhiteToPlay"))
                startToPlay(WHITE);
            else
                setWarning("Ignoring unknown element: " + name);
            m_elementStack.push(name);
            m_characters.setLength(0);
        }

        public void endElement(String namespaceURI, String name,
                               String qualifiedName) throws SAXException
        {
            m_element = m_elementStack.pop();
            if (name.equals("AddBlack"))
                endSetup(BLACK);
            else if (name.equals("AddWhite"))
                endSetup(WHITE);
            else if (name.equals("Annotation"))
                m_info.set(StringInfo.ANNOTATION, getCharacters());
            else if (name.equals("Arg"))
                m_sgfArgs.add(getCharacters());
            else if (name.equals("at"))
                endAt();
            else if (name.equals("Black"))
                endMove(BLACK);
            else if (name.equals("BlackPlayer"))
                m_info.set(StringInfoColor.NAME, BLACK, getCharacters());
            else if (name.equals("BlackRank"))
                m_info.set(StringInfoColor.RANK, BLACK, getCharacters());
            else if (name.equals("BlackTeam"))
                m_info.set(StringInfoColor.TEAM, BLACK, getCharacters());
            else if (name.equals("BlackToPlay"))
                endToPlay();
            else if (name.equals("BoardSize"))
                endBoardSize();
            else if (name.equals("Comment"))
                endComment();
            else if (name.equals("Copyright"))
                endCopyright();
            else if (name.equals("Date"))
                m_info.set(StringInfo.DATE, getCharacters());
            else if (name.equals("Delete"))
                endSetup(EMPTY);
            else if (name.equals("Go"))
                checkNoCharacters();
            else if (name.equals("GoGame"))
                checkNoCharacters();
            else if (name.equals("Handicap"))
                endHandicap();
            else if (name.equals("Information"))
                checkNoCharacters();
            else if (name.equals("Komi"))
                endKomi();
            else if (name.equals("Mark"))
                endMark();
            else if (name.equals("Node"))
                endNode();
            else if (name.equals("Nodes"))
                checkNoCharacters();
            else if (name.equals("P"))
                endP();
            else if (name.equals("Result"))
                m_info.set(StringInfo.RESULT, getCharacters());
            else if (name.equals("Round"))
                m_info.set(StringInfo.ROUND, getCharacters());
            else if (name.equals("Rules"))
                m_info.set(StringInfo.RULES, getCharacters());
            else if (name.equals("SGF"))
                endSgf();
            else if (name.equals("Source"))
                m_info.set(StringInfo.SOURCE, getCharacters());
            else if (name.equals("Time"))
                endTime();
            else if (name.equals("User"))
                m_info.set(StringInfo.USER, getCharacters());
            else if (name.equals("White"))
                endMove(WHITE);
            else if (name.equals("WhitePlayer"))
                m_info.set(StringInfoColor.NAME, WHITE, getCharacters());
            else if (name.equals("WhiteRank"))
                m_info.set(StringInfoColor.RANK, WHITE, getCharacters());
            else if (name.equals("WhiteTeam"))
                m_info.set(StringInfoColor.TEAM, WHITE, getCharacters());
            else if (name.equals("WhiteToPlay"))
                endToPlay();
            else if (name.equals("Variation"))
                endVariation();
            m_characters.setLength(0);
        }

        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            m_characters.append(ch, start, length);
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
            throwError(e.getMessage());
        }

        /** Return a fake go.dtd, if go.dtd does not exist as file.
            GoGui does not validate the document anyway, but this avoids a
            missing entity error message, if an XML file references go.dtd,
            but it is not found. */
        public InputSource resolveEntity(String publicId, String systemId)
        {
            if (systemId == null)
                return null;
            URI uri;
            try
            {
                uri = new URI(systemId);
            }
            catch (URISyntaxException e)
            {
                return null;
            }
            if (! "file".equals(uri.getScheme()))
                return null;
            File file = new File(uri.getPath());
            if (file.exists() || ! "go.dtd".equals(file.getName()))
                return null;
            String text = "<?xml version='1.0' encoding='UTF-8'?>";
            return new InputSource(new ByteArrayInputStream(text.getBytes()));
        }

        public void setDocumentLocator(Locator locator)
        {
            m_locator = locator;
        }

        public void error(SAXParseException e)
        {
            setWarning(e.getMessage());
        }

        public void warning(SAXParseException e)
        {
            setWarning(e.getMessage());
        }
    }

    private static final int DEFAULT_BOARDSIZE = 19;

    private boolean m_isFirstElement;

    private boolean m_isFirstNode;

    private boolean m_isBoardSizeKnown;

    private int m_numberGames;

    private int m_numberTrees;

    /** Board size.
        If board size is not explicitely set, this variable is used to track
        the maximum size necessary for all points seen. */
    private int m_boardSize;

    private int m_lastPercent;

    private final long m_streamSize;

    /** Element stack. */
    private Stack<String> m_elementStack = new Stack<String>();

    /** Current node. */
    private Node m_node;

    private Stack<Node> m_variation = new Stack<Node>();

    private GameInfo m_info = new GameInfo();

    private Node m_root;

    private GameTree m_tree;

    /** Current element */
    private String m_element;

    /** Attributes of current element */
    private Attributes m_atts;

    /** Type of current SGF element. */
    private String m_sgfType;

    /** Arguments of current SGF element. */
    private ArrayList<String> m_sgfArgs = new ArrayList<String>();

    /** Characters in current element. */
    private StringBuilder m_characters = new StringBuilder();

    /** Contains strings with warnings. */
    private final Set<String> m_warnings = new TreeSet<String>();

    private Locator m_locator;

    /** Current mark type in Mark element. */
    private MarkType m_markType;

    /** Current label in Mark element. */
    private String m_label;

    private String m_gameName;

    private ByteCountInputStream m_byteCountInputStream;

    private final ProgressShow m_progressShow;

    /** Time settings information for current node from legacy SGF
        properties. */
    private int m_byoyomiMoves;

    /** Time settings information for current node from legacy SGF
        properties. */
    private long m_byoyomi;

    /** Time settings information for current node from legacy SGF
        properties. */
    private long m_preByoyomi;

    private long m_gameInfoPreByoyomi;

    /** Has current node inconsistent SGF/FF3 overtime settings properties. */
    private boolean m_ignoreOvertime;

    private String m_paragraphElementText;

    private void checkAttributes(String... atts) throws SAXException
    {
        List<String> list = Arrays.asList(atts);
        for (int i = 0; i < m_atts.getLength(); ++i)
        {
            String name = m_atts.getLocalName(i);
            if (! list.contains(name))
                setWarning("Unknown attribute \"" + name + "\" for element \""
                           + m_element + "\"");
        }
    }

    private void checkNoCharacters() throws SAXException
    {
        if (! getCharacters().trim().equals(""))
            setWarning("Cannot handle text content in element \"" + m_element
                       + "\"");
    }

    private void checkRoot() throws SAXException
    {
        String parent = parentElement();
        if (parent != null)
            throwError("Element \"" + m_element + "\" cannot be child of \""
                       + parent + "\"");
    }

    private void checkParent(String... parents) throws SAXException
    {
        String parent = parentElement();
        if (! Arrays.asList(parents).contains(parent))
            throwError("Element \"" + m_element + "\" cannot be child of \""
                       + parent + "\"");
    }

    private void createNode()
    {
        Node node = new Node();
        if (m_node != null)
            m_node.append(node);
        else if (! m_variation.isEmpty())
            m_variation.peek().getFather().append(node);
        m_node = node;
    }

    private void endAt() throws SAXException
    {
        GoPoint p = getPoint(getCharacters());
        String parent = parentElement();
        if (parent.equals("Black"))
            m_node.setMove(Move.get(BLACK, p));
        else if (parent.equals("White"))
            m_node.setMove(Move.get(WHITE, p));
        else if (parent.equals("AddBlack"))
            m_node.addStone(BLACK, p);
        else if (parent.equals("AddWhite"))
            m_node.addStone(WHITE, p);
        else if (parent.equals("Delete"))
            m_node.addStone(EMPTY, p);
        else if (parent.equals("Mark"))
        {
            if (m_markType != null)
                m_node.addMarked(p, m_markType);
            if (m_label != null)
                m_node.setLabel(p, m_label);
        }
    }

    private void endBoardSize() throws SAXException
    {
        int boardSize = parseInt();
        if (boardSize < 1 || boardSize > GoPoint.MAX_SIZE)
            throw new SAXException("Unsupported board size");
        m_isBoardSizeKnown = true;
        m_boardSize = boardSize;
    }

    private void endComment()
    {
        m_node.setComment(getParagraphElementText());
    }

    private void endCopyright()
    {
        m_info.set(StringInfo.COPYRIGHT, getParagraphElementText());
    }

    private void endHandicap() throws SAXException
    {
        int handicap = parseInt();
        if (handicap == 1 || handicap < 0)
            setWarning("Ignoring invalid handicap: " + handicap);
        else
            m_info.setHandicap(handicap);
    }

    private void endKomi() throws SAXException
    {
        String komi = getCharacters();
        try
        {
            m_info.setKomi(Komi.parseKomi(komi));
        }
        catch (InvalidKomiException e)
        {
            setWarning("Invalid komi: " + komi);
        }
    }

    private void endMark() throws SAXException
    {
        // According to the DTD, mark cannot contain
        // text content, but we accept it, if the point is text content
        // instead of a at-subelement or an at-attribute
        String value = getCharacters();
        if (! value.trim().equals(""))
        {
            GoPoint p = getPoint(value);
            if (m_markType != null)
                m_node.addMarked(p, m_markType);
            if (m_label != null)
                m_node.setLabel(p, m_label);
        }
    }

    private void endMove(GoColor c) throws SAXException
    {
        // According to the DTD, Black and White cannot contain text
        // content, but we accept it, if the move is text content instead
        // of a at-subelement or an at-attribute
        String value = getCharacters();
        if (! value.trim().equals(""))
            m_node.setMove(Move.get(c, getPoint(value)));
    }

    private void endNode() throws SAXException
    {
        checkNoCharacters();
        setSgfTimeSettings();
    }

    private void endP() throws SAXException
    {
        m_paragraphElementText =
            m_paragraphElementText + getMergedLines() + "\n";
    }

    private void endSetup(GoColor c) throws SAXException
    {
        // According to the DTD, AddBlack, AddWhite, and Delete cannot contain
        // text content, but we accept it, if the point is text content instead
        // of a at-subelement or an at-attribute
        String value = getCharacters();
        if (! value.trim().equals(""))
            m_node.addStone(c, getPoint(value));
    }

    private void endSgf() throws SAXException
    {
        checkNoCharacters();
        if (m_sgfType == null)
            return;
        if (m_sgfType.equals("AN"))
            endSgfInfo(StringInfo.ANNOTATION);
        else if (m_sgfType.equals("BL"))
            endSgfTimeLeft(BLACK);
        else if (m_sgfType.equals("BR"))
            endSgfInfo(StringInfoColor.RANK, BLACK);
        else if (m_sgfType.equals("BT"))
            endSgfInfo(StringInfoColor.TEAM, BLACK);
        else if (m_sgfType.equals("CP"))
            endSgfInfo(StringInfo.COPYRIGHT);
        else if (m_sgfType.equals("DT"))
            endSgfInfo(StringInfo.DATE);
        else if (m_sgfType.equals("HA"))
            endSgfHandicap();
        else if (m_sgfType.equals("OB"))
            endSgfMovesLeft(BLACK);
        else if (m_sgfType.equals("OM"))
            endSgfOvertimeMoves();
        else if (m_sgfType.equals("OP"))
            endSgfOvertimePeriod();
        else if (m_sgfType.equals("OT"))
            endSgfOvertime();
        else if (m_sgfType.equals("OW"))
            endSgfMovesLeft(WHITE);
        else if (m_sgfType.equals("KM"))
            endSgfKomi();
        else if (m_sgfType.equals("PB"))
            endSgfInfo(StringInfoColor.NAME, BLACK);
        else if (m_sgfType.equals("PW"))
            endSgfInfo(StringInfoColor.NAME, WHITE);
        else if (m_sgfType.equals("PL"))
            endSgfPlayer();
        else if (m_sgfType.equals("RE"))
            endSgfInfo(StringInfo.RESULT);
        else if (m_sgfType.equals("RO"))
            endSgfInfo(StringInfo.ROUND);
        else if (m_sgfType.equals("RU"))
            endSgfInfo(StringInfo.RULES);
        else if (m_sgfType.equals("SL"))
            endSgfSelect();
        else if (m_sgfType.equals("WL"))
            endSgfTimeLeft(WHITE);
        else if (m_sgfType.equals("TM"))
            endSgfTime();
        else if (m_sgfType.equals("WR"))
            endSgfInfo(StringInfoColor.RANK, WHITE);
        else if (m_sgfType.equals("WT"))
            endSgfInfo(StringInfoColor.TEAM, WHITE);
        else if (m_sgfType.equals("US"))
            endSgfInfo(StringInfo.USER);
        else
            m_node.addSgfProperty(m_sgfType, m_sgfArgs);
    }

    /** Handle non-root handicap info from SGF properties. */
    private void endSgfHandicap()
    {
        if (m_sgfArgs.size() == 0)
            return;
        try
        {
            int handicap = Integer.parseInt(m_sgfArgs.get(0));
            GameInfo info = m_node.createGameInfo();;
            info.setHandicap(handicap);
        }
        catch (NumberFormatException e)
        {
        }
    }

    /** Handle non-root game info from SGF properties. */
    private void endSgfInfo(StringInfo type)
    {
        if (m_sgfArgs.size() == 0)
            return;
        GameInfo info = m_node.createGameInfo();;
        info.set(type, m_sgfArgs.get(0));
    }

    /** Handle non-root game info from SGF properties. */
    private void endSgfInfo(StringInfoColor type, GoColor c)
    {
        if (m_sgfArgs.size() == 0)
            return;
        GameInfo info = m_node.createGameInfo();;
        info.set(type, c, m_sgfArgs.get(0));
    }

    /** Handle non-root komi from SGF properties. */
    private void endSgfKomi()
    {
        if (m_sgfArgs.size() == 0)
            return;
        try
        {
            Komi komi = Komi.parseKomi(m_sgfArgs.get(0));
            GameInfo info = m_node.createGameInfo();;
            info.setKomi(komi);
        }
        catch (InvalidKomiException e)
        {
        }
    }

    private void endSgfMovesLeft(GoColor c)
    {
        if (m_sgfArgs.size() == 0)
            return;
        try
        {
            int movesLeft = Integer.parseInt(m_sgfArgs.get(0));
            if (movesLeft >= 0)
                m_node.setMovesLeft(c, movesLeft);
        }
        catch (NumberFormatException e)
        {
        }
    }

    /** FF4 OT property */
    private void endSgfOvertime()
    {
        if (m_sgfArgs.size() == 0)
            return;
        String value = m_sgfArgs.get(0).trim();
        if (value.equals("") || value.equals("-"))
            return;
        SgfUtil.Overtime overtime = SgfUtil.parseOvertime(value);
        if (overtime == null)
        {
            setWarning("Overtime settings in unknown format");
            m_node.addSgfProperty("OT", value); // Preserve information
        }
        else
        {
            m_byoyomi = overtime.m_byoyomi;
            m_byoyomiMoves = overtime.m_byoyomiMoves;
        }
    }

    /** FF3 OM property */
    private void endSgfOvertimeMoves()
    {
        if (m_sgfArgs.size() == 0)
            return;
        try
        {
            m_byoyomiMoves = Integer.parseInt(m_sgfArgs.get(0));
        }
        catch (NumberFormatException e)
        {
            setWarning("Invalid value for byoyomi moves");
            m_ignoreOvertime = true;
        }
    }

    /** FF3 OP property */
    private void endSgfOvertimePeriod()
    {
        if (m_sgfArgs.size() == 0)
            return;
        try
        {
            m_byoyomi = (long)(Double.parseDouble(m_sgfArgs.get(0)) * 1000);
        }
        catch (NumberFormatException e)
        {
            setWarning("Invalid value for byoyomi time");
            m_ignoreOvertime = true;
        }
    }

    private void endSgfPlayer()
    {
        if (m_sgfArgs.size() == 0)
            return;
        String value = m_sgfArgs.get(0).trim().toLowerCase(Locale.ENGLISH);
        GoColor c;
        if (value.equals("b") || value.equals("black"))
            c = BLACK;
        else if (value.equals("w") || value.equals("white"))
            c = WHITE;
        else
            return;
        m_node.setPlayer(c);
    }

    private void endSgfSelect() throws SAXException
    {
        for (int i = 0; i < m_sgfArgs.size(); ++i)
            m_node.addMarked(getSgfPoint(m_sgfArgs.get(i)), MarkType.SELECT);
    }

    /** Handle BL, WL SGF properties.
        XmlWriter uses these legacy SGF properties to preserve time left
        information that cannot be stored in a timleft-attribute of a move,
        because the node has no move or not a move of the corresponding color.
        Jago's blacktime/whitetime Node-attribute is not defined in
        go.dtd (2007) */
    private void endSgfTimeLeft(GoColor c)
    {
        if (m_sgfArgs.size() == 0)
            return;
        try
        {
            double timeLeft = Double.parseDouble(m_sgfArgs.get(0));
            m_node.setTimeLeft(c, timeLeft);
        }
        catch (NumberFormatException e)
        {
        }
    }

    private void endSgfTime()
    {
        if (m_sgfArgs.size() == 0)
            return;
        String value = m_sgfArgs.get(0).trim();
        if (value.equals("") || value.equals("-"))
            return;
        long preByoyomi = SgfUtil.parseTime(value);
        if (preByoyomi < 0)
        {
            setWarning("Unknown format in time property");
            m_node.addSgfProperty("TM", value); // Preserve information
        }
        else
            m_preByoyomi = preByoyomi;

    }

    private void endTime() throws SAXException
    {
        String value = getCharacters().trim();
        if (value.equals("") || value.equals("-"))
            return;
        long preByoyomi = SgfUtil.parseTime(value);
        if (preByoyomi < 0)
        {
            setWarning("Unknown format in Time element");
            m_node.addSgfProperty("TM", value); // Preserve information
        }
        else
        {
            // Set time settings now but also remember value, because time
            // settings could be overwritten in setSgfTimeSettings() after
            // overtime information is known from SGF element
            m_gameInfoPreByoyomi = preByoyomi;
            TimeSettings timeSettings = new TimeSettings(preByoyomi);
            m_info.setTimeSettings(timeSettings);
        }
    }

    private void endToPlay() throws SAXException
    {
        if (! getCharacters().trim().equals(""))
            setWarning("Ignoring text content in element \"" + m_element
                       + "\"");
    }

    private void endVariation() throws SAXException
    {
        checkNoCharacters();
        m_node = m_variation.pop();
    }

    private String getCharacters()
    {
        return m_characters.toString();
    }

    private String getMergedLines()
    {
        String chars = getCharacters();
        StringBuilder result = new StringBuilder(chars.length());
        BufferedReader reader = new BufferedReader(new StringReader(chars));
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (result.length() > 0)
                    result.append(' ');
                result.append(line);
            }
        }
        catch (IOException e)
        {
            assert(false);
        }
        return result.toString();
    }

    private String getParagraphElementText()
    {
        String text = m_paragraphElementText;
        String mergedLines = getMergedLines();
        // Handle direct text content even if not allowed by DTD
        if (! mergedLines.equals(""))
            text = text + mergedLines + "\n";
        // Remove exactly one trailing newline
        if (text.endsWith("\n"))
            text = text.substring(0, text.length() - 1);
        return text;
    }

    private GoPoint getPoint(String value) throws SAXException
    {
        value = value.trim();
        if (value.equals(""))
            return null;
        GoPoint p;
        try
        {
            if (m_isBoardSizeKnown)
                p = GoPoint.parsePoint(value, m_boardSize);
            else
            {
                p = GoPoint.parsePoint(value, GoPoint.MAX_SIZE);
                if (p != null)
                {
                    m_boardSize = Math.max(m_boardSize, p.getX());
                    m_boardSize = Math.max(m_boardSize, p.getY());
                }
            }
            return p;
        }
        catch (InvalidPointException e)
        {
            throwError(e.getMessage());
        }
        return null; // Unreachable; avoid compiler error
    }

    private GoPoint getSgfPoint(String s) throws SAXException
    {
        s = s.trim().toLowerCase(Locale.ENGLISH);
        if (s.equals(""))
            return null;
        if (s.length() > 2
            || (s.length() == 2 && s.charAt(1) < 'a' || s.charAt(1) > 'z'))
            // Human-readable encoding as used by SmartGo
            return getPoint(s);
        else if (s.length() != 2)
            throwError("Invalid SGF coordinates: " + s);
        if (! m_isBoardSizeKnown)
        {
            // We need to know the boardsize for parsing SGF points to mirror
            // the y-coordinate and the size is not allowed to change later
            m_boardSize = DEFAULT_BOARDSIZE;
            m_isBoardSizeKnown = true;
        }
        if (s.equals("tt") && m_boardSize <= 19)
            return null;
        int x = s.charAt(0) - 'a';
        int y = m_boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= m_boardSize || y < 0 || y >= m_boardSize)
        {
            if (x == m_boardSize && y == -1)
                // Some programs encode pass moves, e.g. as jj for boardsize 9
                return null;
            throwError("Coordinates \"" + s + "\" outside board size "
                       + m_boardSize);
        }
        return GoPoint.get(x, y);
    }

    private void setSgfTimeSettings()
    {
        long preByoyomi = m_preByoyomi;
        if (m_node == m_root && preByoyomi < 0)
            preByoyomi = m_gameInfoPreByoyomi;
        TimeSettings s = null;
        if (preByoyomi > 0
            && (m_ignoreOvertime || m_byoyomi <= 0 || m_byoyomiMoves <= 0))
            s = new TimeSettings(preByoyomi);
        else if (preByoyomi <= 0 && ! m_ignoreOvertime && m_byoyomi > 0
                 && m_byoyomiMoves > 0)
            s = new TimeSettings(0, m_byoyomi, m_byoyomiMoves);
        else if (preByoyomi > 0  && ! m_ignoreOvertime && m_byoyomi > 0
                 && m_byoyomiMoves > 0)
            s = new TimeSettings(preByoyomi, m_byoyomi, m_byoyomiMoves);
        if (s != null)
        {
            if (m_node == m_root)
                m_info.setTimeSettings(s);
            else
                m_node.createGameInfo().setTimeSettings(s);
        }
    }

    private void showProgress()
    {
        int percent;
        if (m_streamSize > 0)
        {
            long count = m_byteCountInputStream.getCount();
            percent = (int)(count * 100 / m_streamSize);
        }
        else
            percent = 100;
        if (percent != m_lastPercent)
            m_progressShow.showProgress(percent);
        m_lastPercent = percent;
    }

    private void startComment() throws SAXException
    {
        checkParent("Nodes", "Node", "Variation");
        checkAttributes();
        m_paragraphElementText = "";
    }

    private void startCopyright() throws SAXException
    {
        checkParent("Information");
        checkAttributes();
        m_paragraphElementText = "";
    }

    private void startGo() throws SAXException
    {
        checkRoot();
        checkAttributes();
    }

    private void startGoGame() throws SAXException
    {
        checkParent("Go");
        checkAttributes("name");
        String name = m_atts.getValue("name");
        if (name != null)
            // Not supported in game.GameInformation, put it in later
            // in SGF properties
            m_gameName = name;
        if (++m_numberGames > 1)
            throwError("Multiple games per file not supported");
    }

    private void startInfoElemWithFormat() throws SAXException
    {
        checkParent("Information");
        checkAttributes("format");
        String format = m_atts.getValue("format");
        if (format == null)
            return;
        format = format.trim().toLowerCase(Locale.ENGLISH);
        if (! format.equals("sgf"))
            setWarning("Unknown format attribute \"" + format
                       + "\" for element \"" + m_element + "\"");
    }

    private void startInfoElemWithoutFormat() throws SAXException
    {
        checkParent("Information");
        checkAttributes();
    }

    private void startInformation() throws SAXException
    {
        checkParent("GoGame");
        checkAttributes();
    }

    private void startLine() throws SAXException
    {
        // Line has no legal parent according to the DTD, so we
        // ignore it
        setWarning("Element \"Line\" cannot be child of element \""
                   + parentElement() + "\"");
    }

    private void startMark() throws SAXException
    {
        checkParent("Node");
        checkAttributes("at", "label", "territory", "type");
        m_markType = null;
        m_label = m_atts.getValue("label");
        String type = m_atts.getValue("type");
        String territory = m_atts.getValue("territory");
        if (type != null)
        {
            if (type.equals("triangle"))
                m_markType = MarkType.TRIANGLE;
            else if (type.equals("circle"))
                m_markType = MarkType.CIRCLE;
            else if (type.equals("square"))
                m_markType = MarkType.SQUARE;
            else
                setWarning("Unknown mark type " + type);
        }
        if (territory != null)
        {
            if (territory.equals("black"))
                m_markType = MarkType.TERRITORY_BLACK;
            else if (territory.equals("white"))
                m_markType = MarkType.TERRITORY_WHITE;
            else
                setWarning("Unknown territory type " + territory);
        }
        if (type == null && territory == null && m_label == null)
            m_markType = MarkType.MARK;
        String value = m_atts.getValue("at");
        if (value != null)
        {
            GoPoint p = getPoint(value);
            if (m_markType != null)
                m_node.addMarked(p, m_markType);
            if (m_label != null)
                m_node.setLabel(p, m_label);
        }
    }

    private void startMove(GoColor c) throws SAXException
    {
        checkParent("Node", "Nodes", "Variation");
        if (! parentElement().equals("Node"))
            createNode();
        checkAttributes("annotate", "at", "timeleft", "name", "number");
        String name = m_atts.getValue("name");
        if (name != null)
            // Not supported in game.Node, put it in SGF properties
            m_node.addSgfProperty("N", name);
        if (m_atts.getValue("annotate") != null)
            // Allowed by DTD, but unclear content and not supported in
            // game.Node
            setWarning("Attribute \"annotate\" in element \""
                       + m_element + "\" not supported");
        String value = m_atts.getValue("at");
        if (value != null)
            m_node.setMove(Move.get(c, getPoint(value)));
        value = m_atts.getValue("timeleft");
        if (value != null)
        {
            try
            {
                m_node.setTimeLeft(c, Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    private void startNode() throws SAXException
    {
        checkParent("Nodes", "Variation");
        // blacktime and whitetime are not allowed in the DTD, but used
        // by Jago 5.0
        checkAttributes("blacktime", "name", "whitetime");
        // Don't create new node, if this is the first node and nothing
        // was added to the root node yet. This allows having an implicit
        // root node to handle cases like Comment being the first child of
        // Nodes (example on Jago's webpage) without creating an unnecessary
        // node if the first child of Nodes is a Node
        if (! m_isFirstNode || ! m_node.isEmpty())
            createNode();
        m_isFirstNode = false;
        String name = m_atts.getValue("name");
        if (name != null)
            // Not supported in game.Node, put it in SGF properties
            m_node.addSgfProperty("N", name);
        String value = m_atts.getValue("blacktime");
        if (value != null)
        {
            try
            {
                m_node.setTimeLeft(BLACK, Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
            }
        }
        value = m_atts.getValue("whitetime");
        if (value != null)
        {
            try
            {
                m_node.setTimeLeft(WHITE, Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
            }
        }
        m_ignoreOvertime = false;
        m_byoyomiMoves = -1;
        m_byoyomi = -1;
        m_preByoyomi = -1;
    }

    private void startNodes() throws SAXException
    {
        checkParent("GoGame");
        checkAttributes();
        if (++m_numberTrees > 1)
            throwError("More than one Nodes element in element GoGame");
    }

    private void startP() throws SAXException
    {
        checkParent("Comment", "Copyright");
        checkAttributes();
    }

    private void startSetup(GoColor c) throws SAXException
    {
        checkParent("Node");
        checkAttributes("at");
        String value = m_atts.getValue("at");
        if (value != null)
            m_node.addStone(c, getPoint(value));
    }

    private void startSGF() throws SAXException
    {
        checkParent("Node");
        checkAttributes("type");
        m_sgfType = m_atts.getValue("type");
        m_sgfArgs.clear();
    }

    private void startToPlay(GoColor c) throws SAXException
    {
        // According to the DTD, BlackToPlay and WhiteToPlay can never
        // occur in a valid document, because they have no legal parent.
        // I assume that they were meant to be child elements of Node
        // and set the player in setup positions
        checkParent("Node");
        checkAttributes();
        m_node.setPlayer(c);
    }

    private void startVariation() throws SAXException
    {
        checkParent("Nodes", "Variation");
        checkAttributes();
        if (m_node == null)
            throwError("Variation without main node");
        assert m_node.hasFather();
        m_variation.push(m_node);
        m_node = null;
    }

    private String parentElement()
    {
        if (m_elementStack.isEmpty())
            return null;
        return m_elementStack.peek();
    }

    private int parseInt() throws SAXException
    {
        try
        {
            return Integer.parseInt(getCharacters());
        }
        catch (NumberFormatException e)
        {
            throw new SAXException("Expected integer in element " + m_element);
        }
    }

    private void setWarning(String message)
    {
        m_warnings.add(message);
    }

    private void throwError(String message) throws SAXException
    {
        if (m_locator != null)
            message = "Line " + m_locator.getLineNumber() + ":"
                + m_locator.getColumnNumber() + ": " + message;
        throw new SAXException(message);
    }
}
