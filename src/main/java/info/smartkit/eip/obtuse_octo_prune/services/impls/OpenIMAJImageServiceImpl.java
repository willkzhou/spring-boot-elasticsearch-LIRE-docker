package info.smartkit.eip.obtuse_octo_prune.services.impls;

import info.smartkit.eip.obtuse_octo_prune.VOs.AnalysisResponseVO;
import info.smartkit.eip.obtuse_octo_prune.services.OpenIMAJImageService;
import org.apache.log4j.Logger;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.asift.ASIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.processing.face.detection.CLMDetectedFace;
import org.openimaj.image.processing.face.detection.CLMFaceDetector;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.feature.DoGSIFTFeature;
import org.openimaj.image.processing.face.feature.comparison.DoGSIFTFeatureComparator;
import org.openimaj.image.processing.face.similarity.FaceSimilarityEngine;
import org.openimaj.image.processing.face.util.CLMDetectedFaceRenderer;
import org.openimaj.image.processing.face.util.KEDetectedFaceRenderer;
import org.openimaj.image.processing.face.util.SimpleDetectedFaceRenderer;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.tools.faces.FaceSimilarityTool;
import org.openimaj.util.pair.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by smartkit on 2016/11/21.
 */
public class OpenIMAJImageServiceImpl implements OpenIMAJImageService{

    private static Logger LOG = org.apache.log4j.LogManager.getLogger(OpenIMAJImageServiceImpl.class);

    @Override
    public AnalysisResponseVO analysis(File imgFile) throws IOException {
        MBFImage mbf = ImageUtilities.readMBF(imgFile);
        // A simple Haar-Cascade face detector
        AnalysisResponseVO analysisResponseVO = new AnalysisResponseVO();
        HaarCascadeDetector det1 = new HaarCascadeDetector();
        DetectedFace face1 = det1.detectFaces(mbf.flatten()).get(0);
        new SimpleDetectedFaceRenderer()
                .drawDetectedFace(mbf,10,face1);
        analysisResponseVO.setDetectedFace(face1);
//// Get the facial keypoints
        FKEFaceDetector det2 = new FKEFaceDetector();
        KEDetectedFace face2 = det2.detectFaces(mbf.flatten()).get(0);
        new KEDetectedFaceRenderer()
                .drawDetectedFace(mbf,10,face2);

        analysisResponseVO.setKeDetectedFace(face2);
////// With the CLM Face Model
        CLMFaceDetector det3 = new CLMFaceDetector();
        CLMDetectedFace face3 = det3.detectFaces(mbf.flatten()).get(0);
        new CLMDetectedFaceRenderer()
                .drawDetectedFace(mbf,10,face3);
        analysisResponseVO.setClmDetectedFace(face3);
//        DisplayUtilities.displayName(mbf, "OPenIMAJ Analysis");//for GUI testing.

        return analysisResponseVO;
    }

    //@see http://grepcode.com/file/repo1.maven.org/maven2/org.openimaj/examples/1.3/org/openimaj/examples/image/feature/local/ASIFTMatchingExample.java#ASIFTMatchingExample.createFastBasicMatcher%28%29
    private static LocalFeatureMatcher<Keypoint> createConsistentRANSACHomographyMatcher() {
        final ConsistentLocalFeatureMatcher2d<Keypoint> matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
                createFastBasicMatcher());
        matcher.setFittingModel(new RobustHomographyEstimator(10.0, 1000, new RANSAC.BestFitStoppingCondition(),
                HomographyRefinement.NONE));

        return matcher;
    }
    private static LocalFeatureMatcher<Keypoint> createFastBasicMatcher() {
        return new FastBasicKeypointMatcher<Keypoint>(8);
    }

    @Override
    public List<Pair<Keypoint>> matching(File input1, File input2) throws IOException {

        // Read the images from two streams
        final FImage input_1 = ImageUtilities.readF(new FileInputStream(input1));
        final FImage input_2 = ImageUtilities.readF(new FileInputStream(input2));

        // Prepare the engine to the parameters in the IPOL demo
        final ASIFTEngine engine = new ASIFTEngine(false, 7);

        // Extract the keypoints from both images
        final LocalFeatureList<Keypoint> input1Feats = engine.findKeypoints(input_1);
        System.out.println("Extracted input1: " + input1Feats.size());
        final LocalFeatureList<Keypoint> input2Feats = engine.findKeypoints(input_2);
        System.out.println("Extracted input2: " + input2Feats.size());

        // Prepare the matcher, uncomment this line to use a basic matcher as
        // opposed to one that enforces homographic consistency
        // LocalFeatureMatcher<Keypoint> matcher = createFastBasicMatcher();
        final LocalFeatureMatcher<Keypoint> matcher = createConsistentRANSACHomographyMatcher();

        // Find features in image 1
        matcher.setModelFeatures(input1Feats);
        // ... against image 2
        matcher.findMatches(input2Feats);

        // Get the matches
        final List<Pair<Keypoint>> matches = matcher.getMatches();
        System.out.println("NMatches: " + matches.size());

        // Display the results
        final MBFImage inp1MBF = input_1.toRGB();
        final MBFImage inp2MBF = input_2.toRGB();
//        		DisplayUtilities.display(MatchingUtilities.drawMatches(inp1MBF, inp2MBF, matches, RGBColour.RED));//for GUI testing.

        return matches;
    }


    @Override
    public Map<String,Map<String,Double>> similarity(List<File> imgFiles, Boolean withFirst) {
        FaceSimilarityTool fst = new FaceSimilarityTool();
        final HaarCascadeDetector detector = HaarCascadeDetector.BuiltInCascade.frontalface_default.load();
        final DoGSIFTFeature.Extractor extractor = new DoGSIFTFeature.Extractor();
        final DoGSIFTFeatureComparator comparator = new DoGSIFTFeatureComparator();
        Map<String,Map<String,Double>> results = fst.getDistances(imgFiles,withFirst,new FaceSimilarityEngine<>(detector,extractor,comparator));
        LOG.info("similarity results:"+results.toString());
        return results;
    }
}
