package regression;

import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class RegressionManagerApp {

	public static final String CURRENT_DIRECTORY = "regression/current/";
	public static final String BASELINE_DIRECTORY = "regression/baseline/";


	public static List<TextFileRegression> getRegressions() {
		List<TextFileRegression> list = new ArrayList<TextFileRegression>();

//		list.add( new CornerDetectorChangeRegression());
		// TODO add corner feature intensity images
		list.add( new DetectDescribeRegression());
		// TODO add descriptor stability
//		list.add( new ObjectTrackingRegression());
		// TODO add point trackers
		// TODO compute visual odometry
		//      -- Stereo
		//      -- Kinect
		//      -- Mono-plane
		// TODO Calibration key points change
		// TODO Calibration optimization

		return list;
	}

	public static void clearWorkDirectory() {
		File files[] = new File(".").listFiles();

		// sanity check the directory before it starts deleting shit
		if( !contains(files,"boofcv"))
			throw new RuntimeException("Can't find boofcv in working directory");
		if( !contains(files,"lib"))
			throw new RuntimeException("Can't find lib in working directory");
		if( !contains(files,"regression"))
			throw new RuntimeException("Can't find regression in working directory");

		File tmp = new File("tmp");
		if( tmp.exists() ) {
			delete(tmp);
		}

		for( File f : files ) {
			if( f.isDirectory() )
				continue;

			if( f.isHidden() )
				continue;

			if( f.getName().contains(".iml") )
				continue;

			if( f.getName().equals("readme.txt") )
				continue;

			if( f.getName().contains(".txt") )
				if( !f.delete() )
					throw new RuntimeException("Can't clean work directory: "+f.getName());

		}
	}

	private static boolean contains( File[] files, String name ) {
		for( File f : files ) {
			if( f.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	public static void clearCurrentDirectory() {
		delete( new File(CURRENT_DIRECTORY));
		if( !new File(CURRENT_DIRECTORY).mkdir() )
			throw new RuntimeException("Can't create directory");
		if( !new File(CURRENT_DIRECTORY+"U8").mkdir() )
			throw new RuntimeException("Can't create directory");
		if( !new File(CURRENT_DIRECTORY+"F32").mkdir() )
			throw new RuntimeException("Can't create directory");
	}

	public static void delete( File directory ) {
		File[] files = directory.listFiles();

		for( File f : files ) {
			if( f.isDirectory() ) {
				delete(f);
			} else {
				if( !f.delete() ) {
					throw new RuntimeException("Can't delete file "+f);
				}
			}
		}
		if( !directory.delete() )
			throw new RuntimeException("Can't delete directory "+directory);
	}

	public static void main(String[] args) {
		clearCurrentDirectory();

		List<TextFileRegression> tests = getRegressions();

		ImageDataType[] dataTypes = new ImageDataType[]{ImageDataType.U8,ImageDataType.F32};

		// TODO clear the regression current directory

		for( ImageDataType dataType : dataTypes ) {
			clearWorkDirectory();
			for( TextFileRegression t : tests ) {
				t.setOutputDirectory(CURRENT_DIRECTORY+"/"+dataType+"/");
				try {
					t.process(dataType);
				} catch (IOException e) {
					e.printStackTrace();
				}

				// TODO compare output to baseline
			}
		}

		// TODO print summary of results
	}
}
