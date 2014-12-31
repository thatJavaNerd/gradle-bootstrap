package net.dean.gbs.web.resources

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType
import net.dean.gbs.web.Parameter
import javax.ws.rs.core.UriInfo

/**
 * Reasons why a request did not succeed
 */
public enum class ErrorCode {
    DOWNLOAD_NOT_READY
    INVALID_PARAM
    MALFORMED_UUID
    NOT_FOUND
    MISSING_PARAM
    NOT_ENUM_VALUE
}

/**
 * This class provides a model to be serialized into JSON and shown to the client
 */
public data class JsonError(public val error: ErrorCode,
                            public val why: String,
                            public val param: Parameter<*>,
                            public val path: String)

/**
 * This class provides an abstraction for forming the basic parts of an error message
 *
 * resourceClass: The resource class which is being requested
 * errorId: A constant shared between all errors of similar natures
 * why: Why this exception is being thrown and what can be done to fix it
 * status: The HTTP status to respond to the client with
 * param: The parameter in question
 */
public open class RequestException(code: ErrorCode,
                                   why: String,
                                   status: Int,
                                   public val param: Parameter<*>) : WebApplicationException(
        Response.status(status)
                .entity(JsonError(path = param.uri,
                        error = code,
                        why = why,
                        param = param))
                .type(MediaType.APPLICATION_JSON)
                .build()) {

    class object {
        public fun formatParam(p: Parameter<*>): String = "<${p.name}>"
    }
}

/**
 * This class provides an abstraction for classes whose parameters are invalid. The response HTTP status will always
 * be 422 Unprocessable Entity.
 */
public abstract class MalformedParameterException(errorId: ErrorCode,
                                                  why: String,
                                                  param: Parameter<*>) : RequestException (
        code = errorId,
        why = why,
        status = 422, // 422 Unprocessable Entity
        param = param
)

/**
 * This class is thrown when a parameter was checked to be missing, but was required.
 */
public class MissingRequiredParamException(param: Parameter<*>) : MalformedParameterException(
                errorId = ErrorCode.MISSING_PARAM,
                why = "Missing or empty value for ${RequestException.formatParam(param)}",
                param = param
        )

/**
 * This class is thrown when an parameter in the client's request was not in the format of what was expected by the server.
 */
public class InvalidParamException(why: String,
                                   errorId: ErrorCode,
                                   param: Parameter<*>) : MalformedParameterException(
                errorId = ErrorCode.INVALID_PARAM,
                why = why,
                param = param
        )

public class NotFoundException(why: String,
                               errorId: ErrorCode,
                               param: Parameter<*>) : RequestException(
                code = ErrorCode.NOT_FOUND,
                why = why,
                param = param,
                status = 404
)