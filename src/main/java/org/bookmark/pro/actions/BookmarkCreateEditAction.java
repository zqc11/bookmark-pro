package org.bookmark.pro.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.bookmark.pro.context.BookmarkRunService;
import org.bookmark.pro.dialogs.modify.BookmarkEditDialog;
import org.bookmark.pro.domain.model.BookmarkNodeModel;
import org.bookmark.pro.utils.BookmarkProUtil;
import org.bookmark.pro.utils.CharacterUtil;
import org.bookmark.pro.utils.SignatureUtil;
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
        // 创建书签操作
        createOneBookmark(project, editor, file, caretModel, markLine);
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
        bookmarkModel.setUuid(UUID.randomUUID().toString());
        bookmarkModel.setLine(markLine);
        // 获取标记行内容
        bookmarkModel.setMarkLineMd5(SignatureUtil.getMd5Digest(BookmarkProUtil.getAutoDescription(editor, markLine)));
        bookmarkModel.setInvalid(false);
        bookmarkModel.setColumn(column);
        // 设置书签标记文档
        bookmarkModel.setVirtualFile(file);
        if (CharacterUtil.isNotEmpty(selectedText)) {
            bookmarkModel.setDesc(selectedText);
        }
        bookmarkModel.setName(file.getName());

        // 新建书签窗口
        new BookmarkEditDialog(project, true).defaultNode(bookmarkModel, getMaxLine(editor), true).showAndCallback((name, desc, lineNum, parentNode, enableGroup) -> {
            if (lineNum != markLine) {
                // 再书签操作页更新过标记行，重新获取
                String markContent = BookmarkProUtil.getAutoDescription(editor, lineNum);
                bookmarkModel.setMarkLineMd5(SignatureUtil.getMd5Digest(markContent));
                bookmarkModel.setLine(lineNum);
            }
            bookmarkModel.setName(name);
            bookmarkModel.setGroup(enableGroup);
            bookmarkModel.setBookmark(true);
            bookmarkModel.setDesc(desc);
            // 添加书签记录
            BookmarkRunService.getBookmarkManagerPanel(project).addOneBookmark(project, parentNode, bookmarkModel);
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
