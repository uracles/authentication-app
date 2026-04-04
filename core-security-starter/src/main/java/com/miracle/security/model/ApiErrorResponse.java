package com.miracle.security.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Canonical error response returned by all exception handlers in the starter.
 *
 * Design decision: A single, stable error envelope across all consuming applications
 * means API clients only need one error-parsing path. The {@code details} field is
 * optional (null-excluded from JSON) to keep simple errors lean.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"timestamp", "status", "error", "message", "path", "details"})
public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<String> details;

    private ApiErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status    = builder.status;
        this.error     = builder.error;
        this.message   = builder.message;
        this.path      = builder.path;
        this.details   = builder.details;
    }

    public static Builder builder() { return new Builder(); }

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return builder()
                .status(status).error(error).message(message).path(path)
                .build();
    }

//    public Instant getTimestamp() { return timestamp; }
//    public int getStatus()        { return status; }
//    public String getError()      { return error; }
//    public String getMessage()    { return message; }
//    public String getPath()       { return path; }
//    public List<String> getDetails() { return details; }

    public static final class Builder {
        private Instant timestamp = Instant.now();
        private int status;
        private String error;
        private String message;
        private String path;
        private List<String> details;

        public Builder timestamp(Instant t)    { this.timestamp = t; return this; }
        public Builder status(int s)           { this.status = s;    return this; }
        public Builder error(String e)         { this.error = e;     return this; }
        public Builder message(String m)       { this.message = m;   return this; }
        public Builder path(String p)          { this.path = p;      return this; }
        public Builder details(List<String> d) { this.details = d;   return this; }
        public ApiErrorResponse build()        { return new ApiErrorResponse(this); }
    }
}