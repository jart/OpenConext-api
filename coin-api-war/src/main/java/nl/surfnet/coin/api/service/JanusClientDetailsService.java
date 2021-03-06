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

import nl.surfnet.coin.api.oauth.*;
import nl.surfnet.coin.janus.Janus;
import nl.surfnet.coin.janus.domain.ARP;
import nl.surfnet.coin.janus.domain.EntityMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth.common.OAuthException;
import org.springframework.security.oauth.common.signature.SharedConsumerSecretImpl;
import org.springframework.security.oauth.provider.ConsumerDetails;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Client details service that uses Janus as backend. Implements both the oauth1
 * and oauth2 interface.
 */
public class JanusClientDetailsService implements OpenConextClientDetailsService {

  private final static Logger LOG = LoggerFactory.getLogger(JanusClientDetailsService.class);
  
  @Autowired
  private Janus janus;

  @Override
  @Cacheable(value = { "janus-meta-data" })
  public ClientDetails loadClientByClientId(String consumerKey) throws OAuth2Exception {
    EntityMetadata metadata = getJanusMetadataByConsumerKey(consumerKey, OAuth2Exception.create(OAuth2Exception.INVALID_CLIENT, null));
    validateMetadata(consumerKey, metadata);
    final OpenConextClientDetails clientDetails = new OpenConextClientDetails();
    ClientMetaData clientMetaData = new JanusClientMetadata(metadata);
    clientDetails.setClientMetaData(clientMetaData);
    clientDetails.setClientSecret(metadata.getOauthConsumerSecret());
    clientDetails.setClientId(metadata.getOauthConsumerKey());
    clientDetails.setRegisteredRedirectUri(getCallbackUrlCollection(metadata));
    clientDetails.setScope(Arrays.asList("read"));

    clientDetails.setAuthorizedGrantTypes(Arrays.asList("implicit", "authorization_code"));
    if (metadata.isTwoLeggedOauthAllowed()) {
       clientDetails.getAuthorizedGrantTypes().add("client_credentials");
    }
    ArrayList<GrantedAuthority> authorities = new ArrayList<>(clientDetails.getAuthorities());
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    clientDetails.setAuthorities(authorities);
    ClientMetaDataHolder.setClientMetaData(clientMetaData);
    return clientDetails;
  }

  private void validateMetadata(String consumerKey, EntityMetadata metadata) {
    if (metadata == null) {
      String format = String.format("No unique consumer found by consumer key '%s'.",consumerKey);
      throw new RuntimeException(format);
    }
  }

  @Cacheable(value = { "janus-meta-data" })
  public ARP getArp(String clientId) {
    return janus.getArp(clientId);
  }

  /*
   * In janus we can set the callback url as a comma separated list which we need to convert
   */
  private Set<String> getCallbackUrlCollection(final EntityMetadata metadata) {
    final String callbackUrl = metadata.getOauthCallbackUrl();
    //sensible default
    Set<String> result = Collections.emptySet();
    if (callbackUrl != null) {
      if (callbackUrl.contains(",")) {
        //need to trim, therefore more code then calling StringUtils#commaDelimitedListToSet
        String[] callbacksArray = StringUtils.commaDelimitedListToStringArray(callbackUrl);
        result = new HashSet<String>();
        for (String callback : callbacksArray) {
          result.add(callback.trim());
        }
      } else {
        result = Collections.singleton(callbackUrl.trim());
      }
    }
    return result;
  }

  private EntityMetadata getJanusMetadataByConsumerKey(String consumerKey, RuntimeException e) {
    List<String> entityIds = janus.getEntityIdsByMetaData(Janus.Metadata.OAUTH_CONSUMERKEY, consumerKey);
    if (entityIds.size() != 1) {
      throw e;
    }
    String entityId = entityIds.get(0);
    return janus.getMetadataByEntityId(entityId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Cacheable(value = { "janus-meta-data" })
  public ConsumerDetails loadConsumerByConsumerKey(String consumerKey) throws OAuthException {
    EntityMetadata metadata = getJanusMetadataByConsumerKey(consumerKey, new OAuthException(OAuth2Exception.INVALID_CLIENT));
    validateMetadata(consumerKey, metadata);
    final OpenConextConsumerDetails consumerDetails = new OpenConextConsumerDetails();
    consumerDetails.setConsumerKey(consumerKey);
    consumerDetails.setAuthorities(Arrays.<GrantedAuthority> asList(new SimpleGrantedAuthority("ROLE_USER")));
    ClientMetaData clientMetaData = new JanusClientMetadata(metadata);
    consumerDetails.setClientMetaData(clientMetaData);
    ClientMetaDataHolder.setClientMetaData(clientMetaData);

    consumerDetails.setSignatureSecret(new SharedConsumerSecretImpl(metadata.getOauthConsumerSecret()));

    // set to required by default
    consumerDetails.setRequiredToObtainAuthenticatedToken(true);
    if (metadata.isTwoLeggedOauthAllowed()) {
      // two legged allowed
      consumerDetails.setRequiredToObtainAuthenticatedToken(false);
    }

    return consumerDetails;
  }

  /**
   * @param janus the janus to set
   */
  public void setJanus(Janus janus) {
    this.janus = janus;
  }
}
