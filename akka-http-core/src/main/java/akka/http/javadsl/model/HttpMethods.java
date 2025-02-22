/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.http.javadsl.model;

import akka.http.impl.util.Util;
import akka.http.scaladsl.model.HttpMethods$;

import java.util.Optional;

/**
 * Contains static constants for predefined method types.
 */
public final class HttpMethods {
    private HttpMethods() {}

    public static final HttpMethod CONNECT = akka.http.scaladsl.model.HttpMethods.CONNECT();
    public static final HttpMethod DELETE  = akka.http.scaladsl.model.HttpMethods.DELETE();
    public static final HttpMethod GET     = akka.http.scaladsl.model.HttpMethods.GET();
    public static final HttpMethod HEAD    = akka.http.scaladsl.model.HttpMethods.HEAD();
    public static final HttpMethod OPTIONS = akka.http.scaladsl.model.HttpMethods.OPTIONS();
    public static final HttpMethod PATCH   = akka.http.scaladsl.model.HttpMethods.PATCH();
    public static final HttpMethod POST    = akka.http.scaladsl.model.HttpMethods.POST();
    public static final HttpMethod PUT     = akka.http.scaladsl.model.HttpMethods.PUT();
    public static final HttpMethod TRACE   = akka.http.scaladsl.model.HttpMethods.TRACE();

    /**
     * Create a custom method type.
     * @deprecated The created method will compute the presence of Content-Length headers based on deprecated logic (before issue #4213).
     */
    @Deprecated
    public static HttpMethod custom(String value, boolean safe, boolean idempotent, akka.http.javadsl.model.RequestEntityAcceptance requestEntityAcceptance) {
        //This cast is safe as implementation of RequestEntityAcceptance only exists in Scala
        akka.http.scaladsl.model.RequestEntityAcceptance scalaRequestEntityAcceptance
          = (akka.http.scaladsl.model.RequestEntityAcceptance) requestEntityAcceptance;
        return akka.http.scaladsl.model.HttpMethod.custom(value, safe, idempotent, scalaRequestEntityAcceptance);
    }

    /**
     * Create a custom method type.
     */
    public static HttpMethod custom(String value, boolean safe, boolean idempotent, akka.http.javadsl.model.RequestEntityAcceptance requestEntityAcceptance, boolean contentLengthAllowed) {
        //This cast is safe as implementation of RequestEntityAcceptance only exists in Scala
        akka.http.scaladsl.model.RequestEntityAcceptance scalaRequestEntityAcceptance
          = (akka.http.scaladsl.model.RequestEntityAcceptance) requestEntityAcceptance;
        return akka.http.scaladsl.model.HttpMethod.custom(value, safe, idempotent, scalaRequestEntityAcceptance, contentLengthAllowed);
    }

    /**
     * Looks up a predefined HTTP method with the given name.
     */
    public static Optional<HttpMethod> lookup(String name) {
        return Util.<HttpMethod, akka.http.scaladsl.model.HttpMethod>lookupInRegistry(HttpMethods$.MODULE$, name);
    }
}
