package com.daifu.manage.stats.controller;

import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.api.Result;
import com.daifu.manage.stats.service.StatsService;
import com.daifu.manage.stats.vo.ReportGenerateVO;
import com.daifu.manage.stats.vo.StatsSummaryVO;
import com.daifu.manage.stats.vo.UserStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/users")
    public Result<PageResult<UserStatsVO>> users(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String startTime,
                                                 @RequestParam(required = false) String endTime) {
        return Result.ok(statsService.userStats(pageNum, pageSize, startTime, endTime));
    }

    @GetMapping("/summary")
    public Result<StatsSummaryVO> summary(@RequestParam(required = false) String startTime,
                                          @RequestParam(required = false) String endTime) {
        return Result.ok(statsService.summary(startTime, endTime));
    }

    @PostMapping("/report/generate")
    public Result<ReportGenerateVO> generate(@RequestParam(required = false) String statDate) {
        return Result.ok(statsService.generateDailyReport(statDate));
    }
}
