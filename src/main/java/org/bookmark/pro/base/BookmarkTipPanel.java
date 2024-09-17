package org.bookmark.pro.base;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HtmlPanel;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.bookmark.pro.domain.model.AbstractTreeNodeModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 用于展示标签树节点的描述信息
 *
 * @author Nonoas
 * @date 2024/3/24 10:54
 */
public class BookmarkTipPanel extends JBPanel<BookmarkTipPanel> {

    public BookmarkTipPanel(AbstractTreeNodeModel model) {

        setLayout(new BorderLayout());

        TipHtmlPanel tipHtmlPanel = new TipHtmlPanel(model);

        JBScrollPane scrollPane = new JBScrollPane(tipHtmlPanel);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, Integer.MAX_VALUE));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty());

        add(scrollPane);
    }

    protected static class TipHtmlPanel extends HtmlPanel {
        public TipHtmlPanel(@NotNull AbstractTreeNodeModel nodeModel) {
            StringBuilder sb = new StringBuilder();
            sb.append("<h3>").append(nodeModel.getName()).append("</h3>");
            String desc = StringUtils.isNotBlank(nodeModel.getDesc()) ? nodeModel.getDesc() : I18N.get("tips.not.desc");
            sb.append("<p>").append(desc.replace("\n", "<br/>")).append("</p>");
            setBody(sb.toString());
            Border borderWithPadding = JBUI.Borders.empty(0, 10, 10, 10);
            setBorder(borderWithPadding);
            setSize(desc.indexOf("\n") * 10, (int) desc.lines().count() * 20 + 50);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension preferredSize = getSize();
            return new Dimension(Math.max(preferredSize.width, 300), preferredSize.height);
        }

        @Override
        protected @NotNull @Nls String getBody() {
            return getText();
        }
    }

}
