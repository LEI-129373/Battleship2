
package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

public class Move implements IMove {

	private final int number;
	private final List<IPosition> shots;
	private final List<IGame.ShotResult> shotResults;

	public Move(int moveNumber, List<IPosition> moveShots, List<IGame.ShotResult> moveResults) {
		this.number = moveNumber;
		this.shots = moveShots;
		this.shotResults = moveResults;
	}

	@Override
	public String toString() {
		return "Move{" +
				"number=" + number +
				", shots=" + shots.size() +
				", results=" + shotResults.size() +
				'}';
	}

	@Override
	public int getNumber() {
		return this.number;
	}

	@Override
	public List<IPosition> getShots() {
		return this.shots;
	}

	@Override
	public List<IGame.ShotResult> getShotResults() {
		return this.shotResults;
	}

	@Override
	public String processEnemyFire(boolean verbose) {
		FireSummary summary = calculateFireSummary();

		int outsideShots = Game.NUMBER_SHOTS
				- summary.validShots - summary.repeatedShots;

		if (verbose) {
			printEnemyFireSummary(summary, outsideShots);
		}

		Map<String, Object> response = buildEnemyFireResponse(summary, outsideShots);
		String jsonString = serializeResponse(response);

		System.out.println(jsonString);
		System.out.println();

		return jsonString;
	}

	private FireSummary calculateFireSummary() {
		FireSummary summary = new FireSummary();

		for (IGame.ShotResult result : this.shotResults) {
			if (!result.valid()) {
				continue;
			}

			if (result.repeated()) {
				summary.repeatedShots++;
			} else {
				summary.validShots++;

				if (result.ship() == null) {
					summary.missedShots++;
				} else {
					String boatName = result.ship().getCategory();

					summary.hitsPerBoat.put(
							boatName,
							summary.hitsPerBoat.getOrDefault(boatName, 0) + 1
					);

					if (result.sunk()) {
						summary.sunkBoatsCount.put(
								boatName,
								summary.sunkBoatsCount.getOrDefault(boatName, 0) + 1
						);
					}
				}
			}
		}

		return summary;
	}

	private void printEnemyFireSummary(FireSummary summary, int outsideShots) {
		StringBuilder output = new StringBuilder();

		if (summary.validShots == 0 && summary.repeatedShots > 0) {
			appendRepeatedShots(output, summary.repeatedShots);
		} else {
			appendValidShots(output, summary.validShots);
			appendSunkBoats(output, summary.sunkBoatsCount);
			appendBoatHits(output, summary.hitsPerBoat, summary.sunkBoatsCount);
			appendMissedShots(output, summary);
			appendRepeatedShotsAfterValid(output, summary);
		}

		appendOutsideShots(output, outsideShots);

		System.out.println("Jogada nº" + this.number + " -> " + output);
	}

	private void appendValidShots(StringBuilder output, int validShots) {
		if (validShots > 0) {
			output.append(validShots)
					.append(" tiro")
					.append(validShots > 1 ? "s" : "")
					.append(" válido")
					.append(validShots > 1 ? "s" : "")
					.append(": ");
		}
	}

	private void appendSunkBoats(
			StringBuilder output,
			Map<String, Integer> sunkBoatsCount) {

		for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
			String boatName = entry.getKey();
			int count = entry.getValue();

			output.append(count)
					.append(" ")
					.append(boatName)
					.append(count > 1 ? "s" : "")
					.append(" ao fundo")
					.append(" + ");
		}
	}

	private void appendBoatHits(
			StringBuilder output,
			Map<String, Integer> hitsPerBoat,
			Map<String, Integer> sunkBoatsCount) {

		for (Map.Entry<String, Integer> entry : hitsPerBoat.entrySet()) {
			String boatName = entry.getKey();
			int hits = entry.getValue();

			if (!sunkBoatsCount.containsKey(boatName)) {
				output.append(hits)
						.append(" tiro")
						.append(hits > 1 ? "s" : "")
						.append(" num(a) ")
						.append(boatName)
						.append(" + ");
			}
		}
	}

	private void appendMissedShots(StringBuilder output, FireSummary summary) {
		if (summary.missedShots > 0) {
			output.append(summary.missedShots)
					.append(" tiro")
					.append(summary.missedShots > 1 ? "s" : "")
					.append(" na água");
		} else if (!summary.sunkBoatsCount.isEmpty()
				|| !summary.hitsPerBoat.isEmpty()) {
			output.setLength(output.length() - 2);
		}
	}

	private void appendRepeatedShotsAfterValid(
			StringBuilder output,
			FireSummary summary) {

		if (summary.repeatedShots > 0) {
			if (summary.validShots > 0) {
				output.append(", ");
			}

			appendRepeatedShots(output, summary.repeatedShots);
		}
	}

	private void appendRepeatedShots(StringBuilder output, int repeatedShots) {
		output.append(repeatedShots)
				.append(" tiro")
				.append(repeatedShots > 1 ? "s" : "")
				.append(" repetido")
				.append(repeatedShots > 1 ? "s" : "");
	}

	private void appendOutsideShots(StringBuilder output, int outsideShots) {
		if (outsideShots > 0) {
			if (!output.isEmpty()) {
				output.append(", ");
			}

			output.append(outsideShots)
					.append(" tiro")
					.append(outsideShots > 1 ? "s" : "")
					.append(" exterior")
					.append(outsideShots > 1 ? "es" : "");
		}
	}

	private Map<String, Object> buildEnemyFireResponse(
			FireSummary summary,
			int outsideShots) {

		Map<String, Object> response = new HashMap<>();

		response.put("validShots", summary.validShots);
		response.put("outsideShots", outsideShots);
		response.put("repeatedShots", summary.repeatedShots);
		response.put("missedShots", summary.missedShots);

		response.put("sunkBoats", buildSunkBoats(summary.sunkBoatsCount));

		response.put(
				"hitsOnBoats",
				buildBoatHits(summary.hitsPerBoat, summary.sunkBoatsCount)
		);

		return response;
	}

	private List<Map<String, Object>> buildSunkBoats(
			Map<String, Integer> sunkBoatsCount) {

		List<Map<String, Object>> sunkBoats = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
			Map<String, Object> boat = new HashMap<>();

			boat.put("type", entry.getKey());
			boat.put("count", entry.getValue());

			sunkBoats.add(boat);
		}

		return sunkBoats;
	}

	private List<Map<String, Object>> buildBoatHits(
			Map<String, Integer> hitsPerBoat,
			Map<String, Integer> sunkBoatsCount) {

		List<Map<String, Object>> boatHits = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : hitsPerBoat.entrySet()) {
			if (!sunkBoatsCount.containsKey(entry.getKey())) {
				Map<String, Object> boat = new HashMap<>();

				boat.put("type", entry.getKey());
				boat.put("hits", entry.getValue());

				boatHits.add(boat);
			}
		}

		return boatHits;
	}

	private String serializeResponse(Map<String, Object> response) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(
					"Erro ao serializar o JSON dos resultados da jogada", e
			);
		}
	}

	private static class FireSummary {
		int validShots;
		int repeatedShots;
		int missedShots;

		final Map<String, Integer> sunkBoatsCount = new HashMap<>();
		final Map<String, Integer> hitsPerBoat = new HashMap<>();
	}
}
