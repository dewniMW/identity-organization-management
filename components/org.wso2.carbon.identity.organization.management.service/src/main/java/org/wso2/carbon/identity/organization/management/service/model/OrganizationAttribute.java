/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.service.model;

/**
 * This class represents an attribute of an Organization entity.
 */
public class OrganizationAttribute {

    private String key;
    private String value;
    private String attributeId;

    public OrganizationAttribute(String key, String value, String attributeId) {

        this.key = key;
        this.value = value;
        this.attributeId = attributeId;
    }

    public OrganizationAttribute(String key, String value) {

        this.key = key;
        this.value = value;
    }

    public OrganizationAttribute() {

    }

    public String getAttributeId() {

        return attributeId;
    }

    public void setAttributeId(String attributeId) {

        this.attributeId = attributeId;
    }

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getValue() {

        return value;
    }

    public void setValue(String value) {

        this.value = value;
    }

    @Override
    public String toString() {

        return "key : " + this.getKey() + " value : " + this.getValue();
    }
}
