package info.smartkit.eip.obtuse_octo_prune.VOs;

import info.smartkit.eip.obtuse_octo_prune.utils.LireFeatures;

import java.util.Arrays;
import java.util.List;

/**
 * Created by smartkit on 2016/11/6.
 */
public class Feature_CCED {
    private List<String> hash = Arrays.asList(LireFeatures.CCED.getHash());

    public Feature_CCED(List<String> hash) {
        this.hash = hash;
    }

    public List<String> getHash() {
        return hash;
    }

    public void setHash(List<String> hash) {
        this.hash = hash;
    }
}
