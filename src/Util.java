import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangYucong on 2017/3/20.
 */
public class Util {
    public List<Integer> clone(List<Integer> source){
        List<Integer> destination = new ArrayList<>();
        for (int i=0; i<source.size(); i++){
            destination.add(source.get(i));
        }
        return destination;
    }
}
