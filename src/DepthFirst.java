import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ZhangYucong on 2017/3/10.
 */
public class DepthFirst {

    public int stirlingNum(int scNum, int gNum) {
        if (scNum == 1 && gNum == 0)
            return 1;
        else if (scNum == gNum || gNum == 1)
            return 1;
        else return stirlingNum(scNum - 1, gNum) * gNum + stirlingNum(scNum - 1, gNum - 1);
    }

    /** reGrouping scan chains */
    public List<List<List<Integer>>> reGrouping(List<Integer> scanChain, int gNum) {
        if (scanChain.size() < gNum)
            throw new IllegalArgumentException("gNum must =< scNum!");

        if (scanChain.size() == 0)
            return new ArrayList<>();

        List<List<List<Integer>>> scGrouping = new ArrayList<List<List<Integer>>>();

        if (scanChain.size() == gNum) {
            List<List<Integer>> grouping = new ArrayList<>();

            for (int i = 0; i < scanChain.size(); i++){
                List<Integer> gr = new ArrayList<>();
                gr.add(scanChain.get(i));
                grouping.add(gr);
            }
            scGrouping.add(grouping);
            return scGrouping;
            //List<List<Integer>> grouping = scanChain.stream().map(Arrays::asList).collect(Collectors.toList());
        }

        /** take the first element */
        int firstElement = scanChain.get(0);
        /** put the rest elements into a new sublist */
        List<Integer> subList = scanChain.subList(1, scanChain.size());
        if (scanChain.size() > gNum) {
            /** put the first into a individual group, available number of groups -1,
             * and put the rest elements into the rest of groups */
            {
                List<Integer> oneGroup = new ArrayList<>();
                oneGroup.add(firstElement);
                List<List<List<Integer>>> scGroupingSub = reGrouping(subList, gNum - 1);
                for (Iterator<List<List<Integer>>> iterator = scGroupingSub.iterator(); iterator.hasNext(); ) {
                    List<List<Integer>> group = iterator.next();
                    group.add(oneGroup);
                }
                scGrouping.addAll(scGroupingSub);
            }
            /** put the rest of elements into all groups,
             * and put the first element into all these groups */
            {
                List<List<List<Integer>>> scGroupingSub = reGrouping(subList, gNum);
                for (Iterator<List<List<Integer>>> iterator = scGroupingSub.iterator(); iterator.hasNext(); ) {
                    List<List<Integer>> newGroup = new ArrayList<>(iterator.next());
                    for (int i = 0; i < newGroup.size(); i++) {
                        // clone
                        List<List<Integer>> buf = new ArrayList<>(newGroup.size());
                        for (List<Integer> gr : newGroup){
                            buf.add(new ArrayList<>(gr));
                        }
                        buf.get(i).add(firstElement);
                        scGrouping.add(buf);
                    }

                }
                return scGrouping;
            }
        }
        return new ArrayList<>();
    }
}