/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.docker.controller

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST_REFERENCE_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST_TAG_SUFFIX
import com.tencent.bkrepo.docker.constant.MANIFEST_PATTERN
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.service.DockerV2LocalRepoService
import com.tencent.bkrepo.docker.util.PathUtil
import com.tencent.bkrepo.docker.util.UserUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

/**
 *  ManifestImpl validates and impl the manifest interface
 */
@RestController
class ManifestController @Autowired constructor(val dockerRepo: DockerV2LocalRepoService) {

    @PutMapping(DOCKER_MANIFEST_TAG_SUFFIX)
    fun putManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        tag: String,
        contentType: String,
        artifactFile: ArtifactFile
    ): ResponseEntity<Any> {
        val uId = UserUtil.getContextUserId(userId)
        val name = PathUtil.artifactName(request, MANIFEST_PATTERN, projectId, repoName)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.uploadManifest(pathContext, tag, contentType, artifactFile)
    }

    @GetMapping(DOCKER_MANIFEST_REFERENCE_SUFFIX)
    fun getManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        reference: String
    ): ResponseEntity<Any> {
        val name = PathUtil.artifactName(request, MANIFEST_PATTERN, projectId, repoName)
        val uId = UserUtil.getContextUserId(userId)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.getManifest(pathContext, reference)
    }

    @RequestMapping(method = [RequestMethod.HEAD], value = [DOCKER_MANIFEST_REFERENCE_SUFFIX])
    fun existManifest(
        request: HttpServletRequest,
        userId: String?,
        projectId: String,
        repoName: String,
        reference: String
    ): ResponseEntity<Any> {
        val name = PathUtil.artifactName(request, MANIFEST_PATTERN, projectId, repoName)
        val uId = UserUtil.getContextUserId(userId)
        val pathContext = RequestContext(uId, projectId, repoName, name)
        return dockerRepo.getManifest(pathContext, reference)
    }
}
