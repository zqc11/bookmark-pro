package org.bookmark.pro.domain.model;

/**
 * 抽象树节点模型
 *
 * @author Nonoas
 * @date 2023/6/6
 */
public interface AbstractTreeNodeModel {

    String getDesc();

    boolean isBookmark();

    boolean isGroup();

    String getName();

    String getCommitHash();

    String toString();

    // 是否为搜索重建所得
    boolean isSearchView();

    void setIsSearchView(boolean b);
}
