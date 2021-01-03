package boofcv.regression;

import boofcv.common.BaseRegression;
import boofcv.common.BoofRegressionConstants;
import boofcv.common.ImageRegression;
import boofcv.struct.image.ImageDataType;

import java.io.IOException;

/**
 * Performs MVS on an image sequence and computes the quality of the final 3D cloud for regions which have been
 * labeled as being planar. It's assumed that planar regions are planar and that no visible objects are behind them.
 * For each image which has been labeled the cloud is projected on to it and points inside each planar region are
 * found. A plane is fit to those points. Points far behind the plane are found and other metrics, such as area
 * covered are found.
 *
 * @author Peter Abeles
 */
public class MultiViewStereoPlanarRegression extends BaseRegression implements ImageRegression {
    public MultiViewStereoPlanarRegression() { super(BoofRegressionConstants.TYPE_GEOMETRY); }

    @Override public void process(ImageDataType type) throws IOException {

    }
}
