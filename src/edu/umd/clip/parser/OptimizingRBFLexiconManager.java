package edu.umd.clip.parser;

import edu.umd.clip.math.DifferentiableFunction;
import edu.umd.clip.math.LBFGSMinimizer;
import edu.umd.clip.util.BiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author jda
 */
public class OptimizingRBFLexiconManager extends RBFLexiconManager {

    @Override
    protected void updateBasisWeights() {
        basisWeights = new BiMap<Integer, String, DoubleArray>();
        //for (int tag = 0; tag < tagCountsMap; tag++) {
        for (int tag : tagCountsMap.keySet()) {
            Map<String,Double>[] perTagWeights = new Map[numStates[tag]];
            for (int refinedIndex = 0; refinedIndex < numStates[tag]; refinedIndex++) {
                perTagWeights[refinedIndex] = getOneTagWeights(tag, refinedIndex);
            }
            for (String word : vectorDict.keySet()) {
                if (!basisWeights.containsKey(tag, word)) {
                    basisWeights.put(tag, word, new DoubleArray(new double[numStates[tag]]));
                }
                for (int rI = 0; rI < numStates[tag]; rI++) {
                    basisWeights.get(tag,word).getArray()[rI] = perTagWeights[rI].get(word);
                }
            }
        }

        // TODO fill in scores for words that we don't have vector reprs of
        for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagCounts : latentTagWordCountsMap.entrySet()) {
            int tag = coarseTagCounts.getKey();
            for (Entry<String, DoubleArray> fineTagCounts : coarseTagCounts.getValue().entrySet()) {
                String word = fineTagCounts.getKey();
                String pWord = vectorDict.preprocessKey(word);
                if (!basisWeights.containsKey(tag, pWord)) {
                    basisWeights.put(tag, pWord, fineTagCounts.getValue());
                }
            }
        }
    }

    protected Map<String,Double> getOneTagWeights(int tag, int refinement) {
        final double[] init = new double[vectorDict.keySet().size()];
        init[0] = 10;
        init[1] = -5;
        init[3] = 27;
        final List<String> keyOrder = new ArrayList<String>(vectorDict.keySet());

        double[] result = new LBFGSMinimizer().minimize(new DifferentiableFunction() {

            public static final double L2_PENALTY = 0.1;

            @Override
            public double[] derivativeAt(double[] x) {

                double[] r = new double[x.length];

                for (int i = 0; i < x.length; i++) {
                    r[i] += L2_PENALTY * (x[i] - 5);
                }

                return r;
            }

            @Override
            public int dimension() {
                return init.length;
            }

            @Override
            public double valueAt(double[] x) {
                double r = 0;

                double reg = 0;
                for (int i = 0; i < x.length; i++) {
                    reg += (x[i] - 5) * (x[i] - 5);
                }
                reg /= 2;
                reg *= L2_PENALTY;

                r += reg;
                return r;
            }
        }, init, 1);

        Map<String,Double> out = new HashMap<String,Double>();
        for (int i = 0; i < keyOrder.size(); i++) {
            out.put(keyOrder.get(i), result[i]);
        }
        return out;
    }
}
