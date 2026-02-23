# Questing Plugin - Quest Journal UI

## Overview

The quest journal is the primary UI for players to track quest progress. It displays:
- Active quests with current step progress
- Completed quests
- Quest details and requirements
- Step-by-step journal entries

---

## Journal Entry Model

```java
package org.runetale.questing.journal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class JournalEntry {
    private final String stepId;
    private final String text;
    private final JournalEntryStatus status;
    private final int currentProgress;
    private final int requiredProgress;

    public JournalEntry(
            @Nonnull String stepId,
            @Nonnull String text,
            @Nonnull JournalEntryStatus status,
            int currentProgress,
            int requiredProgress) {
        this.stepId = stepId;
        this.text = text;
        this.status = status;
        this.currentProgress = currentProgress;
        this.requiredProgress = requiredProgress;
    }

    @Nonnull
    public static JournalEntry complete(@Nonnull String stepId, @Nonnull String text) {
        return new JournalEntry(stepId, text, JournalEntryStatus.COMPLETE, 1, 1);
    }

    @Nonnull
    public static JournalEntry incomplete(@Nonnull String stepId, @Nonnull String text) {
        return new JournalEntry(stepId, text, JournalEntryStatus.INCOMPLETE, 0, 1);
    }

    @Nonnull
    public static JournalEntry inProgress(@Nonnull String stepId, @Nonnull String text, int current, int required) {
        return new JournalEntry(stepId, text, JournalEntryStatus.IN_PROGRESS, current, required);
    }

    @Nonnull public String stepId() { return stepId; }
    @Nonnull public String text() { return text; }
    @Nonnull public JournalEntryStatus status() { return status; }
    public int currentProgress() { return currentProgress; }
    public int requiredProgress() { return requiredProgress; }

    public boolean isComplete() {
        return status == JournalEntryStatus.COMPLETE;
    }

    @Nonnull
    public String formatProgress() {
        if (requiredProgress <= 1) {
            return "";
        }
        return String.format("(%d/%d)", currentProgress, requiredProgress);
    }
}
```

---

## JournalEntryStatus Enum

```java
package org.runetale.questing.journal;

public enum JournalEntryStatus {
    COMPLETE,       // [x] Fully completed
    IN_PROGRESS,    // [~] Partially complete (for kill/collect)
    INCOMPLETE      // [ ] Not started or no progress
}
```

---

## QuestJournalEntry (Per-Quest Display)

```java
package org.runetale.questing.journal;

import org.runetale.questing.domain.QuestDefinition;
import org.runetale.questing.domain.QuestStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public final class QuestJournalEntry {
    private final QuestDefinition quest;
    private final QuestStatus status;
    private final List<JournalEntry> stepEntries;
    private final String completionJournal;
    private final Instant startedAt;
    private final Instant completedAt;

    public QuestJournalEntry(
            @Nonnull QuestDefinition quest,
            @Nonnull QuestStatus status,
            @Nonnull List<JournalEntry> stepEntries,
            @Nullable String completionJournal,
            @Nullable Instant startedAt,
            @Nullable Instant completedAt) {
        this.quest = quest;
        this.status = status;
        this.stepEntries = List.copyOf(stepEntries);
        this.completionJournal = completionJournal;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    @Nonnull public QuestDefinition quest() { return quest; }
    @Nonnull public QuestStatus status() { return status; }
    @Nonnull public List<JournalEntry> stepEntries() { return stepEntries; }
    @Nullable public String completionJournal() { return completionJournal; }
    @Nullable public Instant startedAt() { return startedAt; }
    @Nullable public Instant completedAt() { return completedAt; }

    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }

    public int completedSteps() {
        return (int) stepEntries.stream().filter(JournalEntry::isComplete).count();
    }

    public int totalSteps() {
        return stepEntries.size();
    }
}
```

---

## JournalService

