import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class SqlExtractor {

    private Log log = LogFactory.getLog(getClass());

    public static SqlExtractor getInstance() {
        return new SqlExtractor();
    }

    public String extractSql(XmlTag tag, Map<String, XmlElement> fragmentMap) {
        String sql = constructTextValue(tag, new HashMap<>(), fragmentMap)
            .trim();
        if (!sql.endsWith(";")) {
            sql = sql + ";";
        }
        return sql;
    }

    @NotNull
    private String constructTextValue(XmlTag base, Map<String, String> properties, Map<String, XmlElement> map) {
        StringBuilder sb = new StringBuilder();
        PsiElement[] children = base.getChildren();
        for (PsiElement child : children) {
            if (child instanceof XmlText) {
                XmlText xmlText = (XmlText) child;
                sb.append(stringify(xmlText, properties));
            } else if (child instanceof XmlTag) {
                XmlTag tag = (XmlTag) child;
                sb.append(stringify(tag, properties, map));
            } else if (child instanceof XmlComment) {
                XmlComment xmlComment = (XmlComment)child;
                sb.append(stringify(xmlComment, properties));
            }
        }
        return sb.toString();
    }

    /* For this to return anything you need an include that looks like this:

        <include refid="someinclude">
            <property name="prefix" value="Some"/>
            <property name="include_target" value="sometable"/>
        </include>

        It would then return a map of {prefix:Some, include_target:sometable}.

        Fairly unlikely as I suspect I'm the only person that knows this feature exists at work. :)
    */
    private Map<String, String> generateIncludePropertyMap(XmlTag tag) {
        HashMap<String, String> map = new HashMap<>();
        if (tag != null && "include".equals(tag.getName())) {
            for (PsiElement psiElement : tag.getChildren()) {
                if (psiElement instanceof XmlTag && "property".equals(((XmlTag)psiElement).getName())) {
                    // holy moly! an include property!
                    XmlTag property = (XmlTag)psiElement;
                    map.put(property.getAttributeValue("name"), property.getAttributeValue("value"));
                }
            }
        }
        return map;
    }

    private String stringify(XmlText xmlText, Map<String, String> properties) {
        return renderTextProperties(xmlText.getValue(), properties);
    }
    private String stringify(XmlComment xmlText, Map<String, String> properties) {
        return "/*" + renderTextProperties(xmlText.getText(), properties) + "*/";
    }

    private String stringify(XmlTag tag, Map<String, String> properties, Map<String, XmlElement> map) {
        String text = "";
        String id = getId(tag);

        if (id == null) {
            if (tag instanceof XmlTagImpl) {
                text = stringifyUnknownXmlTag((XmlTagImpl) tag);
            }
        } else {
            Map<String, String> localProperties = null;
            if ("include".equals(tag.getName())) {
                localProperties = generateIncludePropertyMap(tag);
                properties.putAll(localProperties);
            }

            id = renderTextProperties(id, properties);

            if (map.containsKey(id)) {
                XmlElement lookup = map.get(id);
                if (lookup instanceof XmlTag) {
                    XmlTag fragment = (XmlTag)lookup;
                    text = constructTextValue(fragment, properties, map);
                }
            }

            if (localProperties != null) {
                for (String key : localProperties.keySet()) {
                    properties.remove(key);
                }
            }
        }
        return text;
    }

    private String renderTextProperties(String text, Map<String, String> properties) {
        if (text.contains("${")) {
            for (String property : properties.keySet()) {
                String propertyMarker = "${" + property + "}";
                if (text.contains(propertyMarker)) {
                    text = text.replace(propertyMarker, properties.get(property));
                }
            }
        }
        return convertBindVariables(text);
    }

    private String stringifyUnknownXmlTag(XmlTagImpl tag) {
        return "/*" + tag.getChars() + "*/";
    }

    private String getId(XmlTag tag) {
        String id = tag.getAttributeValue("id");
        if (id == null && tag.getName().equals("include")) {
            id = tag.getAttributeValue("refid");
        }
        return id;
    }

    private String convertBindVariables(String textValue) {
        return textValue.replaceAll("#\\{[\\w|\\.]+\\}", "? /*$0*/");
    }
}
