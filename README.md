# identity-organization-management
This is the **Organization Management** implementation for **WSO2 - Organization Management Feature for Identity Server**.
For now, the implementation was done as an **OSGI bundle** and uses **H2 database** and **WSO2 IS 5.12.0**.

# Configurations

- Do the following configurations in **deployment.toml** file in`{IS-HOME}/repository/conf/deployment.toml`

    - Add the following configurations to use inbuilt H2 database.
      ```
      [database.identity_db]
      type = "h2"
      url = "jdbc:h2:./repository/database/WSO2IDENTITY_DB;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=60000"
      username = "wso2carbon"
      password = "wso2carbon"
      ```
      ```
      [database.shared_db]
      type = "h2"
      url = "jdbc:h2:./repository/database/WSO2SHARED_DB;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=60000"
      username = "wso2carbon"
      password = "wso2carbon"
      ```
      ```
      [database_configuration]
      enable_h2_console = "true"
      ```
    - Set H2 database as the primary user store.
      ```
      [user_store]
      type = "database_unique_id"
      ```
    - Set `resource-access-control` to use the API.
      ```
      [[resource.access_control]]
      context="(.*)/api/identity/organization-mgt/v1.0/(.*)"
      secure = "true"
      http_method = "ALL"
      ```
    - Next start the IS and go to the H2 console using, `http://localhost:8082` and provide the JDBC URL, username and password given above.
      `JDBC URL = jdbc:h2:{IS-HOME}/repository/database/WSO2SHARED_DB`
      `username = wso2carbon`
      `password = wso2carbon`

    - Then use the following queries to add tables.
      ```
      CREATE TABLE IF NOT EXISTS UM_ORG (
      UM_ID VARCHAR(255) NOT NULL,
      UM_ORG_NAME VARCHAR(255) NOT NULL,
      UM_ORG_DESCRIPTION VARCHAR(1024),
      UM_CREATED_TIME TIMESTAMP NOT NULL,
      UM_LAST_MODIFIED TIMESTAMP NOT NULL,
      UM_STATUS VARCHAR(255) DEFAULT 'ACTIVE' NOT NULL, UM_TENANT_ID INTEGER DEFAULT 0,
      UM_PARENT_ID VARCHAR(255), PRIMARY KEY (UM_ID), UNIQUE(UM_ORG_NAME, UM_TENANT_ID), FOREIGN KEY (UM_PARENT_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE );
       ```

      |  UM_ID|UM_ORG_NAME|UM_ORG_DESCRIPTION|UM_CREATED_TIME|UM_LAST_MODIFIED|UM_STATUS|UM_TENANT_ID|UM_PARENT_ID
      |---------|--------------------|------------------------------|-------------------------|-----------|--------------|--------------|-----

       ```
       CREATE TABLE IF NOT EXISTS UM_ORG_ATTRIBUTE ( 
       UM_ID INTEGER NOT NULL AUTO_INCREMENT, 
       UM_ORG_ID VARCHAR(255) NOT NULL, 
       UM_ATTRIBUTE_KEY VARCHAR(255) NOT NULL, 
       UM_ATTRIBUTE_VALUE VARCHAR(512), PRIMARY KEY (UM_ID), 
       UNIQUE (UM_ORG_ID, UM_ATTRIBUTE_KEY), 
       FOREIGN KEY (UM_ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE );
       ```
       |  UM_ID|UM_ORG_ID|UM_ATTRIBUTE_KEY|UM_ATTRIBUTE_VALUE
       |---------|--------------------|------------------------------|-------|
      ```
      CREATE TABLE UM_USER_ROLE_ORG (
      UM_ID VARCHAR2(255) NOT NULL,
      UM_USER_ID VARCHAR2(255) NOT NULL,
      UM_ROLE_ID VARCHAR2(1024) NOT NULL,
      UM_HYBRID_ROLE_ID INTEGER NOT NULL,
      UM_TENANT_ID INTEGER DEFAULT 0,
      ORG_ID VARCHAR2(255) NOT NULL,
      ASSIGNED_AT VARCHAR2(255) NOT NULL,
      MANDATORY INTEGER DEFAULT 0,
      PRIMARY KEY (UM_ID),
      CONSTRAINT FK_UM_USER_ROLE_ORG_UM_HYBRID_ROLE FOREIGN KEY (UM_HYBRID_ROLE_ID, UM_TENANT_ID) REFERENCES UM_HYBRID_ROLE(UM_ID, UM_TENANT_ID) ON DELETE CASCADE,
      CONSTRAINT FK_UM_USER_ROLE_ORG_UM_ORG FOREIGN KEY (ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE,
      CONSTRAINT FK_UM_USER_ROLE_ORG_ASSIGNED_AT FOREIGN KEY (ASSIGNED_AT) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE);
       ```

      | UM_ID|UM_USER_ID|UM_ROLE_ID|UM_HYBRID_ROLE_ID|UM_TENANT_ID|ORG_ID|ASSIGNED_AT|MANDATORY
      |--------|---------|---------------|--------------------|---------------|-------------|---------|-----------|

