package edu.umd.clip.dense;

import edu.umd.clip.parser.EnglishLexiconManager;
import org.jblas.DoubleMatrix;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author jda
 */
public class VectorDictionary implements Serializable {

    protected HashMap<String,DoubleMatrix> backingMap;
    //protected HashMap<String,Integer> ranks;
    protected HashMap<String,List<String>> nearestNeighbors;

    public static final int DIMENSION = 50;
    //public static final String DEFAULT_KEY = "*UNKNOWN*";
    public static final String DEFAULT_KEY = "UNKNOWN";
    //public static final int RANK_CUTOFF = 100000;
    public static final int NEIGHBORS_TO_KEEP = 20;

    // public class RankComparator implements Comparator<String>, Serializable {
    //     @Override
    //     public int compare(String o1, String o2) {
    //         return getRank(o1) - getRank(o2);
    //     }
    // }

    public VectorDictionary(String path) {
        backingMap = new HashMap<String,DoubleMatrix>();
        //ranks = new HashMap<String,Integer>();
        loadDictEntries(path);
    }

    protected void loadDictEntries(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            int counter = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s");
                String word = parts[0];
                if (word.equals("(")) {
                    word = "-lrb-";
                } else if (word.equals(")")) {
                    word = "-lrb-";
                } else if (word.equals("[")) {
                    word = "-lrb-";
                } else if (word.equals("]")) {
                    word = "-lrb-";
                } else if (word.equals("{")) {
                    word = "-lrb-";
                } else if (word.equals("}")) {
                    word = "-lrb-";
                }
                //word = preprocessKey(word);
                //word = word.toLowerCase();
                DoubleMatrix vec = DoubleMatrix.zeros(DIMENSION);
                for (int i = 1; i < DIMENSION + 1; i++) {
                    vec.put(i-1, Double.parseDouble(parts[i])) ;
                }
                backingMap.put(word, vec);
                if (word.equals("what")) {
                    System.out.println("added what");
                }
                //ranks.put(word, counter++);
            }
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
    }

    private void sort(String[] words, float[] dists) {
        for (int i = 0; i < dists.length; i++) {
            String thisWord = words[i];
            float thisDist = dists[i];
            int j = i;
            while (j > 0 && dists[j-1] > thisDist) {
                dists[j] = dists[j-1];
                words[j] = words[j-1];
                j--;
            }
            dists[j] = thisDist;
            words[j] = thisWord;
        }
    }

    public List<String> getNewNearestNeighbors(String key) {
        String pKey = preprocessKey(key);
        if (nearestNeighbors.containsKey(pKey)) {
            return nearestNeighbors.get(pKey);
        }
        DoubleMatrix keyVec = backingMap.get(pKey);
        String[] wordBeam = new String[NEIGHBORS_TO_KEEP+1];
        float[] distBeam = new float[NEIGHBORS_TO_KEEP+1];
        int beamPtr = 0;
        for (String oKey : backingMap.keySet()) {
            float dist = (float)backingMap.get(oKey).squaredDistance(keyVec);
            if (beamPtr < NEIGHBORS_TO_KEEP) {
                wordBeam[beamPtr] = oKey;
                distBeam[beamPtr] = dist;
                beamPtr++;
                continue;
            }
            wordBeam[NEIGHBORS_TO_KEEP] = oKey;
            distBeam[NEIGHBORS_TO_KEEP] = dist;
            sort(wordBeam, distBeam);

        }
        String[] neighbors = new String[NEIGHBORS_TO_KEEP];
        System.arraycopy(wordBeam, 0, neighbors, 0, NEIGHBORS_TO_KEEP);
        List<String> nl = Arrays.asList(neighbors);
        nearestNeighbors.put(key, nl);
        return nl;
    }

    public void updateNearestNeighbors(Collection<String> observed) {

        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream("neighbors.ser"));
            nearestNeighbors = (HashMap<String,List<String>>) is.readObject();
            return;
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).info("Unable to find precomputed nearest neighbors---computing now. This will take a while....");
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).info("Unable to load precomputed neighbors. Move neighbors.ser out of the way or figure out what went wrong.");
            System.exit(1);
        }

        Logger.getLogger(getClass().getName()).info("Beginning NN precomputation...");
        nearestNeighbors = new HashMap<String, List<String>>();
        int counter = 0;
        //for (String key : backingMap.keySet()) {

        //System.out.println(observed);

        for (String uKey : observed) {
            String key = preprocessKey(uKey);
            DoubleMatrix keyVec = backingMap.get(key);
            if (counter ++ % 100 == 0) {
                System.out.println(counter + "/" + observed.size());
            }
            if (!backingMap.containsKey(key)) {
                continue;
            }
            //if (counter == 200) {
            //    System.exit(0);
            //}
            String[] wordBeam = new String[NEIGHBORS_TO_KEEP+1];
            float[] distBeam = new float[NEIGHBORS_TO_KEEP+1];
            int beamPtr = 0;
            //for (String oKey : backingMap.keySet()) {
            for (String uoKey : observed) {
                String oKey = preprocessKey(uoKey);
                if (!backingMap.containsKey(oKey)) {
                    continue;
                }
                float dist = (float)backingMap.get(oKey).squaredDistance(keyVec);
                if (beamPtr < NEIGHBORS_TO_KEEP) {
                    wordBeam[beamPtr] = oKey;
                    distBeam[beamPtr] = dist;
                    beamPtr++;
                    continue;
                }
                boolean skip = false;
                for (int i = 0; i < wordBeam.length; i++) {
                    if (oKey.equals(wordBeam[i])) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
                wordBeam[NEIGHBORS_TO_KEEP] = oKey;
                distBeam[NEIGHBORS_TO_KEEP] = dist;
                sort(wordBeam, distBeam);

            }
            String[] neighbors = new String[NEIGHBORS_TO_KEEP];
            System.arraycopy(wordBeam, 0, neighbors, 0, NEIGHBORS_TO_KEEP);
            nearestNeighbors.put(key, Arrays.asList(neighbors));
            // System.out.println(key);
            // System.out.println(Arrays.toString(neighbors));
        }
        Logger.getLogger(getClass().getName()).info("done computing NNs.");
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("neighbors.ser"));
            os.writeObject(nearestNeighbors);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).info("Unable to write NN file.");
        }
    }

    public DoubleMatrix get(String key) {
        String pKey = preprocessKey(key);
        // // assert backingMap.containsKey(pKey);
        // // return backingMap.get(pKey);
        //if (backingMap.containsKey(key) && ranks.get(key) < RANK_CUTOFF) {
        //    return backingMap.get(key);
        //}
        //String signature = EnglishLexiconManager.getSignature(key);
        //System.out.println(signature);
        //assert backingMap.containsKey(signature);
        //return (backingMap.get(signature));
        // return backingMap.get(DEFAULT_KEY);
        if (containsKey(pKey)) {
            return backingMap.get(pKey);
        }
        System.out.println("missing key " + key);
        return DoubleMatrix.zeros(DIMENSION);
        // assert false;
        // return null;
        //System.out.println
        //return backingMap.get(DEFAULT_KEY);
    }

    public List<String> getNearestNeighbors(String key) {
        String pKey = preprocessKey(key);
        //assert nearestNeighbors.containsKey(pKey);
        return nearestNeighbors.get(pKey);
    }

    //public int getRank(String key) {
    //    if (!ranks.containsKey(key)) {
    //        throw new IllegalArgumentException("Word " + key + " not in dictionary.");
    //    }
    //    return ranks.get(key);
    //}

    public String preprocessKey(String key) {
        String pKey = key.toLowerCase();
        //String pKey = key;
        //if (!backingMap.containsKey(pKey)) {
        //    return EnglishLexiconManager.getSignature(pKey).toLowerCase();
        //}
        return pKey;
    }

    public boolean containsKey(String key) {
        return backingMap.containsKey(preprocessKey(key));
        //return backingMap.containsKey(key);
    }

    public Set<String> keySet() {
        return backingMap.keySet();
    }

    //public RankComparator getRankComparator() {
    //    return new RankComparator();
    //}

}
