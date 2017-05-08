package jp.ac.kyutech.ci.datatable;

import jp.ac.kyutech.ci.grouping.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/20.
 * read .ScAggre file to get scan chain -- reachable aggressor pid id list
 */
public class ScAggre {
    public List<List<Integer>> scAggreId = new ArrayList<>();

    private String filePath = ".\\testdata\\";
    private String extension = ".ScAggre";

    public ScAggre(String circuitName) throws IOException {
        filePath = filePath.concat(circuitName + "\\");
        circuitName = circuitName.concat(extension);
        filePath = filePath.concat(circuitName);
        System.out.println(filePath);
        scAggreId.add(new ArrayList<>());
        scAggreReader();
    }

    /** read .ScAggre file to get scan chain -- reachable aggressor set table*/
    private void scAggreReader() throws IOException {
        File scAggre = new File(filePath);
        BufferedReader bufReader = new BufferedReader(new FileReader(scAggre));
        List<Integer> tempAggre = new ArrayList<>();
        Util util = new Util();

        String tempString = null;
        while ((tempString = bufReader.readLine())!=null){
            if ("".equals(tempString))
                continue;
            if (tempString.contains("scan")){
                if (tempAggre.isEmpty())
                    continue;
                List<Integer> temp = new ArrayList<>();
                temp.addAll(util.clone(tempAggre));
                scAggreId.add(temp);
                tempAggre.clear();
                continue;
            }
            String aggreLine[] = tempString.split("\\s+");
            tempAggre.add(Integer.parseInt(aggreLine[1]));
        }
        List<Integer> temp = new ArrayList<>();
        temp.addAll(util.clone(tempAggre));
        scAggreId.add(temp);
        bufReader.close();
    }
}
