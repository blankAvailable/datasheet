package jp.ac.kyutech.ci.grouping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class PartitionGeneratorFile extends PartitionGenerator {

	private BufferedReader br;
	
	private int clocks;
	
	private int[] clocking;

	public PartitionGeneratorFile(File f) throws IOException {
		log.info("Loading clocking from file: " + f);
		br = new BufferedReader(new FileReader(f));
		clocking = parseNext();
	}

	private int[] parseNext() throws IOException {
		String line;
		ArrayList<Integer> arr = null;
		while ((line = br.readLine()) != null) {
			if (!line.contains("Clocking"))
				continue;
			String[] fields = line.split(" ");
			for (String f : fields) {
				if (arr != null) {
					arr.add(Integer.parseInt(f));
				}
				if (f.equals("Clocking"))
					arr = new ArrayList<>();
			}
			break;
		}
		if (arr != null) {
			clocks = 0;
			int[] ret = new int[arr.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = arr.get(i);
				clocks = Math.max(clocks, ret[i]+1);
			}
			return ret;
		}
		return null;
	}

	@Override
	public Iterator<int[]> iterator() {
		// FIXME: reset stream?
		return this;
	}

	@Override
	public boolean hasNext() {
		return clocking != null;
	}

	@Override
	public int[] next() {
		int c[] = Arrays.copyOf(clocking, clocking.length);
		try {
			parseNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return c;
	}

	public int clocks() {
		return clocks;
	}

}