```java
package org.runetale.questing.journal;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.domain.*;
import org.runetale.questing.service.QuestProgressService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class JournalService {

    private final QuestRegistry questRegistry;
    private final QuestProgressService progressService;

    public JournalService(
            @Nonnull QuestRegistry questRegistry,
            @Nonnull QuestProgressService progressService) {
        this.questRegistry = questRegistry;
        this.progressService = progressService;
    }

    @Nonnull
    public List<QuestJournalEntry> getActiveQuests(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        List<QuestJournalEntry> entries = new ArrayList<>();

        for (QuestProgress progress : progressService.getActiveQuests(accessor, playerRef)) {
            QuestDefinition quest = questRegistry.getQuest(progress.questId());
            if (quest != null) {
                entries.add(buildJournalEntry(quest, progress, accessor, playerRef));
            }
        }

        return entries;
    }

    @Nonnull
    public List<QuestJournalEntry> getCompletedQuests(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        List<QuestJournalEntry> entries = new ArrayList<>();

        for (String questId : progressService.getCompletedQuestIds(accessor, playerRef)) {
            QuestDefinition quest = questRegistry.getQuest(questId);
            QuestProgress progress = progressService.getQuestProgress(accessor, playerRef, questId).orElse(null);

            if (quest != null && progress != null) {
                entries.add(buildJournalEntry(quest, progress, accessor, playerRef));
            }
        }

        return entries;
    }

    @Nonnull
    public QuestJournalEntry getQuestJournal(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId) {

        QuestDefinition quest = questRegistry.getQuest(questId);
        if (quest == null) {
            throw new IllegalArgumentException("Unknown quest: " + questId);
        }

        QuestProgress progress = progressService.getQuestProgress(accessor, playerRef, questId)
            .orElse(QuestProgress.notStarted(questId));

        return buildJournalEntry(quest, progress, accessor, playerRef);
    }

    @Nonnull
    private QuestJournalEntry buildJournalEntry(
            @Nonnull QuestDefinition quest,
            @Nonnull QuestProgress progress,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        List<JournalEntry> stepEntries = new ArrayList<>();
        String currentStepId = progress.currentStepId();
        boolean foundCurrentStep = false;

        for (QuestStep step : quest.steps()) {
            JournalEntry entry = buildStepEntry(step, progress, accessor, playerRef, currentStepId, foundCurrentStep);
            stepEntries.add(entry);

            if (step.id().equals(currentStepId)) {
                foundCurrentStep = true;
            }
        }

        return new QuestJournalEntry(
            quest,
            progress.status(),
            stepEntries,
            quest.completionJournal(),
            progress.startedAt(),
            progress.completedAt()
        );
    }

    @Nonnull
    private JournalEntry buildStepEntry(
            @Nonnull QuestStep step,
            @Nonnull QuestProgress progress,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nullable String currentStepId,
            boolean pastCurrentStep) {

        if (progress.status() == QuestStatus.COMPLETED || pastCurrentStep) {
            return JournalEntry.complete(step.id(), step.journalText());
        }

        if (progress.status() == QuestStatus.NOT_STARTED) {
            return JournalEntry.incomplete(step.id(), step.journalText());
        }

        boolean isCurrentStep = step.id().equals(currentStepId);
        if (!isCurrentStep) {
            return JournalEntry.incomplete(step.id(), step.journalText());
        }

        return buildCurrentStepEntry(step, progress, accessor, playerRef);
    }

    @Nonnull
    private JournalEntry buildCurrentStepEntry(
            @Nonnull QuestStep step,
            @Nonnull QuestProgress progress,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        int stepProgress = progress.getStepProgress(step.id());

        return switch (step) {
            case CollectStep collect -> {
                int completed = countCompletedItems(collect, accessor, playerRef);
                yield JournalEntry.inProgress(step.id(), step.journalText(), completed, collect.items().size());
            }
            case KillStep kill -> {
                int required = kill.quantity();
                yield stepProgress >= required
                    ? JournalEntry.complete(step.id(), step.journalText())
                    : JournalEntry.inProgress(step.id(), step.journalText(), stepProgress, required);
            }
            case SkillActionStep skill -> {
                int required = skill.quantity();
                yield stepProgress >= required
                    ? JournalEntry.complete(step.id(), step.journalText())
                    : JournalEntry.inProgress(step.id(), step.journalText(), stepProgress, required);
            }
            default -> JournalEntry.inProgress(step.id(), step.journalText(), 0, 1);
        };
    }

    private int countCompletedItems(
            @Nonnull CollectStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {
        int completed = 0;
        for (ItemRequirement req : step.items()) {
            int has = itemIntegration.getItemCount(accessor, playerRef, req.itemId());
            if (has >= req.quantity()) {
                completed++;
            }
        }
        return completed;
    }
}
```