## Build

This is a multi-module project containing two modules,
- `org.wso2.carbon.identity.organization.management.service`
- `org.wso2.carbon.identity.organization.management.endpoint`

Type,
`mvn clean install` to generate the `jar` file in `core` module and `war` file in `endpoint` module.
Alternatively can use `mvn clean install -DskipTests` or `mvn clean install Dmaven.skip.test=true` to skip tests.

- Copy the `api#identitiy#organization-mgt#v1.0.war` file to `{IS-HOME}/repository/deployment/server/webapps`
- Copy the `org.wso2.carbon.identity.organization.management.service-<version>.jar` file to `{IS-HOME}/repository/components/dropins`

## Check the OSGI Service is working

Run **WSO2 IS** using the following command to check the **OSGI** service.
`sh wso2server.sh -dosgiConsole`
After deploying the **WSO2 IS** check the service is working or not by,
`ss <jar filename>` and selecting the `jar` file.

## Debugging

To debug, open the **WSO2 IS** in remote debugging.
`sh wso2server.sh -debug <port-name>`
To disable checkstyle and findbugs plugins, comment from line 319-326.
```
<plugin>
   <groupId>com.github.spotbugs</groupId>
   <artifactId>spotbugs-maven-plugin</artifactId>
</plugin>
<plugin>
   <groupId>org.apache.maven.plugins</groupId>
   <artifactId>maven-checkstyle-plugin</artifactId>
</plugin>
```

## Features
### Add organization
**API**  
`https://localhost:9443/t/{tenant}/api/identity/organization-mgt/v1.0/organizations`
  
**Sample Request Body**
```
{
  "name": "org07",
  "description": "building site",
  "parentId": "orgid01",
  "attributes": [
    {
      "key": "Country",
      "value": "France"
    },
    {
      "key": "Language",
      "value": ""
    },
    {
      "key": "Color",
      "value": "Blue"
    }
  ]
}
```
- Here, an Organization is added to the database. And if there are mandatory organization-user-role mappings coming from the parent organization, those mandatory organization-user-role mappings are added too.

### Get organization
**API**  
`https://localhost:9443/t/{tenant}/api/identity/organization-mgt/v1.0/organizations/{org-id}`

**Query Parameters**  
`showChildren`

- Here, the organization details are taken from the database using organization id.

### List organizations
**API**  
`https://localhost:9443/t/{tenant}/api/identity/organization-mgt/v1.0/organizations`

- This gives the list of organizations created by the tenant.

### Delete organization.
**API**  
`https://localhost:9443/t/{tenant-id}/api/identity/organization-mgt/v1.0/organizations/{org-id}`

**Query Parameters**  
`force`

- This enables the deletion of an organization.
- If the organization is in **ACTIVE** status it will give an error. To bypass such errors the query parameter `force` can be used.

### Put organization.
**API**  
`https://localhost:9443/t/{tenant}/api/identity/organization-mgt/v1.0/organizations/{org-id}`

**Sample Request Body**  
```
{
  "name": "Test Org 1 Put update",
  "description": "Building constructions update",
  "attributes": [
    {
      "key": "Language",
      "value": "Sinhala"
    }
  ]
}
```
- Updates the whole organization.

### Patch organization.
`https://localhost:9443/t/{tenant}/api/identity/organization-mgt/v1.0/organizations/{org-id}`

**Sample Request Body**
```
[
    {
        "operation": "ADD",
        "path": "/description",
        "value": "dew"
    },
    {
        "operation": "ADD",
        "path": "/attributes/Country", 
        "value": "patchcountry"
    },
    {
        "operation": "ADD",
        "path": "/attributes/test",
        "value": "patchtest"
    }
]
```
- Can add, remove or replace the values residing inside an organization.
