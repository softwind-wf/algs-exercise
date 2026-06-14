package exercise2.exercise2_2;

public class LinkedListSort {
    public ListNode sortList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        // 1. 计算链表长度
        int length = 0;
        ListNode cur = head;
        while (cur != null) {
            length++;
            cur = cur.next;
        }

        ListNode dummy = new ListNode(0, head);
        // 2. 自底向上归并，步长从1开始翻倍
        for (int step = 1; step < length; step <<= 1) {
            ListNode prev = dummy;
            cur = dummy.next;
            while (cur != null) {
                // 拆分第一个子链表
                ListNode left = cur;
                ListNode right = split(left, step);
                // 拆分第二个子链表
                cur = split(right, step);
                // 归并两个子链表
                prev = merge(left, right, prev);
            }
        }
        return dummy.next;
    }

    // 拆分链表：将链表从head开始，切出前step个节点，返回后半部分的头节点
    private ListNode split(ListNode head, int step) {
        if (head == null) return null;
        for (int i = 1; head.next != null && i < step; i++) {
            head = head.next;
        }
        ListNode next = head.next;
        head.next = null; // 切断链表
        return next;
    }

    // 归并两个有序链表，将结果接在prev后面，返回归并后的尾节点
    private ListNode merge(ListNode l1, ListNode l2, ListNode prev) {
        ListNode cur = prev;
        while (l1 != null && l2 != null) {
            if (l1.val < l2.val) {
                cur.next = l1;
                l1 = l1.next;
            } else {
                cur.next = l2;
                l2 = l2.next;
            }
            cur = cur.next;
        }
        // 接上剩余节点
        cur.next = (l1 != null) ? l1 : l2;
        // 找到尾节点
        while (cur.next != null) {
            cur = cur.next;
        }
        return cur;
    }
}