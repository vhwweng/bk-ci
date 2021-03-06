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
 
package com.tencent.bk.codecc.apiquery.op.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * appcode工具权限
 * 
 * @date 2020/11/20
 * @version V1.0
 */
@Data
@Document(collection = "t_appcode_tool")
public class AppCodeToolEntity extends CommonEntity {
    @Field("app_code")
    @Indexed(unique = true, background = true)
    private String appCode;

    @Field("tool_name_list")
    private Set<String> toolNameList;
}
