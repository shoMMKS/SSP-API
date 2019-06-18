/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package SSP_API;

import java.util.Collections;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import io.helidon.common.http.Http;
import io.helidon.common.http.Parameters;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import java.util.*;

/**
 * A simple service to greet you. Examples:
 *
 * Get default greeting message: curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe: curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting curl -X PUT -H "Content-Type: application/json" -d
 * '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * The message is returned as a JSON object
 */

public class JankenService implements Service {

	/**
	 * The config value for the key {@code greeting}.
	 */
	private String greeting;

	private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

	JankenService(Config config) {
		this.greeting = config.get("app.greeting").asString().orElse("Ciao");
	}

	public 

	/**
	 * A service registers itself by updating the routine rules.
	 *
	 * @param rules the routing rules.
	 */
	@Override
	public void update(Routing.Rules rules) {
		rules
		.get("/", this::getDefaultMessageHandler)
		.post("/", this::playJanken);

	}

	/**
	 * Return a wordly greeting message.
	 *
	 * @param request  the server request
	 * @param response the server response
	 */
	private void getDefaultMessageHandler(ServerRequest request, ServerResponse response) {
		sendResponse(response, "World");
	}

	/**
	 * Perform a janken game and return the result.
	 *
	 * @param request  the server request
	 * @param response the server response
	 */
	private void playJanken(ServerRequest request, ServerResponse response) {
		Parameters params = request.queryParams();
		Optional<String> hand = params.first("hand");
		Boolean ishand = hand.isPresent();
		if (!ishand) {
				JsonObject jsonErrorObject = JSON.createObjectBuilder().add("error", "No hand was specified.").build();
				response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
				return;
		}
		// 0:stone 1:scissors 2:paper
		Rnadom random = new Random();
		Integer rand = random.nextInt(3);

		hand.get();


		String name = request.path().param("name");
		sendResponse(response, name);

	}

	private void sendResponse(ServerResponse response, String name) {
		String msg = String.format("%s %s!", greeting, name);

		JsonObject returnObject = JSON.createObjectBuilder().add("message", msg).build();
		response.send(returnObject);
	}

	private void updateGreetingFromJson(JsonObject jo, ServerResponse response) {

		if (!jo.containsKey("greeting")) {
			JsonObject jsonErrorObject = JSON.createObjectBuilder().add("error", "No greeting provided").build();
			response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
			return;
		}

		greeting = jo.getString("greeting");
		response.status(Http.Status.NO_CONTENT_204).send();
	}

	/**
	 * Set the greeting to use in future messages.
	 *
	 * @param request  the server request
	 * @param response the server response
	 */
	private void updateGreetingHandler(ServerRequest request, ServerResponse response) {
		request.content().as(JsonObject.class).thenAccept(jo -> updateGreetingFromJson(jo, response));
	}

}