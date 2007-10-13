//----------------------------------------------------------------------------
// XmlReader.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
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
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.ErrorMessage;

/** Read files in Jago's XML format.
    This class reads files in Jago's XML format, see
    http://www.rene-grothmann.de/jago. It can understand valid XML files
    according to the go.dtd from the Jago webpage (10/2007) and also handles
    some deviations used by Jago or in the examples used on the Jago
    webpage, see also the appendix "XML Format" of the GoGui documentation.
    The implementation uses SAX for memory efficient parsing of large files.
*/
public final class XmlReader
{
    public XmlReader(InputStream in)
        throws ErrorMessage
    {
        try
        {
            m_root = new Node();
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
            // Prune root node, if it is an unnecessary empty node
            if (m_root.isEmpty() && m_root.getNumberChildren() == 1
                && m_root.getChild().getMove() == null)
            {
                m_root = m_root.getChild();
                m_root.setFather(null);
            }
            m_tree = new GameTree(size, m_root);
            m_tree.getGameInfo(m_root).copyFrom(m_info);
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
            checkNoCharacters();
            m_element = name;
            m_atts = atts;
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
                checkParent("Information");
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
                appendComment(true);
            else if (name.equals("Copyright"))
                appendCopyright(true);
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

        /** Return internal go.dtd, if file does not exist.
            Currently, GoGui does not validate the documents, but this
            still avoids a missing entity error message, if an XML file
            references go.dtd, but it is not found.
        */
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
            String resource = "net/sf/gogui/xml/go.dtd";
            URL url = ClassLoader.getSystemClassLoader().getResource(resource);
            if (url == null)
            {
                assert false;
                return null;
            }
            try
            {
                return new InputSource(url.openStream());
            }
            catch (IOException e)
            {
                assert false;
                return null;
            }
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

    private boolean m_isBoardSizeKnown;

    private int m_numberGames;

    private int m_numberTrees;

    /** Board size.
        If board size is not explicitely set, this variable is used to track
        the maximum size necessary for all points seen.
    */
    private int m_boardSize;

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

    private void appendComment(boolean onlyIfNotEmpty)
    {
        String comment = m_node.getComment();
        String mergedLines = getMergedLines();
        if (onlyIfNotEmpty && mergedLines.equals(""))
            return;
        if (comment == null)
            m_node.setComment(mergedLines);
        else
            m_node.setComment(comment + "\n" + mergedLines);
    }

    private void appendCopyright(boolean onlyIfNotEmpty)
    {
        String copyright = m_info.get(StringInfo.COPYRIGHT);
        String mergedLines = getMergedLines();
        if (onlyIfNotEmpty && mergedLines.equals(""))
            return;
        if (copyright == null)
            m_info.set(StringInfo.COPYRIGHT, mergedLines);
        else
            m_info.set(StringInfo.COPYRIGHT, copyright + "\n"
                       + mergedLines);
    }

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

    private void endP() throws SAXException
    {
        String text = getCharacters();
        String parent = parentElement();
        if (parent.equals("Comment"))
            appendComment(false);
        else if (parent.equals("Copyright"))
            appendCopyright(false);
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
        if (m_sgfType.equals("SL"))
        {
            for (int i = 0; i < m_sgfArgs.size(); ++i)
            m_node.addMarked(getSgfPoint(m_sgfArgs.get(i)), MarkType.SELECT);
        }
        else if (m_sgfType.equals("OB"))
            endSgfMovesLeft(BLACK);
        else if (m_sgfType.equals("OW"))
            endSgfMovesLeft(WHITE);
        else if (m_sgfType.equals("PL"))
            endSgfPlayer();
        else
            m_node.addSgfProperty(m_sgfType, m_sgfArgs);
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

    private void endTime() throws SAXException
    {
        String time = getCharacters();
        try
        {
            m_info.setTimeSettings(TimeSettings.parse(time));
        }
        catch (ErrorMessage e)
        {
            setWarning("Unknown time settings: " + e.getMessage());
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
        GoPoint p;
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

    private void startComment() throws SAXException
    {
        checkParent("Nodes", "Node", "Variation");
        checkAttributes();
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
        if (m_atts.getValue("name") != null)
            // Not supported in game.GameInformation
            setWarning("Attribute \"name\" in element"
                       +" \"GoGame\" not supported");
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
                       + "\" for element \"" + m_element +"\"");
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
        if (m_node == null)
            createNode();
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
        checkAttributes("annotate", "at", "timeleft", "name", "number");
        if (! parentElement().equals("Node"))
            createNode();
        if (m_atts.getValue("name") != null)
            // Not allowed by DTD, but used by Jago 5.0
            setWarning("Ignoring attribute \"name\" in element \"" + m_element
                       + "\"");
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
        createNode();
        if (m_atts.getValue("name") != null)
            // Not supported in game.Node
            setWarning("Attribute \"name\" in element \"Node\" not supported");
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
        if (m_node == null)
            createNode();
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
