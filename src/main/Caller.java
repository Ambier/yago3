package main;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.administrative.Parameters;
import javatools.parsers.NumberFormatter;
import basics.Theme;
import extractors.Extractor;

/**
 * Caller - YAGO2s
 * 
 * Calls the extractors, as given in the ini-file. The format in the ini-file
 * is: extractors = extractors.HardExtractor(./mydatafolder),
 * extractors.WikipediaExtractor(myWikipediaFile), ...
 * 
 * @author Fabian
 * 
 */
public class Caller {

	/** Where the files shall go */
	public static File outputFolder;

	/** Header for the YAGO files */
	public static String header = "This file is part of the ontology YAGO2s\nIt is licensed under a Creative-Commons Attribution License by the YAGO team\nat the Max Planck Institute for Informatics/Germany.\nSee http://yago-knowledge.org for all details.\n\n";

	/** Calls specific extractors in the given order */
	public static void callNow(List<Extractor> extractors) throws Exception {
		Announce.doing("Calling extractors");
		for (Extractor e : extractors) {
			e.extract(outputFolder, header);
		}
		Announce.done();
	}

	/** Calls all extractors in the right order */
	public static void call(List<Extractor> extractors) throws Exception {
		Set<Theme> themesWeHave = new TreeSet<Theme>();
		Announce.doing("Calling extractors");
		Announce.message("Extractors", extractors);
		for (int i = 0; i < extractors.size(); i++) {
			Extractor e = extractors.get(i);
			if (e.input().isEmpty() || themesWeHave.containsAll(e.input())) {
				e.extract(outputFolder, header);
				themesWeHave.addAll(e.output().keySet());
				extractors.remove(i);
				Announce.message("----------------------------");
				Announce.message("Current themes:", themesWeHave);
				Announce.message("Current extractors:", extractors);
				i = -1; // Start again from the beginning
			}
		}
		// Call the ALL extractors
		for (int i = 0; i < extractors.size(); i++) {
			Extractor e = extractors.get(i);
			if (!e.input().contains(Theme.ALL))
				continue;
			e.extract(outputFolder, header);
			themesWeHave.addAll(e.output().keySet());
			extractors.remove(i);
			Announce.message("----------------------------");
			Announce.message("Current themes:", themesWeHave);
			Announce.message("Current extractors:", extractors);
			i--;
		}
		if (!extractors.isEmpty())
			Announce.warning("Could not call", extractors);
		Announce.done();
	}

	/** Creates extractors as given by the names */
	public static List<Extractor> extractors(List<String> extractorNames) {
		Announce.doing("Creating extractors");
		if (extractorNames == null) {
			Announce.error("No extractors given\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
		}
		if (extractorNames.isEmpty()) {
			Announce.error("Empty extractor list\nThe ini file should contain:\nextractors = extractorClass(fileName), ...");
		}
		List<Extractor> extractors = new ArrayList<Extractor>();
		for (String extractorName : extractorNames) {
			Extractor e = extractorForCall(extractorName);
			if (e != null)
				extractors.add(e);
		}
		Announce.done();
		return (extractors);
	}

	/** Creates an extractor for a call of the form "extractorName(File)" */
	public static Extractor extractorForCall(String extractorName) {
		Announce.doing("Creating", extractorName);
		Matcher m = Pattern.compile("([A-Za-z0-9\\.]+)\\(([A-Za-z_0-9\\-:/\\.]*)\\)").matcher(extractorName);
		if (!m.matches()) {
			Announce.error("Cannot understand extractor call:", extractorName);
			Announce.failed();
			return (null);
		}
		Extractor extractor = Extractor.forName(m.group(1), m.group(2) == null || m.group(2).isEmpty() ? null
				: new File(m.group(2)));
		if (extractor == null) {
			Announce.failed();
			return (null);
		}
		Announce.done();
		return (extractor);
	}

	/** Run */
	public static void main(String[] args) throws Exception {
		File logFile=new File("yago_"+NumberFormatter.timeStamp()+".log");
		Announce.message("Output written to"+logFile);
		Writer log=new FileWriter(logFile);
		Announce.setWriter(log);
		Announce.doing("Creating YAGO");
		long time = System.currentTimeMillis();
		Announce.message("Starting at", NumberFormatter.ISOtime());
		String initFile = args.length == 0 ? "yago.ini" : args[0];
		Announce.doing("Initializing from", initFile);
		Parameters.init(initFile);
		Announce.done();
		outputFolder = Parameters.getOrRequestAndAddFile("yagoFolder", "the folder where YAGO should be created");
		if (Parameters.isDefined("callNow")) {
			callNow(extractors(Parameters.getList("callNow")));
		} else {
			call(extractors(Parameters.getList("extractors")));
		}
		long now = System.currentTimeMillis();
		Announce.message("Finished at", NumberFormatter.ISOtime());
		Announce.message("Time needed:", NumberFormatter.formatMS(now - time));
		Announce.done();
		log.close();
	}
}
