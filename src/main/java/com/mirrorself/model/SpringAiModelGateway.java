package com.mirrorself.model;

import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

@Component
public class SpringAiModelGateway implements ModelGateway {
    private final ExtractionProperties properties;
    private final ModelAuthorization authorization;
    private volatile OpenAiChatModel model;
    public SpringAiModelGateway(ExtractionProperties properties, ModelAuthorization authorization) {
        this.properties = properties; this.authorization = authorization;
    }
    @Override public String generate(UUID owner, UUID grantId, String instructions,
            String input, List<SourceView> sources) {
        authorization.check(owner, grantId, sources);
        try {
            var response = client().call(new Prompt(List.of(new SystemMessage(instructions), new UserMessage(input))));
            if (response.getResult().getOutput().hasToolCalls())
                throw new ModelFailure(ModelFailure.Code.INVALID_MODEL_OUTPUT);
            String result = response.getResult().getOutput().getText();
            if (result == null || result.length() > 100000) throw new ModelFailure(ModelFailure.Code.INVALID_MODEL_OUTPUT);
            return result;
        } catch (ModelFailure ex) { throw ex; }
        catch (RuntimeException ex) { throw new ModelFailure(ModelFailure.Code.MODEL_UNAVAILABLE); }
    }
    private synchronized OpenAiChatModel client() {
        if (model == null) {
            if (!properties.local() && (properties.apiKey() == null || properties.apiKey().isBlank()))
                throw new ModelFailure(ModelFailure.Code.MODEL_UNAVAILABLE);
            model = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
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
