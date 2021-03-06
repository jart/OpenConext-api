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

package nl.surfnet.coin.api.saml;

import java.util.Collections;
import java.util.List;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.xml.XMLObject;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import nl.surfnet.spring.security.opensaml.Provisioner;

/**
 * Implementation of Spring-security-opensaml's Provisioner interface, which provisions a UserDetails object based on a SAML Assertion.
 */
public class SAMLProvisioner implements Provisioner {

  private String uuidAttribute ;

  /**
   * @param uuidAttribute
   */
  public SAMLProvisioner(String uuidAttribute) {
    super();
    this.uuidAttribute = uuidAttribute;
  }


  @Override
  public UserDetails provisionUser(Assertion assertion) {
    String userId = getValueFromAttributeStatements(assertion, uuidAttribute);
    Assert.hasLength(userId, "SAML assertion does not contain required personId (" + uuidAttribute + ")");
    return new User(userId, "N/A", Collections.singletonList(new SimpleAuthority("USER")));
  }

  public static class SimpleAuthority implements GrantedAuthority {

    private static final long serialVersionUID = 1L;

    String name;

    public SimpleAuthority(String name) {
      this.name = name;
    }

    @Override
    public String getAuthority() {
      return name;
    }
  }


  private String getValueFromAttributeStatements(final Assertion assertion, final String name) {
    final List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
    for (AttributeStatement attributeStatement : attributeStatements) {
      final List<Attribute> attributes = attributeStatement.getAttributes();
      for (Attribute attribute : attributes) {
        if (name.equals(attribute.getName())) {
          XMLObject xmlObject = attribute.getAttributeValues().get(0);
          String nodeValue = xmlObject.getDOM().getFirstChild().getNodeValue();
          return nodeValue;
        }
      }
    }
    return "";
  }
}
