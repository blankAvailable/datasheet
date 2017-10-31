package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.kyupi.graph.Graph.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CBInfo {
    public HashSet<Node> all_clock_buffers = new HashSet<>();
    public HashMap<Node, HashSet<Node>> sff_to_clock_buffer_set = new HashMap<>();
}
