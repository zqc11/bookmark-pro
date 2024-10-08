package org.bookmark.pro.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.bookmark.pro.constants.BookmarkIcons;
import org.bookmark.pro.domain.model.BookmarkNodeModel;
import org.bookmark.pro.service.base.document.DocumentService;
import org.bookmark.pro.service.tree.TreeService;
import org.bookmark.pro.service.tree.component.BookmarkTreeNode;
import org.bookmark.pro.utils.BookmarkUtil;
import org.bookmark.pro.utils.SignatureUtil;
import org.bookmark.pro.windows.BookmarkPanel;
import org.bookmark.pro.windows.mark.BookmarkEditDialog;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 书签创建或编辑
 *
 * @author Lyon
 * @date 2024/03/21
 */
public class BookmarkCreateEditAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || editor == null || file == null) {
            return;
        }
        // 创建或编辑书签组件
        CaretModel caretModel = editor.getCaretModel();
        // 获取添加标记的行号
        int markLine = caretModel.getLogicalPosition().line;
        BookmarkTreeNode treeNode = DocumentService.getInstance(project).getBookmarkNode(file, markLine);
        if (treeNode != null) {
            updateOneBookmark(project, editor, treeNode, markLine);
        } else {
            // 创建书签操作
            createOneBookmark(project, editor, file, caretModel, markLine);
        }
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
        // 添加标记行的内容
        String markLineContent = BookmarkUtil.getAutoDescription(editor, markLine);
        // 原书签信息
        BookmarkNodeModel nodeModel = (BookmarkNodeModel) treeNode.getUserObject();
        // 本行内容的MD5值
        String contentMd5 = SignatureUtil.getMd5Digest(markLineContent);
        // 书签可以添加的最大行号
        int maxLineNum = getMaxLine(editor);
        if (contentMd5.equals(nodeModel.getMarkLineMd5())) {
            new BookmarkEditDialog(project, false).defaultNode(nodeModel, maxLineNum, true).showAndCallback((name, desc, lineNum, parentNode, enableGroup) -> {
                TreeService.getInstance(project).removeBookmarkNode(treeNode);
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
                TreeService.getInstance(project).addBookmarkNode(parentNode, treeNode);
            });
        } else {
            // 不一致 置为失效书签
            treeNode.setInvalid(true);
            nodeModel.setInvalid(true);
            TreeService.getInstance(project).changeBookmarkNode(null, treeNode);
            // 更新书签操作
            new BookmarkEditDialog(project, false).defaultNode(nodeModel, maxLineNum, true).defaultWarning(BookmarkIcons.INVALID_SIGN).showAndCallback((name, desc, lineNum, parentNode, enableGroup) -> {
                TreeService.getInstance(project).removeBookmarkNode(treeNode);
                nodeModel.setName(name);
                nodeModel.setDesc(desc);
                if (lineNum != markLine) {
                    // 再书签操作页更新过标记行，重新获取
                    String markContent = BookmarkUtil.getAutoDescription(editor, lineNum);
                    nodeModel.setMarkLineMd5(SignatureUtil.getMd5Digest(markContent));
                    nodeModel.setLine(lineNum);
                } else {
                    nodeModel.setMarkLineMd5(contentMd5);
                }
                nodeModel.setInvalid(false);
                nodeModel.setGroup(enableGroup);
                nodeModel.setBookmark(true);
                treeNode.setGroup(enableGroup);
                treeNode.setBookmark(true);
                treeNode.setInvalid(false);
                TreeService.getInstance(project).addBookmarkNode(parentNode, treeNode);
            });
        }
        TreeService.getInstance(project).changeBookmarkNode(treeNode);
    }

    /**
     * 创建一个书签
     *
     * @param project    项目
     * @param editor     编辑 器
     * @param file       文件
     * @param caretModel 插入符号模型
     * @param markLine   标记行
     */
    private void createOneBookmark(Project project, Editor editor, VirtualFile file, CaretModel caretModel, int markLine) {
        // 获取添加标记的列
        int column = caretModel.getLogicalPosition().column;
        // 获取选中文本
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        selectedText = selectedText == null ? "" : (" " + selectedText + " ");
        // 书签唯一标识
        BookmarkNodeModel bookmarkModel = new BookmarkNodeModel();
        bookmarkModel.setCommitHash(UUID.randomUUID().toString());
        bookmarkModel.setLine(markLine);
        // 获取标记行内容
        bookmarkModel.setMarkLineMd5(SignatureUtil.getMd5Digest(BookmarkUtil.getAutoDescription(editor, markLine)));
        bookmarkModel.setInvalid(false);
        bookmarkModel.setColumn(column);
        // 设置书签标记文档
        bookmarkModel.setVirtualFile(file);
        if (StringUtils.isNotBlank(selectedText)) {
            bookmarkModel.setDesc(selectedText);
        }
        bookmarkModel.setName(file.getName());

        // 新建书签窗口
        new BookmarkEditDialog(project, true).defaultNode(bookmarkModel, getMaxLine(editor), true).showAndCallback((name, desc, lineNum, parentNode, enableGroup) -> {
            if (lineNum != markLine) {
                // 再书签操作页更新过标记行，重新获取
                String markContent = BookmarkUtil.getAutoDescription(editor, lineNum);
                bookmarkModel.setMarkLineMd5(SignatureUtil.getMd5Digest(markContent));
                bookmarkModel.setLine(lineNum);
            }
            bookmarkModel.setName(name);
            bookmarkModel.setGroup(enableGroup);
            bookmarkModel.setBookmark(true);
            bookmarkModel.setDesc(desc);
            // 添加书签记录
            BookmarkPanel.getInstance(project).addOneBookmark(parentNode, bookmarkModel);
        });
    }

    /**
     * 获取最大行号
     *
     * @param editor 编辑 器
     * @return int
     */
    private int getMaxLine(Editor editor) {
        Document document = editor.getDocument();
        return document.getLineCount();
    }

}
