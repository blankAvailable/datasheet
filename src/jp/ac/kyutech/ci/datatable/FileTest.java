package jp.ac.kyutech.ci.datatable;

/**
 * Created by ZhangYucong on 2017/3/12.
 */
public class FileTest {
    public static void main(String[] args) throws Exception {
        CommandProcess command = new CommandProcess();
        command.setArgs(args);
        ReachableAggre reachableAggre = new ReachableAggre();
        reachableAggre.callWithCircuitName(command.circuitName);
    }
}
