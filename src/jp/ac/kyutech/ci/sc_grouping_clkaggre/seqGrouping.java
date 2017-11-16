package jp.ac.kyutech.ci.sc_grouping_clkaggre;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class seqGrouping extends ScanChainGrouping{

    private int chainCount;
    private int groupCount;

    private int[] k;
    private int[] m;

    public seqGrouping(int chainCount, int groupCount){
        this.chainCount = chainCount;
        this.groupCount = groupCount;
        initFirstGrouping();
    }

    private void initFirstGrouping(){
        int n = chainCount;
        int p = groupCount;

        k = new int[chainCount];
        m = new int[chainCount];

        for (int i = 0; i < n-p; i++){
            k[i] = 0;
            m[i] = 0;
        }
        for (int i = n-p+1; i < n; i++){
            k[i] = i-(n-p);
            m[i] = k[i];
        }
    }
    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override
    public Iterator<int[]> iterator() {
        initFirstGrouping();
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
        return k != null;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public int[] next() {
        int[] ret = Arrays.copyOf(k, chainCount);
        if (ret == null)
            throw new NoSuchElementException();

        int p = groupCount;
        int n = chainCount;

        boolean have_more = false;
        for (int i = n-1; i >= 1; i--){
            if (k[i] < (p-1) && k[i] <= m[i-1]) {
                k[i] = k[i]+1;
                m[i] = Math.max(m[i], k[i]);
                for (int j = i+1; j <= n-(p-m[i]); j++) {
                    k[j] = 0;
                    m[j] = m[i];
                }
                for (int j = n-(p-m[i])+1; j <= n-1; j++) {
                    k[j] = p-(n-j);
                    m[j] = k[j];
                }
                have_more = true;
                break;
            }
        }
        if (!have_more)
            k = null;

        return ret;
    }
}
