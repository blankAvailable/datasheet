package jp.ac.kyutech.ci.sc_grouping_clkaggre;


import java.util.Iterator;
import java.util.Random;

public class RandomGrouping extends ScanChainGrouping{

    private int groupingCount;
    private long seed;
    private long initSeed;

    private int[] prt;

    public RandomGrouping(int setSize, int groupingCount, long initSeed){
        this.prt = new int[setSize];
        this.groupingCount = groupingCount;
        this.initSeed = initSeed;
        seed = initSeed;

    }
    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<int[]> iterator() {
        seed = initSeed;
        return this;
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return true;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public int[] next() {
        Random random = new Random(seed);
        for (int i = 0; i < prt.length; i++){
            prt[i] = random.nextInt(groupingCount);
        }
        seed++;
        return prt;
    }
}
