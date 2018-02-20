package edu.umd.clip.parser;

import edu.umd.clip.dense.VectorDictionary;
import edu.umd.clip.math.ArrayMath;
import edu.umd.clip.util.BiMap;
import org.jblas.DoubleMatrix;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * @author jda
 */
public class RBFLexiconManager extends EnglishLexiconManager {

    //public static final String VECTOR_DICT_PATH = "data/embeddings_cbow_2.txt";
    //public static final String VECTOR_DICT_PATH = "/work/jda/neural-parser/camera/data/embeddings_cbow_2.txt";
	
	// public static final String VECTOR_DICT_PATH = "/work/jda/neural-parser/camera/data/embeddings_50_cw.txt";
	
	// modified by Djamé , better to get it parameterized
	public static final String VECTOR_DICT_PATH =System.getenv("DENSEPARSER_VECTOR_FILE");
	
    //public static final String VECTOR_DICT_PATH = "/work/jda/neural-parser/camera/data/embeddings_fr_cbow_2.txt";

    //public static final int MAX_BASES_PER_TAG = 500;
    public static final double KERNEL_INV_RADIUS = 0.3;

    protected VectorDictionary vectorDict;
    //protected Map<Integer,SortedSet<String>> basisSets;
    protected BiMap<Integer, String, DoubleArray> basisWeights;
    //protected BiMap<Integer,String,List<String>> nearestNeighbors;

    public RBFLexiconManager() {
        super();
        vectorDict = new VectorDictionary(VECTOR_DICT_PATH);
    }

    @Override
    public void initParams() {
        super.initParams();
        vectorDict.updateNearestNeighbors(wordCountsMap.keySet());
    }

    @Override
    public void doMStep() {
        tieRareWordCounts();
        updateBasisWeights();
        //updateBasisSets();
        precomputeWordScores();
    }

    @Override
    public double[] tryScoreWord(int tag, String word) {
        //if (!vectorDict.containsKey(word)) {
        //}
        //return getRBFScores(tag, word, numStates[tag]);

        if (wordCountsMap.getCount(word) > 0) {
            if (latentTagWordCounts[tag] != null && latentTagWordCounts[tag].containsKey(word)) {
                return getRBFScores(tag, word, numStates[tag]);
            } else {
                return new double[numStates[tag]];
            }
        } else {
            return null;
        }

        //if (latentTagWordCounts[tag] != null
        //        && latentTagWordCounts[tag].containsKey(word)) {
        //    return getRBFScores(tag, word, numStates[tag]);
        //} else {
        //    return new double[numStates[tag]];
        //}
    }
    //public double[] getProbs(int tag, String word, boolean viterbi) {
    //    double[] scores;
    //    if (wordCountsMap.getCount(word) > 0 && tagWordCounts[tag].getCount(word) == 0) {
    //        //System.out.println("I never saw " + word + " with " + tag);
    //        scores = new double[numStates[tag]];
    //    } else {
    //        scores = getRBFScores(tag, word, numStates[tag]);
    //    }
    //    if (viterbi) {
    //        for (int i = 0; i < scores.length; i++) {
    //            scores[i] = Math.log(scores[i]);
    //        }
    //    }
    //    return smooth(scores, tag, smoothingMatrix[tag]);
    //}

    @Override
    public void setupArray() {
        vectorDict.updateNearestNeighbors(wordCountsMap.keySet());
        updateBasisWeights();
        // if we call this first, it will wipe out latentTagWordCountsMap
        super.setupArray();
    }

    // // TODO(jda) this is copy-pasted from EnglishLexiconManager; should eventually refactor
    // protected double[] smooth(double[] scores, int tag, double[][] smoothingMatrix) {
    //     int ns = scores.length;
    //     double newScores[] = new double[ns];
    //     for (int i = 0; i < ns; i++) {
    //         for (int j = 0; j < ns; j++) {
    //             newScores[i] += smoothingMatrix[i][j] * scores[j];
    //         }
    //     }
    //     return newScores;
    // }

