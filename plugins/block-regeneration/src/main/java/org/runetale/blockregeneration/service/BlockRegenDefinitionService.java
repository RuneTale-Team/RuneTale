package org.runetale.blockregeneration.service;

import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.blockregeneration.domain.BlockRegenConfig;
import org.runetale.blockregeneration.domain.BlockRegenDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class BlockRegenDefinitionService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, BlockRegenDefinition> byExactBlockId = new ConcurrentHashMap<>();
    private final Map<String, BlockRegenDefinition> byPlaceholderBlockId = new ConcurrentHashMap<>();
    private final List<WildcardDefinitionMapping> wildcardMappings = new CopyOnWriteArrayList<>();

    public void load(@Nonnull BlockRegenConfig config) {
        this.byExactBlockId.clear();
        this.byPlaceholderBlockId.clear();
        this.wildcardMappings.clear();

        for (BlockRegenDefinition definition : config.definitions()) {
            if (!definition.enabled()) {
                continue;
            }
            register(definition);
        }
    }

    private void register(@Nonnull BlockRegenDefinition definition) {
        String normalizedPattern = normalize(definition.blockIdPattern());
        if (isWildcardPattern(normalizedPattern)) {
            registerWildcard(definition, normalizedPattern);
            return;
        }

        BlockRegenDefinition previous = this.byExactBlockId.put(normalizedPattern, definition);
        if (previous != null && previous != definition) {
            LOGGER.atWarning().log("[BlockRegen] Replaced exact mapping block=%s oldId=%s newId=%s",
                    definition.blockIdPattern(),
                    previous.id(),
                    definition.id());
        }

        registerPlaceholder(definition);
    }

    private void registerWildcard(@Nonnull BlockRegenDefinition definition, @Nonnull String normalizedPattern) {
        this.wildcardMappings.removeIf(mapping -> mapping.rawPattern().equals(normalizedPattern));
        this.wildcardMappings.add(new WildcardDefinitionMapping(
                normalizedPattern,
                createWildcardPattern(normalizedPattern),
                definition));
        registerPlaceholder(definition);
    }

    private void registerPlaceholder(@Nonnull BlockRegenDefinition definition) {
        String normalizedPlaceholder = normalize(definition.placeholderBlockId());
        BlockRegenDefinition previous = this.byPlaceholderBlockId.put(normalizedPlaceholder, definition);
        if (previous != null && previous != definition) {
            LOGGER.atWarning().log("[BlockRegen] Replaced placeholder mapping block=%s oldId=%s newId=%s",
                    definition.placeholderBlockId(),
                    previous.id(),
                    definition.id());
        }
    }

    @Nullable
    public BlockRegenDefinition findByBlockId(@Nullable String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }

        String normalized = normalize(blockId);
        BlockRegenDefinition exact = this.byExactBlockId.get(normalized);
        if (exact != null) {
            return exact;
        }

        String simplified = simplifyBlockId(normalized);
        if (!simplified.equals(normalized)) {
            exact = this.byExactBlockId.get(simplified);
            if (exact != null) {
                return exact;
            }
        }

        for (WildcardDefinitionMapping mapping : this.wildcardMappings) {
            if (matches(mapping.pattern(), normalized, simplified)) {
                return mapping.definition();
            }
        }
        return null;
    }

    @Nullable
    public BlockRegenDefinition findByPlaceholderBlockId(@Nullable String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }

        String normalized = normalize(blockId);
        BlockRegenDefinition definition = this.byPlaceholderBlockId.get(normalized);
        if (definition != null) {
            return definition;
        }

        String simplified = simplifyBlockId(normalized);
        if (!simplified.equals(normalized)) {
            return this.byPlaceholderBlockId.get(simplified);
        }
        return null;
    }

    @Nonnull
    public List<BlockRegenDefinition> listAllDefinitions() {
        LinkedHashSet<BlockRegenDefinition> deduped = new LinkedHashSet<>(this.byExactBlockId.values());
        for (WildcardDefinitionMapping mapping : this.wildcardMappings) {
            deduped.add(mapping.definition());
        }
        return new ArrayList<>(deduped);
    }

    private static boolean isWildcardPattern(@Nonnull String token) {
        return token.indexOf('*') >= 0;
    }

    private static boolean matches(@Nonnull Pattern pattern, @Nonnull String normalizedBlockId, @Nonnull String simplifiedBlockId) {
        if (pattern.matcher(normalizedBlockId).matches()) {
            return true;
        }
        return !simplifiedBlockId.equals(normalizedBlockId) && pattern.matcher(simplifiedBlockId).matches();
    }

    @Nonnull
    private static Pattern createWildcardPattern(@Nonnull String normalizedPattern) {
        StringBuilder regexBuilder = new StringBuilder(normalizedPattern.length() + 8);
        regexBuilder.append('^');
        int segmentStart = 0;
        for (int i = 0; i < normalizedPattern.length(); i++) {
            if (normalizedPattern.charAt(i) != '*') {
                continue;
            }

            if (segmentStart < i) {
                String literalSegment = normalizedPattern.substring(segmentStart, i);
                regexBuilder.append(Pattern.quote(literalSegment));
            }
            regexBuilder.append(".*");
            segmentStart = i + 1;
        }

        if (segmentStart < normalizedPattern.length()) {
            String literalSegment = normalizedPattern.substring(segmentStart);
            regexBuilder.append(Pattern.quote(literalSegment));
        }
        regexBuilder.append('$');
        return Pattern.compile(regexBuilder.toString());
    }

    @Nonnull
    private static String simplifyBlockId(@Nonnull String normalizedBlockId) {
        String simplified = normalizedBlockId;

        int namespaceSeparator = simplified.lastIndexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < simplified.length()) {
            simplified = simplified.substring(namespaceSeparator + 1);
        }

        int pathSeparator = simplified.lastIndexOf('/');
        if (pathSeparator >= 0 && pathSeparator + 1 < simplified.length()) {
            simplified = simplified.substring(pathSeparator + 1);
        }

        return simplified;
    }

    @Nonnull
    private static String normalize(@Nonnull String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }

    private record WildcardDefinitionMapping(
            @Nonnull String rawPattern,
            @Nonnull Pattern pattern,
            @Nonnull BlockRegenDefinition definition) {
    }
}
