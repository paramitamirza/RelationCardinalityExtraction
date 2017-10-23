package de.mpg.mpiinf.cardinality.autoextraction.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.mpg.mpiinf.cardinality.autoextraction.OSValidator;

public class Classifier {
	
	private String crfDir;
	private String modelDir;
	private String templateFile;
	private String relName;
	
	private static int NTHREADS = 4;
	
	public static void setNumberOfThreads(int n) {
		NTHREADS = n;
	}

	public Classifier(String relName, String crfDir, String modelDir, String templateFile) {
		this.setRelName(relName);
		this.setCrfDir(crfDir);
		this.setModelDir(modelDir);
		this.ensureDirectory(new File(this.getModelDir()));
		this.setTemplateFile(templateFile);
	}
	
	public static void main(String[] args) throws IOException {
		
		Options options = getPreprocessingOptions();

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
            
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			formatter.printHelp("RelationCardinalityExtraction: Classifier", options);

			System.exit(1);
			return;
		}
		
		String dirModels = "./models/";
		if (cmd.hasOption("m")) {
			dirModels = cmd.getOptionValue("models");
		}
		String relName = cmd.getOptionValue("relname");
		String dirCRF = cmd.getOptionValue("crf");
		String templateFile = cmd.getOptionValue("template");
		
		Classifier cl = new Classifier(relName, dirCRF, dirModels, templateFile);
		if (cmd.hasOption("n")) Classifier.setNumberOfThreads(Integer.parseInt(cmd.getOptionValue("thread")));
		String trainFile = cmd.getOptionValue("train");
		String evalFile = trainFile;
		if (cmd.hasOption("e")) evalFile = cmd.getOptionValue("eval");
		
		//Train model
		cl.trainModel(trainFile);
		
		//Test model
		cl.testModel(evalFile);
		
	}
	
	public void trainModel(String trainFile) {
		long startTime = System.currentTimeMillis();
		System.out.print("Training a CRF++ classification model... ");
		
//		String crfLearn = "crf_learn";
		String crfLearn = "usr/local/bin/crf_learn";
		if (OSValidator.isWindows()) crfLearn = "crf_learn.exe";
		
		try {
	    	ProcessBuilder builder = new ProcessBuilder(this.getCrfDir() + "/" + crfLearn, 
	    			"-p", NTHREADS+"", this.getTemplateFile(), 
	    			trainFile, this.getModelDir() + "/" + this.getRelName() + ".model");
	        Process process = builder.start();
	        
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String line = bufferedReader.readLine();
            
            while (line != null) {
            	System.err.println(line);
            	line = bufferedReader.readLine();
            }
            
            inputStream.close();
            bufferedReader.close();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
	}
	
	public void testModel(String testFile) throws IOException {
		long startTime = System.currentTimeMillis();
		System.out.print("Test a CRF++ classification model... ");
		
//		String crfTest = "crf_test";
		String crfTest = "usr/local/bin/crf_test";
		if (OSValidator.isWindows()) crfTest = "crf_test.exe";
		
//		BufferedWriter bw = new BufferedWriter(new FileWriter(testFile.replace(".data", ".out")));
		File file = new File(testFile);
		BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile().getParent() + "/" + this.getRelName() + "_cardinality.out"));
		
		try {
	    	ProcessBuilder builder = new ProcessBuilder(this.getCrfDir() + "/" + crfTest, 
	    			"-v2", 
	    			"-m", this.getModelDir() + "/" + this.getRelName() + ".model", 
	    			testFile);
	        Process process = builder.start();
	        
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String line = bufferedReader.readLine();
            
            while (line != null) {
            	bw.write(line);
            	bw.newLine();
            	line = bufferedReader.readLine();
            }
            
            inputStream.close();
            bufferedReader.close();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		long endTime   = System.currentTimeMillis();
		float totalTime = (endTime - startTime)/(float)1000;
		System.out.println("done [ " + totalTime + " sec].");
		
		bw.close();
	}
	
	private void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public static Options getPreprocessingOptions() {
		Options options = new Options();
		
		Option crf = new Option("c", "crf", true, "CRF++ directory");
		crf.setRequired(true);
		options.addOption(crf);
		
		Option relName = new Option("p", "relname", true, "Property/relation name");
		relName.setRequired(true);
		options.addOption(relName);
		
		Option train = new Option("t", "train", true, "Input train feature file (.data) path");
		train.setRequired(true);
		options.addOption(train);
		
		Option eval = new Option("e", "eval", true, "Input evaluation feature file (.data) path");
		eval.setRequired(true);
		options.addOption(eval);
		
		Option template = new Option("l", "template", true, "CRF++ template file");
		template.setRequired(true);
		options.addOption(template);
		
		Option models = new Option("m", "models", true, "Output directory of CRF++ model files");
		models.setRequired(false);
		options.addOption(models);
		
		Option nThreads = new Option("n", "thread", true, "Number of threads");
		nThreads.setRequired(false);
		options.addOption(nThreads);
		
		return options;
	}

	public String getCrfDir() {
		return crfDir;
	}

	public void setCrfDir(String crfDir) {
		this.crfDir = crfDir;
	}

	public String getModelDir() {
		return modelDir;
	}

	public void setModelDir(String modelDir) {
		this.modelDir = modelDir;
	}

	public String getTemplateFile() {
		return templateFile;
	}

	public void setTemplateFile(String templateFile) {
		this.templateFile = templateFile;
	}

	public String getRelName() {
		return relName;
	}

	public void setRelName(String relName) {
		this.relName = relName;
	}
}
