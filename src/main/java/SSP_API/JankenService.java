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

import java.util.function.Function;

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
import java.lang.IllegalArgumentException;
import redis.clients.jedis.Jedis;

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
	 * This enum implies hand type.
	 */
  public enum hand_type {
    stone, scissors, paper
  };

	/**
	 * The config value for the key {@code greeting}.
	 */
	private String greeting;

	private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
  private static Jedis jedis = new Jedis("localhost", 6379);

	JankenService(Config config) {
		this.greeting = config.get("app.greeting").asString().orElse("Ciao");
	}

  public hand_type judgeHand(Integer hand) {
    switch (hand) {
    	case 0:
      		return hand_type.stone;

    	case 1:
      		return hand_type.scissors;

      		case 2:
      		return hand_type.paper;

    	default:
      		return null;
    }
  }
  
  public JsonObject matchGame(hand_type users_hand, hand_type enemys_hand, String user_name) {

		boolean user_win = false,pc_win = false;

    switch (users_hand) {
    case stone:
      switch (enemys_hand) {
      case stone:
        break;
      case scissors:
        user_win = true;
        break;
      case paper:
        pc_win = true;
        break;
      default:
        break;

      }
      break;
    case scissors:
      switch (enemys_hand) {
      case stone:
        pc_win = true;
        break;
      case scissors:
        break;
      case paper:
        user_win = true;
        break;
      default:
        break;

      }
      break;
    case paper:
      switch (enemys_hand) {
      case stone:
        user_win = true;
        break;
      case scissors:
        pc_win = true;
        break;
      case paper:
        break;
      default:
        break;

      }
      break;
    }
    String id = UUID.randomUUID().toString();
    jedis.hset(id, "user_hand", users_hand.toString());
    jedis.hset(id, "pc_hand", enemys_hand.toString());
    jedis.hset(id, "user_win", Boolean.toString(user_win));
    jedis.hset(id, "pc_win", Boolean.toString(pc_win));
    jedis.hset(id, "id", id);
    jedis.hset(id, "user", user_name);
    jedis.hset(id, "timestamp", new Date().toString());
    System.out.println(jedis.hgetAll(id));
    // todo: idの決め方
    return JSON.createObjectBuilder()
            .add("user_hand", users_hand.toString())
            .add("pc_hand", enemys_hand.toString())
            .add("user_win", user_win)
            .add("pc_win", pc_win)
            .add("id", id)
            .add("user", user_name)
            .add("timestamp", new Date().toString())
        .build();
  }

	/**
	 * A service registers itself by updating the routine rules.
	 *
	 * @param rules the routing rules.
	 */
	@Override
	public void update(Routing.Rules rules) {
		rules
		.get("/", this::getResult)
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

  private void getResult(ServerRequest request, ServerResponse response){
    Parameters params = request.queryParams();
    Optional<String> hand = params.first("hand");

    System.out.println(jedis.hgetAll("0"));
    sendResponse(response, "Hello");
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
		Random random = new Random();
		Integer rand = random.nextInt(3);

    String a_hand = hand.get();
    hand_type users_hand = hand_type.stone;
    try {
      users_hand = hand_type.valueOf(a_hand);
    } catch(IllegalArgumentException e) {
      JsonObject jsonErrorObject = JSON.createObjectBuilder().add("error", a_hand + ": unknown hand.").build();
      response.status(Http.Status.BAD_REQUEST_400).send(jsonErrorObject);
      return;
    }
    hand_type enemys_hand = judgeHand(rand);
		JsonObject result = matchGame(users_hand, enemys_hand, params.first("user").orElse("nanashi"));

    response.status(Http.Status.CREATED_201).send(result);
    return;
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
