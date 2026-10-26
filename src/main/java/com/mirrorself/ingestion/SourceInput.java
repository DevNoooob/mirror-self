package com.mirrorself.ingestion;

import java.time.Instant;

public record SourceInput(String sourceSystem, String locator, String title, String role,
                          String rawJson, String text, Instant occurredAt) { }
