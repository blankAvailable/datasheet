import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Created by ZhangYucong on 2017/3/12.
 */
public class Scffaggre {

    public List<List<Integer>> scFFid = new ArrayList<>();
    public List<List<Integer>> ffidAggre = new ArrayList<>();
    public int[] pidFanout;
    private String filePath = "..\\originalData\\";
    private String extension = ".ScFFAggre";

    /** initialize ffidAggre list, pidFanout array and .ScFFAggre file path */
    public Scffaggre(String circuitName) throws IOException {
        int ffNum = this.ffNum();
        for (int i=0; i<ffNum; i++){
            ffidAggre.add(new ArrayList<>());
        }
        pidFanout = new int[this.pidSize()];

        filePath = filePath.concat(circuitName + "\\");
        circuitName = circuitName.concat(extension);
        filePath = filePath.concat(circuitName);
        System.out.println(filePath);
    }

    /** get number of flip flops from .ctsppilist file */
    private int ffNum() throws IOException {
        File ppiList = new File("..\\originalData\\b20\\b20.ctsppilist");
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
    private int pidSize() throws IOException {
        File pid = new File("..\\originalData\\b20\\b20.pid");
        BufferedReader bufReader = new BufferedReader(new FileReader(pid));

        int pidSize = 0;
        while (bufReader.readLine()!=null){
            pidSize++;
        }
        bufReader.close();
        System.out.println("pid size is: " + pidSize);
        return pidSize;
    }

    /** read .ScFFAggre file to get scan chain number -- flip flop id table,
     * and pid id -- number of fanout array */
    public void scFFidReader() throws IOException {
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
    public void ffidAggreReader() throws IOException {
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
