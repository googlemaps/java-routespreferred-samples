// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// [START maps_routespreferred_samples_default]
package com.example;

import com.google.maps.routes.v1.*;
import com.google.type.LatLng;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoutesPreferredClient {
    // For more detail on inserting API keys, see:
    // https://cloud.google.com/endpoints/docs/grpc/restricting-api-access-with-api-keys#java
    // For more detail on system parameters (such as FieldMask), see:
    // https://cloud.google.com/apis/docs/system-parameters
    private static final class RoutesPreferredInterceptor implements ClientInterceptor {
        private final String apiKey;
        private static final Logger logger = Logger.getLogger(RoutesPreferredInterceptor.class.getName());
        private static Metadata.Key<String> API_KEY_HEADER = Metadata.Key.of("x-goog-api-key",
                Metadata.ASCII_STRING_MARSHALLER);
        private static Metadata.Key<String> FIELD_MASK_HEADER = Metadata.Key.of("x-goog-fieldmask",
                Metadata.ASCII_STRING_MARSHALLER);

        public RoutesPreferredInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            logger.info("Intercepted " + method.getFullMethodName());
            ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
            call = new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    headers.put(API_KEY_HEADER, apiKey);
                    // Note that setting the field mask to * is OK for testing, but discouraged in
                    // production.
                    // For example, for ComputeRoutes, set the field mask to
                    // "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
                    // in order to get the route distances, durations, and encoded polylines.
                    headers.put(FIELD_MASK_HEADER, "*");
                    super.start(responseListener, headers);
                }
            };
            return call;
        }
    }

    private static final Logger logger = Logger.getLogger(RoutesPreferredClient.class.getName());
    private final RoutesPreferredGrpc.RoutesPreferredBlockingStub blockingStub;

    public RoutesPreferredClient(Channel channel) {
        blockingStub = RoutesPreferredGrpc.newBlockingStub(channel);
    }

    public static Waypoint createWaypointForLatLng(double lat, double lng) {
        return Waypoint.newBuilder()
                .setLocation(Location.newBuilder().setLatLng(LatLng.newBuilder().setLatitude(lat).setLongitude(lng)))
                .build();
    }

    public void computeRoutes() {
        ComputeRoutesRequest request = ComputeRoutesRequest.newBuilder()
                .setOrigin(createWaypointForLatLng(37.420761, -122.081356))
                .setDestination(createWaypointForLatLng(37.420999, -122.086894)).setTravelMode(RouteTravelMode.DRIVE)
                .setRoutingPreference(RoutingPreference.TRAFFIC_AWARE).setComputeAlternativeRoutes(true)
                .setUnits(Units.METRIC).setLanguageCode("en-us")
                .setRouteModifiers(
                        RouteModifiers.newBuilder().setAvoidTolls(false).setAvoidHighways(true).setAvoidFerries(true))
                .setPolylineQuality(PolylineQuality.OVERVIEW).build();
        ComputeRoutesResponse response;
        try {
            logger.info("About to send request: " + request.toString());
            response = blockingStub.withDeadlineAfter(2000, TimeUnit.MILLISECONDS).computeRoutes(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Response: " + response.toString());
    }

    public void computeRouteMatrix() {
        ComputeRouteMatrixRequest request = ComputeRouteMatrixRequest.newBuilder()
                .addOrigins(RouteMatrixOrigin.newBuilder().setWaypoint(createWaypointForLatLng(37.420761, -122.081356))
                        .setRouteModifiers(RouteModifiers.newBuilder().setAvoidTolls(false).setAvoidHighways(true)
                                .setAvoidFerries(true)))
                .addOrigins(RouteMatrixOrigin.newBuilder().setWaypoint(createWaypointForLatLng(37.403184, -122.097371)))
                .addDestinations(RouteMatrixDestination.newBuilder()
                        .setWaypoint(createWaypointForLatLng(37.420999, -122.086894)))
                .addDestinations(RouteMatrixDestination.newBuilder()
                        .setWaypoint(createWaypointForLatLng(37.383047, -122.044651)))
                .setTravelMode(RouteTravelMode.DRIVE).setRoutingPreference(RoutingPreference.TRAFFIC_AWARE).build();
        Iterator<RouteMatrixElement> elements;
        try {
            logger.info("About to send request: " + request.toString());
            elements = blockingStub.withDeadlineAfter(2000, TimeUnit.MILLISECONDS).computeRouteMatrix(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        while (elements.hasNext()) {
            logger.info("Element response: " + elements.next().toString());
        }
    }

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("GOOGLE_MAPS_API_KEY");

        // The standard TLS port is 443
        Channel channel = NettyChannelBuilder.forAddress("routespreferred.googleapis.com", 443).build();
        channel = ClientInterceptors.intercept(channel, new RoutesPreferredInterceptor(apiKey));

        RoutesPreferredClient client = new RoutesPreferredClient(channel);
        client.computeRoutes();
        client.computeRouteMatrix();
    }
}
// [END maps_routespreferred_samples_default]
