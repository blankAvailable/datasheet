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
        Matrix matrix = new Matrix(scNum, gNum, circuitName);
        for (int i=0; i<groupingGenerator.scGrouping.size(); i++){
            System.out.println("Grouping " + i);
            System.out.println(groupingGenerator.scGrouping.get(i).toString());
            System.out.println("Groping evulate(Lower is better): " +
                    matrix.groupEvaluate(groupingGenerator.scGrouping.get(i)));
        }
    }
}
