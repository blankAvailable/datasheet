import java.io.IOException;
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

        DepthFirst groupingGenerator = new DepthFirst(scNum, gNum);
        groupingGenerator.scGrouping.stream().forEach(System.out::println);
        ScAggre scAggre = new ScAggre(circuitName);
        ScffAggre scffAggre = new ScffAggre(circuitName);
        scAggre.scAggreId.get(groupingGenerator.scGrouping.get(0).get(0).get(0)).stream().forEach(System.out::println);
        }
}
