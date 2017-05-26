package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import data.Ballot;
import data.ElGamalTuple;
import data.Election;
import data.MixerResult;
import data.ParallelMixerResult;
import data.ParallelProofOfShuffle;
import tools.FileManipulator;
import tools.Printer;

public class Main {

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		boolean running = true;
		List<Mixer> mixers = new ArrayList<>();
		Election election = null;

		while (running) {
			printMenu();
			String line = scan.nextLine().trim();
			String[] words = line.split(" ");
			int menu_item = Integer.parseInt(words[0]);
			switch (menu_item) {
			case 0: // exit
				running = false;
				break;
			case 1:
				JsonObject election_json = FileManipulator.readJsonObjectFromFile(FileManipulator.ElectionFilename);
				election = new Election(election_json);
				System.out.println("Encryption key: " + Printer.bytesToHex(election.Y.getEncoded(true)));

				if (words.length > 1)
					try {
						int n = Integer.parseInt(words[1]);
						for (int i = 0; i < n; i++)
							mixers.add(new Mixer(i + 1));
						System.out.println(n + " mixers have been generated.");
						break;
					} catch (Exception e) {
						System.out.println("Argument #mixers has to be a number");
						break;
					}
				System.out.println("You need one more argument.");
				break;
			case 2:
				if (election == null) {
					System.out.println("You need to initialize frist.");
					break;
				}

				System.out.println("Parallel mixing ballots " + mixers.size() + " times:");

				int ballot_board_id = 0;
				for (Mixer mixer : mixers) {
					// read ballots from file
					List<Ballot> ballots = new ArrayList<>();
					JsonArray ballots_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getBallotBoardFilename(ballot_board_id));
					for (JsonElement item : ballots_json)
						ballots.add(new Ballot(item.getAsJsonObject()));

					ballot_board_id++;
					ParallelMixerResult result = mixer.mixBallots(ballots, election.Y);

					JsonArray mixed_ballots_json = new JsonArray();
					for (Ballot item : result.mixed_board)
						mixed_ballots_json.add(item.toJsonObject());
					FileManipulator.writeToFile(FileManipulator.getBallotBoardFilename(ballot_board_id), mixed_ballots_json);

					FileManipulator.writeToFile(FileManipulator.getParallelProofFilename(ballot_board_id),
							result.proof_of_shuffle.toJsonObject());
				}

				break;
			case 3:
				break;
			case 4:
				if (election == null) {
					System.out.println("You need to initialize frist.");
					break;
				}

				System.out.println("Mixing votes " + mixers.size() + " times:");
				
				int votes_board_id = 0;
				for (Mixer mixer : mixers) {
					// read ballots from file
					List<ElGamalTuple> votes = new ArrayList<>();
					JsonArray votes_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getVoteBoardFilename(votes_board_id));
					for (JsonElement item : votes_json)
						votes.add(new ElGamalTuple(item.getAsJsonObject()));

					votes_board_id++;
					MixerResult result = mixer.mixVotes(votes, election.Y);

					JsonArray mixed_votes_json = new JsonArray();
					for (ElGamalTuple item : result.mixed_board)
						mixed_votes_json.add(item.toJsonObject());
					FileManipulator.writeToFile(FileManipulator.getVoteBoardFilename(votes_board_id), mixed_votes_json);

					FileManipulator.writeToFile(FileManipulator.getProofFilename(votes_board_id),
							result.proof_of_shuffle.toJsonObject());
				}
				break;
			case 5:
				break;
			case 6:
				if (election == null) {
					System.out.println("You need to initialize frist.");
					break;
				}
				for (int j = 0; j < mixers.size(); j++) {
					List<Ballot> input_board = new ArrayList<>();
					JsonArray ballots_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getBallotBoardFilename(j));
					for (JsonElement item : ballots_json)
						input_board.add(new Ballot(item.getAsJsonObject()));

					List<Ballot> output_board = new ArrayList<>();
					ballots_json = FileManipulator.readJsonArrayFromFile(FileManipulator.getBallotBoardFilename(j + 1));
					for (JsonElement item : ballots_json)
						output_board.add(new Ballot(item.getAsJsonObject()));

					JsonObject proof_json = FileManipulator
							.readJsonObjectFromFile(FileManipulator.getParallelProofFilename(j + 1));
					ParallelProofOfShuffle proof = new ParallelProofOfShuffle(proof_json);

					System.out.println(
							"Proof " + (j + 1) + " check " + proof.check(input_board, output_board, election.Y));
				}
				break;
			case 7:
				break;
			default:
				System.out.println("You need to specify one of the options.");
				break;
			}
		}
		scan.close();
	}

	private static void printMenu() {
		int i = 0;
		System.out.println();
		System.out.println("To exit                        press  " + i++); // 0
		System.out.println("To initialize                  press  " + i++ + " #mixers"); // 1
		System.out.println("To automatically mix ballots   press  " + i++); // 2
		System.out.println("To manually mix ballots        press  " + i++ + " inputfile"); // 3
		System.out.println("To automatically mix enc votes press  " + i++); // 4
		System.out.println("To manually mix enc votes      press  " + i++ + " inputfile"); // 5
		System.out.println("To check parallel proofs       press  " + i++); // 6
		System.out.println("To check proof                 press  " + i++); // 7
	}

}
