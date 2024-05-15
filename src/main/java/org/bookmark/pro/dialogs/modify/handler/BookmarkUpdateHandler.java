package org.bookmark.pro.dialogs.modify.handler;

import com.intellij.openapi.project.Project;
import org.bookmark.pro.context.BookmarkRunService;
import org.bookmark.pro.dialogs.modify.BookmarkEditService;
import org.bookmark.pro.dialogs.modify.BookmarkSelector;
import org.bookmark.pro.domain.model.BookmarkNodeModel;
import org.bookmark.pro.service.document.DocumentService;
import org.bookmark.pro.service.tree.handler.BookmarkTreeNode;
import org.bookmark.pro.utils.BookmarkEditorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 书签更新处理程序
 *
 * @author Lyon
 * @date 2024/04/26
 */
public class BookmarkUpdateHandler extends BaseServiceUtil implements BookmarkEditService {
    // 行号
    private int nowLineNumber = 3;

    // 父节点
    private BookmarkTreeNode parentNode;

    /**
     * 书签启用分组
     */
    private JCheckBox enableGroup;

    @Override
    public void lineNumInspect(JPanel panel, GridBagConstraints constraints, JSpinner bookmarkLineNum, int value, int maxValue) {
        SpinnerModel model = new SpinnerNumberModel(value, 1, maxValue, 1);
        BookmarkEditorUtil.addNumberFormatter(bookmarkLineNum, model);
        bookmarkLineNum(panel, constraints, maxValue, this.nowLineNumber);
        this.nowLineNumber = this.nowLineNumber + 1;
    }

    @Override
    public void setLapseWarning(JPanel panel, GridBagConstraints constraints, String message) {
        message = message + ", please in editor update and save bookmark.";
        bookmarkWarning(panel, constraints, message, this.nowLineNumber);
        this.nowLineNumber = this.nowLineNumber + 1;
    }

    @Override
    public void showBookmarkParent(Project project, JPanel panel, GridBagConstraints constraints, BookmarkNodeModel node) {
        DocumentService documentService = BookmarkRunService.getDocumentService(project);
        // 获取父级书签下拉选项书签
        JComboBox<BookmarkTreeNode> bookmarkType = new JComboBox<>();
        BookmarkTreeNode treeNode = documentService.getBookmarkNode(node.getUuid());
        BookmarkTreeNode nodeModel = (BookmarkTreeNode) treeNode.getParent();
        // 查询所有书签和分组
        for (BookmarkTreeNode bookmarkTreeNode : documentService.getBookmarkGroup()) {
            if (nodeModel.equals(bookmarkTreeNode)) {
                // 获取书签已选择的父级
                this.parentNode = bookmarkTreeNode;
            }
            if (bookmarkTreeNode.equals(treeNode)) {
                // 书签分组跳过自己
                continue;
            }
            bookmarkType.addItem(bookmarkTreeNode);
        }
        // 设置父级菜单
        if (this.parentNode != null) {
            bookmarkType.setSelectedItem(this.parentNode);
            bookmarkParent(panel, constraints, this.nowLineNumber, bookmarkType);
            this.nowLineNumber = this.nowLineNumber + 1;
            bookmarkType.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    parentNode = (BookmarkTreeNode) bookmarkType.getSelectedItem();
                }
            });
        }
    }

    @Override
    public void showBookmarkEnable(JPanel panel, GridBagConstraints constraints, BookmarkNodeModel nodeModel) {
        this.enableGroup = new JCheckBox("Bookmark can be used as group");
        this.enableGroup.setSelected(nodeModel.isGroup());
        bookmarkEnableGroup(panel, constraints, this.nowLineNumber, this.enableGroup);
        this.nowLineNumber = this.nowLineNumber + 1;
    }

    @Override
    public void getSelectorMessage(BookmarkSelector selector) {
        selector.selector(this.parentNode, this.enableGroup.isSelected());
    }
}
