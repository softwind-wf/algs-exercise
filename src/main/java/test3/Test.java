package test3;

public class Test {
    public static void main(String[] args){
        LinearProbingHashST<String,Integer> st=new LinearProbingHashST<>();
        LinearProbingHashST1<String,Integer> st1=new LinearProbingHashST1<>();

        // 简单计时示例
        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            st.put("key" + i, i);
        }
        long end = System.nanoTime();
        System.out.println("插入10w条耗时: " + (end - start) / 1e6 + " ms");

        // 简单计时示例
         start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            st1.put("key" + i, i);
        }
         end = System.nanoTime();
        System.out.println("插入10w条耗时: " + (end - start) / 1e6 + " ms");

    }



}
