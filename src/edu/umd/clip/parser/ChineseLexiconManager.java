/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.util.UniCounter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author zqhuang
 */
public class ChineseLexiconManager extends LexiconManager implements Serializable {

    private static final long serialVersionUID = 1L;
    protected UniCounter<String>[] tagCharCounts;
    protected UniCounter<String> charCounter;

    @Override
    public void setupArray() {
        super.setupArray();

        charCounter = new UniCounter<String>();
        for (Entry<String, Double> entry : wordCountsMap.entrySet()) {
            String word = entry.getKey();
            double count = entry.getValue();
            List<String> charList = getCharList(word);
            double charCount = count / charList.size();
            for (String ch : charList) {
                charCounter.incrementCount(ch, charCount);
            }
        }

        tagCharCounts = new UniCounter[numNodes];
        for (int ni = 0; ni < numNodes; ni++) {
            if (tagWordCounts[ni] == null) {
                continue;
            }
            tagCharCounts[ni] = new UniCounter<String>();
            for (Entry<String, Double> entry : tagWordCounts[ni].entrySet()) {
                String word = entry.getKey();
                double count = entry.getValue();
                List<String> charList = getCharList(word);
                double charCount = count / charList.size();
                for (String ch : charList) {
                    tagCharCounts[ni].incrementCount(ch, charCount);
                }
            }
        }
    }

    @Override
    public void checkTrainedWithPunc() {
        if (!trainedWithPuncChecked) {
            trainedWithPuncChecked = true;
            trainedWithPunc = false;
            if (nodeMap.containsKey("PU")) {
                trainedWithPunc = true;
            }
        }
    }

    @Override
    public double[] getProbs(int tag, String word, boolean viterbi) {
        double pb_W_T = 0;
        double[] resultArray = new double[numStates[tag]];
        Arrays.fill(resultArray, 0.0);
        double c_W = wordCountsMap.getCount(word);
        boolean seen = c_W != 0;

        checkTrainedWithPunc();
        boolean isPuncWord = ClosedPosSets.isPunc(word);
        boolean isPuncTag = false;
        String tagName = nodeList.get(tag);
        if (trainedWithPunc) {
            isPuncTag = (tag == nodeMap.get("PU"));
        }
        if (trainedWithPunc && !seen && isPuncWord != isPuncTag) {
            if (viterbi) {
                Arrays.fill(resultArray, Double.NEGATIVE_INFINITY);
            }
            return resultArray;
        }

        double[] twCounts = null;

        List<String> charList = null;
        boolean[] charSeenList = null;
        if (seen) {
            if (latentTagWordCounts[tag] != null
                    && latentTagWordCounts[tag].containsKey(word)) {
                twCounts = latentTagWordCounts[tag].get(word).getArray();
            } else {
                twCounts = new double[numStates[tag]];
            }
        } else if (oovHandler == OOVHandler.heuristic) {
            charList = getCharList(word);
            charSeenList = new boolean[charList.size()];
            for (int i = 0; i < charList.size(); i++) {
                charSeenList[i] = charCounter.containsKey(charList.get(i));
            }
        }

        for (int tsi = 0; tsi < numStates[tag]; tsi++) {
            if (seen) {
                double c_T = nodeCounts[tag][tsi];
                if (c_T == 0) {
                    continue;
                }
                double c_TW = twCounts[tsi];
                double c_Tunseen = unseenLatentTagCounts[tag][tsi];
                double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
                double pb_T_W = 0;
                if (c_W > unknownSmoothingThreshold) {
                    pb_T_W = (c_TW + 0.0001 * p_T_U) / (c_W + 0.0001);
                } else {
                    pb_T_W = (c_TW + wordSmoothingParam * p_T_U) / (c_W + wordSmoothingParam);
                }
                if (pb_T_W == 0) {
                    continue;
                }
                pb_W_T = pb_T_W * c_W / c_T;
            } else {
                if (oovHandler == OOVHandler.heuristic) {
                    double knownCharNum = 0;
                    pb_W_T = 1;
                    for (int ci = 0; ci < charList.size(); ci++) {
                        if (charSeenList[ci]) {
                            String ch = charList.get(ci);
                            knownCharNum++;
                            pb_W_T *= getCharProb(ch, tag);
                        }
                    }
                    if (knownCharNum > 0 && pb_W_T > 0) {
                        pb_W_T = Math.pow(pb_W_T, 1 / knownCharNum);
                    }
                }
                if (pb_W_T == 0) {
                    double c_Tseen = nodeCounts[tag][tsi];
                    if (trainedWithPunc && isPuncTag && isPuncWord) {
                        double c_surfaceT = tagCounts[tag];
                        pb_W_T = 1.0 / numStates[tag] / totalUnseenTokens / c_surfaceT;
                    } else {
                        double c_Tunseen = unseenLatentTagCounts[tag][tsi];
                        double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
                        pb_W_T = p_T_U / c_Tseen;
                    }
                }
            }
            resultArray[tsi] = pb_W_T;
        }

        resultArray = smooth(resultArray, tag, smoothingMatrix[tag]);
        if (viterbi) {
            for (int si = 0; si < resultArray.length; si++) {
                resultArray[si] = Math.log(resultArray[si]);
            }
        }
        return resultArray;
    }

    public double[] smooth(double[] scores, int tag, double[][] smoothingMatrix) {
        int ns = scores.length;
        double newScores[] = new double[ns];
        for (int i = 0; i < ns; i++) {
            for (int j = 0; j < ns; j++) {
                newScores[i] += smoothingMatrix[i][j] * scores[j];
            }
        }
        return newScores;
    }

    private double getCharProb(String ch, int tag) {
        double c_T = tagCounts[tag];
        double c_C = charCounter.getCount(ch);
        double pb_C_T = 0;
        if (c_T == 0) {
            return 0;
        }

        double c_TC = tagCharCounts[tag].getCount(ch);

        if (c_TC == 0) {
            return 0;
        }
        double c_Tunseen = unseenTagCounts[tag];

        double p_T_U = (totalUnseenTokens == 0) ? 1 : c_Tunseen / totalUnseenTokens;
        double pb_T_C = 0;
        if (c_C > unknownSmoothingThreshold) {
            pb_T_C = c_TC / c_C;
        } else {
            pb_T_C = (c_TC + affixSmoothingParam * p_T_U) / (c_C + affixSmoothingParam);
        }

        if (pb_T_C == 0) {
            return 0;
        }

        pb_C_T = pb_T_C * c_C / c_T;
        return pb_C_T;
    }

    /**
     * @param str a string encoded in "UTF-8"
     * @return the list of character strings in str.
     */
    public List<String> getCharList(String str) {
        List<String> unicodeCharList = new ArrayList<String>();
        char[] charArray = str.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            int codePoint = Character.codePointAt(charArray, i);
            if (codePoint <= '\uFFFF') {
                unicodeCharList.add(new String(charArray, i, 1));
            } else {
                unicodeCharList.add(new String(charArray, i, 2));
                i++;
            }
        }
        return unicodeCharList;
    }
}
