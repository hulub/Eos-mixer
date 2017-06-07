package tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileManipulator {
	private static final String BallotBoardFilename = "ballot_board", VoteBoardFilename = "vote_board",
			ParallelProofFilename = "parallel_proof", ElectionFilename = "election", ProofFilename = "proof",
			ending = ".json";
	private static final String separator = System.getProperty("os.name").contains("Windows") ? "\\" : "/";
	private static final String eos_path = "eos" + separator, ballot_path = eos_path + "mixed_ballots" + separator,
			vote_path = eos_path + "mixed_votes" + separator;

	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static JsonParser parser = new JsonParser();

	public static String getEosFilename() {
		return eos_path;
	}

	public static String getElectionFilename() {
		return eos_path + ElectionFilename + ending;
	}

	public static String getBallotBoardFilename(int i) {
		if (i == 0)
			return eos_path + BallotBoardFilename + ending;
		else {
			File directory = new File(ballot_path);
			if (!directory.exists() && !directory.mkdirs()) {
				System.out.println("Couldn't create dir: " + directory);
				return eos_path + BallotBoardFilename + "_" + i + ending;
			}
			return ballot_path + BallotBoardFilename + "_" + i + ending;
		}
	}

	public static String getParallelProofFilename(int i) {
		File directory = new File(ballot_path);
		if (!directory.exists() && !directory.mkdirs()) {
			System.out.println("Couldn't create dir: " + directory);
			return eos_path + ParallelProofFilename + "_" + i + ending;
		}
		return ballot_path + ParallelProofFilename + "_" + i + ending;
	}

	public static String getVoteBoardFilename(int i) {
		if (i == 0)
			return eos_path + VoteBoardFilename + ending;
		else {
			File directory = new File(vote_path);
			if (!directory.exists() && !directory.mkdirs()) {
				System.out.println("Couldn't create dir: " + directory);
				return eos_path + VoteBoardFilename + "_" + i + ending;
			}
			return vote_path + VoteBoardFilename + "_" + i + ending;
		}
	}

	public static String getProofFilename(int i) {
		File directory = new File(vote_path);
		if (!directory.exists() && !directory.mkdirs()) {
			System.out.println("Couldn't create dir: " + directory);
			return eos_path + ProofFilename + "_" + i + ending;
		}
		return vote_path + ProofFilename + "_" + i + ending;
	}

	public static void writeToFile(String fileName, JsonElement json) {
		try {
			FileWriter writer = new FileWriter(new File(fileName));
			writer.write(gson.toJson(json));
			writer.flush();
			writer.close();
		} catch (IOException ex) {
			System.out.println("Problem reading file");
		}
	}

	public static JsonObject readJsonObjectFromFile(String fileName) {
		try {
			FileReader reader = new FileReader(new File(fileName));
			JsonObject json = parser.parse(reader).getAsJsonObject();
			reader.close();
			return json;
		} catch (FileNotFoundException ex) {
			System.out.println("Couldn't find file " + fileName);
		} catch (IOException e) {
			System.out.println("Problem reading file");
		}
		return null;
	}

	public static JsonArray readJsonArrayFromFile(String fileName) {
		try {
			FileReader reader = new FileReader(new File(fileName));
			JsonArray json = parser.parse(reader).getAsJsonArray();
			reader.close();
			return json;
		} catch (FileNotFoundException ex) {
			System.out.println("Couldn't find file " + fileName);
		} catch (IOException e) {
			System.out.println("Problem reading file");
		}
		return null;
	}

	public static int getLastBallotMixingID() {
		int max = 0;
		File directory = new File(ballot_path);
		if (directory.exists())
			try {
				Optional<Integer> max_opt = Files.walk(directory.toPath()).filter(Files::isRegularFile)
						.filter(p -> p.toString().contains(BallotBoardFilename) && p.toString().contains(ending))
						.map(p -> p.toString())
						.map(s -> s.replace(ballot_path + BallotBoardFilename + "_", "").replace(ending, "")).map(s -> {
							try {
								int x = Integer.parseInt(s);
								return x;
							} catch (NumberFormatException e) {
								return -1;
							}
						}).max(Integer::compare);
				if (max_opt.isPresent())
					max = max_opt.get();
			} catch (IOException e) {
			}

		return max;
	}
	
	public static int getLastVoteMixingID() {
		int max = 0;
		File directory = new File(vote_path);
		if (directory.exists())
			try {
				Optional<Integer> max_opt = Files.walk(directory.toPath()).filter(Files::isRegularFile)
						.filter(p -> p.toString().contains(VoteBoardFilename) && p.toString().contains(ending))
						.map(p -> p.toString())
						.map(s -> s.replace(vote_path + VoteBoardFilename + "_", "").replace(ending, "")).map(s -> {
							try {
								int x = Integer.parseInt(s);
								return x;
							} catch (NumberFormatException e) {
								return -1;
							}
						}).max(Integer::compare);
				if (max_opt.isPresent())
					max = max_opt.get();
			} catch (IOException e) {
			}

		return max;
	}
}
