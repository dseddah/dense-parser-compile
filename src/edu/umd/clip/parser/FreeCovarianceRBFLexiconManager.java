package edu.umd.clip.parser;

import edu.umd.clip.dense.VectorDictionary;
import org.jblas.DoubleMatrix;
import java.util.HashMap;
import java.util.Map.Entry;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author jda
 */
public class FreeCovarianceRBFLexiconManager extends RBFLexiconManager {

    protected double[] invCovs;

    public FreeCovarianceRBFLexiconManager() {
        super();
        invCovs = new double[VectorDictionary.DIMENSION];
        Arrays.fill(invCovs, 0.3);
    }

    @Override
    protected double computeKernel(String word, String basis) {
        DoubleMatrix wordVec = vectorDict.get(word);
        DoubleMatrix basisVec = vectorDict.get(basis);
        return Math.exp(-0.5 * basisVec.mul(new DoubleMatrix(invCovs)).squaredDistance(wordVec));
    }

    @Override
    public void doMStep() {
        tieRareWordCounts();
        updateBasisWeights();
        updateCovariance();
        precomputeWordScores();
        //System.out.println(Arrays.toString(invCovs));
        Logger.getLogger(getClass().getName()).info(Arrays.toString(invCovs));
    }

    boolean firstRun = true;
    public void updateCovariance() {
        if (firstRun) {
            firstRun = false;
            return;
        }

        DoubleMatrix newCovs = new DoubleMatrix(new double[invCovs.length]);

        for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagProbs : latentTagWordProbsMap.entrySet()) {
            int tag = coarseTagProbs.getKey();
            for (Entry<String, DoubleArray> fineTagProbs : coarseTagProbs.getValue().entrySet()) {
                String word = fineTagProbs.getKey();
                String pWord = vectorDict.preprocessKey(word);
                if (!vectorDict.containsKey(pWord)) {
                    continue;
                }
                DoubleMatrix basisVec = vectorDict.get(pWord);
                for (String neighbor : vectorDict.getNearestNeighbors(pWord)) {
                    DoubleMatrix neighborVec = vectorDict.get(neighbor);
                    DoubleMatrix distVec = neighborVec.sub(basisVec);
                    distVec.muli(distVec);
                    DoubleArray probs = latentTagWordProbsMap.get(tag, neighbor);
                    if (probs == null) {
                        continue;
                    }
                    double probsSum = 0;
                    for (int i = 0; i < probs.getArray().length; i++) {
                        probsSum += probs.getArray()[i];
                    }
                    if (Double.isNaN(probsSum)) {
                        continue;
                    }
                    Logger.getLogger(getClass().getName()).info("" + probsSum);
                    distVec.muli(probsSum);
                    newCovs.addi(distVec);
                }
            }
        }

        Logger.getLogger(getClass().getName()).info(Arrays.toString(newCovs.toArray()));

        for (int i = 0; i < invCovs.length; i++) {
            invCovs[i] = 1.0 / newCovs.get(i);
        }

    }

}
