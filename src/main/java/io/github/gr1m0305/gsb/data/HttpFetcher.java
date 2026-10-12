package io.github.gr1m0305.gsb.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

/** Downloads JSON from public APIs off the game thread. */
final class HttpFetcher {
	/** The server answered, but not with what was asked for. */
	static final class StatusException extends IOException {
		final int status;

		StatusException(int status, String url) {
			super("HTTP " + status + " from " + url);
			this.status = status;
		}
	}

	static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Gr1msSkyblock-Data");
		thread.setDaemon(true);
		return thread;
	});

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private HttpFetcher() {
	}

	static <T> T fetch(String url, Function<Reader, T> parser) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Accept-Encoding", "gzip")
				.header("User-Agent", "Gr1msSkyblock")
				.GET()
				.build();
		HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

		try (InputStream body = response.body()) {
			if (response.statusCode() != 200) {
				throw new StatusException(response.statusCode(), url);
			}

			boolean gzip = response.headers().firstValue("Content-Encoding").orElse("").equalsIgnoreCase("gzip");

			try (Reader reader = new InputStreamReader(gzip ? new GZIPInputStream(body) : body, StandardCharsets.UTF_8)) {
				return parser.apply(reader);
			}
		}
	}
}
