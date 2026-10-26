package com.mirrorself.model;

/** Future adapter boundary. No implementation calls a cloud model in V0.1. */
public interface ModelGateway {
    String generateAuthorized(String task, String minimalContext, String reviewedEgressGrantId);
}
