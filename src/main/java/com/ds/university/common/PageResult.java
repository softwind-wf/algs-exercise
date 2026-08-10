package com.ds.university.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用分页结果。
 *
 * @param <T> 记录类型
 */
public class PageResult<T> {

    private final List<T> records;
    private final int page;       // 当前页码，从 1 开始
    private final int size;       // 每页条数
    private final long total;     // 总记录数
    private final int totalPages; // 总页数

    public PageResult(List<T> records, int page, int size, long total) {
        this.records = records == null ? new ArrayList<>() : records;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
    }

    public List<T> getRecords() {
        return records;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasPrev() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < totalPages;
    }

    public int getPrevPage() {
        return Math.max(1, page - 1);
    }

    public int getNextPage() {
        return Math.min(Math.max(totalPages, 1), page + 1);
    }

    /**
     * 分页栏展示的页码序列；0 表示省略号。
     */
    public List<Integer> getPageNumbers() {
        List<Integer> numbers = new ArrayList<>();
        if (totalPages <= 7) {
            for (int i = 1; i <= totalPages; i++) {
                numbers.add(i);
            }
            return numbers;
        }
        numbers.add(1);
        if (page > 4) {
            numbers.add(0);
        }
        int start = Math.max(2, page - 1);
        int end = Math.min(totalPages - 1, page + 1);
        for (int i = start; i <= end; i++) {
            numbers.add(i);
        }
        if (page < totalPages - 3) {
            numbers.add(0);
        }
        numbers.add(totalPages);
        return numbers;
    }

    /** 规范化页码：至少为 1，不超过总页数。 */
    public static int clampPage(int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
        if (page < 1) {
            page = 1;
        }
        if (totalPages > 0 && page > totalPages) {
            page = totalPages;
        }
        return page;
    }

    /** 规范化每页条数：1 ~ 100，默认 10。 */
    public static int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}