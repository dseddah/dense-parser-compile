package edu.umd.clip.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author jda
 */
public class TripleExtractor implements PredicateExtractor, Serializable {

    private static final String beforeKeyPrefix = "PRE__";
    private static final String thisKeyPrefix = "THIS__";
    private static final String afterKeyPrefix = "POST__";
    private PredicateExtractor base;

    public TripleExtractor(PredicateExtractor base) {
        this.base = base;
    }

    @Override
    public List<String> extract(String word) {

        //System.out.println(word);
        String[] parts = word.split("__");

        if (parts.length != 3) {
            Logger.getLogger(getClass().getName()).warning("Unsplit word: " + word);
            return base.extract(word);
        }

        List<String> beforePreds = base.extract(parts[0]);
        List<String> thisPreds = base.extract(parts[1]);
        List<String> afterPreds = base.extract(parts[2]);

        for (int i = 0; i < beforePreds.size(); i++) {
            beforePreds.set(i, (beforeKeyPrefix + beforePreds.get(i)).intern());
        }
        for (int i = 0; i < thisPreds.size(); i++) {
            thisPreds.set(i, (thisKeyPrefix + thisPreds.get(i)).intern());
        }
        for (int i = 0; i < afterPreds.size(); i++) {
            afterPreds.set(i, (afterKeyPrefix + afterPreds.get(i)).intern());
        }

        ArrayList<String> r = new ArrayList<String>();
        r.addAll(beforePreds);
        r.addAll(thisPreds);
        r.addAll(afterPreds);
        System.out.println(r);
        return r;
    }
}
