// XmlUtil.java

package net.sf.gogui.util;

/** Static utility functions related to XML. */
public final class XmlUtil
{
    /** Escape XML special characters for attribute values.
        This assumes that the attribute will be quotes with ", it does
        not escape '. Also strips invalid XML characters. */
    public static String escapeAttr(String text)
    {
        int len = text.length();
        StringBuilder result = new StringBuilder(len);
        for (int i = 0; i < len; ++i)
        {
            char c = text.charAt(i);
            if (isInvalidXml(c))
                continue;
            switch (c)
            {
            case '>':
                result.append("&gt;");
                break;
            case '<':
                result.append("&lt;");
                break;
            case '&':
                result.append("&amp;");
                break;
            case '"':
                result.append("&quot;");
                break;
            default:
                result.append(c);
            }
        }
        return result.toString();
    }

    /** Escape XML special characters for text content.
        Also strips invalid XML characters. */
    public static String escapeText(String text)
    {
        int len = text.length();
        StringBuilder result = new StringBuilder(len);
        for (int i = 0; i < len; ++i)
        {
            char c = text.charAt(i);
            if (isInvalidXml(c))
                continue;
            switch (c)
            {
            case '>':
                result.append("&gt;");
                break;
            case '<':
                result.append("&lt;");
                break;
            case '&':
                result.append("&amp;");
                break;
            default:
                result.append(c);
            }
        }
        return result.toString();
    }

    /** Make constructor unavailable; class is for namespace only. */
    private XmlUtil()
    {
    }

    /** See http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char */
    private static boolean isInvalidXml(char c)
    {
        return ! (c == 0x9 || c == 0xA || c == 0xD
                  || (c >= 0x20 && c <= 0xD7FF)
                  || (c >= 0xE000 && c <= 0xFFFD)
                  || (c >= 0x10000 && c <= 0x10FFFF));
    }
}
