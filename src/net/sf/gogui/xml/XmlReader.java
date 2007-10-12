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
    Uses SAX for memory efficient parsing of large files.
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
                                  true);
            }
            catch (SAXException e)
            {
                setWarning("Could not activate XML validation");
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
            m_element = name;
            if (name.equals("Annotation"))
                checkParent("Information");
            else if (name.equals("Application"))
                checkParent("Information");
            else if (name.equals("AddBlack"))
                handleSetup(BLACK, atts);
            else if (name.equals("AddWhite"))
                handleSetup(WHITE, atts);
            else if (name.equals("Arg"))
                checkParent("SGF");
            else if (name.equals("at"))
                checkParent("Black", "White", "AddBlack", "AddWhite", "Delete",
                            "Mark");
            else if (name.equals("Black"))
                handleMove(BLACK, atts);
            else if (name.equals("BlackPlayer"))
                checkParent("Information");
            else if (name.equals("BlackRank"))
                checkParent("Information");
            else if (name.equals("BlackTeam"))
                checkParent("Information");
            else if (name.equals("BoardSize"))
                checkParent("Information");
            else if (name.equals("Comment"))
                checkParent("Nodes", "Node", "Variation");
            else if (name.equals("Copyright"))
                checkParent("Information");
            else if (name.equals("Date"))
                checkParent("Information");
            else if (name.equals("Delete"))
                handleSetup(EMPTY, atts);
            else if (name.equals("Go"))
                checkParent();
            else if (name.equals("GoGame"))
                handleGoGame();
            else if (name.equals("Handicap"))
                checkParent("Information");
            else if (name.equals("Information"))
                checkParent("GoGame");
            else if (name.equals("Komi"))
                checkParent("Information");
            else if (name.equals("Mark"))
                handleMark(atts);
            else if (name.equals("Node"))
                createNode();
            else if (name.equals("Nodes"))
                handleNodes();
            else if (name.equals("P"))
                checkParent("Comment", "Copyright");
            else if (name.equals("Result"))
                checkParent("Information");
            else if (name.equals("Round"))
                checkParent("Information");
            else if (name.equals("Source"))
                checkParent("Information");
            else if (name.equals("SGF"))
                handleSGF(atts);
            else if (name.equals("Time"))
                checkParent("Information");
            else if (name.equals("User"))
                checkParent("Information");
            else if (name.equals("Variation"))
                handleVariation();
            else if (name.equals("White"))
                handleMove(WHITE, atts);
            else if (name.equals("WhitePlayer"))
                checkParent("Information");
            else if (name.equals("WhiteRank"))
                checkParent("Information");
            else if (name.equals("WhiteTeam"))
                checkParent("Information");
            else
                setWarning("Ignoring unknown element: " + name);
            m_elementStack.push(name);
            m_characters.setLength(0);
        }

        public void endElement(String namespaceURI, String name,
                               String qualifiedName) throws SAXException
        {
            m_elementStack.pop();
            if (name.equals("Annotation"))
                m_info.set(StringInfo.ANNOTATION, getCharacters());
            else if (name.equals("Arg"))
                m_sgfArgs.add(getCharacters());
            else if (name.equals("at"))
                handleEndAt();
            else if (name.equals("BlackPlayer"))
                m_info.set(StringInfoColor.NAME, BLACK, getCharacters());
            else if (name.equals("BlackRank"))
                m_info.set(StringInfoColor.RANK, BLACK, getCharacters());
            else if (name.equals("BlackTeam"))
                m_info.set(StringInfoColor.TEAM, BLACK, getCharacters());
            else if (name.equals("BoardSize"))
                handleEndBoardSize();
            else if (name.equals("Comment"))
                appendComment();
            else if (name.equals("Copyright"))
                appendCopyright();
            else if (name.equals("Date"))
                m_info.set(StringInfo.DATE, getCharacters());
            else if (name.equals("Handicap"))
                handleEndHandicap();
            else if (name.equals("Komi"))
                handleEndKomi();
            else if (name.equals("P"))
                handleEndP();
            else if (name.equals("Result"))
                m_info.set(StringInfo.RESULT, getCharacters());
            else if (name.equals("Round"))
                m_info.set(StringInfo.ROUND, getCharacters());
            else if (name.equals("SGF"))
                handleEndSgf();
            else if (name.equals("Source"))
                m_info.set(StringInfo.SOURCE, getCharacters());
            else if (name.equals("Time"))
                handleEndTime();
            else if (name.equals("User"))
                m_info.set(StringInfo.USER, getCharacters());
            else if (name.equals("WhitePlayer"))
                m_info.set(StringInfoColor.NAME, WHITE, getCharacters());
            else if (name.equals("WhiteRank"))
                m_info.set(StringInfoColor.RANK, WHITE, getCharacters());
            else if (name.equals("WhiteTeam"))
                m_info.set(StringInfoColor.TEAM, WHITE, getCharacters());
            else if (name.equals("Variation"))
                m_node = m_variation.pop();
            m_characters.setLength(0);
        }

        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            m_characters.append(ch, start, length);
        }

        /** Return internal go.dtd, if file does not exist. */
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

    private void appendComment()
    {
        String comment = m_node.getComment();
        String mergedLines = getMergedLines();
        if (mergedLines.equals(""))
            return;
        if (comment == null)
            m_node.setComment(mergedLines);
        else
            m_node.setComment(comment + "\n\n" + mergedLines);
    }

    private void appendCopyright()
    {
        String copyright = m_info.get(StringInfo.COPYRIGHT);
        if (copyright == null)
            m_info.set(StringInfo.COPYRIGHT, getMergedLines());
        else
            m_info.set(StringInfo.COPYRIGHT, copyright + "\n\n"
                       + getMergedLines());
    }

    private void checkParent(String... parents) throws SAXException
    {
        String parent = parentElement();
        if (parents.length == 0)
        {
            if (parent == null)
                return;
            throwError("Element \"" + m_element + "\" must be root element.");
        }
        for (int i = 0; i < parents.length; ++i)
            if (parents[i].equals(parent))
                return;
        throwError("Element \"" + m_element + "\" cannot be child of \""
                   + parent + "\".");
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
                if (line.trim().equals(""))
                    continue;
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

    private void handleEndAt() throws SAXException
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

    private void handleEndBoardSize() throws SAXException
    {
        int boardSize = parseInt();
        if (boardSize < 1 || boardSize > GoPoint.MAX_SIZE)
            throw new SAXException("Unsupported board size");
        m_isBoardSizeKnown = true;
        m_boardSize = boardSize;
    }

    private void handleEndHandicap() throws SAXException
    {
        int handicap = parseInt();
        if (handicap == 1 || handicap < 0)
            setWarning("Ignoring invalif handicap: " + handicap);
        else
            m_info.setHandicap(handicap);
    }

    private void handleEndKomi() throws SAXException
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

    private void handleEndP() throws SAXException
    {
        String text = getCharacters();
        String parent = parentElement();
        if (parent.equals("Comment"))
            appendComment();
        else if (parent.equals("Copyright"))
            appendCopyright();
    }

    private void handleEndSgf() throws SAXException
    {
        if (m_sgfType == null)
            return;
        if (m_sgfType.equals("SL") && m_sgfArgs.size() > 0)
            m_node.addMarked(getSgfPoint(m_sgfArgs.get(0)), MarkType.SELECT);
        else
            m_node.addSgfProperty(m_sgfType, m_sgfArgs);
    }

    private void handleEndTime() throws SAXException
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

    private void handleGoGame() throws SAXException
    {
        checkParent("Go");
        if (++m_numberGames > 1)
            throwError("Multiple games per file not supported");
    }

    private void handleMark(Attributes atts) throws SAXException
    {
        checkParent("Node");
        if (m_node == null)
            createNode();
        m_markType = null;
        m_label = atts.getValue("label");
        String type = atts.getValue("type");
        String territory = atts.getValue("territory");
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
        String value = atts.getValue("at");
        if (value != null)
        {
            GoPoint p = getPoint(value);
            if (m_markType != null)
                m_node.addMarked(p, m_markType);
            if (m_label != null)
                m_node.setLabel(p, m_label);
        }
    }

    private void handleMove(GoColor c, Attributes atts) throws SAXException
    {
        checkParent("Node", "Nodes", "Variation");
        if (! parentElement().equals("Node"))
            createNode();
        String value = atts.getValue("at");
        if (value != null)
            m_node.setMove(Move.get(c, getPoint(value)));
    }

    private void handleNodes() throws SAXException
    {
        checkParent("GoGame");
        if (++m_numberTrees > 1)
            throwError("More than one Nodes element in element GoGame");
    }

    private void handleSetup(GoColor c, Attributes atts) throws SAXException
    {
        checkParent("Node");
        if (m_node == null)
            createNode();
        String value = atts.getValue("at");
        if (value != null)
            m_node.addStone(c, getPoint(value));
    }

    private void handleSGF(Attributes atts) throws SAXException
    {
        checkParent("Node");
        m_sgfType = atts.getValue("type");
        m_sgfArgs.clear();
    }

    private void handleVariation() throws SAXException
    {
        checkParent("Nodes", "Variation");
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
