package com.akaitigo.podflow.auth

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped

/**
 * Test-only gRPC client interceptor that adds a valid Authorization header
 * to every outgoing gRPC call.
 *
 * This ensures existing gRPC integration tests pass without modification
 * after the auth interceptor is added to the server.
 */
@ApplicationScoped
@GlobalInterceptor
class TestAuthClientInterceptor : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val token = TestJwtHelper.generateTestToken()
                headers.put(
                    GrpcAuthInterceptor.AUTHORIZATION_KEY,
                    "Bearer $token",
                )
                super.start(responseListener, headers)
            }
        }
    }
}
