package com.mirrorself.model;

import com.mirrorself.config.ExtractionProperties;
import com.openai.errors.OpenAIServiceException;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

@Component
public class SpringAiModelGateway implements ModelGateway {
    private static final Logger log = LoggerFactory.getLogger(SpringAiModelGateway.class);
    // json_object providers require an explicit JSON instruction, including for validation calls.
    private static final String JSON_INSTRUCTION = "\nReturn only a JSON object.";
    private final ThreadLocal<Integer> responseStatus = new ThreadLocal<>();
    private final ExtractionProperties properties;
    private final ModelAuthorization authorization;
    private volatile OpenAiChatModel model;
    public SpringAiModelGateway(ExtractionProperties properties, ModelAuthorization authorization) {
        this.properties = properties; this.authorization = authorization;
    }
    @Override public String generate(UUID owner, UUID grantId, String instructions,
            String input, List<SourceView> sources) {
        authorization.check(owner, grantId, sources);
        long startedAt = System.nanoTime();
        responseStatus.remove();
        try {
            var response = client().call(new Prompt(List.of(new SystemMessage(instructions + JSON_INSTRUCTION), new UserMessage(input))));
            if (response.getResult().getOutput().hasToolCalls())
                throw new ModelFailure(ModelFailure.Code.INVALID_MODEL_OUTPUT);
            String result = response.getResult().getOutput().getText();
            if (result == null || result.length() > 100000) throw new ModelFailure(ModelFailure.Code.INVALID_MODEL_OUTPUT);
            log.debug("Model call completed: elapsedMs={}", elapsedMillis(startedAt));
            return result;
        } catch (ModelFailure ex) {
            diagnose(ex, startedAt);
            throw ex;
        } catch (RuntimeException ex) {
            diagnose(ex, startedAt);
            throw new ModelFailure(ModelFailure.Code.MODEL_UNAVAILABLE);
        } finally {
            responseStatus.remove();
        }
    }

    /** Strict allowlist: never pass a Throwable, message, headers, request or response to the logger. */
    private void diagnose(RuntimeException exception, long startedAt) {
        List<String> causeTypes = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(exception);
        Integer status = responseStatus.get();
        Throwable current = exception;
        for (int depth = 0; current != null && depth < 16; depth++) {
            if (current instanceof OpenAIServiceException service) status = service.statusCode();
            else if (current instanceof RestClientResponseException response) status = response.getStatusCode().value();
            Throwable cause = current.getCause();
            if (cause == null || !seen.add(cause)) break;
            causeTypes.add(cause.getClass().getName());
            current = cause;
        }
        log.warn("Model call failed: exceptionType={}, causeTypes={}, httpStatus={}, elapsedMs={}",
                exception.getClass().getName(), causeTypes,
                status == null ? "unknown" : status, elapsedMillis(startedAt));
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
    private synchronized OpenAiChatModel client() {
        if (model == null) {
            if (!properties.local() && (properties.apiKey() == null || properties.apiKey().isBlank()))
                throw new ModelFailure(ModelFailure.Code.MODEL_UNAVAILABLE);
            model = OpenAiChatModel.builder().httpClientBuilderCustomizer(builder -> builder.interceptor(chain -> {
                        var response = chain.proceed(chain.request());
                        responseStatus.set(response.code());
                        return response;
                    })).options(OpenAiChatOptions.builder()
                    .baseUrl(properties.baseUrl())
                    .apiKey(properties.local() && properties.apiKey().isBlank() ? "local" : properties.apiKey())
                    .model(properties.model()).timeout(properties.timeout()).maxRetries(0)
                    .store(false).toolChoice("none")
                    .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                            .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT).build())
                    .maxCompletionTokens(4096).build()).build();
        }
        return model;
    }
}
