package com.ds.university.vo;

/** 选课目录中的开课班（含当前学生是否已选标记） */
public class CatalogSectionVO extends SectionVO {

    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}