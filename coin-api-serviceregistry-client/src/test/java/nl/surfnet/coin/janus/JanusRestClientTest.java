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

package nl.surfnet.coin.janus;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.surfnet.coin.janus.domain.ARP;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import nl.surfnet.coin.janus.domain.EntityMetadata;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JanusRestClientTest {

  @InjectMocks
  private JanusRestClient janusRestClient;

  @Mock
  private RestTemplate restTemplate;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void before() {
    janusRestClient = new JanusRestClient();
    janusRestClient.setSecret("secret");
    janusRestClient.setUser("user");
    janusRestClient.setJanusUri(URI.create("http://localhost/"));
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getMetadataByEntityId() {
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("coin:oauth:secret", "hissecret");
    map.put("coin:oauth:two_legged_allowed", Boolean.TRUE);
    when(restTemplate.getForObject((URI) anyObject(), eq(Map.class))).thenReturn(map);
    String result = janusRestClient.getMetadataByEntityId("foo").getOauthConsumerSecret();
    assertEquals("hissecret", result);
    verify(restTemplate).getForObject(captor.capture(), eq(Map.class));
    assertTrue("Query string should contain correct entityid", captor.getValue().getQuery().contains("entityid=foo"));
    assertTrue("Query string should contain correct method", captor.getValue().getQuery().contains("method=getMetadata"));
    assertTrue("Query string should contain correct metadata field names", captor.getValue().getQuery().contains
            ("coin:oauth:secret"));
    assertTrue("Query string should contain correct user", captor.getValue().getQuery().contains("userid=user"));
  }

  @Test
  public void getEntityIdsByMetadata() {
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
    final List<String> ids = new ArrayList<String>(Arrays.asList("id1", "id2"));
    when(restTemplate.getForObject((URI) anyObject(), eq(List.class))).thenReturn(ids);
    List<String> result = janusRestClient.getEntityIdsByMetaData(Janus.Metadata.OAUTH_CONSUMERKEY, "foobar");
    assertEquals("id1", result.get(0));
    assertEquals("id2", result.get(1));

    verify(restTemplate).getForObject(captor.capture(), eq(List.class));
    assertTrue("Query string should contain correct key (the consumer key metadata name)",
            captor.getValue().getQuery().contains("key=coin:gadgetbaseurl"));
    assertTrue("Query string should contain correct value (the consumer key metadata value)",
            captor.getValue().getQuery().contains("value=foobar"));
    assertTrue("Query string should contain correct method", captor.getValue().getQuery().contains
            ("method=findIdentifiersByMetadata"));
    assertTrue("Query string should contain correct user", captor.getValue().getQuery().contains("userid=user"));
  }

  @Test
  public void getAllowedSps() {
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
    final List<String> ids = new ArrayList<String>(Arrays.asList("id1", "id2"));
    when(restTemplate.getForObject((URI) anyObject(), eq(List.class))).thenReturn(ids);
    List<String> result = janusRestClient.getAllowedSps("boobaa");
    assertEquals("id1", result.get(0));
    assertEquals("id2", result.get(1));

    verify(restTemplate).getForObject(captor.capture(), eq(List.class));
    assertTrue("Query string should contain correct idpentityid",
            captor.getValue().getQuery().contains("idpentityid=boobaa"));
    assertTrue("Query string should contain correct method", captor.getValue().getQuery().contains
            ("method=getAllowedSps"));
    assertTrue("Query string should contain correct user", captor.getValue().getQuery().contains("userid=user"));
  }

  @Test
  public void getSpList() {
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
    final Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();
    data.put("entityid", new HashMap<String, Object>());
    when(restTemplate.getForObject((URI) anyObject(), eq(Map.class))).thenReturn(data);
    List<EntityMetadata> result = janusRestClient.getSpList();

    assertNotNull(result.get(0));

    verify(restTemplate).getForObject(captor.capture(), eq(Map.class));
    assertTrue("Query string should contain correct method", captor.getValue().getQuery().contains
            ("method=getSpList"));
    assertTrue("Query string should contain correct user", captor.getValue().getQuery().contains("userid=user"));
    assertTrue("Query string should contain correct metadata field names", captor.getValue().getQuery().contains
            (Janus.Metadata.NAMEIDFORMAT.val()));

  }

  @Test
  public void getEmptyArp() throws IOException {
    Map data = objectMapper.readValue(new ClassPathResource("janus-json/empty-arp.json").getInputStream(), Map.class);
    when(restTemplate.getForObject((URI) anyObject(), eq(Map.class))).thenReturn(data);

    ARP arp = janusRestClient.getArp("entityid");
    assertTrue(arp.isNoArp());
    assertFalse(arp.isNoAttrArp());
    assertTrue(arp.getAttributes().isEmpty());

  }

  @Test
  public void getNoAttributesArp() throws IOException {
    Map data = objectMapper.readValue(new ClassPathResource("janus-json/empty-attributes-arp.json").getInputStream(), Map.class);
    when(restTemplate.getForObject((URI) anyObject(), eq(Map.class))).thenReturn(data);

    ARP arp = janusRestClient.getArp("entityid");
    assertFalse(arp.isNoArp());
    assertTrue(arp.isNoAttrArp());
    assertTrue(arp.getAttributes().isEmpty());
  }

  @Test
  public void getFullAttributesArp() throws IOException {
    Map data = objectMapper.readValue(new ClassPathResource("janus-json/full-arp.json").getInputStream(), Map.class);
    when(restTemplate.getForObject((URI) anyObject(), eq(Map.class))).thenReturn(data);

    ARP arp = janusRestClient.getArp("entityid");
    assertFalse(arp.isNoArp());
    assertFalse(arp.isNoAttrArp());
    assertFalse(arp.getAttributes().isEmpty());
  }


}
