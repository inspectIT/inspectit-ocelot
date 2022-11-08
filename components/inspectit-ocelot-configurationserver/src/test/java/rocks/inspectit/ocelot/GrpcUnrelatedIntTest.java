package rocks.inspectit.ocelot;

import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
// Tests using this annotation do not use gRPC. The gRPC server apparently takes too long to shut down. This leads to
// an exception because the port 9657 is already in use when the config-server tried to start the gRPC server again for
// other integration tests. As a simple solution gRPC is disabled for all non-grpc-related integration tests.
@TestPropertySource(properties = {"grpc.server.port=-1"})
public @interface GrpcUnrelatedIntTest {
}
