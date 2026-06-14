package exercise1.exercise1_3;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateReader {

    public static Date[] readDates() {
        Queue<Date> queue = new LinkedList<>();
        Scanner scanner = new Scanner(System.in);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        // 关键修正：设置为严格解析模式
        dateFormat.setLenient(false); 

        System.out.println("请输入日期（格式 MM/DD/YYYY，每行一个，输入空行结束）：");

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                break;
            }
            try {
                Date date = dateFormat.parse(line);
                queue.add(date);
            } catch (ParseException e) {
                System.out.println("警告：'" + line + "' 不是有效的 MM/DD/YYYY 格式，已忽略。");
            }
        }
        scanner.close();

        Date[] dates = new Date[queue.size()];
        for (int i = 0; i < dates.length; i++) {
            dates[i] = queue.remove();
        }
        return dates;
    }

    public static void main(String[] args) {
        Date[] dates = readDates();

        System.out.println("\n成功读取 " + dates.length + " 个日期：");
        for (Date date : dates) {
            System.out.println(date);
        }
    }
}