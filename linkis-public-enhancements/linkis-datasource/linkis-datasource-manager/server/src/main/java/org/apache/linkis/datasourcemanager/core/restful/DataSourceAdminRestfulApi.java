/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.datasourcemanager.core.restful;

import org.apache.linkis.common.exception.ErrorException;
import org.apache.linkis.datasourcemanager.common.domain.DataSourceEnv;
import org.apache.linkis.datasourcemanager.common.domain.DataSourceParamKeyDefinition;
import org.apache.linkis.datasourcemanager.core.formdata.FormDataTransformerFactory;
import org.apache.linkis.datasourcemanager.core.formdata.MultiPartFormDataTransformer;
import org.apache.linkis.datasourcemanager.core.service.DataSourceInfoService;
import org.apache.linkis.datasourcemanager.core.service.DataSourceRelateService;
import org.apache.linkis.datasourcemanager.core.validate.ParameterValidator;
import org.apache.linkis.datasourcemanager.core.vo.DataSourceEnvVo;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.groups.Default;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(
        value = "/data-source-manager",
        produces = {"application/json"})
public class DataSourceAdminRestfulApi {

    @Autowired private DataSourceInfoService dataSourceInfoService;

    @Autowired private DataSourceRelateService dataSourceRelateService;

    @Autowired private ParameterValidator parameterValidator;

    @Autowired private Validator beanValidator;

    private MultiPartFormDataTransformer formDataTransformer;

    @PostConstruct
    public void initRestful() {
        this.formDataTransformer = FormDataTransformerFactory.buildCustom();
    }

