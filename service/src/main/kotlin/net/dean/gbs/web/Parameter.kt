package net.dean.gbs.web

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.ws.rs.core.UriInfo

/**
 * Represents a parameter sent to the server
 */
public data class Parameter<T>(public val name: String,
                            public val value: T,
                            public val location: ParamLocation,
                            // Ignore uriInfo, will eventually try to serialize the ProjectResource and then act on the
                            // database and throw an exception if no session context is currently bound
                            public @JsonIgnore val uriInfo: UriInfo) {
    public val uri: String = uriInfo.absolutePath.path
}

/**
 * Where a parameter is located
 */
public enum class ParamLocation {
    /** An argument in the query string, such as /resource?name=foo */
    QUERY,
    /** A positional URI param, such as /resource/{name} */
    URI,
    /** An argument in the body of a request. Depends on the media type of the request. */
    BODY,
    /** An argument located in the request's headers, such as X-Foo: Bar */
    HEADER
}
