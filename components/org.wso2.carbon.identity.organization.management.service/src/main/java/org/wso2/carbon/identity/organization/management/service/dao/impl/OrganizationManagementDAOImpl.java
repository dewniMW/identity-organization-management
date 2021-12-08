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

package org.wso2.carbon.identity.organization.management.service.dao.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Operation;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_ATTRIBUTE_KEY_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_ORGANIZATION_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION_ADD_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION_DELETE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION_UPDATE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_CHILD_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_ID_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_ATTR_KEY_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_ATTR_VALUE_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_CREATED_TIME_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_DESCRIPTION_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_LAST_MODIFIED_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_PARENT_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_CHILD_ORGANIZATIONS_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_ORGANIZATION_ATTRIBUTE_KEY_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_ORGANIZATION_EXIST_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_ORGANIZATION_EXIST_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.COUNT_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.DELETE_ORGANIZATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.DELETE_ORGANIZATION_ATTRIBUTES_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.DELETE_ORGANIZATION_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_CHILD_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATIONS_BY_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATION_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATION_ID_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.INSERT_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.INSERT_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.PATCH_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.PATCH_ORGANIZATION_CONCLUDE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_CREATED_TIME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_KEY;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.UPDATE_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.UPDATE_ORGANIZATION_ATTRIBUTE_VALUE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.UPDATE_ORGANIZATION_LAST_MODIFIED;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Organization management dao implementation.
 */
