package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.kyupi.graph.Graph;

import java.util.HashMap;
import java.util.HashSet;

public class CBInfo {
    public HashSet<Graph.Node> all_clock_buffers = new HashSet<>();
    public HashMap<Graph.Node, HashSet<Graph.Node>> sff_to_clock_buffer_set = new HashMap<>();
}
