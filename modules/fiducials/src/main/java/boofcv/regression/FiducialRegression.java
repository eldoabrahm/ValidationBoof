package boofcv.regression;

import boofcv.common.*;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.metrics.*;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FiducialRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseFiducial = new File("data/fiducials");

	String infoString;

	PrintStream runtimeOut;

	public FiducialRegression() {
		super(BoofRegressionConstants.TYPE_FIDCUIALS);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		final ConfigThreshold robust = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,20);
		final ConfigThreshold fast = ConfigThreshold.fixed(80);

		FactoryObject factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
			{return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1), robust, imageType);}};
		process( "BinaryRobust", new EstimateBinaryFiducialToCamera(factory),"square_border_binary",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareBinary(new ConfigFiducialBinary(1), fast, imageType);}};
		process("BinaryFast", new EstimateBinaryFiducialToCamera(factory), "square_border_binary",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareImage(new ConfigFiducialImage(), robust, imageType);}};
		process("ImageRobust", new EstimateImageFiducialToCamera(factory), "square_border_image",false);

		factory = new FactoryObjectAbstract() {
			@Override public Object newInstance()
		{return FactoryFiducial.squareImage(new ConfigFiducialImage(), fast, imageType);}};
		process("ImageFast", new EstimateImageFiducialToCamera(factory), "square_border_image",false);

		process("ChessboardBinary", new EstimateChessboardToCameraBinary(imageType), "chessboard", true);

		process("ChessboardXCorner", new EstimateChessboardToCameraXCorner(imageType), "chessboard", true);

		process("SquareGrid", new EstimateSquareGridToCamera(imageType), "square_grid",false);

		process("CircleHexagonal", new EstimateCircleHexagonalToCamera(imageType), "circle_hexagonal",false);

		process("CircleRegular", new EstimateCircleRegularToCamera(imageType), "circle_regular",false);

		process("Uchiya", new EstimateUchiyaFiducialToCamera(imageType), "uchiya",false);
	}

	private void process(String name, BaseEstimateSquareFiducialToCamera estimate, String type, boolean ignoreOrder )
			throws IOException
	{

		infoString = name;

		estimate.setOutputDirectory(workDirectory);
		estimate.initialize(new File(baseFiducial, type));

		try {
			runtimeOut = new PrintStream(new File(directory,"RUN_Fiducial_" + name + ".txt"));
			BoofRegressionConstants.printGenerator(runtimeOut, getClass());
			runtimeOut.println("# Elapsed time is in milliseconds");
			runtimeOut.println("# dataset (total images) 50% 95% 100%");
			runtimeOut.println();

			computeStandardMetrics(type, "ACC_Fiducial_Standard_" + name + ".txt", estimate,ignoreOrder, 5);
			computeStaticMetrics(type, "ACC_Fiducial_Static_" + name + ".txt", estimate, 5);
			computeAlwaysVisibleMetrics(type, "ACC_Fiducial_AlwaysVisible_" + name + ".txt", estimate);
		} catch( RuntimeException e ) {
			e.printStackTrace();
			e.printStackTrace(errorLog);
		} finally {
			runtimeOut.close();
		}
	}

	private void computeStandardMetrics(String type, String outName,
										BaseEstimateSquareFiducialToCamera estimate ,
										boolean ignoreOrder ,
										double maxPixelError )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));
		BoofRegressionConstants.printGenerator(out,getClass());

		EvaluateFiducialToCamera evaluate = new EvaluateFiducialToCamera();
		evaluate.setJustSummary(true);
		evaluate.setIgnoreWrongOrder(ignoreOrder);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"standard"), out, evaluate);

		out.close();
	}

	private void computeStaticMetrics(String type, String outName,
										BaseEstimateSquareFiducialToCamera estimate ,
										double maxPixelError  )
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));
		BoofRegressionConstants.printGenerator(out,getClass());

		EvaluateStaticFiducialSequence evaluate = new EvaluateStaticFiducialSequence();
		evaluate.setJustSummary(true);
		evaluate.setMaxPixelError(maxPixelError);
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"static"), out, evaluate);
		out.close();
	}

	private void computeAlwaysVisibleMetrics(String type, String outName,
											 BaseEstimateSquareFiducialToCamera estimate)
			throws IOException
	{
		PrintStream out = new PrintStream(new File(directory,outName));
		BoofRegressionConstants.printGenerator(out,getClass());

		EvaluateAlwaysVisibleSequence evaluate = new EvaluateAlwaysVisibleSequence();
		evaluate.setErrorStream(errorLog);
		evaluate.setOutputResults(out);

		processDataSets(estimate, new File(new File(baseFiducial,type),"always_visible"), out, evaluate);

		out.close();
	}

	private void processDataSets(BaseEstimateSquareFiducialToCamera estimate, File dataSetsRoot,
								 PrintStream out,
								 FiducialEvaluateInterface evaluate)
			throws IOException
	{
		if( !dataSetsRoot.exists() ) {
			errorLog.println("Can't compute \"always visible\" metrics.  Doesn't exist. "+dataSetsRoot.getPath());
			return;
		}

		List<File> directories = Arrays.asList(dataSetsRoot.listFiles());
		Collections.sort(directories);

		int totalExpected = 0;
		int totalCorrect = 0;

		for( File dataSet : directories) {
			if( dataSet.isFile() )
				continue;

			if( workDirectory.exists() ) {
				BoofRegressionConstants.delete(workDirectory,errorLog);
			}
			if( !workDirectory.mkdirs() )
				throw new RuntimeException("Can't create work directory");

			try {
				estimate.process(dataSet);
				evaluate.evaluate(workDirectory, dataSet);
				totalExpected += evaluate.getTotalExpected();
				totalCorrect += evaluate.getTotalCorrect();

				estimate.speed.sort();
				double time50 = estimate.speed.getFraction(0.5);
				double time95 = estimate.speed.getFraction(0.95);
				double time100 = estimate.speed.getFraction(1.00);
				runtimeOut.printf("%30s %3d %7.3f %7.3f %7.3f\n",dataSet.getName(),estimate.speed.size,time50,time95,time100);

			} catch( DataSetDoesNotExist e ) {
				System.out.println("DataSetDoesNotExist "+e.getMessage());
				errorLog.println();
				errorLog.println(e.getMessage());
			} catch( RuntimeException e ) {
				errorLog.println();
				errorLog.println("ERROR in "+infoString+" processing data set "+dataSet);
				System.out.println("ERROR in "+infoString+" processing data set "+dataSet);
				System.out.println("  "+e.getMessage());
				e.printStackTrace(errorLog);
			}
			out.println();
			out.println("---------------------------------------------------");
			out.println();
		}

		out.println("---------------------------------------------------");
		out.printf("total correct / total expected = %4d /%4d", totalCorrect,totalExpected);
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{FiducialRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
