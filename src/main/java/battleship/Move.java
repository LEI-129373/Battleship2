
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
				- summary.getValidShots() - summary.getRepeatedShots();

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
				summary.setRepeatedShots(summary.getRepeatedShots() + 1);
			} else {
				registerValidShots(result, summary);
			}
		}

		return summary;
	}

	private static void registerValidShots(IGame.ShotResult result, FireSummary summary) {
		summary.setValidShots(summary.getValidShots() + 1);

		if (result.ship() == null) {
			summary.setMissedShots(summary.getMissedShots() + 1);
		} else {
			String boatName = result.ship().getCategory();

			summary.getHitsPerBoat().put(
					boatName,
					summary.getHitsPerBoat().getOrDefault(boatName, 0) + 1
			);

			if (result.sunk()) {
				summary.getSunkBoatsCount().put(
						boatName,
						summary.getSunkBoatsCount().getOrDefault(boatName, 0) + 1
				);
			}
		}
	}

	private void printEnemyFireSummary(FireSummary summary, int outsideShots) {
		StringBuilder output = new StringBuilder();

		if (summary.getValidShots() == 0 && summary.getRepeatedShots() > 0) {
			appendRepeatedShots(output, summary.getRepeatedShots());
		} else {
			appendValidShots(output, summary.getValidShots());
			appendSunkBoats(output, summary.getSunkBoatsCount());
			appendBoatHits(output, summary.getHitsPerBoat(), summary.getSunkBoatsCount());
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
		if (summary.getMissedShots() > 0) {
			output.append(summary.getMissedShots())
					.append(" tiro")
					.append(summary.getMissedShots() > 1 ? "s" : "")
					.append(" na água");
		} else if (!summary.getSunkBoatsCount().isEmpty()
				|| !summary.getHitsPerBoat().isEmpty()) {
			output.setLength(output.length() - 2);
		}
	}

	private void appendRepeatedShotsAfterValid(
			StringBuilder output,
			FireSummary summary) {

		if (summary.getRepeatedShots() > 0) {
			if (summary.getValidShots() > 0) {
				output.append(", ");
			}

			appendRepeatedShots(output, summary.getRepeatedShots());
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

		response.put("validShots", summary.getValidShots());
		response.put("outsideShots", outsideShots);
		response.put("repeatedShots", summary.getRepeatedShots());
		response.put("missedShots", summary.getMissedShots());

		response.put("sunkBoats", buildSunkBoats(summary.getSunkBoatsCount()));

		response.put(
				"hitsOnBoats",
				buildBoatHits(summary.getHitsPerBoat(), summary.getSunkBoatsCount())
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
		private int validShots;
		private int repeatedShots;
		private int missedShots;

		private final Map<String, Integer> sunkBoatsCount = new HashMap<>();
		private final Map<String, Integer> hitsPerBoat = new HashMap<>();

		public int getValidShots() {
			return validShots;
		}

		public void setValidShots(int validShots) {
			this.validShots = validShots;
		}

		public int getRepeatedShots() {
			return repeatedShots;
		}

		public void setRepeatedShots(int repeatedShots) {
			this.repeatedShots = repeatedShots;
		}

		public int getMissedShots() {
			return missedShots;
		}

		public void setMissedShots(int missedShots) {
			this.missedShots = missedShots;
		}

		public Map<String, Integer> getSunkBoatsCount() {
			return sunkBoatsCount;
		}

		public Map<String, Integer> getHitsPerBoat() {
			return hitsPerBoat;
		}
	}
}
