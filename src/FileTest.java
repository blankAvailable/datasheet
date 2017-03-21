import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by ZhangYucong on 2017/3/12.
 */
public class FileTest {
    public static void main(String[] args) throws IOException {
        int scNum = 0;
        int gNum = 0;
        String circuitName = null;
        List<List<List<Integer>>> scGrouping = new ArrayList<>();

        Scanner input = new Scanner(System.in);
        System.out.println("Input scan chain number: ");
        scNum = input.nextInt();
        System.out.println("Input group number: ");
        gNum = input.nextInt();
        System.out.println("Input circuit name: ");
        circuitName = input.next();

        ScAggre scAggre = new ScAggre(circuitName);
        scAggre.scAggreReader();
        scAggre.scAggreId.stream().forEach(System.out::println);
    }
}
