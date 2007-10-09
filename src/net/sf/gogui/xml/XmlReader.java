//----------------------------------------------------------------------------
// XmlReader.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
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
            reader.parse(new InputSource(in));
            int size;
            if (m_isBoardSizeKnown)
                size = m_boardSize;
            else
                size = Math.max(DEFAULT_BOARDSIZE, m_boardSize);
            m_tree = new GameTree(size, m_root);
            m_tree.getGameInformation(m_root).copyFrom(m_gameInformation);
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
        public void startElement(String namespaceURI, String localName,
                                 String qualifiedName, Attributes atts)
            throws SAXException
        {    
            if (localName.equals("AddBlack"))
                handleSetup(BLACK, atts);
            else if (localName.equals("AddWhite"))
                handleSetup(WHITE, atts);
            else if (localName.equals("Black"))
                handleMove(BLACK, atts);
            else if (localName.equals("Delete"))
                handleSetup(EMPTY, atts);
            else if (localName.equals("GoGame"))
                handleGoGame();
            else if (localName.equals("Mark"))
                handleMark(atts);
            else if (localName.equals("Node"))
                createNode();
            else if (localName.equals("Nodes"))
                handleNodes();
            else if (localName.equals("SGF"))
                handleSGF(atts);
            else if (localName.equals("Variation"))
                handleVariation();
            else if (localName.equals("White"))
                handleMove(WHITE, atts);
            else if (! localName.equals("Application")
                     && ! localName.equals("Arg")
                     && ! localName.equals("BlackPlayer")
                     && ! localName.equals("BlackRank")
                     && ! localName.equals("BoardSize")
                     && ! localName.equals("Comment")
                     && ! localName.equals("Go")
                     && ! localName.equals("Information")
                     && ! localName.equals("WhitePlayer")
                     && ! localName.equals("WhiteRank"))
                setWarning("Ignoring unknown element: " + localName);
            m_element.push(localName);
            m_characters.setLength(0);
        }

        public void endElement(String namespaceURI, String localName,
                               String qualifiedName) throws SAXException
        {
            if (localName.equals("Arg"))
                handleEndArg();
            else if (localName.equals("BlackPlayer"))
                handleEndPlayer(BLACK);
            else if (localName.equals("BlackRank"))
                handleEndRank(BLACK);
            else if (localName.equals("BoardSize"))
                handleEndBoardSize();
            else if (localName.equals("Comment"))
                handleEndComment();
            else if (localName.equals("SGF"))
                handleEndSgf();
            else if (localName.equals("WhitePlayer"))
                handleEndPlayer(WHITE);
            else if (localName.equals("WhiteRank"))
                handleEndRank(WHITE);
            else if (localName.equals("Variation"))
                handleEndVariation();
            m_element.pop();
        }

        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            m_characters.append(ch, start, length);
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

    /** Current element. */
    private Stack<String> m_element = new Stack<String>();

    /** Current node. */
    private Node m_node;

    private Stack<Node> m_variation = new Stack<Node>();

    private GameInformation m_gameInformation = new GameInformation();

    private Node m_root;

    private GameTree m_tree;

    /** Type of current SGF element. */
    private String m_sgfType;

    /** Arguments of current SGF element. */
    private ArrayList<String> m_sgfArgs = new ArrayList<String>();

    /** Characters in current element. */
    private StringBuilder m_characters = new StringBuilder();

    /** Contains strings with warnings. */
    private final Set<String> m_warnings = new TreeSet<String>();

    private void createNode()
    {
        Node node = new Node();
        if (m_node != null)
            m_node.append(node);
        else if (! m_variation.isEmpty())
            m_variation.peek().getFather().append(node);
        m_node = node;
    }

    private String currentElement()
    {
        if (m_element.isEmpty())
            return null;
        return m_element.peek();
    }

    private GoPoint getAt(Attributes atts) throws SAXException
    {
        String value = atts.getValue("at");
        if (value == null)
            throwError("Missing at-property");
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

    private void handleEndArg() throws SAXException
    {
        m_sgfArgs.add(m_characters.toString());
    }

    private void handleEndBoardSize() throws SAXException
    {
        try
        {
            int boardSize = Integer.parseInt(m_characters.toString());
            if (boardSize < 1 || boardSize > GoPoint.MAX_SIZE)
                throw new SAXException("Unsupported board size");
            m_isBoardSizeKnown = true;
            m_boardSize = boardSize;
        }
        catch (NumberFormatException e)
        {
            throw new SAXException("Invalid board size");
        }
    }

    private void handleEndComment() throws SAXException
    {
        m_node.setComment(m_characters.toString());
    }

    private void handleEndPlayer(GoColor c) throws SAXException
    {
        m_gameInformation.setPlayer(c, m_characters.toString());
    }

    private void handleEndRank(GoColor c) throws SAXException
    {
        m_gameInformation.setRank(c, m_characters.toString());
    }

    private void handleEndSgf() throws SAXException
    {
        if (m_sgfType != null)
            m_node.addSgfProperty(m_sgfType, m_sgfArgs);
    }

    private void handleEndVariation() throws SAXException
    {
        m_node = m_variation.pop();
    }

    private void handleGoGame() throws SAXException
    {
        if (++m_numberGames > 1)
            throwError("Multiple games per file not supported");
    }            

    private void handleMark(Attributes atts) throws SAXException
    {
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
        if (! currentElement().equals("Node"))
            createNode();
        GoPoint p = getAt(atts);
        m_node.setMove(Move.get(c, p));
    }

    private void handleNodes() throws SAXException
    {
        if (++m_numberTrees > 1)
            throwError("More than one Nodes elements in element GoGame");
    }            

    private void handleSetup(GoColor c, Attributes atts) throws SAXException
    {
        if (m_node == null)
            createNode();
        GoPoint p = getAt(atts);
        m_node.addStone(c, p);
    }

    private void handleSGF(Attributes atts) throws SAXException
    {
        m_sgfType = atts.getValue("type");
        m_sgfArgs.clear();
    }

    private void handleVariation() throws SAXException
    {
        if (m_node == null)
            throwError("Variation without main node");
        assert m_node.hasFather();
        m_variation.push(m_node);
        m_node = null;
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
