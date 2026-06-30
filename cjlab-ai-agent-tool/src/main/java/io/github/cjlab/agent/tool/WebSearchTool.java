package io.github.cjlab.agent.tool;

import io.github.cjlab.agent.common.AgentException;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearchTool implements AgentTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 8;
    private static final int MAX_BODY_CHARS = 220_000;
    private static final int MAX_OUTPUT_CHARS = 6_000;
    private static final Pattern RESULT_LINK = Pattern.compile(
            "<a[^>]+class=\"[^\"]*result__a[^\"]*\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BING_RESULT_LINK = Pattern.compile(
            "<li[^>]+class=\"[^\"]*b_algo[^\"]*\"[^>]*>.*?<h2[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient httpClient;

    public WebSearchTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "Search public web pages or fetch a public URL. Arguments: query, url, limit.";
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        Map<String, Object> arguments = request == null || request.arguments() == null
                ? Map.of()
                : request.arguments();
        String url = stringArgument(arguments, "url");
        String query = stringArgument(arguments, "query");
        if (query == null || query.isBlank()) {
            query = request == null ? null : request.input();
        }
        if (url == null && looksLikeUrl(query)) {
            url = query;
        }
        String content = url == null || url.isBlank()
                ? search(query, intArgument(arguments, "limit", DEFAULT_LIMIT))
                : fetchUrl(url);
        return new ToolResult(name(), content);
    }

    private String search(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new AgentException("Web search query must not be blank.");
        }
        int normalizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<SearchResult> results = searchDuckDuckGo(query, normalizedLimit);
        if (results.isEmpty()) {
            results = searchBing(query, normalizedLimit);
        }
        if (results.isEmpty()) {
            return "No web search results found for: " + query.trim();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Web search results for: ").append(query.trim()).append('\n');
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(result.title())
                    .append('\n')
                    .append("   URL: ")
                    .append(result.url())
                    .append('\n');
        }
        return truncate(builder.toString(), MAX_OUTPUT_CHARS);
    }

    private List<SearchResult> searchDuckDuckGo(String query, int limit) {
        try {
            URI uri = URI.create("https://duckduckgo.com/html/?q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
            return parseDuckDuckGoSearchResults(send(uri), limit);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<SearchResult> searchBing(String query, int limit) {
        URI uri = URI.create("https://www.bing.com/search?q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        return parseBingSearchResults(send(uri), limit);
    }

    private String fetchUrl(String rawUrl) {
        URI uri = normalizeUrl(rawUrl);
        assertPublicHttpUrl(uri);
        String html = send(uri);
        String title = extractTitle(html);
        String text = extractText(html);
        StringBuilder builder = new StringBuilder();
        builder.append("Fetched web page").append('\n')
                .append("URL: ").append(uri).append('\n')
                .append("Title: ").append(title == null ? "-" : title).append('\n')
                .append("Content:").append('\n')
                .append(truncate(text, MAX_OUTPUT_CHARS));
        return builder.toString();
    }

    private String send(URI uri) {
        assertPublicHttpUrl(uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "CJLab-Agent/1.0 (+web_search)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentException("Web request failed with HTTP " + response.statusCode() + ": " + uri);
            }
            return truncate(response.body(), MAX_BODY_CHARS);
        } catch (AgentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AgentException("Web request failed: " + uri, exception);
        }
    }

    private List<SearchResult> parseDuckDuckGoSearchResults(String html, int limit) {
        List<SearchResult> results = new ArrayList<>();
        Matcher matcher = RESULT_LINK.matcher(html);
        while (matcher.find() && results.size() < limit) {
            String url = normalizeDuckDuckGoUrl(htmlDecode(stripTags(matcher.group(1))));
            String title = htmlDecode(stripTags(matcher.group(2))).trim();
            if (!url.isBlank() && !title.isBlank()) {
                results.add(new SearchResult(title, url));
            }
        }
        return results;
    }

    private List<SearchResult> parseBingSearchResults(String html, int limit) {
        List<SearchResult> results = new ArrayList<>();
        Matcher matcher = BING_RESULT_LINK.matcher(html);
        while (matcher.find() && results.size() < limit) {
            String url = htmlDecode(stripTags(matcher.group(1))).trim();
            String title = htmlDecode(stripTags(matcher.group(2))).trim();
            if (!url.isBlank() && !title.isBlank()) {
                results.add(new SearchResult(title, url));
            }
        }
        return results;
    }

    private String normalizeDuckDuckGoUrl(String url) {
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        try {
            URI uri = URI.create(url);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    int split = part.indexOf('=');
                    if (split > 0 && "uddg".equals(part.substring(0, split))) {
                        return java.net.URLDecoder.decode(part.substring(split + 1), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Keep original URL if parsing fails.
        }
        return url;
    }

    private URI normalizeUrl(String rawUrl) {
        String value = rawUrl == null ? "" : rawUrl.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new AgentException("Invalid URL: " + rawUrl, exception);
        }
    }

    private void assertPublicHttpUrl(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new AgentException("Only http and https URLs are allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new AgentException("URL host must not be blank.");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".local")) {
            throw new AgentException("Local and private network URLs are not allowed.");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new AgentException("Local and private network URLs are not allowed.");
                }
            }
        } catch (AgentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AgentException("Failed to resolve URL host: " + host, exception);
        }
    }

    private boolean looksLikeUrl(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("www.");
    }

    private String extractTitle(String html) {
        Matcher matcher = TITLE.matcher(html);
        return matcher.find() ? htmlDecode(stripTags(matcher.group(1))).trim() : null;
    }

    private String extractText(String html) {
        return htmlDecode(html)
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripTags(String value) {
        return value == null ? "" : value.replaceAll("(?is)<[^>]+>", " ");
    }

    private String htmlDecode(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");
    }

    private String stringArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        return value == null ? null : value.toString();
    }

    private int intArgument(Map<String, Object> arguments, String name, int defaultValue) {
        Object value = arguments.get(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n...[truncated]";
    }

    private record SearchResult(String title, String url) {
    }
}
