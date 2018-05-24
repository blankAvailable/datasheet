package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import java.util.HashMap;
import java.util.HashSet;

import org.kyupi.circuit.Cell;

public class CBInfo {
    public HashSet<Cell> all_clock_buffers = new HashSet<>();
    public HashMap<Cell, HashSet<Cell>> sff_to_clock_buffer_set = new HashMap<>();
}
