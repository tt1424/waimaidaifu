package com.daifu.manage.stats.vo;

import java.time.LocalDate;

public record ReportGenerateVO(
        LocalDate statDate,
        int userCount
) {
}
