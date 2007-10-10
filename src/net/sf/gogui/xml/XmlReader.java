//----------------------------------------------------------------------------
// XmlReader.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import net.sf.gogui.game.GameInformation;
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
            reader.setContentHandler(new ContentHandler());
            reader.setEntityResolver(new Resolver());
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
            m_tree.getGameInformation(m_root).copyFrom(m_info);
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

    private class ContentHandler
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
            if (name.equals("Annotation"))
                m_info.set(StringInfo.ANNOTATION, getCharacters());
            else if (name.equals("Arg"))
                m_sgfArgs.add(getCharacters());
            else if (name.equals("BlackPlayer"))
                m_info.set(StringInfoColor.NAME, BLACK, getCharacters());
            else if (name.equals("BlackRank"))
                m_info.set(StringInfoColor.RANK, BLACK, getCharacters());
            else if (name.equals("BlackTeam"))
                m_info.set(StringInfoColor.TEAM, BLACK, getCharacters());
            else if (name.equals("BoardSize"))
                handleEndBoardSize();
            else if (name.equals("Comment"))
                m_node.setComment(getCharacters());
            else if (name.equals("Copyright"))
                m_info.set(StringInfo.COPYRIGHT, getCharacters());
            else if (name.equals("Date"))
                m_info.set(StringInfo.DATE, getCharacters());
            else if (name.equals("Handicap"))
                handleEndHandicap();
            else if (name.equals("Komi"))
                handleEndKomi();
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
            m_elementStack.pop();
        }

        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            m_characters.append(ch, start, length);
        }
    }

    private class Resolver
        implements EntityResolver
    {
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

    private GameInformation m_info = new GameInformation();

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

    private GoPoint getAt(Attributes atts) throws SAXException
    {
        String value = atts.getValue("at");
        if (value == null)
            throwError("Missing at-property");
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

    private String getCharacters()
    {
        return m_characters.toString();
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

    private void handleEndSgf() throws SAXException
    {
        if (m_sgfType != null)
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
        GoPoint p = getAt(atts);
        String type = atts.getValue("type");
        String label = atts.getValue("label");
        String territory = atts.getValue("territory");
        if (label != null)
            m_node.setLabel(p, label);
        if (type != null)
        {
            if (type.equals("triangle"))
                m_node.addMarked(p, MarkType.TRIANGLE);
            else if (type.equals("circle"))
                m_node.addMarked(p, MarkType.CIRCLE);
            else if (type.equals("square"))
                m_node.addMarked(p, MarkType.SQUARE);
            else
                setWarning("Unknown mark type " + type);
        }
        if (territory != null)
        {
            if (territory.equals("black"))
                m_node.addMarked(p, MarkType.TERRITORY_BLACK);
            else if (territory.equals("white"))
                m_node.addMarked(p, MarkType.TERRITORY_WHITE);
            else
                setWarning("Unknown territory type " + territory);
        }
        if (type == null && territory == null && label == null)
            m_node.addMarked(p, MarkType.MARK);
    }

    private void handleMove(GoColor c, Attributes atts) throws SAXException
    {
        checkParent("Node", "Nodes", "Variation");
        if (! parentElement().equals("Node"))
            createNode();
        GoPoint p = getAt(atts);
        m_node.setMove(Move.get(c, p));
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
        GoPoint p = getAt(atts);
        m_node.addStone(c, p);
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
        throw new SAXException(message);
    }
}
