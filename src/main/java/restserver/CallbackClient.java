
package restserver;

import battleship.*;
import restserver.Dtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class CallbackClient {

	private final HttpClient http;
	private final ObjectMapper mapper;

	public CallbackClient() {
		this.http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.mapper = new ObjectMapper();
	}

	/**
	 * Fire AI shots at the student's callback URL and record the results.
	 *
	 * @return true if the callback succeeded; false on error
	 */
	public boolean sendShots(GameSession session, List<IPosition> aiShots) {
		ShotRequest dto = buildShotRequest(aiShots);

		String url = session.getCallbackUrl()
				+ "/game/" + session.getGameId() + "/shots";

		String body;
		try {
			body = mapper.writeValueAsString(dto);
		} catch (IOException e) {
			System.err.println(
					"[Callback] Failed to serialize AI shots: " + e.getMessage()
			);
			return false;
		}

		HttpRequest request = buildHttpRequest(url, body);

		HttpResponse<String> response;
		try {
			response = http.send(
					request,
					HttpResponse.BodyHandlers.ofString()
			);
		} catch (IOException | InterruptedException e) {
			System.err.println(
					"[Callback] HTTP call to student failed: " + e.getMessage()
			);
			return false;
		}

		if (!isSuccessfulResponse(response)) {
			return false;
		}

		ShotResponse m2b;
		try {
			m2b = mapper.readValue(response.body(), ShotResponse.class);
		} catch (IOException e) {
			System.err.println(
					"[Callback] Failed to parse student m2b response: " + e.getMessage()
			);
			return false;
		}

		recordAiMove(session.getGame(), aiShots, m2b);
		checkAiVictory(session, m2b);

		return true;
	}

	private ShotRequest buildShotRequest(List<IPosition> aiShots) {
		ShotRequest dto = new ShotRequest();
		dto.shots = new ArrayList<>();

		for (IPosition pos : aiShots) {
			ShotPosition sp = new ShotPosition();
			sp.row = String.valueOf(pos.getClassicRow());
			sp.column = pos.getClassicColumn();
			dto.shots.add(sp);
		}

		return dto;
	}

	private HttpRequest buildHttpRequest(String url, String body) {
		return HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(10))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
	}

	private boolean isSuccessfulResponse(HttpResponse<String> response) {
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			System.err.printf(
					"[Callback] Student returned HTTP %d: %s%n",
					response.statusCode(),
					response.body()
			);
			return false;
		}

		return true;
	}

	private void checkAiVictory(GameSession session, ShotResponse m2b) {
		if (m2b.shipsRemaining == 0 || "GAME_OVER".equals(m2b.gameStatus)) {
			session.markAiWins();

			System.out.printf(
					"[%s] GAME OVER — AI wins against '%s'!%n",
					session.getGameId(),
					session.getPlayerName()
			);
		}
	}

	/**
	 * Records the AI's shots and the student's reported results into the Game.
	 */
	private void recordAiMove(
			IGame game,
			List<IPosition> shots,
			ShotResponse m2b) {

		if (m2b.results == null) {
			return;
		}

		List<IGame.ShotResult> shotResults = new ArrayList<>();

		for (int i = 0; i < shots.size() && i < m2b.results.size(); i++) {
			ShotResult r = m2b.results.get(i);

			boolean valid = !"INVALID".equals(r.outcome);
			boolean repeated = "REPEATED".equals(r.outcome);
			boolean sunk = "SUNK".equals(r.outcome);

			shotResults.add(
					new IGame.ShotResult(valid, repeated, null, sunk)
			);
		}

		Move move = new Move(
				game.getAlienMoves().size() + 1,
				shots,
				shotResults
		);

		game.getAlienMoves().add(move);
	}
}
