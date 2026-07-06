package com.example.cinema.service.impl;

import com.example.cinema.dto.ChatMessageDto;
import com.example.cinema.entity.Cinema;
import com.example.cinema.entity.CinemaItem;
import com.example.cinema.entity.Movie;
import com.example.cinema.entity.Showtime;
import com.example.cinema.entity.Promotion;
import com.example.cinema.entity.Combo;     
import com.example.cinema.repository.CinemaRepository;
import com.example.cinema.repository.CinemaItemRepository;
import com.example.cinema.repository.MovieRepository;
import com.example.cinema.repository.ShowtimeRepository;
import com.example.cinema.repository.PromotionRepository; 
import com.example.cinema.repository.ComboRepository;   
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl {

    private final MovieRepository movieRepository;
    private final CinemaItemRepository cinemaItemRepository;
    private final CinemaRepository cinemaRepository;
    private final ShowtimeRepository showtimeRepository;
    private final PromotionRepository promotionRepository;
    private final ComboRepository comboRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${ai.gemini.backup-model:gemini-1.5-flash}")
    private String geminiBackupModel;

    @Value("${ai.gemini.thinking-budget:0}")
    private int thinkingBudget;

    @Value("${ai.gemini.max-output-tokens:512}")
    private int maxOutputTokens;

    @Value("${ai.gemini.temperature:0.7}") // Tăng nhẹ nhiệt độ để AI nói chuyện tự nhiên hơn
    private double temperature;

    @Value("${ai.gemini.min-request-interval-ms:3000}")
    private long minRequestIntervalMs;

    @Value("${ai.gemini.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_HISTORY_MESSAGES = 4;
    private static final int MAX_CACHE_ENTRIES = 200;
    private static final long DEFAULT_RATE_LIMIT_COOLDOWN_MS = 60_000;
    private static final long DAILY_QUOTA_COOLDOWN_MS = 24 * 60 * 60 * 1000L;

    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private final Map<String, Long> modelBlockedUntil = new ConcurrentHashMap<>();
    private final AtomicLong nextAllowedGeminiCallAt = new AtomicLong(0);

    public String getAiResponse(String userMessage, List<ChatMessageDto> history) {
        if (!hasText(userMessage)) {
            return "Bạn cần mình hỗ trợ thông tin gì ạ?";
        }

        if ("[SYSTEM_CLOSE]".equalsIgnoreCase(userMessage.trim())) {
            return "Phiên trò chuyện với Quản lý đã kết thúc. A&K AI đã trở lại, bạn cần mình giúp gì thêm không?";
        }

        if (!hasText(aiApiKey)) {
            log.warn("Gemini API key is missing. Set GEMINI_API_KEY or ai.api.key before calling AI chat.");
            return "Hệ thống AI đang bảo trì, bạn vui lòng chọn 'Gặp Quản Lý' để được hỗ trợ nhé!";
        }

        String cacheKey = buildCacheKey(userMessage, history);
        String cachedReply = getCachedReply(cacheKey);
        if (cachedReply != null) {
            return cachedReply;
        }

        if (isRequestTooSoon()) {
            return getRateLimitFallbackMessage();
        }

        try {
            if (isModelCoolingDown(geminiModel)) {
                log.warn("Gemini model {} is cooling down. Trying backup model.", geminiModel);
                return tryBackupModel(userMessage, history, cacheKey, new GeminiApiException(429, "Primary model is cooling down", null));
            }

            String reply = callGeminiApi(geminiModel, userMessage, history);
            cacheReply(cacheKey, reply);
            return reply;
        } catch (GeminiApiException e) {
            if (e.isAuthOrPermissionError()) return getApiKeyFallbackMessage();
            if (e.isModelNotFound()) return getModelFallbackMessage();
            if (e.isQuotaOrRateLimit()) {
                rememberModelCooldown(geminiModel, e);
                return tryBackupModel(userMessage, history, cacheKey, e);
            }
            if (shouldTryBackupModel(e)) return tryBackupModel(userMessage, history, cacheKey, e);
            
            return getFallbackErrorMessage();
        } catch (Exception e) {
            log.warn("Gemini chat failed unexpectedly", e);
            return getFallbackErrorMessage();
        }
    }

    private boolean shouldTryBackupModel(GeminiApiException exception) {
        return hasText(geminiBackupModel) && !geminiBackupModel.equals(geminiModel) && exception.isTransientServerError();
    }

    private String tryBackupModel(String userMessage, List<ChatMessageDto> history, String cacheKey, GeminiApiException primaryException) {
        if (!canTryBackupModel()) {
            return primaryException != null && primaryException.isQuotaOrRateLimit() ? getQuotaFallbackMessage() : getFallbackErrorMessage();
        }

        if (isModelCoolingDown(geminiBackupModel)) {
            return primaryException != null && primaryException.isQuotaOrRateLimit() ? getQuotaFallbackMessage() : getFallbackErrorMessage();
        }

        try {
            String reply = callGeminiApi(geminiBackupModel, userMessage, history);
            cacheReply(cacheKey, reply);
            return reply;
        } catch (Exception backupException) {
            return getFallbackErrorMessage();
        }
    }

    private boolean canTryBackupModel() {
        return hasText(geminiModel) && hasText(geminiBackupModel) && !geminiBackupModel.trim().equals(geminiModel.trim());
    }

    private boolean isModelCoolingDown(String modelName) {
        if (!hasText(modelName)) return false;
        String normalizedModel = modelName.trim();
        Long blockedUntil = modelBlockedUntil.get(normalizedModel);
        if (blockedUntil == null) return false;
        if (System.currentTimeMillis() >= blockedUntil) {
            modelBlockedUntil.remove(normalizedModel);
            return false;
        }
        return true;
    }

    private void rememberModelCooldown(String modelName, GeminiApiException exception) {
        if (!hasText(modelName)) return;
        long cooldownMs = exception.retryDelayMillis();
        if (exception.isDailyQuotaExceeded()) {
            cooldownMs = Math.max(cooldownMs, DAILY_QUOTA_COOLDOWN_MS);
        } else if (cooldownMs <= 0) {
            cooldownMs = DEFAULT_RATE_LIMIT_COOLDOWN_MS;
        }
        modelBlockedUntil.put(modelName.trim(), System.currentTimeMillis() + cooldownMs);
    }

    private boolean isRequestTooSoon() {
        if (minRequestIntervalMs <= 0) return false;
        long now = System.currentTimeMillis();
        long nextAllowed = nextAllowedGeminiCallAt.get();
        if (now < nextAllowed) return true;
        return !nextAllowedGeminiCallAt.compareAndSet(nextAllowed, now + minRequestIntervalMs);
    }

    private String callGeminiApi(String modelName, String userMessage, List<ChatMessageDto> history) throws Exception {
        ObjectNode rootRequestNode = objectMapper.createObjectNode();
        
        ObjectNode systemInstruction = objectMapper.createObjectNode();
        ArrayNode siParts = systemInstruction.putArray("parts");
        siParts.addObject().put("text", buildSystemPrompt());
        rootRequestNode.set("systemInstruction", systemInstruction);

        ObjectNode generationConfig = rootRequestNode.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("temperature", temperature); 

        StringBuilder contextBuilder = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            List<ChatMessageDto> validHistory = history.stream()
                .filter(msg -> hasText(msg.getContent()) && !"[SYSTEM_CLOSE]".equals(msg.getContent()))
                .collect(Collectors.toList());

            int startIdx = Math.max(0, validHistory.size() - MAX_HISTORY_MESSAGES);
            for (int i = startIdx; i < validHistory.size(); i++) {
                ChatMessageDto msg = validHistory.get(i);
                String content = msg.getContent();
                if (i == validHistory.size() - 1 && content.equals(userMessage)) continue;
                String prefix = "BOT".equals(msg.getSenderRole()) ? "AI: " : "Khách: ";
                contextBuilder.append(prefix).append(content).append("\n");
            }
        }

        String finalPrompt = (contextBuilder.length() > 0 ? "Lịch sử chat:\n" + contextBuilder.toString() + "\n" : "") + 
                             "Khách hỏi: " + userMessage;

        ArrayNode contentsArray = rootRequestNode.putArray("contents");
        ObjectNode userContent = contentsArray.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", finalPrompt);

        String requestBody = objectMapper.writeValueAsString(rootRequestNode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", aiApiKey.trim());
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        String fullUrl = GEMINI_BASE_URL + modelName.trim() + ":generateContent";
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, request, String.class);
            return extractTextFromGeminiResponse(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new GeminiApiException(e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new GeminiApiException(504, e.getMessage(), e);
        }
    }

    private String getFallbackErrorMessage() {
        return "Xin lỗi bạn, hệ thống AI đang bận. Bạn vui lòng chọn 'Gặp Quản Lý' nhé!";
    }

    private String getQuotaFallbackMessage() {
        return "Xin lỗi bạn, AI đang tạm hết lượt. Bạn chọn 'Gặp Quản Lý' để được hỗ trợ ngay nhé!";
    }

    private String getApiKeyFallbackMessage() {
        return "Lỗi kết nối AI. Bạn vui lòng chọn 'Gặp Quản Lý' để được hỗ trợ nhé!";
    }

    private String getModelFallbackMessage() {
        return "Model AI chưa khả dụng. Bạn vui lòng chọn 'Gặp Quản Lý' nhé!";
    }

    private String getRateLimitFallbackMessage() {
        return "Bạn nhắn hơi nhanh, đợi vài giây rồi thử lại giúp mình nhé!";
    }
 // ======================================================================================================
    // 🔥 PROMPT ĐÃ ĐƯỢC LÀM SẠCH, ÉP AI TRẢ LỜI CÚ PHÁP CHUẨN 100% BẰNG VÍ DỤ
    // ======================================================================================================
    private String buildSystemPrompt() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<Cinema> allCinemas = cinemaRepository.findAll();
        StringBuilder cinemaDataBuilder = new StringBuilder();
        for (Cinema cinema : allCinemas) {
            List<CinemaItem> subItems = cinemaItemRepository.findByCinemaId(cinema.getId());
            cinemaDataBuilder.append("- ").append(cinema.getName()).append("\n");
            for (CinemaItem item : subItems) {
                cinemaDataBuilder.append("  * ").append(item.getName())
                        .append(" (").append(item.getCity()).append(")\n");
            }
        }
        String cinemaBranches = cinemaDataBuilder.toString();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOf3Days = LocalDate.now().atStartOfDay().plusDays(4);
        
        List<Showtime> allFutureShowtimes = showtimeRepository.findByStartTimeAfterOrderByStartTimeAsc(now);
        List<Showtime> futureShowtimes = allFutureShowtimes.stream()
                .filter(s -> s.getStartTime().isBefore(endOf3Days))
                .collect(Collectors.toList());

        Set<Long> moviesWithShowtimes = futureShowtimes.stream()
                .map(s -> s.getMovie().getId())
                .collect(Collectors.toSet());

        StringBuilder showtimeDataBuilder = new StringBuilder();
        if (futureShowtimes.isEmpty()) {
            showtimeDataBuilder.append("Hiện tại chưa có suất chiếu nào.\n");
        } else {
            Map<String, List<Showtime>> showtimesByDate = futureShowtimes.stream()
                    .collect(Collectors.groupingBy(s -> s.getStartTime().format(dateFormatter)));

            showtimesByDate.forEach((dateStr, showtimeList) -> {
                showtimeDataBuilder.append("Ngày: ").append(dateStr).append("\n");
                Map<String, List<Showtime>> showtimesByMovie = showtimeList.stream()
                        .collect(Collectors.groupingBy(s -> s.getMovie().getTitle()));

                showtimesByMovie.forEach((movieTitle, movieShowtimes) -> {
                    String hours = movieShowtimes.stream()
                            .map(s -> s.getStartTime().format(timeFormatter) + "[" + s.getCinemaItem().getName() + "]")
                            .collect(Collectors.joining(", "));
                    showtimeDataBuilder.append(" + ").append(movieTitle).append(": ").append(hours).append("\n");
                });
            });
        }
        String allShowtimesCatalog = showtimeDataBuilder.toString();

        List<Movie> allMovies = movieRepository.findAll(); 
        
        String showingMoviesWithShowtimes = allMovies.stream()
                .filter(m -> m.getStatus() != null && (m.getStatus().equalsIgnoreCase("SHOWING") || m.getStatus().equalsIgnoreCase("NOW_SHOWING") || m.getStatus().equalsIgnoreCase("Đang chiếu")))
                .filter(m -> moviesWithShowtimes.contains(m.getId()))
                .map(m -> " + Phim(ID:" + m.getId() + "): " + m.getTitle() + " | Poster: " + m.getPosterUrl())
                .collect(Collectors.joining("\n"));

        String upcomingMovies = allMovies.stream()
                .filter(m -> m.getStatus() != null && (m.getStatus().equalsIgnoreCase("COMING_SOON") || m.getStatus().equalsIgnoreCase("UPCOMING") || m.getStatus().equalsIgnoreCase("Sắp chiếu")))
                .map(m -> " + Phim(ID:" + m.getId() + "): " + m.getTitle() + " | Poster: " + m.getPosterUrl())
                .collect(Collectors.joining("\n"));

        List<Promotion> allPromotions = promotionRepository.findAll();
        String promotionCatalog = allPromotions.stream()
                .map(p -> " + " + p.getTitle() + " - " + p.getContent())
                .collect(Collectors.joining("\n"));
        if (promotionCatalog.isEmpty()) promotionCatalog = "Hiện tại rạp chưa có khuyến mãi nào.";

        List<Combo> allCombos = comboRepository.findAll();
        String comboCatalog = allCombos.stream()
                .map(c -> " + " + c.getName() + " (" + c.getPrice() + "đ): " + c.getDescription())
                .collect(Collectors.joining("\n"));
        if (comboCatalog.isEmpty()) comboCatalog = "Chưa có thông tin bắp nước.";

        return "# VAI TRÒ\n" +
                "Bạn là Trợ lý AI thông minh của rạp phim A&K Cinema. \n\n" +
                
                "# QUY TẮC TRẢ LỜI CỐT LÕI (BẮT BUỘC):\n" +
                "1. HỎI ĐÂU ĐÁP ĐÓ: Khách hỏi gì trả lời ĐÚNG TRỌNG TÂM cái đó. Khách hỏi phim thì chỉ nói về phim, hỏi suất chiếu thì chỉ nói suất chiếu.\n" +
                "2. NGẮN GỌN & TỰ NHIÊN: Trả lời như con người thật, lịch sự nhưng súc tích.\n" +
                "3. BẮT BUỘC SỬ DỤNG THẺ $$MOVIE$$ KHI GIỚI THIỆU PHIM:\n" +
                "   - Khi khách hỏi phim đang chiếu hoặc sắp chiếu, BẠN PHẢI TRẢ LỜI BẰNG THẺ $$MOVIE$$.\n" +
                "   - Cú pháp CHUẨN XÁC 100%: $$MOVIE|id_phim|tên_phim|url_ảnh_poster$$\n" +
                "   - VÍ DỤ CÁCH TRẢ LỜI ĐÚNG: Dạ đây là phim mới nhất đang chiếu ạ: $$MOVIE|1|Lật Mặt 7|https://domain.com/poster.jpg$$\n" +
                "   - LƯU Ý QUAN TRỌNG: TUYỆT ĐỐI KHÔNG XUỐNG DÒNG HAY THÊM DẤU CÁCH VÀO GIỮA THẺ. LUÔN KẾT THÚC BẰNG $$\n" +
                "   - Tối đa 2 thẻ phim cho mỗi câu trả lời. Nếu còn nhiều phim, thêm thẻ $$SEEMORE$$ ở cuối.\n" +
                "4. THÔNG TIN CHÍNH XÁC: Chỉ dùng dữ liệu bên dưới để trả lời.\n" +
                "5. LỆNH CẤM KỸ THUẬT: KHÔNG in ra từ khóa [SYSTEM_CLOSE].\n\n" +
                
                "--- DỮ LIỆU RẠP A&K CINEMA HÔM NAY (" + LocalDate.now().format(dateFormatter) + ") ---\n" +
                "## CÁC PHIM ĐANG CÓ SUẤT CHIẾU (Dùng để in thẻ $$MOVIE$$):\n" + (showingMoviesWithShowtimes.isEmpty() ? "Hôm nay tạm hết suất chiếu." : showingMoviesWithShowtimes) + "\n" +
                "## PHIM SẮP CHIẾU:\n" + (upcomingMovies.isEmpty() ? "Chưa có phim mới sắp chiếu." : upcomingMovies) + "\n" +
                "## LỊCH CHIẾU CỤ THỂ (3 ngày tới):\n" + allShowtimesCatalog + "\n" +
                "## CÁC CỤM RẠP:\n" + cinemaBranches + "\n" +
                "## KHUYẾN MÃI:\n" + promotionCatalog + "\n" +
                "## COMBO BẮP NƯỚC:\n" + comboCatalog;
    }
    private String extractTextFromGeminiResponse(String json) throws Exception {
        if (!hasText(json)) {
            throw new GeminiApiException(0, "Gemini returned an empty response", null);
        }

        JsonNode rootNode = objectMapper.readTree(json);
        if (rootNode.has("error")) {
            JsonNode errorNode = rootNode.get("error");
            throw new GeminiApiException(errorNode.path("code").asInt(0), errorNode.toString(), null);
        }

        JsonNode textNode = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || !textNode.isTextual()) {
            throw new GeminiApiException(0, "Gemini response did not include text", null);
        }

        return textNode.asText();
    }

    private String buildCacheKey(String userMessage, List<ChatMessageDto> history) {
        StringBuilder builder = new StringBuilder(normalizeForCache(userMessage));
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessageDto msg = history.get(i);
                String content = msg.getContent();
                if (!hasText(content) || "[SYSTEM_CLOSE]".equals(content)) {
                    continue;
                }
                builder.append('|')
                        .append(msg.getSenderRole())
                        .append(':')
                        .append(normalizeForCache(content));
            }
        }
        return Integer.toHexString(builder.toString().hashCode());
    }

    private String getCachedReply(String cacheKey) {
        CachedResponse cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse == null) {
            return null;
        }

        if (System.currentTimeMillis() > cachedResponse.expiresAtMillis()) {
            responseCache.remove(cacheKey);
            return null;
        }

        return cachedResponse.reply();
    }

    private void cacheReply(String cacheKey, String reply) {
        if (cacheTtlSeconds <= 0 || !hasText(reply)) {
            return;
        }

        if (responseCache.size() >= MAX_CACHE_ENTRIES) {
            responseCache.clear();
        }

        responseCache.put(cacheKey, new CachedResponse(reply, System.currentTimeMillis() + cacheTtlSeconds * 1000));
    }

    private String normalizeForCache(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record CachedResponse(String reply, long expiresAtMillis) {
    }

    private static class GeminiApiException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        GeminiApiException(int statusCode, String responseBody, Throwable cause) {
            super(responseBody, cause);
            this.statusCode = statusCode;
            this.responseBody = responseBody == null ? "" : responseBody;
        }

        boolean isQuotaOrRateLimit() {
            String normalizedBody = responseBody.toLowerCase(Locale.ROOT);
            return statusCode == 429
                    || normalizedBody.contains("resource_exhausted")
                    || normalizedBody.contains("rate limit");
        }

        boolean isDailyQuotaExceeded() {
            String normalizedBody = responseBody.toLowerCase(Locale.ROOT);
            return normalizedBody.contains("perday") || normalizedBody.contains("per day");
        }

        long retryDelayMillis() {
            String marker = "\"retryDelay\"";
            int markerIndex = responseBody.indexOf(marker);
            if (markerIndex < 0) {
                return 0;
            }

            int valueStart = responseBody.indexOf('"', markerIndex + marker.length());
            if (valueStart < 0) {
                return 0;
            }

            int valueEnd = responseBody.indexOf('"', valueStart + 1);
            if (valueEnd < 0) {
                return 0;
            }

            String retryDelay = responseBody.substring(valueStart + 1, valueEnd).trim();
            if (!retryDelay.endsWith("s")) {
                return 0;
            }

            try {
                return Long.parseLong(retryDelay.substring(0, retryDelay.length() - 1)) * 1000;
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        boolean isAuthOrPermissionError() {
            String normalizedBody = responseBody.toLowerCase(Locale.ROOT);
            return statusCode == 401
                    || statusCode == 403
                    || normalizedBody.contains("api_key_invalid")
                    || normalizedBody.contains("permission_denied")
                    || normalizedBody.contains("api key");
        }

        boolean isModelNotFound() {
            return statusCode == 404;
        }

        boolean isTransientServerError() {
            return statusCode == 500 || statusCode == 503 || statusCode == 504;
        }
    }
}