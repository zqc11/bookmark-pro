package org.bookmark.pro.service.tree.component;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.io.input.TaggedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.bookmark.pro.domain.model.BookmarkNodeModel;
import org.bookmark.pro.domain.model.GroupNodeModel;
import org.bookmark.pro.service.ServiceContext;
import org.bookmark.pro.service.base.document.DocumentService;
import org.bookmark.pro.service.tree.TreeService;
import org.bookmark.pro.utils.BookmarkUtil;
import org.bookmark.pro.utils.SignatureUtil;
import org.bookmark.pro.windows.mark.BookmarkEditDialog;
import org.jsoup.internal.StringUtil;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.UUID;

/**
 * 书签树菜单
 *
 * @author Lyon
 * @date 2024/04/17
 */
public class BookmarkMenus {
    private final Project openProject;

    public static BookmarkMenus getInstance(Project project) {
        return ServiceContext.getContextAttribute(project).getBookmarkMenus();
    }

    public BookmarkMenus(Project project) {
        this.openProject = project;
    }

    /**
     * 添加树菜单
     *
     * @param bookmarkTree
     */
    public void addTreeMenus(BookmarkTree bookmarkTree) {
        JBPopupMenu addGroupMenu = createGroupMenu(bookmarkTree);
        JBPopupMenu treeMenus = createTreeMenus(bookmarkTree);

        bookmarkTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                int row = bookmarkTree.getClosestRowForLocation(e.getX(), e.getY());
                if (row < 0) {
                    return;
                }
                if (!bookmarkTree.isRowSelected(row)) {
                    bookmarkTree.setSelectionRow(row);
                }

                if (0 == row) {
                    // 点击空白区域
                    addGroupMenu.show(bookmarkTree, e.getX() + 16, e.getY());
                } else {
                    TreePath path = bookmarkTree.getSelectionPath();
                    if (Objects.isNull(path)) {
                        return;
                    }
                    // 分组上点击操作显示全部菜单
                    treeMenus.show(bookmarkTree, e.getX() + 16, e.getY());
                }
            }
        });
    }

    /**
     * 树上点击菜单
     *
     * @param bookmarkTree 书签树
     * @return {@link JBPopupMenu}
     */
    private JBPopupMenu createTreeMenus(final BookmarkTree bookmarkTree) {
        JBPopupMenu popupMenu = new JBPopupMenu();
        popupMenu.add(createEditMenu(bookmarkTree));
        popupMenu.add(createDeleteMenu(bookmarkTree));
        popupMenu.add(new JPopupMenu.Separator());
        popupMenu.add(addGroupMenu(bookmarkTree));
        return popupMenu;
    }

    /**
     * 创建分组菜单
     *
     * @param bookmarkTree 书签树
     * @return {@link JBPopupMenu}
     */
    private JBPopupMenu createGroupMenu(final BookmarkTree bookmarkTree) {
        JBPopupMenu popupMenuRoot = new JBPopupMenu();
        JBMenuItem imAddGroupRoot = new JBMenuItem("Add Group");
        popupMenuRoot.add(imAddGroupRoot);
        // 增加书签分组
        addActionListener(imAddGroupRoot, bookmarkTree);
        return popupMenuRoot;
    }

    /**
     * 创建分组组菜单
     *
     * @param bookmarkTree 书签树
     * @return {@link JBMenuItem}
     */
    private JBMenuItem addGroupMenu(final BookmarkTree bookmarkTree) {
        JBMenuItem addGroupMenu = new JBMenuItem("AddGroup");
        // 增加书签分组
        addActionListener(addGroupMenu, bookmarkTree);
        return addGroupMenu;
    }

    /**
     * 创建编辑菜单
     *
     * @param bookmarkTree 书签树
     * @return {@link JBMenuItem}
     */
    private JBMenuItem createEditMenu(final BookmarkTree bookmarkTree) {
        JBMenuItem editMenu = new JBMenuItem("Update");
        // 书签编辑操作
        editMenu.addActionListener(e -> {
            TreePath path = bookmarkTree.getSelectionPath();
            if (null == path) {
                return;
            }
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
            if (selectedNode.isGroup() && !selectedNode.isBookmark()) {
                // 修改书签分组
                GroupNodeModel nodeModel = (GroupNodeModel) selectedNode.getUserObject();
                // 校验规则
                InputValidatorEx validatorEx = inputString -> {
                    if (StringUtil.isBlank(inputString)) return "Group name is not empty";
                    return null;
                };
                String groupName = Messages.showInputDialog("name:", "EditGroup", null, nodeModel.getName(), validatorEx);
                if (StringUtil.isBlank(groupName)) {
                    return;
                }
                if (StringUtils.isEmpty(nodeModel.getCommitHash())) {
                    nodeModel.setCommitHash(UUID.randomUUID().toString());
                }
                if (!groupName.equals(nodeModel.getName())) {
                    nodeModel.setName(groupName);
                }
                BookmarkTree.getInstance(this.openProject).getModel().nodeChanged(selectedNode);
            } else {
                BookmarkNodeModel model = (BookmarkNodeModel) selectedNode.getUserObject();
                model.openFileDescriptor(this.openProject);
                Editor editor = FileEditorManager.getInstance(this.openProject).getSelectedTextEditor();
                if (editor == null) return;
                CaretModel caretModel = editor.getCaretModel();
                // 获取添加标记的行号
                int markLine = caretModel.getLogicalPosition().line;
                updateOneBookmark(this.openProject, editor, selectedNode, markLine);
            }
        });
        return editMenu;
    }

    /**
     * 更新一个书签
     *
     * @param project  项目
     * @param editor   编辑器
     * @param treeNode 树节点
     * @param markLine 标记行
     */
    private void updateOneBookmark(Project project, Editor editor, BookmarkTreeNode treeNode, int markLine) {
        // 原书签信息
        BookmarkNodeModel nodeModel = (BookmarkNodeModel) treeNode.getUserObject();
        // 书签可以添加的最大行号
        int maxLineNum = getMaxLine(editor);
        new BookmarkEditDialog(project, false).defaultNode(nodeModel, maxLineNum, true).showAndCallback((name, desc, lineNum, parentNode, enableGroup) -> {
            nodeModel.setName(name);
            nodeModel.setInvalid(false);
            if (lineNum != markLine) {
                // 再书签操作页更新过标记行，重新获取
                String markContent = BookmarkUtil.getAutoDescription(editor, lineNum);
                nodeModel.setMarkLineMd5(SignatureUtil.getMd5Digest(markContent));
                nodeModel.setLine(lineNum);
            }
            nodeModel.setInvalid(false);
            nodeModel.setGroup(enableGroup);
            nodeModel.setBookmark(true);
            treeNode.setGroup(enableGroup);
            treeNode.setBookmark(true);
            nodeModel.setDesc(desc);
            TreeService.getInstance(project).changeBookmarkNode(parentNode, treeNode);
        });
    }

    /**
     * 获取最大行号
     *
     * @param editor 编辑器
     * @return int
     */
    private int getMaxLine(Editor editor) {
        Document document = editor.getDocument();
        return document.getLineCount();
    }

    /**
     * 创建删除菜单
     *
     * @param bookmarkTree 书签树
     * @return {@link JBMenuItem}
     */
    private JBMenuItem createDeleteMenu(final BookmarkTree bookmarkTree) {
        JBMenuItem deleteMenu = new JBMenuItem("Delete");
        // 书签删除操作
        deleteMenu.addActionListener(e -> {
            int result = Messages.showOkCancelDialog(this.openProject, "Delete Selected Bookmark", "Delete Bookmark", "Delete", "Cancel", Messages.getQuestionIcon());
            if (result == Messages.CANCEL) {
                return;
            }
            // 获取选定的节点
            TreePath[] selectionPaths = bookmarkTree.getSelectionPaths();
            if (selectionPaths == null) {
                return;
            }
            for (TreePath path : selectionPaths) {
                BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
                BookmarkTreeNode parent = (BookmarkTreeNode) node.getParent();
                if (null == parent) {
                    continue;
                }
                bookmarkTree.removeNode(node);
            }
        });
        return deleteMenu;
    }

    /**
     * 添加操作侦听器
     *
     * @param item         项目
     * @param bookmarkTree 书签树
     */
    private void addActionListener(JBMenuItem item, final BookmarkTree bookmarkTree) {
        item.addActionListener(e -> {
            // 获取选定的节点
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) bookmarkTree.getLastSelectedPathComponent();
            if (null == selectedNode) {
                return;
            }

            @SuppressWarnings("all") InputValidatorEx validatorEx = inputString -> {
                if (StringUtil.isBlank(inputString)) return "Group name is not empty";
                return null;
            };

            @SuppressWarnings("all") String groupName = Messages.showInputDialog("name:", "AddGroup", null, null, validatorEx);

            if (StringUtil.isBlank(groupName)) {
                return;
            }

            BookmarkTreeNode parent;
            if (selectedNode.isGroup()) {
                parent = selectedNode;
            } else {
                parent = (BookmarkTreeNode) selectedNode.getParent();
            }

            // 新的分组节点
            BookmarkTreeNode groupNode = new BookmarkTreeNode(new GroupNodeModel(groupName, UUID.randomUUID().toString()), true);
            TreeService.getInstance(this.openProject).addBookmarkNode(parent, groupNode);
        });
    }
}
