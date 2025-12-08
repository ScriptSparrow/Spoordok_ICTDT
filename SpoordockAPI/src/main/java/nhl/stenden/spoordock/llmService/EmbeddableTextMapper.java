package nhl.stenden.spoordock.llmService;

public interface EmbeddableTextMapper<T> {
    String toEmbeddableText(T object);
}