    @RequestMapping(value = "/env/json", method = RequestMethod.POST)
    public Message insertJsonEnv(@RequestBody DataSourceEnv dataSourceEnv, HttpServletRequest req)
            throws ErrorException {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    String userName = SecurityFilter.getLoginUsername(req);
                    if (!RestfulApiHelper.isAdminUser(userName)) {
                        return Message.error("User '" + userName + "' is not admin user[非管理员用户]");
                    }
                    // Bean validation
                    Set<ConstraintViolation<DataSourceEnv>> result =
                            beanValidator.validate(dataSourceEnv, Default.class);
                    if (result.size() > 0) {
                        throw new ConstraintViolationException(result);
                    }
                    dataSourceEnv.setCreateUser(userName);
                    insertDataSourceEnv(dataSourceEnv);
                    return Message.ok().data("insertId", dataSourceEnv.getId());
                },
                "Fail to insert data source environment[新增数据源环境失败]");
    }

    @RequestMapping(value = "/env-list/all/type/{typeId}", method = RequestMethod.GET)
    public Message getAllEnvListByDataSourceType(@PathVariable("typeId") Long typeId) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    List<DataSourceEnv> envList =
                            dataSourceInfoService.listDataSourceEnvByType(typeId);
                    return Message.ok().data("envList", envList);
                },
                "Fail to get data source environment list[获取数据源环境清单失败]");
    }

    @RequestMapping(value = "/env/{envId}", method = RequestMethod.GET)
    public Message getEnvEntityById(@PathVariable("envId") Long envId) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSourceEnv dataSourceEnv = dataSourceInfoService.getDataSourceEnv(envId);
                    return Message.ok().data("env", dataSourceEnv);
                },
                "Fail to get data source environment[获取数据源环境信息失败]");
    }

    @RequestMapping(value = "/env/{envId}", method = RequestMethod.DELETE)
    public Message removeEnvEntity(@PathVariable("envId") Long envId, HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    String userName = SecurityFilter.getLoginUsername(request);
                    if (!RestfulApiHelper.isAdminUser(userName)) {
                        return Message.error("User '" + userName + "' is not admin user[非管理员用户]");
                    }
                    Long removeId = dataSourceInfoService.removeDataSourceEnv(envId);
                    if (removeId < 0) {
                        return Message.error(
                                "Fail to remove data source environment[删除数据源环境信息失败], [id:"
                                        + envId
                                        + "]");
                    }
                    return Message.ok().data("removeId", removeId);
                },
                "Fail to remove data source environment[删除数据源环境信息失败]");
    }

    @RequestMapping(value = "/env/{envId}/json", method = RequestMethod.PUT)
    public Message updateJsonEnv(
            @RequestBody DataSourceEnv dataSourceEnv,
            @PathVariable("envId") Long envId,
            HttpServletRequest request)
            throws ErrorException {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    String userName = SecurityFilter.getLoginUsername(request);
                    if (!RestfulApiHelper.isAdminUser(userName)) {
                        return Message.error("User '" + userName + "' is not admin user[非管理员用户]");
                    }
                    // Bean validation
                    Set<ConstraintViolation<DataSourceEnv>> result =
                            beanValidator.validate(dataSourceEnv, Default.class);
                    if (result.size() > 0) {
                        throw new ConstraintViolationException(result);
                    }
                    dataSourceEnv.setId(envId);
                    dataSourceEnv.setModifyUser(userName);
                    dataSourceEnv.setModifyTime(Calendar.getInstance().getTime());
                    DataSourceEnv storedDataSourceEnv =
                            dataSourceInfoService.getDataSourceEnv(envId);
                    if (null == storedDataSourceEnv) {
                        return Message.error(
                                "Fail to update data source environment[更新数据源环境失败], "
                                        + "[Please check the id:'"
                                        + envId
                                        + " is correct ']");
                    }
                    dataSourceEnv.setCreateUser(storedDataSourceEnv.getCreateUser());
                    updateDataSourceEnv(dataSourceEnv, storedDataSourceEnv);
                    return Message.ok().data("update_id", envId);
                },
                "Fail to update data source environment[更新数据源环境失败]");
    }

    @RequestMapping(value = "/env", method = RequestMethod.GET)
    public Message queryDataSourceEnv(
            @RequestParam(value = "name", required = false) String envName,
            @RequestParam(value = "typeId", required = false) Long dataSourceTypeId,
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSourceEnvVo dataSourceEnvVo =
                            new DataSourceEnvVo(envName, dataSourceTypeId);
                    dataSourceEnvVo.setCurrentPage(null != currentPage ? currentPage : 1);
                    dataSourceEnvVo.setPageSize(null != pageSize ? pageSize : 10);
                    List<DataSourceEnv> queryList =
                            dataSourceInfoService.queryDataSourceEnvPage(dataSourceEnvVo);
                    return Message.ok().data("queryList", queryList);
                },
                "Fail to query page of data source environment[查询数据源环境失败]");
    }

    /**
     * Inner method to insert data source environment
     *
     * @param dataSourceEnv data source environment entity
     * @throws ErrorException
     */
    private void insertDataSourceEnv(DataSourceEnv dataSourceEnv) throws ErrorException {
        // Get key definitions in environment scope
        List<DataSourceParamKeyDefinition> keyDefinitionList =
                dataSourceRelateService.getKeyDefinitionsByType(
                        dataSourceEnv.getDataSourceTypeId(),
                        DataSourceParamKeyDefinition.Scope.ENV);
        dataSourceEnv.setKeyDefinitions(keyDefinitionList);
        Map<String, Object> connectParams = dataSourceEnv.getConnectParams();
        // Validate connect parameters
        parameterValidator.validate(keyDefinitionList, connectParams);
        dataSourceInfoService.saveDataSourceEnv(dataSourceEnv);
    }

    /**
     * Inner method to update data source environment
     *
     * @param updatedOne new entity
     * @param storedOne old entity
     * @throws ErrorException
     */
    private void updateDataSourceEnv(DataSourceEnv updatedOne, DataSourceEnv storedOne)
            throws ErrorException {
        // Get key definitions in environment scope
        List<DataSourceParamKeyDefinition> keyDefinitionList =
                dataSourceRelateService.getKeyDefinitionsByType(
                        updatedOne.getDataSourceTypeId(), DataSourceParamKeyDefinition.Scope.ENV);
        updatedOne.setKeyDefinitions(keyDefinitionList);
        Map<String, Object> connectParams = updatedOne.getConnectParams();
        // Validate connect parameters
        parameterValidator.validate(keyDefinitionList, connectParams);
        dataSourceInfoService.updateDataSourceEnv(updatedOne, storedOne);
    }
}
