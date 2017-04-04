package jp.ac.kyutech.ci.grouping;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by ZhangYucong on 2017/3/12.
 */
public class FileTest {
    public static void main(String[] args) throws IOException {
        CommandProcess command = new CommandProcess();
        command.setArgs(args);
        DepthFirst groupingGenerator = new DepthFirst(command.scNum, command.gNum);
        groupingGenerator.scGrouping.stream().forEach(System.out::println);
        Matrix matrix = new Matrix(command.circuitName);
        Heuristic heuristic = new Heuristic(command.scNum, command.gNum, command.circuitName, command.thr);
        System.out.println("heuristic grouping: " + heuristic.scGroup);
    }
}