---

## QuestJournalPage (UI)

```java
package org.runetale.questing.page;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.domain.QuestStatus;
import org.runetale.questing.journal.*;

import javax.annotation.Nonnull;
import java.util.List;

public final class QuestJournalPage {

    private final JournalService journalService;
    private JournalView currentView = JournalView.ACTIVE_QUESTS;
    private int selectedQuestIndex = -1;

    public QuestJournalPage(@Nonnull JournalService journalService) {
        this.journalService = journalService;
    }

    @Nonnull
    public String render(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        StringBuilder sb = new StringBuilder();

        renderHeader(sb);
        renderNavigation(sb);
        renderContent(sb, accessor, playerRef);

        return sb.toString();
    }

    private void renderHeader(@Nonnull StringBuilder sb) {
        sb.append("=== Quest Journal ===\n\n");
    }

    private void renderNavigation(@Nonnull StringBuilder sb) {
        sb.append("[Active] [Completed] [Available]\n\n");
    }

    private void renderContent(
            @Nonnull StringBuilder sb,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        switch (currentView) {
            case ACTIVE_QUESTS -> renderActiveQuests(sb, accessor, playerRef);
            case COMPLETED_QUESTS -> renderCompletedQuests(sb, accessor, playerRef);
            case QUEST_DETAIL -> renderQuestDetail(sb, accessor, playerRef);
        }
    }

    private void renderActiveQuests(
            @Nonnull StringBuilder sb,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        List<QuestJournalEntry> entries = journalService.getActiveQuests(accessor, playerRef);

        if (entries.isEmpty()) {
            sb.append("You have no active quests.\n");
            return;
        }

        int index = 1;
        for (QuestJournalEntry entry : entries) {
            String progress = String.format("(%d/%d)", entry.completedSteps(), entry.totalSteps());
            sb.append(String.format("%d. %s %s\n", index++, entry.quest().name(), progress));
        }
    }

    private void renderCompletedQuests(
            @Nonnull StringBuilder sb,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        List<QuestJournalEntry> entries = journalService.getCompletedQuests(accessor, playerRef);

        if (entries.isEmpty()) {
            sb.append("You have not completed any quests.\n");
            return;
        }

        int index = 1;
        for (QuestJournalEntry entry : entries) {
            sb.append(String.format("%d. %s (Complete)\n", index++, entry.quest().name()));
        }
    }

    private void renderQuestDetail(
            @Nonnull StringBuilder sb,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        List<QuestJournalEntry> activeQuests = journalService.getActiveQuests(accessor, playerRef);

        if (selectedQuestIndex < 0 || selectedQuestIndex >= activeQuests.size()) {
            sb.append("No quest selected.\n");
            return;
        }

        QuestJournalEntry entry = activeQuests.get(selectedQuestIndex);

        sb.append("=== ").append(entry.quest().name()).append(" ===\n\n");

        if (entry.status() == QuestStatus.COMPLETED) {
            sb.append("[COMPLETE]\n\n");
            sb.append(entry.completionJournal()).append("\n");
            return;
        }

        for (JournalEntry stepEntry : entry.stepEntries()) {
            renderStepEntry(sb, stepEntry);
        }
    }

    private void renderStepEntry(@Nonnull StringBuilder sb, @Nonnull JournalEntry entry) {
        String checkbox = switch (entry.status()) {
            case COMPLETE -> "[x]";
            case IN_PROGRESS -> "[~]";
            case INCOMPLETE -> "[ ]";
        };

        String progress = entry.formatProgress();
        String text = entry.text();

        sb.append(String.format("%s %s %s\n", checkbox, text, progress));
    }

    public void selectQuest(int index) {
        this.selectedQuestIndex = index;
        this.currentView = JournalView.QUEST_DETAIL;
    }

    public void setView(@Nonnull JournalView view) {
        this.currentView = view;
        this.selectedQuestIndex = -1;
    }

    public void goBack() {
        if (currentView == JournalView.QUEST_DETAIL) {
            currentView = JournalView.ACTIVE_QUESTS;
            selectedQuestIndex = -1;
        }
    }
}
```

