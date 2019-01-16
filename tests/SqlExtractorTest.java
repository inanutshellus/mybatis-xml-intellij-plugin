import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.*;
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;

import java.util.Map;

public class SqlExtractorTest extends CodeInsightFixtureTestCase {

    private final static MybatisXmlUtil util = MybatisXmlUtil.getInstance();
    private final static SqlExtractor sqlExtractor = SqlExtractor.getInstance();

    // Demonstrate that we can extract from <sql> fragment tags, even complex ones.
    public void testCompositeFragment() throws Exception {
        String tagName = "sql";
        String queryName = "CompositeFragment";
        int offset =1594; // somewhere in the "CompositeFragment" sql fragment tag
        String expectedSql = "select foo\n" +
            "    \n" +
            "        \n" +
            "        from bar\n" +
            "        \n" +
            "        join baaz on bar.id = baaz.bar_id\n" +
            "    \n" +
            "    \n" +
            "        \n" +
            "        join bux on bar.id = bux.bar_id\n" +
            "        /*<dynamic>\n" +
            "            <ifNull test=\"something\">stuff</ifNull>\n" +
            "        </dynamic>*/;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testBasicSelect() throws Exception {
        String tagName = "select";
        String queryName = "basicSelect";
        int offset =1800; // somewhere in the "basicSelect" select tag
        String expectedSql = "select stuff from somewhere;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testBasicInsert() throws Exception {
        String tagName = "insert";
        String queryName = "basicInsert";
        int offset =1850; // somewhere in the "basicInsert" update tag
        String expectedSql = "insert into somewhere (stuff) values ('stuffity stuff');";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testBasicUpdate() throws Exception {
        String tagName = "update";
        String queryName = "basicUpdate";
        int offset =1970; // somewhere in the "basicUpdate" update tag
        String expectedSql = "update somewhere set stuff=? /*#{stuffityStuff}*/ where 1=2;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testSelectWithCDATA() throws Exception {
        String tagName = "select";
        String queryName = "selectWithCDATA";
        int offset =2171; // somewhere in the "selectWithCDATA" select tag
        String expectedSql = "select stuff from somewhere\n" +
                "        where start_date  <  ? /*#{endDate}*/\n" +
                "        and end_date > ? /*#{startDate}*/;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testSelectWithXmlComment() throws Exception {
        String tagName = "select";
        String queryName = "selectWithXMLComment";
        int offset =2350; // somewhere in the "selectWithXMLComment" tag
        String expectedSql = "select stuff from somewhere\n" +
                "        /*<!-- Yay an xml comment -->*/\n" +
                "        where stuff = other_stuff;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    // This example is particularly freaky. It comes straight from http://www.mybatis.org/mybatis-3/sqlmap-xml.html
    // and has you using a property value to define an include's name. Yeesh. Just go look.
    public void testSelectWithIncludeProperties() throws Exception {
        String tagName = "select";
        String queryName = "selectWithIncludeProperties";
        int offset = 2660; // somewhere in the "selectWithIncludeProperties" select tag
        String expectedSql = "select\n" +
                "        field1, field2, field3\n" +
                "        \n" +
                "        from\n" +
                "        \n" +
                "        SomeTable;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testSelectWithLogicTags_UserCanRightClickInsideLogicTags() throws Exception {
        String tagName = "select";
        String queryName = "selectWithLogicTags";
        int offset = 3128; // inside the "<foreach>" tag of the "selectWithIncludeProperties" select tag
        String expectedSql = "select\n" +
            "        field1, field2, field3\n" +
            "        from sometable\n" +
            "        where id in\n" +
            "        /*<foreach item=\"someId\" collection=\"list\" open=\"(\" close=\")\" separator=\",\">\n" +
            "            #{someId}\n" +
            "        </foreach>*/\n" +
            "\n" +
            "        /*<if test=\"someField != null\">\n" +
            "            and somefield = 'Foo'\n" +
            "        </if>*/\n" +
            "        order by field1;";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    public void testSelectWithNoContent() throws Exception {
        String tagName = "select";
        String queryName = "nullTagTest";
        int offset = 4600; // within the "<select/>" tag's attributes of the "nullTagTest" select tag.
        String expectedSql = ";";
        verifyQueryAtOffset(tagName, queryName, offset, expectedSql);
    }

    private void verifyQueryAtOffset(String tagName, String queryName, int offset, String expectedSql) {
        XmlFile file = (XmlFile)myFixture.configureByFile("tests/testData/SqlExample.xml");
        XmlDocument document = file.getDocument();
        PsiElement selectedElement = util.findSelectedElement(document, offset);
        assertTrue(selectedElement != null);

        XmlTag xmlTag = util.findMybatisQueryTag(selectedElement);
        assertTrue(xmlTag != null);
        assertTrue(xmlTag.getName(), tagName.equals(xmlTag.getName()));

        XmlAttribute id = xmlTag.getAttribute("id");
        assertTrue(id != null);
        assertTrue(queryName.equals(id.getValue()));

        String sqlName = xmlTag.getAttributeValue("id");
        assertTrue(queryName.equals(sqlName));

        Map<String, XmlElement> map = util.getFragmentMap(document);
        String sql = sqlExtractor.extractSql(xmlTag, map);

        assertTrue(sql != null);
        assertTrue(sql, sql.trim().equals(expectedSql));
    }

}
