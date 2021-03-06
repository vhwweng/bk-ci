/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.defect.dao;

import com.tencent.bk.codecc.apiquery.defect.model.LintFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * lint类告警查询实体类
 * 
 * @date 2020/4/16
 * @version V1.0
 */
@Repository
public interface LintDefectRepository extends MongoRepository<LintFileEntity, String>
{

    /**
     * 通过任务id和工具名查找
     * @param taskId
     * @param toolName
     * @return
     */
    @Query(fields = "{'url' : 0}", value = "{'task_id' : {$in : ?0}}")
    Page<LintFileEntity> findByTaskIdInAndToolNameOrderByTaskIdAscToolNameAsc(List<Long> taskId, String toolName, Pageable pageable);
}