---

## JournalView Enum

```java
package org.runetale.questing.journal;

public enum JournalView {
    ACTIVE_QUESTS,
    COMPLETED_QUESTS,
    QUEST_DETAIL
}
```

---

## Quest Journal Command

```java
package org.runetale.questing.command;

import com.hypixel.hytale.command.Command;
import com.hypixel.hytale.command.CommandContext;
import org.runetale.questing.page.QuestJournalPage;

import javax.annotation.Nonnull;

public final class QuestCommand implements Command {

    private final QuestJournalPage journalPage;

    public QuestCommand(@Nonnull QuestJournalPage journalPage) {
        this.journalPage = journalPage;
    }

    @Override
    @Nonnull
    public String getName() {
        return "quest";
    }

    @Override
    @Nonnull
    public String[] getAliases() {
        return new String[]{"quests", "journal", "q"};
    }

    @Override
    public void execute(@Nonnull CommandContext ctx) {
        String[] args = ctx.getArguments();

        if (args.length == 0) {
            showJournal(ctx);
        } else {
            handleSubcommand(ctx, args);
        }
    }

    private void showJournal(@Nonnull CommandContext ctx) {
        String content = journalPage.render(ctx.getAccessor(), ctx.getPlayerRef());
        ctx.sendPage(content);
    }

    private void handleSubcommand(@Nonnull CommandContext ctx, @Nonnull String[] args) {
        String subcommand = args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "active" -> {
                journalPage.setView(JournalView.ACTIVE_QUESTS);
                showJournal(ctx);
            }
            case "completed", "done" -> {
                journalPage.setView(JournalView.COMPLETED_QUESTS);
                showJournal(ctx);
            }
            case "view" -> {
                if (args.length > 1) {
                    try {
                        int index = Integer.parseInt(args[1]) - 1;
                        journalPage.selectQuest(index);
                        showJournal(ctx);
                    } catch (NumberFormatException e) {
                        ctx.sendError("Invalid quest number.");
                    }
                }
            }
            case "start" -> {
                if (args.length > 1) {
                    handleStart(ctx, args[1]);
                } else {
                    ctx.sendError("Usage: /quest start <quest_id>");
                }
            }
            case "info" -> {
                if (args.length > 1) {
                    showQuestInfo(ctx, args[1]);
                }
            }
            default -> ctx.sendError("Unknown subcommand. Use: active, completed, view, start, info");
        }
    }

    private void handleStart(@Nonnull CommandContext ctx, @Nonnull String questId) {
        StartQuestResult result = questApi.startQuest(ctx.getAccessor(), ctx.getPlayerRef(), questId);

        if (result.success()) {
            ctx.sendSuccess("Started quest: " + questId);
        } else {
            ctx.sendError("Cannot start quest: " + result.failureReason());
        }
    }

    private void showQuestInfo(@Nonnull CommandContext ctx, @Nonnull String questId) {
        QuestDefinition quest = questApi.getQuestDefinition(questId);

        if (quest == null) {
            ctx.sendError("Unknown quest: " + questId);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(quest.name()).append(" ===\n\n");
        sb.append(quest.description()).append("\n\n");
        sb.append("Difficulty: ").append(quest.difficulty()).append("\n");
        sb.append("Length: ").append(quest.length()).append("\n");
        sb.append("Quest Points: ").append(quest.totalQuestPoints()).append("\n");

        if (!quest.prerequisites().isEmpty()) {
            sb.append("\nRequirements:\n");
            PrerequisiteCheckResult check = prerequisiteService.check(
                ctx.getAccessor(), ctx.getPlayerRef(), quest.prerequisites(), completedQuestsProvider);

            for (String req : check.missingRequirements()) {
                sb.append("  - ").append(req).append("\n");
            }

            if (check.satisfied()) {
                sb.append("  All requirements met!\n");
            }
        }

        ctx.sendPage(sb.toString());
    }
}
```

---

## Example Output

### Active Quests List

