import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ui.TextTransferable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SimpleAction extends AnAction {

    private final static SqlExtractor sqlExtractor = SqlExtractor.getInstance();
    private final static MybatisXmlUtil mybatisXmlUtil = MybatisXmlUtil.getInstance();
    private static Notification clipboardNotification = new Notification(
            "MyBATIS SQL Extractor Clipboard Copy Notification",
            "MyBATIS SQL Extractor",
            "SQL copied to clipboard",
            NotificationType.INFORMATION
    );

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        XmlDocument document = mybatisXmlUtil.findDocument(anActionEvent);
        Caret caret = anActionEvent.getData(CommonDataKeys.CARET);
        PsiElement selectedElement = mybatisXmlUtil.findSelectedElement(document, caret);
        XmlTag tag = mybatisXmlUtil.findMybatisQueryTag(selectedElement);

        if (tag != null) {
            String localName = tag.getLocalName();
            if (mybatisXmlUtil.isQueryTag(localName)) {
                Map<String, XmlElement> map = mybatisXmlUtil.getFragmentMap(document);
                String sqlName = tag.getAttributeValue("id");
                String sql = sqlExtractor.extractSql(tag, map);
                sql = "--BEGIN " + sqlName + "\n" + sql + "\n--END " + sqlName;
                sendExtractedSql(sqlName, sql);
            }
        }
    }

    private void sendExtractedSql(String sqlName, String sql) {
        CopyPasteManager.getInstance().setContents(new TextTransferable(sql));

        clipboardNotification.setContent("SQL for `" + sqlName + "` copied to clipboard");
        Notifications.Bus.notify(clipboardNotification);
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        XmlDocument document = mybatisXmlUtil.findDocument(anActionEvent);
        Caret caret = anActionEvent.getData(CommonDataKeys.CARET);
        PsiElement selectedElement = mybatisXmlUtil.findSelectedElement(document, caret);
        XmlTag tag = mybatisXmlUtil.findMybatisQueryTag(selectedElement);

        boolean enabledAndVisible = false;
        if (tag != null) {
            String localName = tag.getLocalName();
            enabledAndVisible = mybatisXmlUtil.isQueryTag(localName);
        }
        anActionEvent.getPresentation().setEnabledAndVisible(enabledAndVisible);
    }

}

