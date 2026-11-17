package app.myliberty.easy_records.util.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

open class LibertyGenericException(
    override val message: String,
    val payload: Any? = null,
    statusCode: HttpStatus = HttpStatus.BAD_REQUEST
) : ResponseStatusException(statusCode, message)
