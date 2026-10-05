package com.example.demo.service.impl;

import com.example.demo.dto.PersonMemoryWithPerson;
import com.example.demo.dto.RelationshipReminder;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.mapper.PersonMemoryMapper;
import com.example.demo.pojo.Person;
import com.example.demo.service.RelationshipReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RelationshipReminderServiceImpl implements RelationshipReminderService {

    private static final int STALE_DAYS = 30;
    private static final int MAX_REMINDERS = 10;
    private static final Pattern FULL_DATE_PATTERN =
            Pattern.compile("(20\\d{2})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*[日号]?");

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private PersonMemoryMapper personMemoryMapper;

    @Override
    public List<RelationshipReminder> list(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusDays(STALE_DAYS);
        List<RelationshipReminder> reminders = new ArrayList<>();

        for (Person person : personMapper.findByUserId(userId)) {
            LocalDateTime latestMemoryTime =
                    personMemoryMapper.findLatestUpdatedAtByPersonId(userId, person.getId());
            LocalDateTime latestUpdate = latest(person.getUpdatedAt(), latestMemoryTime);

            if (latestUpdate != null && latestUpdate.isBefore(staleBefore)) {
                reminders.add(new RelationshipReminder(
                        person.getId(),
                        person.getName(),
                        "STALE",
                        "关系资料需要更新",
                        "已经超过 " + STALE_DAYS
                                + " 天没有更新这个人的资料，可以补充最近的近况或联系提醒。",
                        latestUpdate
                ));
            }
        }

        for (PersonMemoryWithPerson memory : personMemoryMapper.findDateMemoriesByUserId(userId)) {
            if (memory.getContent() == null || memory.getContent().trim().isEmpty()) {
                continue;
            }

            RelationshipReminder reminder = buildDateReminder(memory, now.toLocalDate());
            reminders.add(reminder);
        }

        reminders.sort(Comparator
                .comparing(RelationshipReminder::getReminderType)
                .thenComparing(reminder -> reminder.getDaysUntil() == null
                                ? Integer.MAX_VALUE
                                : reminder.getDaysUntil(),
                        Comparator.naturalOrder())
                .thenComparing(RelationshipReminder::getReferenceTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        return reminders.stream().limit(MAX_REMINDERS).toList();
    }

    private RelationshipReminder buildDateReminder(PersonMemoryWithPerson memory, LocalDate today) {
        String content = memory.getContent().trim();
        LocalDateTime referenceTime =
                memory.getUpdatedAt() == null ? memory.getCreatedAt() : memory.getUpdatedAt();
        DateInfo dateInfo = parseDate(content, today);

        RelationshipReminder reminder = new RelationshipReminder(
                memory.getPersonId(),
                memory.getPersonName(),
                "DATE",
                "重要日期记录",
                content,
                referenceTime
        );
        reminder.setSource(memory.getSource());

        if (dateInfo == null) {
            reminder.setTitle("重要日期待确认");
            reminder.setDetail(content + "，建议补充明确的月日或年月日。");
            reminder.setDateStatus("UNPARSED");
            return reminder;
        }

        int daysUntil = (int) (dateInfo.date().toEpochDay() - today.toEpochDay());
        reminder.setEventDate(dateInfo.date().toString());
        reminder.setDaysUntil(daysUntil);

        if (daysUntil == 0) {
            reminder.setTitle("今天是重要日期");
            reminder.setDetail(content + "，今天需要关注。");
            reminder.setDateStatus("TODAY");
        } else if (daysUntil > 0) {
            reminder.setTitle("重要日期 · 还有 " + daysUntil + " 天");
            reminder.setDetail(content + "，距离该日期还有 " + daysUntil + " 天。");
            reminder.setDateStatus("UPCOMING");
        } else {
            reminder.setTitle("重要日期已过去");
            reminder.setDetail(content + "，该日期已经过去 " + Math.abs(daysUntil) + " 天。");
            reminder.setDateStatus("PASSED");
        }

        return reminder;
    }

    private DateInfo parseDate(String content, LocalDate today) {
        Matcher fullMatcher = FULL_DATE_PATTERN.matcher(content);
        if (fullMatcher.find()) {
            LocalDate date = safeDate(
                    Integer.parseInt(fullMatcher.group(1)),
                    Integer.parseInt(fullMatcher.group(2)),
                    Integer.parseInt(fullMatcher.group(3))
            );

            if (date == null) {
                return null;
            }

            if (isRecurring(content)) {
                date = nextAnnualDate(date.getMonthValue(), date.getDayOfMonth(), today);
                if (date == null) {
                    return null;
                }
            }
            return new DateInfo(date);
        }

        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(content);
        if (!monthDayMatcher.find()) {
            return null;
        }

        int month = Integer.parseInt(monthDayMatcher.group(1));
        int day = Integer.parseInt(monthDayMatcher.group(2));
        LocalDate date = nextAnnualDate(month, day, today);
        return date == null ? null : new DateInfo(date);
    }

    private LocalDate nextAnnualDate(int month, int day, LocalDate today) {
        LocalDate currentYear = safeDate(today.getYear(), month, day);
        if (currentYear == null) {
            return null;
        }

        if (currentYear.isBefore(today)) {
            return safeDate(today.getYear() + 1, month, day);
        }
        return currentYear;
    }

    private LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private boolean isRecurring(String content) {
        return content.contains("生日")
                || content.contains("纪念日")
                || content.contains("周年")
                || content.contains("每年");
    }

    private record DateInfo(LocalDate date) {
    }

    private LocalDateTime latest(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }
}
