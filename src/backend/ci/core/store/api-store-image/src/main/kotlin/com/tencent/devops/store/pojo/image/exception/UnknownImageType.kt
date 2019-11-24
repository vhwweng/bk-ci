package com.tencent.devops.store.pojo.image.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.ErrorCodeException

class UnknownImageType(
    override val message: String?,
    override val errorCode: String = PARAMETER_IS_INVALID,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode, message, params)