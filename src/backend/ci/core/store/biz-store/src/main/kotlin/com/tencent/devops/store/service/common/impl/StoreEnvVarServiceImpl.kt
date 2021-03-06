/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.StoreEnvVarDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_ENCRYPT_FLAG
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_SCOPE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.KEY_VAR_DESC
import com.tencent.devops.store.pojo.common.KEY_VAR_NAME
import com.tencent.devops.store.pojo.common.KEY_VAR_VALUE
import com.tencent.devops.store.pojo.common.KEY_VERSION
import com.tencent.devops.store.pojo.common.StoreEnvChangeLogInfo
import com.tencent.devops.store.pojo.common.StoreEnvVarInfo
import com.tencent.devops.store.pojo.common.StoreEnvVarRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreEnvVarService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("ALL")
@Service
class StoreEnvVarServiceImpl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storeEnvVarDao: StoreEnvVarDao
) : StoreEnvVarService {

    private val logger = LoggerFactory.getLogger(StoreEnvVarServiceImpl::class.java)

    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${aes.aesMock}")
    private lateinit var aesMock: String

    override fun create(userId: String, storeEnvVarRequest: StoreEnvVarRequest): Result<Boolean> {
        logger.info("storeEnvVar create userId:$userId,storeEnvVarRequest:$storeEnvVarRequest")
        val storeCode = storeEnvVarRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(storeEnvVarRequest.storeType).type.toByte()
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType)) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val lockKey = "$storeCode:$storeType:${storeEnvVarRequest.varName}"
        val lock = RedisLock(redisOperation, lockKey, 60)
        try {
            if (lock.tryLock()) {
                // 查询该环境变量在数据库中最大的版本
                val maxVersion = storeEnvVarDao.getEnvVarMaxVersion(
                    dslContext = dslContext,
                    storeType = storeType,
                    storeCode = storeCode,
                    varName = storeEnvVarRequest.varName
                ) ?: 0
                storeEnvVarDao.create(
                    dslContext = dslContext,
                    userId = userId,
                    version = maxVersion + 1,
                    storeEnvVarRequest = storeEnvVarRequest
                )
            }
        } catch (t: Throwable) {
            logger.error("storeEnvVar create failed", t)
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        } finally {
            lock.unlock()
        }
        return Result(true)
    }

    override fun delete(
        userId: String,
        storeType: String,
        storeCode: String,
        varNames: String
    ): Result<Boolean> {
        logger.info("storeEnvVar delete userId:$userId,storeType:$storeType,storeCode:$storeCode,varNames:$varNames")
        val storeTypeObj = StoreTypeEnum.valueOf(storeType)
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeObj.type.toByte()
            )
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        storeEnvVarDao.batchDelete(
            dslContext = dslContext,
            storeType = storeTypeObj.type.toByte(),
            storeCode = storeCode,
            varNameList = varNames.split(",")
        )
        return Result(true)
    }

    override fun getLatestEnvVarList(
        userId: String,
        storeType: String,
        storeCode: String,
        scopes: String?,
        varName: String?,
        isDecrypt: Boolean,
        checkPermissionFlag: Boolean
    ): Result<List<StoreEnvVarInfo>?> {
        logger.info("storeEnvVar getLatestEnvVarList userId:$userId,storeType:$storeType,storeCode:$storeCode")
        val storeTypeObj = StoreTypeEnum.valueOf(storeType)
        if (checkPermissionFlag && !storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeTypeObj.type.toByte()
            )
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val latestEnvVarRecords = storeEnvVarDao.getLatestEnvVarList(
            dslContext = dslContext,
            storeType = storeTypeObj.type.toByte(),
            storeCode = storeCode,
            scopeList = scopes?.split(","),
            varName = varName
        )
        return if (latestEnvVarRecords != null) {
            val storeEnvVarList = mutableListOf<StoreEnvVarInfo>()
            latestEnvVarRecords.forEach {
                val encryptFlag = it[KEY_ENCRYPT_FLAG] as Boolean
                val varValue = if (encryptFlag) {
                    if (isDecrypt) AESUtil.decrypt(aesKey, it[KEY_VAR_VALUE] as String) else aesMock
                } else {
                    it[KEY_VAR_VALUE] as String
                }
                storeEnvVarList.add(
                    StoreEnvVarInfo(
                        id = it[KEY_ID] as String,
                        storeCode = it[KEY_STORE_CODE] as String,
                        storeType = StoreTypeEnum.getStoreType((it[KEY_STORE_TYPE] as Byte).toInt()),
                        varName = it[KEY_VAR_NAME] as String,
                        varValue = varValue,
                        varDesc = it[KEY_VAR_DESC] as? String,
                        encryptFlag = encryptFlag,
                        scope = it[KEY_SCOPE] as String,
                        version = it[KEY_VERSION] as Int,
                        creator = it[KEY_CREATOR] as String,
                        modifier = it[KEY_MODIFIER] as String,
                        createTime = DateTimeUtil.toDateTime(it[KEY_CREATE_TIME] as LocalDateTime),
                        updateTime = DateTimeUtil.toDateTime(it[KEY_UPDATE_TIME] as LocalDateTime)
                    )
                )
            }
            Result(data = storeEnvVarList)
        } else {
            Result(data = null)
        }
    }

    override fun getEnvVarChangeLogList(
        userId: String,
        storeType: String,
        storeCode: String,
        varName: String
    ): Result<List<StoreEnvChangeLogInfo>?> {
        logger.info("storeEnvVar getEnvVarChangeLogList userId:$userId,storeType:$storeType,storeCode:$storeCode")
        logger.info("storeEnvVar getEnvVarChangeLogList varName:$varName")
        val storeTypeObj = StoreTypeEnum.valueOf(storeType)
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeTypeObj.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val storeEnvVarRecords = storeEnvVarDao.getEnvVarList(
            dslContext = dslContext,
            storeType = storeTypeObj.type.toByte(),
            storeCode = storeCode,
            varName = varName
        )
        return if (storeEnvVarRecords != null && storeEnvVarRecords.size > 1) {
            val storeEnvChangeLogList = mutableListOf<StoreEnvChangeLogInfo>()
            for (i in storeEnvVarRecords.indices) {
                if (i < storeEnvVarRecords.size - 1) {
                    val nowStoreEnvRecord = storeEnvVarRecords[i]
                    val pastStoreEnvRecord = storeEnvVarRecords[i + 1]
                    storeEnvChangeLogList.add(
                        StoreEnvChangeLogInfo(
                            varName = varName,
                            beforeVarValue = if (pastStoreEnvRecord.encryptFlag) {
                                aesMock
                            } else pastStoreEnvRecord.varValue,
                            afterVarValue = if (nowStoreEnvRecord.encryptFlag) {
                                aesMock
                            } else nowStoreEnvRecord.varValue,
                            modifier = nowStoreEnvRecord.modifier,
                            updateTime = DateTimeUtil.toDateTime(nowStoreEnvRecord.updateTime)
                        )
                    )
                }
            }
            Result(data = storeEnvChangeLogList)
        } else {
            Result(data = null)
        }
    }
}
