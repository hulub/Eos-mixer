package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import data.Ballot;
import data.ElGamalTuple;
import data.Election;
import data.MixerResult;
import data.ProofOfShuffle;
import tools.FileManipulator;
import tools.GitManipulator;
import tools.Printer;

public class Main {

	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		boolean running = true;
		Election election = null;

		while (running) {
			printMenu();
			String line = scan.nextLine().trim();
			String[] words = line.split(" ");
			int menu_item = Integer.parseInt(words[0]);
			switch (menu_item) {
			case 0: {// exit
				running = false;
				break;
			}
			case 1: {
				GitManipulator.initialize();

				break;
			}
			case 2: {
				try {
					JsonObject election_json = FileManipulator
							.readJsonObjectFromFile(FileManipulator.getElectionFilename());
					election = new Election(election_json);
					System.out.println("Encryption key: " + Printer.bytesToHex(election.Y.getEncoded(true)));
				} catch (Exception e) {
					System.out.println("Couldn't read election file");
					break;
				}

				int id = FileManipulator.getLastBallotMixingID();

				System.out.println("Parallel mixing ballots ...");

				// read ballots from file
				try {
					List<Ballot> ballots = new ArrayList<>();
					JsonArray ballots_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getBallotBoardFilename(id));
					for (JsonElement item : ballots_json)
						ballots.add(new Ballot(item.getAsJsonObject()));
					// turn list into two dimension array (n X 3)
					ElGamalTuple[][] board = ballots.stream()
							.map(b -> new ElGamalTuple[] { b.color_enc, b.eID_enc, b.vote_enc })
							.toArray(ElGamalTuple[][]::new);

					// mix
					MixerResult result = Mixer.mix(board, election.Y);

					// turn two dimension array back into list
					List<Ballot> mixed_ballots = Arrays.stream(result.mixed_board)
							.map(a -> new Ballot(a[0], a[1], a[2])).collect(Collectors.toList());

					// write mixed board to file
					JsonArray mixed_ballots_json = new JsonArray();
					for (Ballot item : mixed_ballots)
						mixed_ballots_json.add(item.toJsonObject());
					FileManipulator.writeToFile(FileManipulator.getBallotBoardFilename(id + 1), mixed_ballots_json);

					// write proof to file
					FileManipulator.writeToFile(FileManipulator.getParallelProofFilename(id + 1),
							result.proof_of_shuffle.toJsonObject());

					System.out.println("Mixing finished");
				} catch (Exception e) {
					System.out.println("Couldn't read ballot board");
				}

				break;
			}
			case 3: {
				try {
					JsonObject election_json = FileManipulator
							.readJsonObjectFromFile(FileManipulator.getElectionFilename());
					election = new Election(election_json);
					System.out.println("Encryption key: " + Printer.bytesToHex(election.Y.getEncoded(true)));
				} catch (Exception e) {
					System.out.println("Couldn't read election file");
					break;
				}

				int id = FileManipulator.getLastVoteMixingID();

				System.out.println("Mixing votes ...");

				// read ballots from file
				try {
					List<ElGamalTuple> votes = new ArrayList<>();
					JsonArray votes_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getVoteBoardFilename(id));
					for (JsonElement item : votes_json)
						votes.add(new ElGamalTuple(item.getAsJsonObject()));
					// turn list into two dimension array (n X 1)
					ElGamalTuple[][] board = votes.stream().map(et -> new ElGamalTuple[] { et })
							.toArray(ElGamalTuple[][]::new);

					// mix
					MixerResult result = Mixer.mix(board, election.Y);

					// turn two dimension array back to list
					List<ElGamalTuple> mixed_votes = Arrays.stream(result.mixed_board).map(a -> a[0])
							.collect(Collectors.toList());

					// write mixed board to file
					JsonArray mixed_votes_json = new JsonArray();
					for (ElGamalTuple item : mixed_votes)
						mixed_votes_json.add(item.toJsonObject());
					FileManipulator.writeToFile(FileManipulator.getVoteBoardFilename(id + 1), mixed_votes_json);

					// write proof to file
					FileManipulator.writeToFile(FileManipulator.getProofFilename(id + 1),
							result.proof_of_shuffle.toJsonObject());

					System.out.println("Mixing finished");
				} catch (Exception e) {
					System.out.println("Couldn't read ballot board");
				}
				break;
			}
			case 4: {
				if (election == null) {
					System.out.println("You need to initialize frist.");
					break;
				}
				int n = FileManipulator.getLastBallotMixingID();
				for (int i = 0; i < n; i++) {
					List<Ballot> input_ballots = new ArrayList<>();
					JsonArray ballots_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getBallotBoardFilename(i));
					for (JsonElement item : ballots_json)
						input_ballots.add(new Ballot(item.getAsJsonObject()));
					ElGamalTuple[][] input_board = input_ballots.stream()
							.map(b -> new ElGamalTuple[] { b.color_enc, b.eID_enc, b.vote_enc })
							.toArray(ElGamalTuple[][]::new);

					List<Ballot> output_ballots = new ArrayList<>();
					ballots_json = FileManipulator.readJsonArrayFromFile(FileManipulator.getBallotBoardFilename(i + 1));
					for (JsonElement item : ballots_json)
						output_ballots.add(new Ballot(item.getAsJsonObject()));
					ElGamalTuple[][] output_board = output_ballots.stream()
							.map(b -> new ElGamalTuple[] { b.color_enc, b.eID_enc, b.vote_enc })
							.toArray(ElGamalTuple[][]::new);

					JsonObject proof_json = FileManipulator
							.readJsonObjectFromFile(FileManipulator.getParallelProofFilename(i + 1));
					ProofOfShuffle proof = new ProofOfShuffle(proof_json);

					System.out.println(
							"Proof " + (i + 1) + " check " + proof.check(input_board, output_board, election.Y));
				}
				break;
			}
			case 5: {
				if (election == null) {
					System.out.println("You need to initialize frist.");
					break;
				}
				int n = FileManipulator.getLastVoteMixingID();
				for (int i = 0; i < n; i++) {
					List<ElGamalTuple> input_votes = new ArrayList<>();
					JsonArray votes_json = FileManipulator
							.readJsonArrayFromFile(FileManipulator.getVoteBoardFilename(i));
					for (JsonElement item : votes_json)
						input_votes.add(new ElGamalTuple(item.getAsJsonObject()));
					ElGamalTuple[][] input_board = input_votes.stream().map(b -> new ElGamalTuple[] { b })
							.toArray(ElGamalTuple[][]::new);

					List<ElGamalTuple> output_votes = new ArrayList<>();
					votes_json = FileManipulator.readJsonArrayFromFile(FileManipulator.getVoteBoardFilename(i + 1));
					for (JsonElement item : votes_json)
						output_votes.add(new ElGamalTuple(item.getAsJsonObject()));
					ElGamalTuple[][] output_board = output_votes.stream().map(b -> new ElGamalTuple[] { b })
							.toArray(ElGamalTuple[][]::new);

					JsonObject proof_json = FileManipulator
							.readJsonObjectFromFile(FileManipulator.getProofFilename(i + 1));
					ProofOfShuffle proof = new ProofOfShuffle(proof_json);

					System.out.println(
							"Proof " + (i + 1) + " check " + proof.check(input_board, output_board, election.Y));
				}
				break;
			}
			case 6: {
				GitManipulator.pushGitRepo();
				break;
			}
			default: {
				System.out.println("You need to specify one of the options.");
				break;
			}
			}
		}
		scan.close();
	}

	private static void printMenu() {
		int i = 0;
		System.out.println();
		System.out.println("To exit                        press  " + i++); // 0
		System.out.println("To pull git repo               press  " + i++); // 1
		System.out.println("To mix ballots                 press  " + i++); // 2
		System.out.println("To mix encrypted votes         press  " + i++); // 3
		System.out.println("To check parallel proofs       press  " + i++); // 4
		System.out.println("To check proof                 press  " + i++); // 5
		System.out.println("To push git repo               press  " + i++); // 6
	}

}