    protected void updateBasisWeights() {
        // basisWeights = new BiMap<Integer, String, DoubleArray>();
        // for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagCounts : latentTagWordCountsMap.entrySet()) {
        //     int tag = coarseTagCounts.getKey();
        //     boolean ok = false;
        //     for (Entry<String, DoubleArray> fineTagCounts : coarseTagCounts.getValue().entrySet()) {
        //         String word = fineTagCounts.getKey();
        //         //if (!vectorDict.containsKey(word)) { // || !basisSets.get(tag).contains(word)
        //         //    System.out.println("[" + word + "]" + tag + " not in the dictionary");
        //         //    continue;
        //         //}
        //         DoubleArray counts = fineTagCounts.getValue();
        //         basisWeights.put(tag, vectorDict.preprocessKey(word), counts);
        //         ok = true;
        //     }
        //     if (!ok) {
        //         Logger.getLogger(getClass().getName()).warning("No representative vector for " + tag + ".");
        //     }
        // }

        basisWeights = new BiMap<Integer, String, DoubleArray>();
        for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagCounts : latentTagWordCountsMap.entrySet()) {
            int tag = coarseTagCounts.getKey();
            for (Entry<String, DoubleArray> fineTagCounts : coarseTagCounts.getValue().entrySet()) {
                String word = fineTagCounts.getKey();
				System.err.println("reading word:\t"+word); /// dejbug djame

                String pWord = vectorDict.preprocessKey(word);
				System.err.println("reading pword:\t"+pWord); /// dejbug djame
                DoubleArray counts = fineTagCounts.getValue();
                if (basisWeights.containsKey(tag, pWord)) {
                    basisWeights.get(tag, pWord).add(counts.getArray());
                } else {
                    basisWeights.put(tag, pWord, counts);
                }
            }
        }
    }

    // protected double getMax(double[] in) {
    //     double best = Double.NEGATIVE_INFINITY;
    //     for (double d : in) {
    //         if (d > best) {
    //             best = d;
    //         }
    //     }
    //     return best;
    // }

    // protected double getSingleMaxContribution(int tag, String word, String basis) {
    //     //return getMax(basisWeights.get(tag, basis).getArray()) * computeKernel(word, basis);
    //     return computeKernel(word, basis);
    // }

    // protected void updateBasisSets() {
    //     if (nearestNeighbors != null) {
    //         // TODO(jda) make sure this is OK
    //         return;
    //     }
    //     System.out.println("Starting basis set update...");
    //     nearestNeighbors = new BiMap<Integer, String, List<String>>();
    //     for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagCounts : latentTagWordCountsMap.entrySet()) {
    //         final int tag = coarseTagCounts.getKey();
    //         for (final String word : coarseTagCounts.getValue().keySet()) {
    //             SortedSet<String> ordering = new TreeSet<String>(new Comparator<String>() {
    //                 @Override
    //                 public int compare(String o1, String o2) {
    //                     return (int)(Math.signum(
    //                             getSingleMaxContribution(tag, word, o2) - getSingleMaxContribution(tag, word, o1)
    //                     ));
    //                 }
    //             });
    //             //ordering.addAll(basisWeights.get(tag).keySet());
    //             for (String b : basisWeights.get(tag).keySet()) {
    //                 //ordering.add(vectorDict.preprocessKey(b));
    //                 ordering.add(b);
    //             }
    //             List<String> wordNeighbors = new ArrayList<String>();
    //             double biggestContrib = getSingleMaxContribution(tag, word, ordering.first());
    //             //System.out.println("Word " + word + " -> " + vectorDict.preprocessKey(word) + " has nns:");
    //             while (!ordering.isEmpty()) {
    //                 String f = ordering.first();
    //                 double fContrib = getSingleMaxContribution(tag, word, f);
    //                 if (fContrib / biggestContrib < 0.01) {
    //                     break;
    //                 }
    //                 //System.out.println(f);
    //                 wordNeighbors.add(f);
    //                 ordering.remove(f);
    //             }
    //             //System.out.println();
    //             nearestNeighbors.put(tag, word, wordNeighbors);
    //         }

    //         // final int tag = coarseTagCounts.getKey();
    //         // SortedSet<String> ordering = new TreeSet<String>(new Comparator<String>() {
    //         //     @Override
    //         //     public int compare(String o1, String o2) {
    //         //         return (int)Math.signum(getMax(getRBFScores(tag, o2, numStates[tag])) - getMax(getRBFScores(tag, o1, numStates[tag])));
    //         //     }
    //         // });
    //         // for (Entry<String, DoubleArray> tagBasisWeights : basisWeights.get(tag).entrySet()) {
    //         //     String word = tagBasisWeights.getKey();
    //         //     ordering.add(word);
    //         // }

    //     }
    //     System.out.println("done");
    // }

    // protected void updateBasisSets() {
    //     basisSets = new HashMap<Integer,SortedSet<String>>();
    //     for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagCounts : latentTagWordCountsMap.entrySet()) {
    //         int tag = coarseTagCounts.getKey();
    //         SortedSet<String> ordering = new TreeSet<String>(vectorDict.getRankComparator());
    //         for (String word : coarseTagCounts.getValue().keySet()) {
    //             if (!vectorDict.containsKey(word)) {
    //                 continue;
    //             }
    //             ordering.add(word);
    //             if (ordering.size() > MAX_BASES_PER_TAG) {
    //                 ordering.remove(ordering.last());
    //             }
    //         }
    //         basisSets.put(tag, ordering);
    //     }

    //     //for (Entry<Integer,SortedSet<String>> e : basisSets.entrySet()) {
    //     //    System.out.println(e.getKey() + " : " + e.getValue());
    //     //}
    // }

    protected void precomputeWordScores() {
        for (Entry<Integer, HashMap<String, DoubleArray>> coarseTagCounts : latentTagWordCountsMap.entrySet()) {
            int tag = coarseTagCounts.getKey();
            HashMap<String, DoubleArray> coarseTagProbs = latentTagWordProbsMap.get(tag);
            DoubleArray partition = new DoubleArray(new double[numStates[tag]]);
            //int ok = 0;
            for (Entry<String, DoubleArray> fineTagCounts : coarseTagCounts.getValue().entrySet()) {
                String word = fineTagCounts.getKey();
                //if (ok < 10) {
                //    System.out.println("Tag " + tag + " has word " + word);
                //    ok++;
                //}
                double[] scores = getRBFScores(tag, word, numStates[tag]);
                if (coarseTagProbs.get(word) == null) {
                    coarseTagProbs.put(word, new DoubleArray());
                }
                coarseTagProbs.get(word).setArray(scores);
                partition.add(scores);

                // System.out.print(word);
                // System.out.print(" ");
                // System.out.print(coarseTagProbs.get(word));
                // System.out.print(" ");
                // System.out.print(fineTagCounts.getValue());
                // System.out.print(" ");
                // System.out.print(basisWeights.get(tag, word));
                // System.out.println();
            }

            // // EVIL HACK: we don't know how much mass will be missing when we normalize over known words, but we guess
            // // we've seen at least 90%;
            // for (int i = 0; i < partition.getArray().length; i++) {
            //     partition.getArray()[i] /= 0.1;
            // }

            for (String word : coarseTagCounts.getValue().keySet()) {
                double[] scores = coarseTagProbs.get(word).getArray();
                for (int i = 0; i < scores.length; i++) {
                    scores[i] /= partition.getArray()[i];
                }
            }
        }
    }

    protected double[] getOOVRBFScores(int tag, String word, int numStates, DoubleArray smoothedWeights) {
        double[] scores = new double[numStates];
        System.arraycopy(smoothedWeights.getArray(), 0, scores, 0, numStates);
        if (!vectorDict.containsKey(word)) {
            return scores;
        }
        String pWord = vectorDict.preprocessKey(word);
        for (String basisWord : vectorDict.getNewNearestNeighbors(word)) {
            DoubleArray wordBasisWeights = basisWeights.get(tag, basisWord);
            if (wordBasisWeights == null) {
                continue;
            }
            double kernelValue = computeKernel(word, basisWord);
            assert scores.length == wordBasisWeights.getArray().length;
            for (int i = 0; i < numStates; i++) {
                scores[i] += kernelValue * wordBasisWeights.getArray()[i];
            }
        }
        return scores;
    }

    protected double[] getRBFScores(int tag, String word, int numStates) {

        double[] scores = new double[numStates];

        if (basisWeights.get(tag) == null) {
            return scores;
        }

        //System.out.println("Scoring (" + tag + "," + word + "):");
        String pWord = vectorDict.preprocessKey(word);

        if (!vectorDict.containsKey(word)) {
            System.arraycopy(basisWeights.get(tag, pWord).getArray(), 0, scores, 0, scores.length);
            //System.out.println("alone");
        } else {
            //System.out.println(vectorDict);
            //System.out.println(vectorDict.getNearestNeighbors(word));
            //System.out.println(word);
            for (String basisWord : vectorDict.getNearestNeighbors(word)) {
                DoubleArray wordBasisWeights = basisWeights.get(tag, basisWord);
                if (wordBasisWeights == null) {
                    continue;
                }
                double kernelValue = computeKernel(word, basisWord);
                assert scores.length == wordBasisWeights.getArray().length;
                for (int i = 0; i < numStates; i++) {
                    scores[i] += kernelValue * wordBasisWeights.getArray()[i];
                }

                // if (tag == 62 && word.equals("best")) {
                //     System.out.println(basisWord + "," + kernelValue);
                // }
                //System.out.println(basisWord);
            }
        }

        //HashMap<String, DoubleArray> tagBasisWeights = basisWeights.get(tag);
        //if (tagBasisWeights == null) {
        //    return scores;
        //}
        //for (Entry<String, DoubleArray> basis : tagBasisWeights.entrySet()) {

        //if (nearestNeighbors.get(tag, word) != null) {
        //    for (String basisWord : nearestNeighbors.get(tag, word)) {
        //        DoubleArray wordBasisWeights = basisWeights.get(tag, basisWord);
        //        //String basisWord = basis.getKey();
        //        //DoubleArray wordBasisWeights = basis.getValue();
        //        double kernelValue = computeKernel(word, basisWord);
        //        assert scores.length == wordBasisWeights.getArray().length;
        //        for (int i = 0; i < numStates; i++) {
        //            scores[i] += kernelValue * wordBasisWeights.getArray()[i];
        //        }
        //    }
        //}

        // TODO(jda) weird
        // TODO(jda) should not smooth for unseen word/tag combo?

        // for (int i = 0; i < numStates; i++) {
        //     scores[i] += 0.0001;
        // }
        return scores;
    }

    protected double computeKernel(String word, String basis) {
        double wordFreq = Math.log(wordCountsMap.getCount(word)) + 1;
        assert wordFreq > 0;
        DoubleMatrix wordVec = vectorDict.get(word);
        DoubleMatrix basisVec = vectorDict.get(basis);
        return Math.exp(-0.5 * basisVec.squaredDistance(wordVec) * KERNEL_INV_RADIUS * wordFreq);
        //return Math.exp(-0.5 * cosineDistance(basisVec, wordVec) * KERNEL_INV_RADIUS * wordFreq);
    }

    protected double cosineDistance(DoubleMatrix v1, DoubleMatrix v2) {
        return 1 - v1.dot(v2) / (v1.norm2() * v2.norm2());
    }

}
