

import exercise2.exercise2_2.LinkedListSort;
import exercise2.exercise2_2.ListNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LinkedListSortTest {
    
    // 辅助方法：创建链表
    private ListNode createList(int[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        ListNode head = new ListNode(values[0]);
        ListNode current = head;
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
        }
        return head;
    }
    
    // 辅助方法：将链表转换为数组
    private int[] listToArray(ListNode head) {
        if (head == null) {
            return new int[0];
        }
        int size = 0;
        ListNode current = head;
        while (current != null) {
            size++;
            current = current.next;
        }
        
        int[] result = new int[size];
        current = head;
        for (int i = 0; i < size; i++) {
            result[i] = current.val;
            current = current.next;
        }
        return result;
    }
    
    // 辅助方法：打印链表
    private void printList(ListNode head) {
        StringBuilder sb = new StringBuilder();
        ListNode current = head;
        while (current != null) {
            sb.append(current.val);
            if (current.next != null) {
                sb.append(" -> ");
            }
            current = current.next;
        }
        System.out.println(sb.toString());
    }
    
    // 测试空链表
    @Test
    void testEmptyList() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode result = sorter.sortList(null);
        assertNull(result, "空链表应该返回 null");
    }
    
    // 测试单个节点
    @Test
    void testSingleNode() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{5});
        ListNode result = sorter.sortList(head);
        assertNotNull(result);
        assertEquals(5, result.val);
        assertNull(result.next);
    }
    
    // 测试已经有序的链表
    @Test
    void testAlreadySorted() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{1, 2, 3, 4, 5});
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, sorted);
    }
    
    // 测试完全逆序的链表
    @Test
    void testReverseSorted() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{5, 4, 3, 2, 1});
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, sorted);
    }
    
    // 测试乱序链表
    @Test
    void testRandomOrder() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{3, 1, 4, 1, 5, 9, 2, 6});
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        int[] expected = {1, 1, 2, 3, 4, 5, 6, 9};
        assertArrayEquals(expected, sorted, "排序结果应该正确");
    }
    
    // 测试有重复元素的链表
    @Test
    void testDuplicates() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{3, 1, 2, 3, 1, 2, 3});
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        int[] expected = {1, 1, 2, 2, 3, 3, 3};
        assertArrayEquals(expected, sorted);
    }
    
    // 测试所有元素相同的链表
    @Test
    void testAllSameElements() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{5, 5, 5, 5});
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        assertArrayEquals(new int[]{5, 5, 5, 5}, sorted);
    }
    
    // 测试包含负数的链表
    @Test
    void testWithNegativeNumbers() {
        LinkedListSort sorter = new LinkedListSort();
        ListNode head = createList(new int[]{3, -1, 0, -5, 2});
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        int[] expected = {-5, -1, 0, 2, 3};
        assertArrayEquals(expected, sorted);
    }
    
    // 测试两个节点的链表
    @Test
    void testTwoNodes() {
        LinkedListSort sorter = new LinkedListSort();
        
        // 正序
        ListNode head1 = createList(new int[]{1, 2});
        ListNode result1 = sorter.sortList(head1);
        assertArrayEquals(new int[]{1, 2}, listToArray(result1));
        
        // 逆序
        ListNode head2 = createList(new int[]{2, 1});
        ListNode result2 = sorter.sortList(head2);
        assertArrayEquals(new int[]{1, 2}, listToArray(result2));
    }
    
    // 测试较大规模的链表
    @Test
    void testLargeList() {
        LinkedListSort sorter = new LinkedListSort();
        int[] values = new int[100];
        for (int i = 0; i < 100; i++) {
            values[i] = (int)(Math.random() * 1000);
        }
        
        ListNode head = createList(values);
        ListNode result = sorter.sortList(head);
        int[] sorted = listToArray(result);
        
        // 验证是否有序
        for (int i = 1; i < sorted.length; i++) {
            assertTrue(sorted[i-1] <= sorted[i], 
                "排序后的数组应该是有序的");
        }
        
        // 验证长度
        assertEquals(values.length, sorted.length);
    }
    
    // 可视化测试
    @Test
    void testVisualize() {
        LinkedListSort sorter = new LinkedListSort();
        
        System.out.println("=== 测试 1: 随机顺序 ===");
        ListNode head1 = createList(new int[]{8, 3, 7, 1, 9, 4, 6, 2});
        System.out.print("排序前: ");
        printList(head1);
        ListNode result1 = sorter.sortList(head1);
        System.out.print("排序后: ");
        printList(result1);
        
        System.out.println("\n=== 测试 2: 逆序 ===");
        ListNode head2 = createList(new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1});
        System.out.print("排序前: ");
        printList(head2);
        ListNode result2 = sorter.sortList(head2);
        System.out.print("排序后: ");
        printList(result2);
        
        System.out.println("\n=== 测试 3: 包含负数 ===");
        ListNode head3 = createList(new int[]{-3, 5, -1, 0, 2, -7, 4});
        System.out.print("排序前: ");
        printList(head3);
        ListNode result3 = sorter.sortList(head3);
        System.out.print("排序后: ");
        printList(result3);
    }
}
