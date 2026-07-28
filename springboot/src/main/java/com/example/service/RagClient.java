package com.example.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client for the external FastAPI RAG service.
 */
public interface RagClient {

    RagResponse query(String question, String example);

    final class RagResponse {
        private final boolean available;
        private final String answer;
        private final List<RagSource> sources;

        public RagResponse(boolean available, String answer, List<RagSource> sources) {
            this.available = available;
            this.answer = answer;
            this.sources = sources == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(sources));
        }

        public static RagResponse available(String answer, List<RagSource> sources) {
            return new RagResponse(true, answer, sources);
        }

        public static RagResponse unavailable(String reason) {
            return new RagResponse(false, reason, Collections.emptyList());
        }

        public boolean isAvailable() {
            return available;
        }

        public String getAnswer() {
            return answer;
        }

        public List<RagSource> getSources() {
            return sources;
        }
    }

    final class RagSource {
        private final String path;
        private final String content;

        public RagSource(String path, String content) {
            this.path = path;
            this.content = content;
        }

        public String getPath() {
            return path;
        }

        public String getContent() {
            return content;
        }
    }
}
