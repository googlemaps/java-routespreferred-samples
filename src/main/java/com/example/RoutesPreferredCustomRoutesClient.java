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

// [START maps_routespreferred_samples_custom_routes_default]
package com.example;

import com.google.maps.routes.v1.ComputeCustomRoutesRequest;
import com.google.maps.routes.v1.ComputeCustomRoutesResponse;
import com.google.maps.routes.v1.Location;
import com.google.maps.routes.v1.PolylineQuality;
import com.google.maps.routes.v1.RouteModifiers;
import com.google.maps.routes.v1.RouteObjective;
import com.google.maps.routes.v1.RouteObjective.RateCard;
import com.google.maps.routes.v1.RouteObjective.RateCard.MonetaryCost;
import com.google.maps.routes.v1.RouteTravelMode;
import com.google.maps.routes.v1.RoutingPreference;
import com.google.maps.routes.v1.Units;
import com.google.maps.routes.v1.Waypoint;
import com.google.maps.routes.v1alpha.RoutesAlphaGrpc;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoutesPreferredCustomRoutesClient {
  // For more detail on inserting API keys, see:
  // https://cloud.google.com/endpoints/docs/grpc/restricting-api-access-with-api-keys#java
  // For more detail on system parameters (such as FieldMask), see:
  // https://cloud.google.com/apis/docs/system-parameters
  private static final class RoutesPreferredInterceptor implements ClientInterceptor {
    private final String apiKey;
    private static final Logger logger =
        Logger.getLogger(RoutesPreferredInterceptor.class.getName());
    private static Metadata.Key<String> API_KEY_HEADER =
        Metadata.Key.of("x-goog-api-key", Metadata.ASCII_STRING_MARSHALLER);
    private static Metadata.Key<String> FIELD_MASK_HEADER =
        Metadata.Key.of("x-goog-fieldmask", Metadata.ASCII_STRING_MARSHALLER);

    public RoutesPreferredInterceptor(String apiKey) {
      this.apiKey = apiKey;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      logger.info("Intercepted " + method.getFullMethodName());
      ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
      call =
          new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
              headers.put(API_KEY_HEADER, apiKey);
              // Note that setting the field mask to * is OK for testing, but discouraged in
              // production. For example, for ComputeCustomRoutes, set the field mask to
              // "routes.route.duration,routes.route.distanceMeters,routes.route.polyline"
              // in order to get the route distances, durations, and encoded polylines.
              headers.put(FIELD_MASK_HEADER, "*");
              super.start(responseListener, headers);
            }
          };
      return call;
    }
  }

  private static final Logger logger =
      Logger.getLogger(RoutesPreferredCustomRoutesClient.class.getName());
  private final RoutesAlphaGrpc.RoutesAlphaBlockingStub blockingStub;

  public RoutesPreferredCustomRoutesClient(Channel channel) {
    blockingStub = RoutesAlphaGrpc.newBlockingStub(channel);
  }

  public static Waypoint createWaypointForLatLng(double lat, double lng) {
    return Waypoint.newBuilder()
        .setLocation(
            Location.newBuilder().setLatLng(LatLng.newBuilder().setLatitude(lat).setLongitude(lng)))
        .build();
  }

  public void computeCustomRoutes() {
    ComputeCustomRoutesRequest request =
        ComputeCustomRoutesRequest.newBuilder()
            .setOrigin(createWaypointForLatLng(37.420761, -122.081356))
            .setDestination(createWaypointForLatLng(37.420999, -122.086894))
            .setRouteObjective(
                RouteObjective.newBuilder()
                    .setRateCard(
                        RateCard.newBuilder()
                            .setCostPerMinute(MonetaryCost.newBuilder().setValue(1.1))))
            .setTravelMode(RouteTravelMode.DRIVE)
            .setRoutingPreference(RoutingPreference.TRAFFIC_AWARE)
            .setUnits(Units.METRIC)
            .setLanguageCode("en-us")
            .setRouteModifiers(
                RouteModifiers.newBuilder()
                    .setAvoidTolls(false)
                    .setAvoidHighways(true)
                    .setAvoidFerries(true))
            .setPolylineQuality(PolylineQuality.OVERVIEW)
            .build();
    ComputeCustomRoutesResponse response;
    try {
      logger.info("About to send request: " + request.toString());
      response =
          blockingStub.withDeadlineAfter(2000, TimeUnit.MILLISECONDS).computeCustomRoutes(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Response: " + response.toString());
  }

  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("GOOGLE_MAPS_API_KEY");

    // The standard TLS port is 443
    Channel channel = NettyChannelBuilder.forAddress("routespreferred.googleapis.com", 443).build();
    channel = ClientInterceptors.intercept(channel, new RoutesPreferredInterceptor(apiKey));

    RoutesPreferredCustomRoutesClient client = new RoutesPreferredCustomRoutesClient(channel);
    client.computeCustomRoutes();
  }
}
// [END maps_routespreferred_samples_custom_routes_default]
