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

package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

/**
 * CodeRepoFromAnalyzeLog entity
 *
 * @version V1.0
 * @date 2021/1/25
 */
@Data
public class CodeRepoFromAnalyzeLogModel {
    /**
     * 项目主键id
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 代码仓库
     */
    @JsonProperty("code_repo_list")
    private Set<CodeRepo> codeRepoList;

    @JsonProperty("url")
    private Set<String> urls;

    @Data
    public static class CodeRepo {
        private String buildId;

        private String url;

        private String branch;

        private String version;

        private Long createDate;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CodeRepo) {
                CodeRepo codeRepo = (CodeRepo) obj;
                return (url.equals(codeRepo.url) && branch.equals(codeRepo.branch));
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }
    }
}