```
=== Quest Journal ===

[Active] [Completed] [Available]

1. Cook's Assistant (1/3)
2. Demon Slayer (2/4)
3. Goblin Diplomacy (0/3)

Use /quest view <number> for details.
```

### Quest Detail View

```
=== Cook's Assistant ===

[x] I spoke to the Lumbridge Cook. He needs help gathering ingredients for a cake.
[~] I need to gather an egg, a bucket of milk, and a pot of flour for the Cook. (2/3)
[ ] I should return to the Cook with the ingredients.
```

### Completed Quest

```
=== Cook's Assistant ===

[COMPLETE]

I helped the Cook gather ingredients for his cake.

Rewards received:
- 1 Quest Point
- 300 Cooking XP
- 50 coins
- 20 sardine
```

### Quest Info View

```
=== Demon Slayer ===

A mighty demon is being summoned in Varrock. Only the ancient sword Silverlight can stop him.

Difficulty: INTERMEDIATE
Length: MEDIUM
Quest Points: 3

Requirements:
  - Attack level 10 (have 5)
  - Quest: Cook's Assistant (not started)
```

---

## Test Cases

```java
class JournalServiceTest {

    JournalService service;
    QuestRegistry registry;
    QuestProgressService progressService;

    @Test
    void buildsCorrectJournalEntry() {
        QuestDefinition quest = createTestQuest();
        when(registry.getQuest("test_quest")).thenReturn(quest);

        QuestProgress progress = QuestProgress.notStarted("test_quest")
            .start()
            .withCurrentStep("step_1")
            .withStepProgress("step_1", 0);

        when(progressService.getActiveQuests(accessor, playerRef))
            .thenReturn(List.of(progress));

        List<QuestJournalEntry> entries = service.getActiveQuests(accessor, playerRef);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).quest().id()).isEqualTo("test_quest");
        assertThat(entries.get(0).status()).isEqualTo(QuestStatus.IN_PROGRESS);
    }

    @Test
    void marksCompletedStepsAsComplete() {
        QuestDefinition quest = createTestQuestWithSteps("step_1", "step_2", "step_3");
        when(registry.getQuest("test_quest")).thenReturn(quest);

        QuestProgress progress = QuestProgress.notStarted("test_quest")
            .start()
            .withCurrentStep("step_2");

        when(progressService.getActiveQuests(accessor, playerRef))
            .thenReturn(List.of(progress));

        QuestJournalEntry entry = service.getQuestJournal(accessor, playerRef, "test_quest");

        assertThat(entry.stepEntries().get(0).status()).isEqualTo(JournalEntryStatus.COMPLETE);
        assertThat(entry.stepEntries().get(1).status()).isEqualTo(JournalEntryStatus.IN_PROGRESS);
        assertThat(entry.stepEntries().get(2).status()).isEqualTo(JournalEntryStatus.INCOMPLETE);
    }
}

class QuestJournalPageTest {

    @Test
    void rendersActiveQuestsWithProgress() {
        JournalService service = mock(JournalService.class);
        QuestJournalPage page = new QuestJournalPage(service);

        QuestJournalEntry entry = createEntry("Test Quest", 2, 5);
        when(service.getActiveQuests(any(), any())).thenReturn(List.of(entry));

        String output = page.render(accessor, playerRef);

        assertThat(output).contains("Test Quest (2/5)");
    }

    @Test
    void showsCheckboxForCompletedSteps() {
        JournalEntry complete = JournalEntry.complete("step_1", "Done!");
        JournalEntry incomplete = JournalEntry.incomplete("step_2", "Not done");
        JournalEntry inProgress = JournalEntry.inProgress("step_3", "Working", 2, 5);

        QuestJournalPage page = new QuestJournalPage(mock(JournalService.class));
        
        StringBuilder sb = new StringBuilder();
        page.renderStepEntry(sb, complete);
        page.renderStepEntry(sb, incomplete);
        page.renderStepEntry(sb, inProgress);

        assertThat(sb.toString()).contains("[x]");
        assertThat(sb.toString()).contains("[ ]");
        assertThat(sb.toString()).contains("[~]");
        assertThat(sb.toString()).contains("(2/5)");
    }
}
```
