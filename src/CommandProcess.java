import org.apache.commons.cli.*;

/**
 * Created by ZhangYucong on 2017/4/3.
 */
public class CommandProcess {
    private Options options = new Options();
    private CommandLine argInput;
    private String[] args = null;
    public int scNum = 0;
    public int gNum = 0;
    public int thr = 0;
    public String circuitName = null;

    public CommandProcess(){
        options.addOption("s", "scNum", true, "input the number of scan chains");
        options.addOption("g", "gNum", true, "input the number of available groups");
        options.addOption("c", "circuitName", true, "input the name of circuit file");
        options.addOption("t", "thr", true, "input the threshold value");
        options.addOption("h", "help", false, "print help information");
    }

    public void setArgs(String... args){
        this.args = args;
        CommandLineParser commandLineParser = new GnuParser();
        try {
            argInput = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (argInput.hasOption("h")){
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("datasheet [-s] scan chain number(int) " +
                    "[-g] group number(int) [-c] circuit name(String) [-t] threshold value(int) [-h]", options);
        }
        if (argInput.hasOption("s")){
            scNum = Integer.parseInt(argInput.getOptionValue("s"));
        }
        if (argInput.hasOption("g")){
            gNum = Integer.parseInt(argInput.getOptionValue("g"));
        }
        if (argInput.hasOption("c")){
            circuitName = argInput.getOptionValue("c");
        }
        if (argInput.hasOption("t")){
            thr = Integer.parseInt(argInput.getOptionValue("t"));
        }
    }
}
