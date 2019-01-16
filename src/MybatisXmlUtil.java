import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MybatisXmlUtil {

    public static MybatisXmlUtil getInstance() {
        return new MybatisXmlUtil();
    }

    private final static List<String> QUERY_TAGS = Arrays.asList("select", "insert", "update", "procedure", "sql");

    public XmlDocument findDocument(AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        XmlDocument document = null;
        if (psiFile instanceof XmlFile) {
            XmlFile xmlFile = (XmlFile) psiFile;
            document = xmlFile.getDocument();
        }
        return document;
    }

    public PsiElement findSelectedElement(XmlDocument document, Caret caret) {
        if (document != null && caret != null) {
            return findSelectedElement(document, caret.getOffset());
        }
        return null;
    }

    public PsiElement findSelectedElement(XmlDocument document, int offset) {
        PsiElement tagElement = null;
        if (document != null && document.getOriginalElement() != null) {
            tagElement = document.getOriginalElement().findElementAt(offset);
        }
        return tagElement;
    }

    // Given an element, search upwards until you find a MyBATIS query tag.
    // e.g. if your given element is a logic tag like an "<if>" or "<foreach>" embedded inside a "<select>",
    // search upwards for the select.
    public XmlTag findMybatisQueryTag(PsiElement selectedElement) {
        XmlTag tag = null;
        if (selectedElement != null) {
            while ((!(selectedElement instanceof XmlTag) && !(selectedElement instanceof XmlFile)) ||
                    (selectedElement instanceof XmlTag && !isQueryTag(((XmlTag)selectedElement).getName()))
                ) {
                selectedElement = selectedElement.getParent();
            }
            if (selectedElement instanceof XmlTag && isQueryTag(((XmlTag)selectedElement).getName())) {
                tag = (XmlTag) selectedElement;
            }
        }
        return tag;
    }

    // Returns true if the user right-clicked in a tag thought to contain a SQL statement we can parse and extract from
    public boolean isQueryTag(String localName) {
        return QUERY_TAGS.contains(localName);
    }

    public Map<String, XmlElement> getFragmentMap(XmlDocument document) {

        /* GGC TODO - Take into account namespacing, e.g. <mapper namespace="ShowSchedule">
         *  which leads to includes like <include refid="ShowSchedule.showFragmentThing"/>
         */

        for (int i = 0; i < document.getChildren().length; i++) {
            PsiElement elt = document.getChildren()[i];
            if (elt instanceof XmlTag) {
                XmlTag tag = (XmlTag)elt;
                if (tag.getName().equals("mapper")) {
                    return getChildElementMap(tag, "id", "sql");
                }
            }
        }
        return new HashMap<>();
    }

    private Map<String, XmlElement> getChildElementMap(PsiElement base, String attributeKey, String... tagNames) {
        Map<String, XmlElement> map = new HashMap<>();
        for (int i = 0; i < base.getChildren().length; i++) {
            PsiElement elt = base.getChildren()[i];
            if (elt instanceof XmlTag) {
                XmlTag tag = (XmlTag)elt;
                if (Arrays.stream(tagNames).anyMatch(t->t.equals(tag.getName())) && tag.getAttributeValue(attributeKey) != null) {
                    String key = tag.getAttributeValue(attributeKey);
                    map.put(key, tag);
                }
            }
        }
        return map;
    }
}
