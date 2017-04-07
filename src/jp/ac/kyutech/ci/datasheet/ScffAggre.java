package jp.ac.kyutech.ci.datasheet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/12.
 * read the .ScFFAggre filt to get scan chain -- flip flop id list,
 * flip flop id -- aggressor pid id list
 * and pid id -- fanout number array
 */
public class ScffAggre {

    public List<List<Integer>> scFFid = new ArrayList<>();
    public List<List<Integer>> ffidAggre = new ArrayList<>();
    public int[] pidFanout;

    private String filePath = ".\\testdata\\";
    private String extension = ".ScFFAggre";

    /** initialize ffidAggre list, pidFanout array and .ScFFAggre file path */
    public ScffAggre(String circuitName) throws IOException {
        int ffNum = this.ffNum(circuitName);
        for (int i=0; i<ffNum; i++){
            ffidAggre.add(new ArrayList<>());
        }
        pidFanout = new int[this.pidSize(circuitName)];

        filePath = filePath.concat(circuitName + "\\");
        circuitName = circuitName.concat(extension);
        filePath = filePath.concat(circuitName);
        System.out.println(filePath);
        scFFid.add(new ArrayList<>());
        scFFidReader();
        ffidAggreReader();
    }

    /** get number of flip flops from .ctsppilist file */
    private int ffNum(String circuitName) throws IOException {
        File ppiList = new File(".\\testdata\\" + circuitName + "\\" + circuitName + ".ctsppilist");
        BufferedReader bufReader = new BufferedReader(new FileReader(ppiList));

        int ffNum = 0;
        while (bufReader.readLine()!=null){
            ffNum++;
        }

        bufReader.close();
        System.out.println("Flip Flop number is: " + ffNum);
        return ffNum;
    }

    /** get maximum pid id from .pid file */
    private int pidSize(String circuitName) throws IOException {
        File pid = new File(".\\testdata\\" + circuitName + "\\" + circuitName + ".pid");
        BufferedReader bufReader = new BufferedReader(new FileReader(pid));

        int pidSize = 1;
        while (bufReader.readLine()!=null){
            pidSize++;
        }

        bufReader.close();
        System.out.println("pid size is: " + (pidSize-1));
        return pidSize;
    }

    /** read .ScFFAggre file to get scan chain number -- flip flop id table,
     * and pid id -- number of fanout array */
    private void scFFidReader() throws IOException {
        File scFFAggre = new File(filePath);
        BufferedReader bufReader = new BufferedReader(new FileReader(scFFAggre));
        List<Integer> tempFFid = new ArrayList<>();
        Util util = new Util();

        String tempString = null;
        while ((tempString = bufReader.readLine())!=null){
            if("".equals(tempString))
                continue;
            if(tempString.contains("scan")){
                if (tempFFid.isEmpty())
                    continue;
                List<Integer> temp = new ArrayList<>();
                temp.addAll(util.clone(tempFFid));
                scFFid.add(temp);
                tempFFid.clear();
                continue;
            }
            if (tempString.contains("Flip")){
                String ffLine[] = tempString.split("\\s+");
                tempFFid.add(Integer.parseInt(ffLine[2]));
                continue;
            }
            String aggreLine[] = tempString.split("\\s+");
            pidFanout[Integer.parseInt(aggreLine[1])] = Integer.parseInt(aggreLine[2]);

        }
        List<Integer> temp = new ArrayList<>();
        temp.addAll(util.clone(tempFFid));
        scFFid.add(temp);
        bufReader.close();
    }

    /** read .ScFFAggre file to get flip flop id -- aggressor pid id table*/
    private void ffidAggreReader() throws IOException {
        File scFFAggre = new File(filePath);
        BufferedReader bufReader = new BufferedReader(new FileReader(scFFAggre));
        List<Integer> tempAggreId = new ArrayList<>();
        Util util = new Util();

        int ffId = 0;
        String tempString = null;
        while ((tempString = bufReader.readLine())!=null){
            if ("".equals(tempString)||tempString.contains("scan"))
                continue;
            if (tempString.contains("Flip")){
                if (tempAggreId.isEmpty()) {
                    String aggreLine[] = tempString.split("\\s+");
                    ffId = Integer.parseInt(aggreLine[2]);
                    continue;
                }
                List<Integer> temp = new ArrayList<>();
                temp.addAll(util.clone(tempAggreId));
                ffidAggre.set(ffId, temp);
                tempAggreId.clear();
                String ffidLine[] = tempString.split("\\s+");
                ffId = Integer.parseInt(ffidLine[2]);
                continue;
            }
            String aggreLine[] = tempString.split("\\s+");
            tempAggreId.add(Integer.parseInt(aggreLine[1]));
        }
        List<Integer> temp = new ArrayList<>();
        temp.addAll(util.clone(tempAggreId));
        ffidAggre.set(ffId, temp);
    }
}
