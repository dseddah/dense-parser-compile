/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umd.clip.parser;

import edu.umd.clip.dense.VectorDictionary;
import org.jblas.DoubleMatrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author zqhuang
 */
public class WordVectorExtractor implements PredicateExtractor, Serializable {
    private static final long serialVersionUID = 1L;

    //private static VectorDictionary vectorDictionary = new VectorDictionary("data/embeddings_50_cw.txt");
    //public static final String VECTOR_DICT_PATH = "/work/jda/neural-parser/camera/data/embeddings_cbow_2.txt";
	//    public static final String VECTOR_DICT_PATH = "/work/jda/neural-parser/camera/data/embeddings_50_cw.txt";
	
	// modified by Djamé , better to get it parameterized
	public static final String VECTOR_DICT_PATH =System.getenv("DENSEPARSER_VECTOR_FILE");

	
    private static VectorDictionary vectorDictionary = new VectorDictionary(VECTOR_DICT_PATH);

    private static final String WVEC_KEY = "WVEC_";
    private static final String KEY_ABOVE = "_ABOVE_";
    private static final String KEY_BELOW = "_BELOW_";

    //private static final double[] CUTOFFS_ABOVE = { 0 };
    //private static final double[] CUTOFFS_ABOVE = {-1, -0.3, 0, 0.3, 1 };
    //private static final double[] CUTOFFS_ABOVE = {0, 0.316, 1};
    //private static final double[] CUTOFFS_BELOW = {0, -0.316, -1};
    //private static final double[] CUTOFFS_BELOW = CUTOFFS_ABOVE;

    //private static final double[] BUCKET_BOUNDARIES = {-1, -.316, 0, .316, 1};

    @Override
    public List<String> extract(String word) {
        //String predicate = type + "=" + word;
        //return Arrays.asList(predicate.intern());

        ArrayList<String> feats = new ArrayList<String>();
        DoubleMatrix vec;
        if (vectorDictionary.containsKey(word)) {
            vec = vectorDictionary.get(word);
        } else {
			/// debug djame
			System.err.println(word+" not in vec file");
            vec = vectorDictionary.get(VectorDictionary.DEFAULT_KEY);
        }
        //DoubleMatrix vec = vectorDictionary.get(word);

        for (int i = 0; i < vec.length; i++) {
            double val = vec.get(i);
            //int feat = (int)Math.floor(val * 2);
            int feat = (int)Math.signum(val);
            feats.add(WVEC_KEY + "_" + i + "_" + feat);
            // if (val < BUCKET_BOUNDARIES[0]) {
            //     feats.add((WVEC_KEY + i + "_" + BUCKET_BOUNDARIES[0]));
            // } else if (val >= BUCKET_BOUNDARIES[BUCKET_BOUNDARIES.length - 1]) {
            //     feats.add((WVEC_KEY + i + "_" + BUCKET_BOUNDARIES[BUCKET_BOUNDARIES.length-1]));
            // } else {
            //     for (int j = 0; j < BUCKET_BOUNDARIES.length - 1; j++) {
            //         if (BUCKET_BOUNDARIES[j] <= val && val < BUCKET_BOUNDARIES[j+1]) {
            //             feats.add((WVEC_KEY + i + "_" + BUCKET_BOUNDARIES[j] + "_" + BUCKET_BOUNDARIES[j+1]));
            //             break;
            //         }
            //     }
            // }
            //for (double cutoff : CUTOFFS_ABOVE) {
            //    if (vec.get(i) >= cutoff) {
            //        feats.add((WVEC_KEY + i + KEY_ABOVE + cutoff).intern());
            //    }
            //}
            //for (double cutoff : CUTOFFS_BELOW) {
            //    if (vec.get(i) <= cutoff) {
            //        feats.add((WVEC_KEY + i + KEY_BELOW + cutoff).intern());
            //    }
            //}
        }

        //System.out.println(word + " -> " + feats);

        return feats;
    }
}