public class OrganizationManagementDAOImpl implements OrganizationManagementDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementDAOImpl.class);
    private static final Calendar CALENDAR = Calendar.getInstance(TimeZone.getTimeZone(UTC));

    @Override
    public void addOrganization(int tenantId, String tenantDomain, Organization organization) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                namedJdbcTemplate.executeInsert(INSERT_ORGANIZATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organization.getId());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_NAME, organization.getName());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_DESCRIPTION, organization.getDescription());
                    namedPreparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_CREATED_TIME,
                            Timestamp.from(organization.getCreated()), CALENDAR);
                    namedPreparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED,
                            Timestamp.from(organization.getLastModified()), CALENDAR);
                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, organization.getParent().getId());
                }, organization, false);
                if (CollectionUtils.isNotEmpty(organization.getAttributes())) {
                    insertOrganizationAttributes(organization);
                }
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_ORGANIZATION, e, tenantDomain);
        }
    }

    private void insertOrganizationAttributes(Organization organization) throws TransactionException {

        String organizationId = organization.getId();
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
            namedJdbcTemplate.withTransaction(template -> {
                template.executeBatchInsert(INSERT_ATTRIBUTE, (namedPreparedStatement -> {
                    for (OrganizationAttribute attribute : organization.getAttributes()) {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, attribute.getKey());
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, attribute.getValue());
                        namedPreparedStatement.addBatch();
                    }
                }), organizationId);
                return null;
            });
    }

    @Override
    public boolean isOrganizationExistByName(int tenantId, String organizationName, String tenantDomain) throws
            OrganizationManagementServerException {

        return isOrganizationExist(tenantId, organizationName, tenantDomain, CHECK_ORGANIZATION_EXIST_BY_NAME,
                DB_SCHEMA_COLUMN_NAME_NAME, ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_NAME);
    }

    @Override
    public boolean isOrganizationExistById(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        return isOrganizationExist(tenantId, organizationId, tenantDomain, CHECK_ORGANIZATION_EXIST_BY_ID,
                DB_SCHEMA_COLUMN_NAME_ID, ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_ID);
    }

    private boolean isOrganizationExist(int tenantId, String organization, String tenantDomain,
                                        String checkOrganizationExistQuery, String dbSchemaColumnNameId,
                                        OrganizationManagementConstants.ErrorMessages errorMessage)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            int orgCount = namedJdbcTemplate.fetchSingleRecord(checkOrganizationExistQuery,
                    (resultSet, rowNumber) -> resultSet.getInt(COUNT_COLUMN), namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(dbSchemaColumnNameId, organization);
                    });
            return orgCount > 0;
        } catch (DataAccessException e) {
            throw handleServerException(errorMessage, e, organization, tenantDomain);
        }
    }

    @Override
    public String getOrganizationIdByName(int tenantId, String organizationName, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_ID_BY_NAME,
                    (resultSet, rowNumber) -> resultSet.getString(VIEW_ID_COLUMN), namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_NAME, organizationName);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_ID_BY_NAME, e, organizationName,
                    tenantDomain);
        }
    }

    @Override
    public Organization getOrganization(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<OrganizationRowDataCollector> organizationRowDataCollectors;
        try {
            organizationRowDataCollectors = namedJdbcTemplate
                    .executeQuery(GET_ORGANIZATION_BY_ID,
                            (resultSet, rowNumber) -> {
                                OrganizationRowDataCollector collector = new OrganizationRowDataCollector();
                                collector.setId(organizationId);
                                collector.setName(resultSet.getString(VIEW_NAME_COLUMN));
                                collector.setDescription(resultSet.getString(VIEW_DESCRIPTION_COLUMN));
                                collector.setParentId(resultSet.getString(VIEW_PARENT_ID_COLUMN));
                                collector.setLastModified(resultSet.getTimestamp(VIEW_LAST_MODIFIED_COLUMN, CALENDAR)
                                        .toInstant());
                                collector.setCreated(resultSet.getTimestamp(VIEW_CREATED_TIME_COLUMN, CALENDAR)
                                        .toInstant());
                                collector.setAttributeKey(resultSet.getString(VIEW_ATTR_KEY_COLUMN));
                                collector.setAttributeValue(resultSet.getString(VIEW_ATTR_VALUE_COLUMN));
                                return collector;
                            },
                            namedPreparedStatement -> {
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                            }
                    );
            return (organizationRowDataCollectors == null || organizationRowDataCollectors.size() == 0) ?
                    null : buildOrganizationFromRawData(organizationRowDataCollectors);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_BY_ID, e, organizationId,
                    tenantDomain);
        }
    }

    @Override
    public List<String> getOrganizationIds(int tenantId, String tenantDomain) throws
            OrganizationManagementServerException {

        List<String> organizationIds = new ArrayList<>();
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.executeQuery(GET_ORGANIZATIONS_BY_TENANT_ID,
                    (resultSet, rowNumber) -> organizationIds.add(resultSet.getString(1)),
                    namedPreparedStatement -> namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS, e, tenantDomain);
        }
        return organizationIds;
    }

    @Override
    public void deleteOrganization(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            // Delete organization from UM_ORG table and cascade the deletion to the other table.
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_BY_ID, namedPreparedStatement -> {
                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    @Override
    public boolean hasChildOrganizations(String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            List<Integer> childOrganizationIds = namedJdbcTemplate.executeQuery(CHECK_CHILD_ORGANIZATIONS_EXIST,
                    (resultSet, rowNumber) -> resultSet.getInt(COUNT_COLUMN), namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, organizationId);
                    });
            return CollectionUtils.isNotEmpty(childOrganizationIds);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_CHILD_ORGANIZATIONS, e, organizationId,
                    tenantDomain);
        }
    }

    @Override
    public void patchOrganization(String organizationId, String tenantDomain, Instant lastModifiedInstant,
                                  List<Operation> operations) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                for (Operation operation : operations) {
                    if (operation.getPath().startsWith(PATCH_PATH_ORG_ATTRIBUTES)) {
                        patchOrganizationAttribute(organizationId, operation, tenantDomain);
                    } else {
                        patchOrganizationField(organizationId, operation, tenantDomain);
                    }
                }
                updateLastModifiedTime(organizationId, tenantDomain, lastModifiedInstant);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    @Override
    public void updateOrganization(String organizationId, String tenantDomain, Organization organization) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ORGANIZATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_NAME, organization.getName());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_DESCRIPTION, organization.getDescription());
                    namedPreparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED,
                            Timestamp.from(organization.getLastModified()), CALENDAR);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                deleteOrganizationAttributes(organizationId, tenantDomain);
                if (CollectionUtils.isNotEmpty(organization.getAttributes())) {
                    insertOrganizationAttributes(organization);
                }
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    @Override
    public boolean isAttributeExistByKey(String tenantDomain, String organizationId, String attributeKey)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            int attrCount = namedJdbcTemplate.fetchSingleRecord(CHECK_ORGANIZATION_ATTRIBUTE_KEY_EXIST,
                    (resultSet, rowNumber) -> resultSet.getInt(COUNT_COLUMN), namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, attributeKey);
                    });
            return attrCount > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_ORGANIZATION_ATTRIBUTE_KEY_EXIST, e,
                    attributeKey, organizationId, tenantDomain);
        }
    }

    @Override
    public List<String> getChildOrganizationIds(int tenantId, String organizationId, String tenantDomain,
                                                Organization organization)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<String> childOrganizationIds = new ArrayList<>();
        try {
            namedJdbcTemplate.executeQuery(GET_CHILD_ORGANIZATIONS,
                    (resultSet, rowNumber) -> childOrganizationIds.add(resultSet.getString(1)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, organizationId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS, e, tenantDomain);
        }
        return childOrganizationIds;
    }

    private void deleteOrganizationAttributes(String organizationId, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_ORGANIZATION_ATTRIBUTES_BY_ID, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_ORGANIZATION_ATTRIBUTES, e, organizationId,
                    tenantDomain);
        }
    }

    private String buildQueryOrganization(String path) {

        // Updating a primary field
        StringBuilder sb = new StringBuilder();
        sb.append(PATCH_ORGANIZATION);
        if (path.equals(PATCH_PATH_ORG_NAME)) {
            sb.append(VIEW_NAME_COLUMN);
        } else if (path.equals(PATCH_PATH_ORG_DESCRIPTION)) {
            sb.append(VIEW_DESCRIPTION_COLUMN);
        }
        sb.append(PATCH_ORGANIZATION_CONCLUDE);
        String query = sb.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Organization patch query : " + query);
        }
        return query;
    }

    private void patchOrganizationField(String organizationId, Operation operation, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(buildQueryOrganization(operation.getPath()), namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE,
                            operation.getOp().equals(PATCH_OP_REMOVE) ? null : operation.getValue());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    private void patchOrganizationAttribute(String organizationId, Operation operation, String tenantDomain) throws
            OrganizationManagementServerException {

        String attributeKey = operation.getPath().replace(PATCH_PATH_ORG_ATTRIBUTES, "").trim();
        operation.setPath(attributeKey);
        if (operation.getOp().equals(PATCH_OP_ADD)) {
            insertOrganizationAttribute(organizationId, operation, tenantDomain);
        } else if (operation.getOp().equals(PATCH_OP_REPLACE)) {
            updateOrganizationAttribute(organizationId, operation, tenantDomain);
        } else {
            deleteOrganizationAttribute(organizationId, operation, tenantDomain);
        }
    }

    private void insertOrganizationAttribute(String organizationId, Operation operation, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(INSERT_ATTRIBUTE, (namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, operation.getPath());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, operation.getValue());
                }), null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION_ADD_ATTRIBUTE, e, tenantDomain);
        }
    }

    private void updateOrganizationAttribute(String organizationId, Operation operation, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ORGANIZATION_ATTRIBUTE_VALUE, preparedStatement -> {
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, operation.getValue());
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, operation.getPath());
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION_UPDATE_ATTRIBUTE, e, organizationId,
                    tenantDomain);
        }
    }

    private void deleteOrganizationAttribute(String organizationId, Operation operation, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_ATTRIBUTE, namedPreparedStatement -> {
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, operation.getPath());
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION_DELETE_ATTRIBUTE, e, operation.getPath(),
                    organizationId, tenantDomain);
        }
    }

    private void updateLastModifiedTime(String organizationId, String tenantDomain, Instant lastModifiedInstant) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ORGANIZATION_LAST_MODIFIED, preparedStatement -> {
                    preparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED,
                            Timestamp.from(lastModifiedInstant), CALENDAR);
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    private Organization buildOrganizationFromRawData(List<OrganizationRowDataCollector>
                                                              organizationRowDataCollectors) {

        Organization organization = new Organization();
        organizationRowDataCollectors.forEach(collector -> {
            if (organization.getId() == null) {
                organization.setId(collector.getId());
                organization.setName(collector.getName());
                organization.setDescription(collector.getDescription());
                organization.getParent().setId(collector.getParentId());
                organization.setCreated(collector.getCreated());
                organization.setLastModified(collector.getLastModified());
            }
            List<OrganizationAttribute> attributes = organization.getAttributes();
            List<String> attributeKeys = new ArrayList<>();
            for (OrganizationAttribute attribute : attributes) {
                attributeKeys.add(attribute.getKey());
            }
            if (collector.getAttributeKey() != null && !attributeKeys.contains(collector.getAttributeKey())) {
                organization.getAttributes().add(new OrganizationAttribute(collector.getAttributeKey(),
                        collector.getAttributeValue()));
            }
        });
        return organization;
    }
}
