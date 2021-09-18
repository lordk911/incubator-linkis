/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.linkis.cs.server;

import com.webank.wedatasphere.linkis.cs.common.entity.enumeration.ExpireType;
import com.webank.wedatasphere.linkis.cs.common.exception.CSErrorException;
import com.webank.wedatasphere.linkis.cs.persistence.entity.PersistenceContextID;
import com.webank.wedatasphere.linkis.cs.server.enumeration.ServiceMethod;
import com.webank.wedatasphere.linkis.cs.server.enumeration.ServiceType;
import com.webank.wedatasphere.linkis.cs.server.scheduler.CsScheduler;
import com.webank.wedatasphere.linkis.cs.server.scheduler.HttpAnswerJob;
import com.webank.wedatasphere.linkis.server.Message;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;


@RestController
@RequestMapping(path = "/contextservice")
public class ContextIDRestfulApi implements CsRestfulParent {

    @Autowired
    private CsScheduler csScheduler;

    @RequestMapping(path = "createContextID",method = RequestMethod.POST)
     public Message createContextID(HttpServletRequest req, JsonNode json) throws InterruptedException {
        //contextID是client传过来的序列化的id
        PersistenceContextID contextID = new PersistenceContextID();
        contextID.setUser("neiljianliu");
        contextID.setExpireType(ExpireType.TODAY);
        contextID.setExpireTime(new Date());
        contextID.setInstance("cs-server1");
        contextID.setBackupInstance("cs-server2,cs-server3");
        contextID.setApplication("spark");
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.CREATE, contextID);
        return generateResponse(answerJob, "contextId");
    }

     @RequestMapping(path = "getContextID",method = RequestMethod.GET)
     public Message getContextID( HttpServletRequest req, @RequestParam(value="contextId",required=false) String id) throws InterruptedException, CSErrorException {
        if (StringUtils.isEmpty(id)) {
            throw new CSErrorException(97000, "contxtId cannot be empty");
        }
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.GET, id);
        return generateResponse(answerJob, "");
    }

    @RequestMapping(path = "updateContextID",method = RequestMethod.POST)
     public Message updateContextID( HttpServletRequest req, JsonNode json) throws InterruptedException, CSErrorException {
        PersistenceContextID contextID = new PersistenceContextID();
        contextID.setUser("johnnwang");
        contextID.setExpireType(ExpireType.NEVER);
        contextID.setExpireTime(new Date());
        contextID.setInstance("cs-server2");
        contextID.setBackupInstance("cs-server1,cs-server3");
        contextID.setApplication("hive");
        // TODO: 2020/2/25 这里要填响应的contextId
        contextID.setContextId("84714");
        if (StringUtils.isEmpty(contextID.getContextId())) {
            throw new CSErrorException(97000, "contxtId cannot be empty");
        }
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.UPDATE, contextID);
        return generateResponse(answerJob, "");
    }

    @RequestMapping(path = "resetContextID",method = RequestMethod.POST)
     public Message resetContextID( HttpServletRequest req, JsonNode json) throws InterruptedException, CSErrorException {
        String id = null;
        if (StringUtils.isEmpty(id)) {
            throw new CSErrorException(97000, "contxtId cannot be empty");
        }
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.RESET, id);
        return generateResponse(answerJob, "");
    }


    @RequestMapping(path = "removeContextID",method = RequestMethod.POST)
     public Message removeContextID( HttpServletRequest req, JsonNode json) throws InterruptedException, CSErrorException {
        String id = json.get("contextId").getTextValue();
        if (StringUtils.isEmpty(id)) {
            throw new CSErrorException(97000, "contxtId cannot be empty");
        }
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.REMOVE, id);
        return generateResponse(answerJob, "");
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.CONTEXT_ID;
    }

    @Override
    public CsScheduler getScheduler() {
        return this.csScheduler;
    }

}
