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

package org.wso2.carbon.identity.organization.management.endpoint.exceptions.mapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.endpoint.model.Error;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static org.wso2.carbon.identity.organization.management.endpoint.util.OrganizationManagementEndpointUtil.getError;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNEXPECTED;

/**
 * Handles all the unhandled server errors, (ex:NullPointer).
 * Sends a default error response.
 */
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Log LOG = LogFactory.getLog(DefaultExceptionMapper.class);

    @Override
    public Response toResponse(Throwable e) {

        LOG.error("Server encountered an error while serving the request.", e);
        Error errorDTO = getError(ERROR_CODE_UNEXPECTED.getCode(), ERROR_CODE_UNEXPECTED.getMessage(),
                ERROR_CODE_UNEXPECTED.getDescription());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorDTO)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }
}
