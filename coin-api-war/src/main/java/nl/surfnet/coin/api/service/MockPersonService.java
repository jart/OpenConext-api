/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.surfnet.coin.api.service;

import org.json.JSONArray;
import nl.surfnet.coin.api.client.domain.Person;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class MockPersonService implements PersonService {

    private final static String JSON_BASE_PATH = "json/";
    
    @Override
    public Person getPerson(String userId, String loggedInUser) {
        final ClassPathResource pathResource = new ClassPathResource(JSON_BASE_PATH + userId + ".json");
        final JSONObject jsonObject;
        final Person person = new Person();
        if (pathResource.exists()) {
            try {
                final String json = new Scanner(pathResource.getInputStream()).useDelimiter("\\A").next();
                return Person.fromJSON(json);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            person.setId("mock-id");
            person.setName("mock-name");
        }
        return person;
    }
}