package com.daifu.manage.stats.service;

import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.stats.mapper.UserStatsMapper;
import com.daifu.manage.stats.vo.ReportGenerateVO;
import com.daifu.manage.stats.vo.StatsSummaryVO;
import com.daifu.manage.stats.vo.UserStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class StatsService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserStatsMapper userStatsMapper;

    public StatsService(UserStatsMapper userStatsMapper) {
        this.userStatsMapper = userStatsMapper;
    }

    public PageResult<UserStatsVO> userStats(Integer pageNum, Integer pageSize, String startTime, String endTime) {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        validateRange(start, end);

        long total = userStatsMapper.countUsersForStats();
        long offset = (long) (pageNum - 1) * pageSize;
        List<UserStatsVO> records = userStatsMapper.listUsersForStats(start, end, offset, pageSize);
        return PageResult.of(total, pageNum, pageSize, records);
    }

    public StatsSummaryVO summary(String startTime, String endTime) {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        validateRange(start, end);
        return userStatsMapper.summary(start, end);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportGenerateVO generateDailyReport(String statDateText) {
        LocalDate statDate = parseDate(statDateText);
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        userStatsMapper.deleteDailyByDate(statDate);
        userStatsMapper.insertDailyByDate(statDate, start, end);
        int userCount = userStatsMapper.countDailyRows(statDate);
        return new ReportGenerateVO(statDate, userCount);
    }

    private LocalDateTime parseDateTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATETIME_FMT);
        } catch (DateTimeParseException ex) {
            throw new BizException("invalid datetime format, expected yyyy-MM-dd HH:mm:ss");
        }
    }

    private LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(text, DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new BizException("invalid date format, expected yyyy-MM-dd");
        }
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BizException("endTime must be after startTime");
        }
    }
}
